package com.mywifiadata.app.data

import com.mywifiadata.app.data.model.AppSettings
import com.mywifiadata.app.data.model.TrafficTotals
import com.mywifiadata.app.ui.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات النماذج والحسابات المشتقّة في حالة الواجهة.
 */
class UsageModelsTest {

    @Test
    fun `totals add up rx and tx`() {
        val first = TrafficTotals(rxBytes = 1_000, txBytes = 500)
        val second = TrafficTotals(rxBytes = 2_000, txBytes = 250)
        val sum = first + second

        assertEquals(3_000, sum.rxBytes)
        assertEquals(750, sum.txBytes)
        assertEquals(3_750, sum.totalBytes)
        assertEquals(0, TrafficTotals.Empty.totalBytes)
    }

    @Test
    fun `quota progress and remaining bytes are derived from the cycle totals`() {
        val state = UiState(
            now = System.currentTimeMillis(),
            settings = AppSettings(quotaBytes = 10_000_000_000, cycleStartDay = 1),
            cycle = TrafficTotals(rxBytes = 3_000_000_000, txBytes = 1_000_000_000),
        )

        assertEquals(0.4f, state.quotaProgress, 0.0001f)
        assertEquals(6_000_000_000L, state.remainingBytes)
        assertFalse(state.quotaExceeded)
        assertTrue(state.hasQuota)
    }

    @Test
    fun `exceeding the quota clamps the progress and flags it`() {
        val state = UiState(
            settings = AppSettings(quotaBytes = 1_000_000_000),
            cycle = TrafficTotals(rxBytes = 1_500_000_000, txBytes = 0),
        )

        assertEquals(1f, state.quotaProgress, 0.0001f)
        assertEquals(0L, state.remainingBytes)
        assertTrue(state.quotaExceeded)
    }

    @Test
    fun `without a quota the progress stays at zero`() {
        val state = UiState(cycle = TrafficTotals(rxBytes = 500_000, txBytes = 500_000))
        assertFalse(state.hasQuota)
        assertEquals(0f, state.quotaProgress, 0.0001f)
    }

    @Test
    fun `average per day and projection use the elapsed days of the cycle`() {
        val state = UiState(
            now = System.currentTimeMillis(),
            settings = AppSettings(quotaBytes = 50_000_000_000, cycleStartDay = 1),
            cycle = TrafficTotals(rxBytes = 3_000_000_000, txBytes = 0),
        )

        assertEquals(state.cycle.totalBytes / state.elapsedDays, state.averagePerDay)
        assertTrue(state.projectedBytes >= state.cycle.totalBytes)
        assertTrue(state.daysLeft in 0..31)
    }
}
