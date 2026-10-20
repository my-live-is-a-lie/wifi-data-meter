package com.mywifiadata.app.core

import com.mywifiadata.app.data.model.SpeedTestRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedTestDataUsageTest {

    @Test
    fun `no previous tests falls back to the default estimate`() {
        val estimate = SpeedTestDataUsage.estimate(emptyList())

        assertEquals(SpeedTestDataUsage.DEFAULT_ESTIMATE_BYTES, estimate.bytes)
        assertEquals(0, estimate.basedOnTests)
        assertFalse(estimate.isBasedOnHistory)
    }

    @Test
    fun `measured consumption is averaged with more weight for recent tests`() {
        val history = listOf(
            record(timestamp = 2L, dataBytes = 40_000_000L),
            record(timestamp = 1L, dataBytes = 20_000_000L),
        )

        val estimate = SpeedTestDataUsage.estimate(history)

        // (40×2 + 20×1) ÷ 3 = 33.3 MB، وبهامش الأمان ثم التقريب لأعلى: 40 MB.
        assertEquals(40_000_000L, estimate.bytes)
        assertEquals(2, estimate.basedOnTests)
        assertTrue(estimate.isBasedOnHistory)
    }

    @Test
    fun `only the five newest tests shape the estimate`() {
        val oldest = record(timestamp = 1L, dataBytes = 300_000_000L)
        val recent = (2L..6L).map { timestamp ->
            record(timestamp = timestamp, dataBytes = 10_000_000L)
        }

        val estimate = SpeedTestDataUsage.estimate(listOf(oldest) + recent)

        assertEquals(5, estimate.basedOnTests)
        assertEquals(15_000_000L, estimate.bytes)
    }

    @Test
    fun `legacy records without measured bytes are estimated from their speeds`() {
        val estimate = SpeedTestDataUsage.estimate(
            listOf(
                SpeedTestRecord(
                    downloadBitsPerSecond = 8_000_000.0,
                    uploadBitsPerSecond = 4_000_000.0,
                    timestampMillis = 1L,
                    usedFastCom = true,
                    dataBytes = 0L,
                ),
            ),
        )

        // تنزيل 15 ثانية + رفع 6 ثوانٍ + بايتات التجهيز ≈ 20 MB، ثم الهامش والتقريب.
        assertEquals(25_000_000L, estimate.bytes)
        assertEquals(1, estimate.basedOnTests)
    }

    @Test
    fun `estimate stays inside the sensible bounds`() {
        val tiny = SpeedTestDataUsage.estimate(listOf(record(1L, dataBytes = 1_000_000L)))
        val huge = SpeedTestDataUsage.estimate(listOf(record(2L, dataBytes = 2_000_000_000L)))

        assertEquals(5_000_000L, tiny.bytes)
        assertEquals(500_000_000L, huge.bytes)
    }

    @Test
    fun `warning returns once the full snooze day has passed`() {
        val now = 1_800_000_000_000L
        val until = SpeedTestDataUsage.snoozeUntil(now)

        assertEquals(now + 24L * 60L * 60L * 1_000L, until)
        assertFalse(SpeedTestDataUsage.shouldWarn(until, now))
        assertFalse(SpeedTestDataUsage.shouldWarn(until, until - 1L))
        assertTrue(SpeedTestDataUsage.shouldWarn(until, until))
        assertTrue(SpeedTestDataUsage.shouldWarn(until, until + 1L))
        assertTrue(SpeedTestDataUsage.shouldWarn(0L, now))
    }

    private fun record(timestamp: Long, dataBytes: Long) = SpeedTestRecord(
        downloadBitsPerSecond = 10_000_000.0,
        uploadBitsPerSecond = 5_000_000.0,
        timestampMillis = timestamp,
        usedFastCom = true,
        dataBytes = dataBytes,
    )
}
