package com.mywifiadata.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryRetentionTest {

    private val day = 24L * 60L * 60L * 1_000L

    @Test
    fun `options run from one month through eleven then years then unlimited`() {
        assertEquals(
            listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 24, 36, 0),
            HistoryRetention.options,
        )
    }

    @Test
    fun `default is the first month option`() {
        assertEquals(1, HistoryRetention.DEFAULT_MONTHS)
        assertEquals(HistoryRetention.options.first(), HistoryRetention.DEFAULT_MONTHS)
    }

    @Test
    fun `unknown values fall back to one month`() {
        assertEquals(1, HistoryRetention.normalize(13))
        assertEquals(1, HistoryRetention.normalize(-5))
        assertEquals(1, HistoryRetention.normalize(99))
        assertEquals(36, HistoryRetention.normalize(36))
        assertEquals(0, HistoryRetention.normalize(0))
    }

    @Test
    fun `retention millis uses thirty day months`() {
        assertEquals(30 * day, HistoryRetention.retentionMillis(1))
        assertEquals(60 * day, HistoryRetention.retentionMillis(2))
        assertEquals(330 * day, HistoryRetention.retentionMillis(11))
        assertEquals(360 * day, HistoryRetention.retentionMillis(12))
        assertEquals(720 * day, HistoryRetention.retentionMillis(24))
        assertEquals(1_080 * day, HistoryRetention.retentionMillis(36))
    }

    @Test
    fun `unlimited means zero millis`() {
        assertEquals(0L, HistoryRetention.retentionMillis(HistoryRetention.UNLIMITED))
    }
}
