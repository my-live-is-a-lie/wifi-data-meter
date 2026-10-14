package com.mywifiadata.app.ui

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
}
