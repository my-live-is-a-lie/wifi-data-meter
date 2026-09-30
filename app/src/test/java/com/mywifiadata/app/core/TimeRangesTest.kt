package com.mywifiadata.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * اختبارات حساب الفترات الزمنية ودورة الفاتورة.
 */
class TimeRangesTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun dayStart(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun at(year: Int, month: Int, day: Int, hour: Int): Long =
        LocalDateTime.of(year, month, day, hour, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `today period starts at midnight`() {
        val now = at(2026, 3, 10, 14)
        val range = TimeRanges.forPeriod(UsagePeriod.TODAY, now)
        assertEquals(dayStart(2026, 3, 10), range.startMillis)
        assertEquals(now, range.endMillis)
    }

    @Test
    fun `week period covers seven days`() {
        val now = at(2026, 3, 10, 14)
        val range = TimeRanges.forPeriod(UsagePeriod.WEEK, now)
        assertEquals(dayStart(2026, 3, 4), range.startMillis)
        assertEquals(7, TimeRanges.daysOf(UsagePeriod.WEEK))
    }

    @Test
    fun `month period covers thirty days`() {
        val now = at(2026, 3, 10, 14)
        val range = TimeRanges.forPeriod(UsagePeriod.MONTH, now)
        assertEquals(dayStart(2026, 2, 9), range.startMillis)
        assertEquals(30, TimeRanges.daysOf(UsagePeriod.MONTH))
    }

    @Test
    fun `cycle range starting on the first of the month`() {
        val now = at(2026, 3, 10, 12)
        val range = TimeRanges.cycleRange(cycleStartDay = 1, now = now)
        assertEquals(dayStart(2026, 3, 1), range.startMillis)
        assertEquals(dayStart(2026, 4, 1), range.endMillis)
    }

    @Test
    fun `cycle range keeps the start day when it is earlier than today`() {
        val now = at(2026, 3, 10, 12)
        val range = TimeRanges.cycleRange(cycleStartDay = 15, now = now)
        assertEquals(dayStart(2026, 2, 15), range.startMillis)
        assertEquals(dayStart(2026, 3, 15), range.endMillis)
    }

    @Test
    fun `cycle range handles short months`() {
        val now = at(2026, 2, 20, 9)
        val range = TimeRanges.cycleRange(cycleStartDay = 31, now = now)
        assertEquals(dayStart(2026, 1, 28), range.startMillis)
        assertEquals(dayStart(2026, 2, 28), range.endMillis)
    }

    @Test
    fun `days left in cycle is computed until the end of the cycle`() {
        val now = at(2026, 3, 10, 12)
        assertEquals(22, TimeRanges.daysLeftInCycle(cycleStartDay = 1, now = now))
    }

    @Test
    fun `last days returns consecutive days oldest first`() {
        val now = at(2026, 3, 10, 8)
        val days = TimeRanges.lastDays(7, now)
        assertEquals(7, days.size)
        assertEquals(dayStart(2026, 3, 4), days.first().startMillis)
        assertEquals(dayStart(2026, 3, 10), days.last().startMillis)
        assertEquals(dayStart(2026, 3, 11), days.last().endMillis)
        days.forEach { assertTrue(it.endMillis > it.startMillis) }
    }
}
