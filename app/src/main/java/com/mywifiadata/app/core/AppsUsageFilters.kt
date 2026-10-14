package com.mywifiadata.app.core

import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.AppsFilter

/** Kept independent of the dashboard's Wi-Fi-only measurements. */
enum class UsageNetwork { WIFI, MOBILE }
enum class AppsUsageScope { ALL_TIME, CURRENT_CYCLE }

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

fun filterApps(apps: List<AppUsage>, filter: AppsFilter, limit: Int? = null): List<AppUsage> {
    val sorted = apps.filter {
        when (filter) {
            AppsFilter.ALL -> true
            AppsFilter.INSTALLED -> !it.isSystem
            AppsFilter.SYSTEM -> it.isSystem
        }
    }.sortedByDescending { it.totals.totalBytes }
    return if (limit == null) sorted else sorted.take(limit.coerceAtLeast(0))
}
