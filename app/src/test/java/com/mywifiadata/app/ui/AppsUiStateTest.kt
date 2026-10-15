package com.mywifiadata.app.ui

import com.mywifiadata.app.core.AppsSortOrder
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.TrafficTotals
import org.junit.Assert.assertEquals
import org.junit.Test

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

}
