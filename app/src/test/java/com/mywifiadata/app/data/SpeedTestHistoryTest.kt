package com.mywifiadata.app.data

import com.mywifiadata.app.data.model.SpeedTestRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedTestHistoryTest {

    @Test
    fun `newest results come first and history keeps ten records`() {
        val initial = (1L..SPEED_TEST_HISTORY_LIMIT.toLong()).map(::record)
        val newest = record(11L)

        val history = SpeedTestHistory.prepend(initial, newest)

        assertEquals(SPEED_TEST_HISTORY_LIMIT, history.size)
        assertEquals((11L downTo 2L).toList(), history.map(SpeedTestRecord::timestampMillis))
    }

    @Test
    fun `adding the same record twice does not duplicate it`() {
        val result = record(42L)

        val history = SpeedTestHistory.prepend(
            SpeedTestHistory.prepend(emptyList(), result),
            result,
        )

        assertEquals(listOf(result), history)
    }

    @Test
    fun `history codec round trips every field`() {
        val expected = listOf(
            SpeedTestRecord(
                downloadBitsPerSecond = 8_900_000.0,
                uploadBitsPerSecond = 4_500_000.0,
                timestampMillis = 1_800_000_000_000L,
                usedFastCom = false,
                uploadMeasured = false,
                dataBytes = 42_000_000L,
            ),
            record(1_799_000_000_000L),
        )

        assertEquals(expected, SpeedTestHistory.decode(SpeedTestHistory.encode(expected)))
    }

    @Test
    fun `decoder skips corrupt rows and sorts valid records`() {
        val older = record(10L)
        val newer = record(20L)
        val encoded = listOf(
            SpeedTestHistory.encode(listOf(older)).substringBefore(';'),
            "not,a,valid,row",
            SpeedTestHistory.encode(listOf(newer)).substringBefore(';'),
        ).joinToString(";")

        val decoded = SpeedTestHistory.decode(encoded)

        assertEquals(listOf(newer, older), decoded)
        assertTrue(SpeedTestHistory.decode("").isEmpty())
        assertFalse(SpeedTestHistory.decode("1,2,0,0,0").isNotEmpty())
    }

    @Test
    fun `legacy five field rows decode with unknown data usage`() {
        // 4607182418800017408 هي بتات العدد العشري 1.0 في تمثيل Double الخام.
        val decoded = SpeedTestHistory.decode("4607182418800017408,4607182418800017408,1800000000000,1,1")

        assertEquals(1, decoded.size)
        assertEquals(0L, decoded.single().dataBytes)
    }

    @Test
    fun `prepending keeps the newest copy of a test with the same timestamp`() {
        val older = record(5L).copy(dataBytes = 0L)
        val newer = record(5L).copy(dataBytes = 9_000_000L)

        assertEquals(listOf(newer), SpeedTestHistory.prepend(listOf(older), newer))
    }

    private fun record(timestampMillis: Long) = SpeedTestRecord(
        downloadBitsPerSecond = timestampMillis.toDouble(),
        uploadBitsPerSecond = timestampMillis / 2.0,
        timestampMillis = timestampMillis,
        usedFastCom = timestampMillis % 2L == 0L,
        uploadMeasured = timestampMillis % 3L != 0L,
    )
}
