package com.mywifiadata.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * اختبارات مرصد اختبار السرعة: انقطاع الاتصال، توقّف وصول البيانات،
 * والسقف الزمني الكامل — وهي الأساس في رسائل «أُغلق التحميل».
 */
class SpeedTestWatchdogTest {

    private val start = 1_000_000L

    private fun watchdog(
        overall: Long = 45_000L,
        stall: Long = 12_000L,
    ) = SpeedTestWatchdog(
        startedAtMillis = start,
        overallTimeoutMillis = overall,
        stallTimeoutMillis = stall,
    )

    @Test
    fun `a healthy running test reports no failure`() {
        val watchdog = watchdog()
        assertNull(watchdog.check(nowMillis = start + 3_000L, isOnline = true))
    }

    @Test
    fun `lost connectivity is reported immediately`() {
        val watchdog = watchdog()
        assertEquals(
            SpeedTestEngine.Failure.CONNECTION_LOST,
            watchdog.check(nowMillis = start + 1_000L, isOnline = false),
        )
    }

    @Test
    fun `no incoming data for the stall window is reported as stalled`() {
        val watchdog = watchdog()
        assertNull(watchdog.check(nowMillis = start + 11_999L, isOnline = true))
        assertEquals(
            SpeedTestEngine.Failure.STALLED,
            watchdog.check(nowMillis = start + 12_000L, isOnline = true),
        )
    }

    @Test
    fun `progress resets the stall timer`() {
        val watchdog = watchdog()
        watchdog.onProgress(start + 10_000L)
        assertNull(watchdog.check(nowMillis = start + 20_000L, isOnline = true))
        assertEquals(
            SpeedTestEngine.Failure.STALLED,
            watchdog.check(nowMillis = start + 22_000L, isOnline = true),
        )
    }

    @Test
    fun `no result within the overall limit is reported as timeout`() {
        val watchdog = watchdog(overall = 30_000L, stall = 60_000L)
        assertNull(watchdog.check(nowMillis = start + 29_999L, isOnline = true))
        assertEquals(
            SpeedTestEngine.Failure.TIMEOUT,
            watchdog.check(nowMillis = start + 30_000L, isOnline = true),
        )
    }

    @Test
    fun `connection loss wins over the stall timer`() {
        val watchdog = watchdog()
        assertEquals(
            SpeedTestEngine.Failure.CONNECTION_LOST,
            watchdog.check(nowMillis = start + 40_000L, isOnline = false),
        )
    }

    @Test
    fun `progress never moves backwards due to out of order reports`() {
        val watchdog = watchdog()
        watchdog.onProgress(start + 5_000L)
        watchdog.onProgress(start + 2_000L)
        assertNull(watchdog.check(nowMillis = start + 16_000L, isOnline = true))
    }
}
