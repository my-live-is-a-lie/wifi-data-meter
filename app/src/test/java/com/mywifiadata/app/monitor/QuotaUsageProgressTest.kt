package com.mywifiadata.app.monitor

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuotaUsageProgressTest {

    @Test
    fun `missing quota keeps the original wifi icon`() {
        assertNull(QuotaUsageProgressCalculator.calculate(240L, 0L))
        assertNull(QuotaUsageProgressCalculator.calculate(240L, -1L))
    }

    @Test
    fun `percentage and ring track usage proportionally`() {
        val progress = requireNotNull(QuotaUsageProgressCalculator.calculate(240L, 1_000L))

        assertEquals(24, progress.percent)
        assertEquals(0.24f, progress.fraction, 0.0001f)
    }

    @Test
    fun `percentage rounds to the nearest whole number`() {
        assertEquals(
            25,
            QuotaUsageProgressCalculator.calculate(245L, 1_000L)?.percent,
        )
    }

    @Test
    fun `progress ring stops at full while text shows over-quota usage`() {
        val progress = requireNotNull(QuotaUsageProgressCalculator.calculate(1_500L, 1_000L))

        assertEquals(150, progress.percent)
        assertEquals(1f, progress.fraction, 0f)
    }

    @Test
    fun `negative usage is treated as zero`() {
        val progress = requireNotNull(QuotaUsageProgressCalculator.calculate(-10L, 1_000L))

        assertEquals(0, progress.percent)
        assertEquals(0f, progress.fraction, 0f)
    }

    @Test
    fun `percent digits follow the selected app language`() {
        assertEquals("٢٤", QuotaUsageProgressCalculator.formatDigits(24, Locale.forLanguageTag("ar")))
        assertEquals("24", QuotaUsageProgressCalculator.formatDigits(24, Locale.ENGLISH))
    }

    @Test
    fun `percentage is capped to three digits so it fits the icon`() {
        assertEquals(
            999,
            QuotaUsageProgressCalculator.calculate(Long.MAX_VALUE, 1L)?.percent,
        )
    }
}
