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

    /**
     * ساعات اليوم منذ منتصف الليل حتى «الآن» — الأحدث أولًا.
     * الساعة الحالية تنتهي عند «الآن» (جزئية)، ولا تُدرج ساعات مستقبلية فارغة.
     */
    fun hoursOfToday(now: Long = System.currentTimeMillis()): List<TimeRange> {
        val start = startOfToday(now)
        var cursor = Instant.ofEpochMilli(now).atZone(zone)
            .toLocalDateTime()
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        val hours = mutableListOf<TimeRange>()
        while (true) {
            val hourStart = cursor.atZone(zone).toInstant().toEpochMilli()
            if (hourStart < start) break
            hours += TimeRange(hourStart, minOf(hourStart + 3_600_000L, now))
            cursor = cursor.minusHours(1)
        }
        return hours
    }

    /**
     * آخر [days] يومًا مقسّمة إلى أسابيع متدحرجة (الأحدث أولًا، كل مجموعة أسبوع
     * كامل 7 أيام عدا الأقدم التي تستكمل ما تبقّى من الفترة).
     * تُستخدم لعرض «آخر 4 أسابيع» من قسم التطبيقات.
     */
    fun lastWeeks(days: Int = 30, now: Long = System.currentTimeMillis()): List<TimeRange> =
        lastDays(days, now)
            .chunked(7)
            .map { group -> TimeRange(group.first().startMillis, group.last().endMillis) }

    /** الأيام الكاملة داخل [range] — الأحدث أولًا. */
    fun daysInRange(range: TimeRange): List<TimeRange> {
        val days = mutableListOf<TimeRange>()
        var cursor = range.startMillis
        while (cursor < range.endMillis) {
            val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val dayStart = startOfDay(date)
            val dayEnd = startOfDay(date.plusDays(1))
            if (dayStart >= range.endMillis) break
            days += TimeRange(dayStart, minOf(dayEnd, range.endMillis))
            cursor = dayEnd
        }
        return days.reversed()
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

    /** الشهر والسنة الحاليان حسب التوقيت المحلي. */
    fun currentYearMonth(now: Long = System.currentTimeMillis()): HistoryYearMonth {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return HistoryYearMonth(year = today.year, month = today.monthValue)
    }

    /** نطاق سنة ميلادية كاملة (مقصوص عند [now] إن كانت السنة الحالية). */
    fun yearRange(year: Int, now: Long = System.currentTimeMillis()): TimeRange {
        val start = startOfDay(LocalDate.of(year, 1, 1))
        val end = minOf(startOfDay(LocalDate.of(year + 1, 1, 1)), now).coerceAtLeast(start)
        return TimeRange(start, end)
    }

    /** نطاق شهر ميلادي محدد (مقصوص عند [now] إن كان الشهر الحالي). */
    fun monthRange(year: Int, month: Int, now: Long = System.currentTimeMillis()): TimeRange {
        val safeMonth = month.coerceIn(1, 12)
        val firstDay = LocalDate.of(year, safeMonth, 1)
        val start = startOfDay(firstDay)
        val end = minOf(startOfDay(firstDay.plusMonths(1)), now).coerceAtLeast(start)
        return TimeRange(start, end)
    }

    /**
     * أيام الشهر الميلادي المحدد (الأقدم أولاً: من يوم 1 حتى نهاية الشهر،
     * أو حتى اليوم الحالي إذا كان الشهر المحدد هو الشهر الجاري).
     */
    fun daysOfMonth(year: Int, month: Int, now: Long = System.currentTimeMillis()): List<TimeRange> {
        val safeMonth = month.coerceIn(1, 12)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val firstDay = LocalDate.of(year, safeMonth, 1)
        if (firstDay.isAfter(today)) return emptyList()
        val lastDayOfMonth = firstDay.lengthOfMonth()
        val lastDay = if (today.year == year && today.monthValue == safeMonth) {
            today.dayOfMonth
        } else {
            lastDayOfMonth
        }
        return (1..lastDay).map { dayOfMonth ->
            val day = LocalDate.of(year, safeMonth, dayOfMonth)
            TimeRange(startOfDay(day), startOfDay(day.plusDays(1)))
        }
    }
}
