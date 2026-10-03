package com.mywifiadata.app.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * أدوات تنسيق الأرقام والتواريخ.
 *
 * الأرقام تُنسَّق دائمًا بالأرقام اللاتينية (1, 2, 3) وبفاصلة عشرية نقطية مهما كانت لغة الجهاز،
 * لأن ذلك أوضح في السياق التقني. أما أسماء الأيام والأشهر فتُنسَّق بلغة الجهاز.
 *
 * وحدات القياس عشرية (1 كيلوبايت = 1000 بايت، 1 جيجابايت = 1000^3 بايت) لتطابق
 * الطريقة التي تحسب بها شركات الاتصالات الباقات.
 */
object Format {

    private const val KB = 1_000.0
    private const val MB = 1_000_000.0
    private const val GB = 1_000_000_000.0
    private const val TB = 1_000_000_000_000.0

    private val numbers = Locale.US

    /** مثال: "‎128 MB" أو "‎3.42 GB" */
    fun bytes(value: Long): String = when {
        value < 0 -> bytes(0)
        value >= TB -> String.format(numbers, "%.2f TB", value / TB)
        value >= GB -> String.format(numbers, "%.2f GB", value / GB)
        value >= MB -> String.format(numbers, "%.1f MB", value / MB)
        value >= KB -> String.format(numbers, "%.0f KB", value / KB)
        else -> String.format(numbers, "%d B", value)
    }

    /** مثال: "‎3.4 GB" (مختصر للمخططات) */
    fun bytesShort(value: Long): String = when {
        value >= TB -> String.format(numbers, "%.1fT", value / TB)
        value >= GB -> String.format(numbers, "%.1fG", value / GB)
        value >= MB -> String.format(numbers, "%.0fM", value / MB)
        value >= KB -> String.format(numbers, "%.0fK", value / KB)
        else -> String.format(numbers, "%dB", value)
    }

    /** مثال: "‎1.2 MB/ث" */
    fun speed(bytesPerSecond: Double): String = when {
        bytesPerSecond.isNaN() || bytesPerSecond <= 0.0 -> String.format(numbers, "0 KB/ث")
        bytesPerSecond >= MB -> String.format(numbers, "%.1f MB/ث", bytesPerSecond / MB)
        bytesPerSecond >= KB -> String.format(numbers, "%.0f KB/ث", bytesPerSecond / KB)
        else -> String.format(numbers, "%.0f B/ث", bytesPerSecond)
    }

    /** مثال: "‎125 Mbit/s" أو "‎40 kbit/s" (لنتائج اختبار السرعة). */
    fun bits(bitsPerSecond: Double): String = when {
        bitsPerSecond.isNaN() || bitsPerSecond <= 0.0 -> String.format(numbers, "0 kbit/s")
        bitsPerSecond >= 1_000_000_000.0 -> String.format(numbers, "%.2f Gbit/s", bitsPerSecond / 1_000_000_000.0)
        bitsPerSecond >= 1_000_000.0 -> String.format(numbers, "%.1f Mbit/s", bitsPerSecond / 1_000_000.0)
        bitsPerSecond >= 1_000.0 -> String.format(numbers, "%.0f kbit/s", bitsPerSecond / 1_000.0)
        else -> String.format(numbers, "%.0f bit/s", bitsPerSecond)
    }

    /** تحويل البايت/ثانية إلى ميجابت/ثانية (لتفاصيل الشبكة). */
    fun megabits(bytesPerSecond: Double): String =
        String.format(numbers, "%.1f", bytesPerSecond * 8.0 / 1_000_000.0)

    /** مثال: "42%" */
    fun percent(value: Float): String = String.format(numbers, "%.0f%%", value * 100f)

    fun percent(value: Int): String = String.format(numbers, "%d%%", value)

    fun gigabytesToBytes(gb: Double): Long = (gb * GB).toLong()

    fun bytesToGigabytes(bytes: Long): Double = bytes / GB

    private fun pattern(format: String): SimpleDateFormat =
        SimpleDateFormat(format, Locale.getDefault())

    /** "14:05" */
    fun clock(millis: Long): String = pattern("HH:mm").format(Date(millis))

    /** "12 مارس" */
    fun shortDate(millis: Long): String = pattern("d MMMM").format(Date(millis))

    /** "الأربعاء" */
    fun weekday(millis: Long): String = pattern("EEEE").format(Date(millis))

    /** "أ" أو "Wed" — تُستخدم في محور المخططات. */
    fun weekdayShort(millis: Long): String = pattern("EE").format(Date(millis)).trim()

    /**
     * "12" — رقم اليوم في الشهر.
     * يُستخدم في محور المخططات لأن أسماء الأيام العربية طويلة وتتكسّر داخل الأعمدة الضيقة.
     */
    fun dayOfMonth(millis: Long): String = pattern("d").format(Date(millis))

    /** "12 مارس 2026" */
    fun fullDate(millis: Long): String = pattern("d MMMM yyyy").format(Date(millis))

    /** عدد الأيام بين تاريخين (بحد أدنى 1). */
    fun daysBetween(startMillis: Long, endMillis: Long): Int {
        val millis = (endMillis - startMillis).coerceAtLeast(0L)
        return (millis / 86_400_000L).toInt().coerceAtLeast(1)
    }
}
