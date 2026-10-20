package com.mywifiadata.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * اختبارات تنسيق الأرقام والوحدات (وحدات عشرية: 1 كيلوبايت = 1000 بايت).
 */
class FormatTest {

    @Test
    fun `bytes below one kilobyte are shown in bytes`() {
        assertEquals("0 B", Format.bytes(0))
        assertEquals("512 B", Format.bytes(512))
    }

    @Test
    fun `bytes are formatted with the largest suitable unit`() {
        assertEquals("2 KB", Format.bytes(2_000))
        assertEquals("1.5 MB", Format.bytes(1_500_000))
        assertEquals("3.00 GB", Format.bytes(3_000_000_000))
        assertEquals("2.00 TB", Format.bytes(2_000_000_000_000))
    }

    @Test
    fun `negative values are treated as zero`() {
        assertEquals("0 B", Format.bytes(-10))
    }

    @Test
    fun `speed is formatted per second`() {
        assertEquals("1.5 MB/ث", Format.speed(1_500_000.0))
        assertEquals("250 KB/ث", Format.speed(250_000.0))
        assertEquals("0 KB/ث", Format.speed(0.0))
        assertEquals("0 KB/ث", Format.speed(Double.NaN))
    }

    @Test
    fun `plain bit rate drops the per second suffix`() {
        assertEquals("125 kbit", Format.bitsPlain(125_000.0))
        assertEquals("40 kbit", Format.bitsPlain(40_000.0))
        assertEquals("1.5 Mbit", Format.bitsPlain(1_500_000.0))
        assertEquals("0 kbit", Format.bitsPlain(0.0))
        assertEquals("0 kbit", Format.bitsPlain(Double.NaN))
    }

    @Test
    fun `byte values split into amount and unit for the reference layout`() {
        assertEquals("532.9" to "MB", Format.bytesParts(532_900_000))
        assertEquals("2.75" to "GB", Format.bytesParts(2_750_000_000))
        assertEquals("512" to "B", Format.bytesParts(512))
        assertEquals("0" to "B", Format.bytesParts(-10))
    }

    @Test
    fun `megabits conversion uses the metric definition`() {
        assertEquals("8.0", Format.megabits(1_000_000.0))
    }

    @Test
    fun `percentages are rounded`() {
        assertEquals("80%", Format.percent(0.8f))
        assertEquals("42%", Format.percent(42))
    }

    @Test
    fun `gigabytes and bytes convert in both directions`() {
        assertEquals(1_500_000_000L, Format.gigabytesToBytes(1.5))
        assertEquals(2.0, Format.bytesToGigabytes(2_000_000_000L), 0.0001)
    }

    @Test
    fun `day of month is a plain number`() {
        // 15 يناير 2026 حسب توقيت الاختبار (منتصف اليوم لتفادي فروق المناطق الزمنية)
        val millis = java.time.LocalDate.of(2026, 1, 15)
            .atTime(12, 0)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals("15", Format.dayOfMonth(millis))
    }

    @Test
    fun `days between is at least one day`() {
        assertEquals(1, Format.daysBetween(0L, 0L))
        assertEquals(1, Format.daysBetween(0L, 3_600_000L))
        assertEquals(2, Format.daysBetween(0L, 2L * 86_400_000L))
    }

    @Test
    fun `session elapsed time shows minutes and seconds`() {
        assertEquals("00:00", Format.elapsed(0L))
        assertEquals("00:00", Format.elapsed(-5_000L))
        assertEquals("00:09", Format.elapsed(9_400L))
        assertEquals("05:09", Format.elapsed(309_000L))
        assertEquals("1:02:33", Format.elapsed(3_753_000L))
    }

    @Test
    fun `short byte format is compact`() {
        assertEquals("1.5G", Format.bytesShort(1_500_000_000))
        assertEquals("500M", Format.bytesShort(500_000_000))
    }
}
