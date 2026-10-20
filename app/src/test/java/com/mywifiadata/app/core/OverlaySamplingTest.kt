package com.mywifiadata.app.core

import com.mywifiadata.app.data.model.TrafficTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** حساب جلسة النافذة العائمة من قراءات إحصاءات الشبكة الحقيقية. */
class OverlaySamplingTest {

    @Test
    fun `session totals subtract the reading taken at session start`() {
        val start = TrafficTotals(rxBytes = 1_000L, txBytes = 200L)
        val now = TrafficTotals(rxBytes = 4_500L, txBytes = 700L)

        assertEquals(
            TrafficTotals(rxBytes = 3_500L, txBytes = 500L),
            OverlaySampling.sessionTotals(start, now),
        )
    }

    @Test
    fun `session totals never go negative when the system resets its counters`() {
        val start = TrafficTotals(rxBytes = 9_000L, txBytes = 900L)
        val now = TrafficTotals(rxBytes = 10L, txBytes = 20L)

        assertEquals(TrafficTotals.Empty, OverlaySampling.sessionTotals(start, now))
    }

    @Test
    fun `rate divides the real byte delta by the real elapsed time`() {
        val first = UsageSample(0L, TrafficTotals(rxBytes = 1_000L, txBytes = 500L))
        val last = UsageSample(4_000L, TrafficTotals(rxBytes = 21_000L, txBytes = 2_500L))

        val rate = OverlaySampling.rate(first, last)

        assertEquals(5_000.0, rate.rxBytesPerSecond, 0.001)
        assertEquals(500.0, rate.txBytesPerSecond, 0.001)
        assertEquals(5_500.0, rate.totalBytesPerSecond, 0.001)
    }

    @Test
    fun `rate stays zero when too little time has passed`() {
        val first = UsageSample(1_000L, TrafficTotals(rxBytes = 1_000L, txBytes = 0L))
        val last = UsageSample(1_250L, TrafficTotals(rxBytes = 90_000L, txBytes = 10_000L))

        val rate = OverlaySampling.rate(first, last)

        assertEquals(0.0, rate.totalBytesPerSecond, 0.0)
    }

    @Test
    fun `rate ignores a decrease instead of showing negative speed`() {
        val first = UsageSample(0L, TrafficTotals(rxBytes = 5_000L, txBytes = 5_000L))
        val last = UsageSample(2_000L, TrafficTotals(rxBytes = 100L, txBytes = 100L))

        val rate = OverlaySampling.rate(first, last)

        assertEquals(0.0, rate.rxBytesPerSecond, 0.0)
        assertEquals(0.0, rate.txBytesPerSecond, 0.0)
    }

    @Test
    fun `rate window starts at the oldest reading inside the window`() {
        val samples = (0..5).map { index ->
            UsageSample(index * 4_000L, TrafficTotals(rxBytes = index * 1_000L, txBytes = 0L))
        }

        // النافذة 16 ثانية: عند اللحظة 20 ثانية تبدأ من القراءة عند 4 ثوانٍ.
        assertEquals(samples[1], OverlaySampling.rateWindowStart(samples, 20_000L))
    }

    @Test
    fun `rate window falls back to the first reading while history is short`() {
        val samples = listOf(
            UsageSample(10_000L, TrafficTotals(rxBytes = 5L, txBytes = 0L)),
            UsageSample(12_000L, TrafficTotals(rxBytes = 9L, txBytes = 0L)),
        )

        assertEquals(samples.first(), OverlaySampling.rateWindowStart(samples, 12_000L))
    }

    @Test
    fun `empty history has no window`() {
        assertNull(OverlaySampling.rateWindowStart(emptyList(), 5_000L))
    }

    @Test
    fun `rate window matches the platform refresh cadence`() {
        // أندرويد لا يسمح بتحديث الإحصاءات أكثر من مرة كل 15 ثانية للمستخدم العادي.
        assertEquals(16_000L, OverlaySampling.RATE_WINDOW_MILLIS)
    }
}
