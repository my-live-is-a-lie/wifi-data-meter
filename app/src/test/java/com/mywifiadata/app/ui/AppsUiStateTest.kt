package com.mywifiadata.app.ui

import com.mywifiadata.app.core.AppsUsageScope
import com.mywifiadata.app.core.TimeRange
import com.mywifiadata.app.core.TimeRanges
import com.mywifiadata.app.core.UsageNetwork
import com.mywifiadata.app.core.UsagePeriod
import com.mywifiadata.app.core.AppsSortOrder
import com.mywifiadata.app.data.model.AppSearchSelection
import com.mywifiadata.app.data.model.AppsFilter
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.AppsUsageFilterMode
import com.mywifiadata.app.data.model.PlanCapacityUnit
import com.mywifiadata.app.data.model.TrafficTotals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class AppsUiStateTest {
    @Test
    fun `top five only limits displayed rows not summary or share denominator`() {
        val apps = (1..10).map { AppUsage(it, "app.$it", "App $it", false, TrafficTotals(it.toLong(), 0)) }
        val state = UiState(apps = apps, periodApps = apps, appsCountLimit = 5)
        assertEquals(listOf(10, 9, 8, 7, 6), state.visibleApps.map { it.uid })
        assertEquals(10, state.visiblePeriodApps.size)
        assertEquals(55L, state.visiblePeriodApps.sumOf { it.totals.totalBytes })
        assertEquals(55L, state.filteredApps.sumOf { it.totals.totalBytes })
    }

    @Test
    fun `absolute app usage filter affects list rows but not summaries`() {
        val apps = listOf(
            AppUsage(1, "app.1", "App 1", false, TrafficTotals(5_000_000, 0)),
            AppUsage(2, "app.2", "App 2", false, TrafficTotals(5_000_001, 0)),
            AppUsage(3, "app.3", "App 3", false, TrafficTotals(20_000_000, 0)),
        )
        val state = UiState(
            apps = apps,
            periodApps = apps,
            settings = UiState().settings.copy(
                appsUsageFilterEnabled = true,
                appsUsageFilterMode = AppsUsageFilterMode.AMOUNT,
                appsUsageFilterAmount = 5,
                appsUsageFilterUnit = PlanCapacityUnit.MB,
            ),
        )

        assertEquals(listOf(3, 2), state.visibleApps.map { it.uid })
        assertEquals(3, state.filteredApps.size)
        assertEquals(3, state.visiblePeriodApps.size)
    }

    @Test
    fun `percentage app filter hides the low usage tail while preserving the full-period summary`() {
        val apps = listOf(
            AppUsage(1, "app.1", "App 1", false, TrafficTotals(70, 0)),
            AppUsage(2, "app.2", "App 2", false, TrafficTotals(10, 0)),
            AppUsage(3, "app.3", "App 3", false, TrafficTotals(20, 0)),
        )
        val state = UiState(
            apps = apps,
            periodApps = apps,
            settings = UiState().settings.copy(
                appsUsageFilterEnabled = true,
                appsUsageFilterMode = AppsUsageFilterMode.PERCENTAGE,
                appsUsageFilterPercent = 30,
            ),
        )

        assertEquals(listOf(1), state.visibleApps.map { it.uid })
        assertEquals(100L, state.filteredApps.sumOf { it.totals.totalBytes })
        assertEquals(100L, state.visiblePeriodApps.sumOf { it.totals.totalBytes })
    }

    @Test
    fun `disabled app usage filter leaves every app in the list`() {
        val apps = (1..4).map { AppUsage(it, "app.$it", "App $it", false, TrafficTotals(it.toLong(), 0)) }
        val state = UiState(
            apps = apps,
            settings = UiState().settings.copy(
                appsUsageFilterEnabled = false,
                appsUsageFilterMode = AppsUsageFilterMode.AMOUNT,
                appsUsageFilterAmount = 100,
            ),
        )

        assertEquals(listOf(4, 3, 2, 1), state.visibleApps.map { it.uid })
    }

    @Test
    fun `changing order preserves count and highest consumer in summary`() {
        val apps = (1..10).map { AppUsage(it, "app.$it", "App $it", false, TrafficTotals(it.toLong(), 0)) }
        val highest = UiState(apps = apps, periodApps = apps, appsCountLimit = 5)
        val lowest = highest.copy(appsSortOrder = AppsSortOrder.LOWEST_USAGE)
        assertEquals(listOf(1, 2, 3, 4, 5), lowest.visibleApps.map { it.uid })
        assertEquals(highest.appsCountLimit, lowest.appsCountLimit)
        assertEquals(highest.visiblePeriodApps, lowest.visiblePeriodApps)
        assertEquals(10, lowest.visiblePeriodApps.first().uid)
        assertEquals(highest.filteredApps, lowest.filteredApps)
        assertEquals(listOf(10, 9, 8, 7, 6), lowest.copy(appsSortOrder = AppsSortOrder.HIGHEST_USAGE).visibleApps.map { it.uid })
    }

    @Test
    fun `reversing order twice restores rows while retaining other filters`() {
        val apps = (1..10).map { AppUsage(it, "app.$it", "App $it", false, TrafficTotals(it.toLong(), 0)) }
        val original = UiState(apps = apps, periodApps = apps, appsCountLimit = 5)
        val reversed = original.copy(appsSortOrder = original.appsSortOrder.reversed())
        assertEquals(AppsSortOrder.LOWEST_USAGE, reversed.appsSortOrder)
        assertEquals(listOf(1, 2, 3, 4, 5), reversed.visibleApps.map { it.uid })
        assertEquals(original.appsCountLimit, reversed.appsCountLimit)
        assertEquals(original.appsNetwork, reversed.appsNetwork)
        assertEquals(original.appsScope, reversed.appsScope)
        assertEquals(original.appsFilter, reversed.appsFilter)
        assertEquals(original.visiblePeriodApps, reversed.visiblePeriodApps)
        val restored = reversed.copy(appsSortOrder = reversed.appsSortOrder.reversed())
        assertEquals(original, restored)
        assertEquals(original.visibleApps, restored.visibleApps)
    }

    private fun at(day: Int, hour: Int = 0): Long =
        LocalDateTime.of(2026, 10, day, hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `all time scope still honors each selected period on both networks`() {
        val now = at(15, 12)
        UsageNetwork.entries.forEach { network ->
            val state = UiState(now = now, appsNetwork = network, appsScope = AppsUsageScope.ALL_TIME)
            UsagePeriod.entries.forEach { period ->
                assertEquals(TimeRanges.forPeriod(period, now), state.copy(appsPeriod = period).appsPeriodRange())
            }
        }
    }

    @Test
    fun `current wifi plan intersects rather than replaces the selected period`() {
        val now = at(15, 12)
        val state = UiState(now = now, appsScope = AppsUsageScope.CURRENT_CYCLE)
        val currentPlan = state.copy(settings = state.settings.copy(cycleStartDay = 12))
        assertEquals(TimeRange(at(15), now), currentPlan.copy(appsPeriod = UsagePeriod.TODAY).appsPeriodRange())
        assertEquals(TimeRange(at(12), now), currentPlan.copy(appsPeriod = UsagePeriod.WEEK).appsPeriodRange())
        assertEquals(TimeRange(at(12), now), currentPlan.copy(appsPeriod = UsagePeriod.MONTH).appsPeriodRange())

        // An older plan must not broaden the seven-day selection.
        val olderPlan = currentPlan.copy(settings = currentPlan.settings.copy(cycleStartDay = 1))
        assertEquals(TimeRange(at(9), now), olderPlan.copy(appsPeriod = UsagePeriod.WEEK).appsPeriodRange())
    }

    @Test
    fun `hidden wifi plan filter does not override mobile period selection`() {
        val now = at(15, 12)
        val state = UiState(now = now, appsNetwork = UsageNetwork.MOBILE, appsScope = AppsUsageScope.CURRENT_CYCLE)
        UsagePeriod.entries.forEach { period ->
            assertEquals(TimeRanges.forPeriod(period, now), state.copy(appsPeriod = period).appsPeriodRange())
        }
    }

    @Test
    fun `summary period ignores chart drilldown sorting and row count`() {
        val state = UiState(now = at(15, 12), appsPeriod = UsagePeriod.MONTH)
        val filtered = state.copy(
            appsWeek = TimeRange(at(9), at(15)),
            appsSlice = TimeRange(at(14), at(15)),
            appsSortOrder = AppsSortOrder.LOWEST_USAGE,
            appsCountLimit = 5,
        )
        assertEquals(state.appsPeriodRange(), filtered.appsPeriodRange())
    }

    @Test
    fun `search matches all filtered apps before applying the row limit`() {
        val apps = (1..10).map { AppUsage(it, "app.$it", "App $it", false, TrafficTotals(it.toLong(), 0)) }
        val state = UiState(apps = apps, periodApps = apps, appsCountLimit = 5)
        val searching = state.copy(appsSearchActive = true, appsSearchQuery = "app.1")
        assertEquals(listOf(10, 1), searching.visibleApps.map { it.uid })
        assertEquals(state.visiblePeriodApps, searching.visiblePeriodApps)
        assertEquals(state.filteredApps, searching.filteredApps)
    }

    @Test
    fun `search suggestions respect app type and frequency but closing restores usage order`() {
        val apps = (1..10).map { AppUsage(it, "app.$it", "App $it", it == 2, TrafficTotals(it.toLong(), 0)) }
        val state = UiState(apps = apps, periodApps = apps, appsCountLimit = 5)
        val searching = state.copy(
            appsSearchActive = true,
            appsFilter = AppsFilter.INSTALLED,
            settings = state.settings.copy(appSearchHistory = listOf(
                AppSearchSelection("app.1", 3, 100),
                AppSearchSelection("app.2", 10, 500),
                AppSearchSelection("app.3", 1, 200),
            )),
        )
        assertEquals(listOf(1, 3, 10, 9, 8), searching.visibleApps.map { it.uid })
        assertEquals(listOf(1, 3, 10, 9, 8), searching.copy(appsSearchQuery = "App").visibleApps.map { it.uid })
        assertEquals(listOf(10, 9, 8, 7, 6), searching.copy(appsSearchActive = false).visibleApps.map { it.uid })
    }

    @Test
    fun `blank search promotes history without hiding remaining apps and clearing restores suggestions`() {
        val apps = (1..10).map { AppUsage(it, "app.$it", "App $it", false, TrafficTotals(it.toLong(), 0)) }
        val state = UiState(apps = apps, periodApps = apps, appsSearchActive = true)
        assertEquals((10 downTo 1).toList(), state.visibleApps.map { it.uid })
        val withHistory = state.copy(settings = state.settings.copy(
            appSearchHistory = listOf(AppSearchSelection("app.1", 1, 100)),
        ))
        assertEquals(listOf(1) + (10 downTo 2).toList(), withHistory.visibleApps.map { it.uid })
        assertEquals(listOf(2), withHistory.copy(appsSearchQuery = "App 2").visibleApps.map { it.uid })
        assertEquals(listOf(1) + (10 downTo 2).toList(), withHistory.copy(appsSearchQuery = "").visibleApps.map { it.uid })
        assertEquals(10, withHistory.copy(appsSearchActive = false).visibleApps.size)
        assertEquals(state.visiblePeriodApps, withHistory.visiblePeriodApps)
    }

}
