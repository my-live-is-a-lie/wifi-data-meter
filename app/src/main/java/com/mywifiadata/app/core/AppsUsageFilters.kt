package com.mywifiadata.app.core

import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.AppsFilter

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
            AppsFilter.INSTALLED -> !it.isSystem
            AppsFilter.SYSTEM -> it.isSystem
        }
    }
    val sorted = when (sortOrder) {
        AppsSortOrder.HIGHEST_USAGE -> filtered.sortedByDescending { it.totals.totalBytes }
        AppsSortOrder.LOWEST_USAGE -> filtered.sortedBy { it.totals.totalBytes }
    }
    return if (limit == null) sorted else sorted.take(limit.coerceAtLeast(0))
}
