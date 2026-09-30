package com.mywifiadata.app.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** الفترة الزمنية المختارة في الواجهة. */
enum class UsagePeriod { TODAY, WEEK, MONTH }

/** نطاق زمني مغلق [startMillis, endMillis). */
data class TimeRange(val startMillis: Long, val endMillis: Long)

/**
 * حسابات الفترات الزمنية (اليوم، الأسبوع، الشهر، دورة الفاتورة) بتوقيت الجهاز المحلي.
 */
object TimeRanges {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun startOfToday(now: Long = System.currentTimeMillis()): Long =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    fun startOfDay(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    /** نطاق الفترة المختارة بالنسبة للآن. */
    fun forPeriod(period: UsagePeriod, now: Long = System.currentTimeMillis()): TimeRange {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return when (period) {
            UsagePeriod.TODAY -> TimeRange(startOfDay(today), now)
            UsagePeriod.WEEK -> TimeRange(startOfDay(today.minusDays(6)), now)
            UsagePeriod.MONTH -> TimeRange(startOfDay(today.minusDays(29)), now)
        }
    }

    /** عدد الأيام التي تغطيها الفترة. */
    fun daysOf(period: UsagePeriod): Int = when (period) {
        UsagePeriod.TODAY -> 1
        UsagePeriod.WEEK -> 7
        UsagePeriod.MONTH -> 30
    }

    /** نطاقات الأيام الأخيرة، الأقدم أولاً: (بداية اليوم، نهاية اليوم). */
    fun lastDays(days: Int, now: Long = System.currentTimeMillis()): List<TimeRange> {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return (days - 1 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            TimeRange(startOfDay(day), startOfDay(day.plusDays(1)))
        }
    }

    /** نطاق دورة الفاتورة الحالية انطلاقًا من يوم بدء الدورة. */
    fun cycleRange(cycleStartDay: Int, now: Long = System.currentTimeMillis()): TimeRange {
        val day = cycleStartDay.coerceIn(1, 28)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

        var start = today.withDayOfMonth(day.coerceAtMost(today.lengthOfMonth()))
        if (today.isBefore(start)) {
            val previous = today.minusMonths(1)
            start = previous.withDayOfMonth(day.coerceAtMost(previous.lengthOfMonth()))
        }
        val nextMonth = start.plusMonths(1)
        val end = nextMonth.withDayOfMonth(day.coerceAtMost(nextMonth.lengthOfMonth()))
        return TimeRange(startOfDay(start), startOfDay(end))
    }

    /** عدد الأيام بين تاريخين (بحد أدنى يوم واحد). */
    fun daysBetween(startMillis: Long, endMillis: Long): Int =
        Format.daysBetween(startMillis, endMillis)

    /** عدد الأيام المتبقية حتى نهاية الدورة. */
    fun daysLeftInCycle(cycleStartDay: Int, now: Long = System.currentTimeMillis()): Int {
        val range = cycleRange(cycleStartDay, now)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(range.endMillis).atZone(zone).toLocalDate()
        return java.time.temporal.ChronoUnit.DAYS.between(today, end).toInt().coerceAtLeast(0)
    }
}
