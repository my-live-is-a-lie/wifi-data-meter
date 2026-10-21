package com.mywifiadata.app.core

import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.AppsFilter
import com.mywifiadata.app.data.model.PlanCapacityUnit
import com.mywifiadata.app.data.model.TrafficTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AppsUsageFiltersTest {
    private fun day(day: Int, month: Int = 10): Long =
        LocalDate.of(2026, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun app(uid: Int, bytes: Long, system: Boolean = false) =
        AppUsage(uid, "app.$uid", "App $uid", system, TrafficTotals(rxBytes = bytes))

    @Test
    fun `all time includes history older than the chart month`() {
        val now = day(14)
        assertEquals(TimeRange(0L, now), appsUsageRange(UsageNetwork.WIFI, AppsUsageScope.ALL_TIME, 1, now))
    }

    @Test
    fun `current plan begins at configured cycle day and stops now`() {
        val now = day(14)
        assertEquals(TimeRange(day(5), now), appsUsageRange(UsageNetwork.WIFI, AppsUsageScope.CURRENT_CYCLE, 5, now))
    }

    @Test
    fun `current plan can begin in previous month`() {
        val now = day(14)
        assertEquals(TimeRange(day(20, 9), now), appsUsageRange(UsageNetwork.WIFI, AppsUsageScope.CURRENT_CYCLE, 20, now))
    }

    @Test
    fun `cycle boundary starts a new plan`() {
        val now = day(14)
        assertEquals(TimeRange(now, now), appsUsageRange(UsageNetwork.WIFI, AppsUsageScope.CURRENT_CYCLE, 14, now))
    }

    @Test
    fun `hidden wifi scope does not constrain mobile data`() {
        val now = day(14)
        assertEquals(TimeRange(0L, now), appsUsageRange(UsageNetwork.MOBILE, AppsUsageScope.CURRENT_CYCLE, 5, now))
    }

    @Test
    fun `chart slices are clipped to plan start and now`() {
        val bounds = TimeRange(100L, 500L)
        assertEquals(TimeRange(100L, 200L), TimeRange(50L, 200L).within(bounds))
        assertEquals(TimeRange(400L, 500L), TimeRange(400L, 600L).within(bounds))
        assertEquals(TimeRange(200L, 300L), TimeRange(200L, 300L).within(bounds))
        val before = TimeRange(0L, 50L).within(bounds)
        val after = TimeRange(600L, 700L).within(bounds)
        assertEquals(before.startMillis, before.endMillis)
        assertEquals(after.startMillis, after.endMillis)
    }

    @Test
    fun `count options are exactly all five ten fifteen twenty twenty five`() {
        assertEquals(listOf(null, 5, 10, 15, 20, 25), appsCountOptions)
    }

    @Test
    fun `each limit returns highest consumers in descending order`() {
        val apps = (1..30).map { app(it, it.toLong()) }
        appsCountOptions.forEach { limit ->
            val result = filterApps(apps, AppsFilter.ALL, limit)
            assertEquals(limit ?: 30, result.size)
            assertEquals((30 downTo (31 - (limit ?: 30))).toList(), result.map { it.uid })
        }
        assertEquals(1, apps.first().uid) // Sorting must not mutate the source.
    }

    @Test
    fun `app type filtering happens before the count limit`() {
        val apps = listOf(app(1, 500, true), app(2, 100), app(3, 300), app(4, 400, true))
        assertEquals(listOf(3, 2), filterApps(apps, AppsFilter.INSTALLED, 5).map { it.uid })
        assertEquals(listOf(1), filterApps(apps, AppsFilter.SYSTEM, 1).map { it.uid })
    }

    @Test
    fun `empty and short lists remain safe for every count option`() {
        appsCountOptions.forEach { limit ->
            assertTrue(filterApps(emptyList(), AppsFilter.ALL, limit).isEmpty())
            assertEquals(1, filterApps(listOf(app(1, 100)), AppsFilter.ALL, limit).size)
        }
    }

    @Test
    fun `lowest usage is sorted before applying each count limit`() {
        val apps = (30 downTo 1).map { app(it, it.toLong()) }
        appsCountOptions.forEach { limit ->
            val result = filterApps(apps, AppsFilter.ALL, limit, AppsSortOrder.LOWEST_USAGE)
            assertEquals((1..(limit ?: 30)).toList(), result.map { it.uid })
        }
        assertEquals(30, apps.first().uid)
    }

    @Test
    fun `lowest usage respects app type before selecting rows`() {
        val apps = listOf(app(1, 10, true), app(2, 100), app(3, 30), app(4, 40, true))
        assertEquals(listOf(3), filterApps(apps, AppsFilter.INSTALLED, 1, AppsSortOrder.LOWEST_USAGE).map { it.uid })
        assertEquals(listOf(1, 4), filterApps(apps, AppsFilter.SYSTEM, 5, AppsSortOrder.LOWEST_USAGE).map { it.uid })
    }

    @Test
    fun `lowest usage handles empty lists and ties`() {
        assertTrue(filterApps(emptyList(), AppsFilter.ALL, 5, AppsSortOrder.LOWEST_USAGE).isEmpty())
        val apps = listOf(app(1, 20), app(2, 20), app(3, 10))
        assertEquals(listOf(3, 1, 2), filterApps(apps, AppsFilter.ALL, 5, AppsSortOrder.LOWEST_USAGE).map { it.uid })
    }

    @Test
    fun `removed apps stay in all but are excluded from installed filter`() {
        val installed = app(1, 100)
        val removed = app(2, 300).copy(isInstalled = false)
        val apps = listOf(installed, removed)
        assertEquals(listOf(2, 1), filterApps(apps, AppsFilter.ALL).map { it.uid })
        assertEquals(listOf(1), filterApps(apps, AppsFilter.INSTALLED).map { it.uid })
        assertEquals(listOf(1, 2), filterApps(apps, AppsFilter.ALL, sortOrder = AppsSortOrder.LOWEST_USAGE).map { it.uid })
    }

    @Test
    fun `usage amount filter hides apps at or below the selected byte amount`() {
        val apps = listOf(app(1, 5_000_000), app(2, 5_000_001), app(3, 20_000_000))
        assertEquals(listOf(2, 3), filterAppsAtOrBelowUsage(apps, 5_000_000).map { it.uid })
        assertEquals(listOf(1, 2, 3), filterAppsAtOrBelowUsage(apps, -1).map { it.uid })
        assertEquals(5_000_000L, appUsageThresholdBytes(5, PlanCapacityUnit.MB))
        assertEquals(5_000_000_000L, appUsageThresholdBytes(5, PlanCapacityUnit.GB))
        assertEquals(5_000_000_000_000L, appUsageThresholdBytes(5, PlanCapacityUnit.TB))
    }

    @Test
    fun `percentage filter hides the lowest usage tail closest to the selected share`() {
        val apps = listOf(app(1, 70), app(2, 10), app(3, 20))
        assertEquals(listOf(1), filterAppsByCumulativeUsagePercent(apps, 30).map { it.uid })
        assertEquals(apps.map { it.uid }, filterAppsByCumulativeUsagePercent(apps, 0).map { it.uid })
    }

    @Test
    fun `percentage filter never hides the only app or removes a dominant final app`() {
        assertEquals(listOf(1), filterAppsByCumulativeUsagePercent(listOf(app(1, 100)), 30).map { it.uid })
        assertEquals(listOf(2), filterAppsByCumulativeUsagePercent(listOf(app(1, 1), app(2, 99)), 30).map { it.uid })
    }

    @Test
    fun `percentage filter leaves zero total lists unchanged and hides zero usage tail when total exists`() {
        assertEquals(
            listOf(1, 2),
            filterAppsByCumulativeUsagePercent(listOf(app(1, 0), app(2, 0)), 5).map { it.uid },
        )
        assertEquals(
            listOf(2),
            filterAppsByCumulativeUsagePercent(listOf(app(1, 0), app(2, 100)), 5).map { it.uid },
        )
    }

}
