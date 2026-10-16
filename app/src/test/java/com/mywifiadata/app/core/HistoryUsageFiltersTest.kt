package com.mywifiadata.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryUsageFiltersTest {

    @Test
    fun `available years only includes years with recorded usage sorted newest first`() {
        val recorded = listOf(
            HistoryYearMonth(2024, 11),
            HistoryYearMonth(2026, 9),
            HistoryYearMonth(2024, 12),
            HistoryYearMonth(2026, 10),
        )
        assertEquals(listOf(2026, 2024), availableHistoryYears(recorded))
    }

    @Test
    fun `available months only includes recorded months for the selected year`() {
        val recorded = listOf(
            HistoryYearMonth(2025, 12),
            HistoryYearMonth(2026, 10),
            HistoryYearMonth(2026, 8),
            HistoryYearMonth(2026, 10),
        )
        assertEquals(listOf(8, 10), availableHistoryMonths(recorded, 2026))
        assertEquals(listOf(12), availableHistoryMonths(recorded, 2025))
        assertEquals(emptyList<Int>(), availableHistoryMonths(recorded, 2024))
    }

    @Test
    fun `resolving month for a new year keeps current month if available or selects latest recorded month`() {
        val recorded = listOf(
            HistoryYearMonth(2025, 6),
            HistoryYearMonth(2025, 11),
            HistoryYearMonth(2026, 9),
            HistoryYearMonth(2026, 10),
        )
        assertEquals(11, resolveHistoryMonthForYear(recorded, targetYear = 2025, currentSelectedMonth = 10))
        assertEquals(9, resolveHistoryMonthForYear(recorded + HistoryYearMonth(2025, 9), targetYear = 2025, currentSelectedMonth = 9))
    }

    @Test
    fun `custom history month is false only when both month and year match current month and year`() {
        val current = HistoryYearMonth(2026, 10)
        assertFalse(isCustomHistoryMonth(2026, 10, current))
        assertTrue(isCustomHistoryMonth(2026, 11, current))
        assertTrue(isCustomHistoryMonth(2025, 10, current))
        assertTrue(isCustomHistoryMonth(2025, 11, current))
    }
}
