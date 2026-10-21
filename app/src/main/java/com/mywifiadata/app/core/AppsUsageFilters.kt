package com.mywifiadata.app.core

import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.AppsFilter
import com.mywifiadata.app.data.model.PlanCapacityUnit
import kotlin.math.abs

/** Kept independent of the dashboard's Wi-Fi-only measurements. */
enum class UsageNetwork { WIFI, MOBILE }
enum class AppsUsageScope { ALL_TIME, CURRENT_CYCLE }
enum class AppsSortOrder {
    HIGHEST_USAGE, LOWEST_USAGE;

    fun reversed(): AppsSortOrder = when (this) {
        HIGHEST_USAGE -> LOWEST_USAGE
        LOWEST_USAGE -> HIGHEST_USAGE
    }
}

val appsCountOptions: List<Int?> = listOf(null, 5, 10, 15, 20, 25)

/** Cycle through 1–30 inclusively instead of clamping at either end. */
fun wrapAppsUsageFilterPercent(value: Int): Int = Math.floorMod(value - 1, 30) + 1

fun appsUsageRange(
    network: UsageNetwork,
    scope: AppsUsageScope,
    cycleStartDay: Int,
    now: Long,
): TimeRange = if (network == UsageNetwork.WIFI && scope == AppsUsageScope.CURRENT_CYCLE) {
    TimeRange(TimeRanges.cycleRange(cycleStartDay, now).startMillis, now)
} else {
    // Android only returns the history it still retains, not lifetime usage.
    TimeRange(0L, now)
}

/** Clip chart selections to the selected scope; an empty intersection is not queried. */
fun TimeRange.within(bounds: TimeRange): TimeRange {
    val start = startMillis.coerceAtLeast(bounds.startMillis)
    return TimeRange(start, endMillis.coerceAtMost(bounds.endMillis).coerceAtLeast(start))
}

/** Apply the app-type filter, then sort, then limit the displayed rows. */
fun filterApps(
    apps: List<AppUsage>,
    filter: AppsFilter,
    limit: Int? = null,
    sortOrder: AppsSortOrder = AppsSortOrder.HIGHEST_USAGE,
): List<AppUsage> {
    val filtered = apps.filter {
        when (filter) {
            AppsFilter.ALL -> true
            AppsFilter.INSTALLED -> !it.isSystem && it.isInstalled
            AppsFilter.SYSTEM -> it.isSystem
        }
    }
    val sorted = when (sortOrder) {
        AppsSortOrder.HIGHEST_USAGE -> filtered.sortedByDescending { it.totals.totalBytes }
        AppsSortOrder.LOWEST_USAGE -> filtered.sortedBy { it.totals.totalBytes }
    }
    return if (limit == null) sorted else sorted.take(limit.coerceAtLeast(0))
}

/** Convert a whole-number MB/GB/TB setting to bytes without overflowing. */
fun appUsageThresholdBytes(amount: Int, unit: PlanCapacityUnit): Long {
    val safeAmount = amount.coerceAtLeast(0).toLong()
    return if (safeAmount > Long.MAX_VALUE / unit.bytesFactor) {
        Long.MAX_VALUE
    } else {
        safeAmount * unit.bytesFactor
    }
}

/** Hide apps that use at most the selected amount; summaries are intentionally unaffected. */
fun filterAppsAtOrBelowUsage(apps: List<AppUsage>, thresholdBytes: Long): List<AppUsage> =
    apps.filter { it.totals.totalBytes > thresholdBytes.coerceAtLeast(0L) }

/**
 * Hide the least-used tail whose combined usage is closest to the selected share of total usage.
 * Whole apps cannot always add up to the exact percentage, so this picks the nearest whole-app
 * boundary and never hides the last remaining app.
 */
fun filterAppsByCumulativeUsagePercent(apps: List<AppUsage>, percentage: Int): List<AppUsage> {
    if (apps.size <= 1) return apps

    val usageBytes = apps.map { it.totals.totalBytes.coerceAtLeast(0L).toDouble() }
    val totalBytes = usageBytes.sum()
    if (totalBytes <= 0.0) return apps

    val targetBytes = totalBytes * percentage.coerceIn(1, 30) / 100.0
    val ascendingIndices = apps.indices.sortedBy { usageBytes[it] }
    var hiddenCount = 0
    var hiddenBytes = 0.0

    // Keep at least the largest app visible. The comparison makes the chosen whole-app sum
    // as close as possible to the target without unexpectedly removing a dominant app.
    for (index in ascendingIndices.dropLast(1)) {
        val appBytes = usageBytes[index]
        if (appBytes == 0.0) {
            hiddenCount++
            continue
        }
        val candidateBytes = hiddenBytes + appBytes
        if (abs(targetBytes - candidateBytes) < abs(targetBytes - hiddenBytes)) {
            hiddenBytes = candidateBytes
            hiddenCount++
        } else {
            break
        }
    }

    val hiddenIndices = ascendingIndices.take(hiddenCount).toHashSet()
    return apps.filterIndexed { index, _ -> index !in hiddenIndices }
}
