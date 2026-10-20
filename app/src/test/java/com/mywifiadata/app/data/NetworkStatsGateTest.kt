package com.mywifiadata.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * أندرويد لا يفرض تحديثًا لإحصاءات الشبكة إلا إذا مرّ 15 ثانية على آخر جلسة قراءة،
 * وأي نداء أسرع يعيد ضبط المهلة ويمنع التحديث. تتحقق هذه الاختبارات من أن البوابة
 * تسمح بنداء واحد كل نافذة فقط.
 */
class NetworkStatsGateTest {

    @Before
    fun setUp() {
        NetworkStatsGate.reset()
    }

    @Test
    fun `first read is always allowed`() {
        assertTrue(NetworkStatsGate.tryAcquire(1_000L))
    }

    @Test
    fun `reads inside the poll window are refused`() {
        assertTrue(NetworkStatsGate.tryAcquire(1_000L))

        assertFalse(NetworkStatsGate.tryAcquire(2_000L))
        assertFalse(NetworkStatsGate.tryAcquire(16_999L))
    }

    @Test
    fun `a read after the window is allowed again`() {
        assertTrue(NetworkStatsGate.tryAcquire(1_000L))

        assertTrue(NetworkStatsGate.tryAcquire(17_000L))
        assertFalse(NetworkStatsGate.tryAcquire(17_500L))
    }

    @Test
    fun `window is never shorter than the platform limit`() {
        assertTrue(NetworkStatsGate.MIN_INTERVAL_MILLIS >= 15_000L)
    }
}
