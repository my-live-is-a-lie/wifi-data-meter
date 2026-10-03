package com.mywifiadata.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات اختيار مصدر القياس: تفضيل fast.com عند جهوزيته، والانتقال إلى
 * أسرع مصدر بديل عند تعطّله — وهو جوهر إصلاح «تعذّر إجراء الاختبار».
 */
class SourceSelectorTest {

    private val order = listOf("fast.com", "Google", "jsDelivr", "Hetzner-DE")
    private val minBytes = 400_000L

    @Test
    fun `preferred source wins when it reached the minimum`() {
        val bytes = mapOf("fast.com" to 900_000L, "Google" to 5_000_000L)
        assertEquals("fast.com", SourceSelector.pick(order, bytes, minBytes, "fast.com"))
    }

    @Test
    fun `fastest fallback wins when the preferred source is blocked`() {
        val bytes = mapOf("fast.com" to 0L, "Google" to 1_200_000L, "jsDelivr" to 3_400_000L)
        assertEquals("jsDelivr", SourceSelector.pick(order, bytes, minBytes, "fast.com"))
    }

    @Test
    fun `a slow but working fallback below the minimum still wins if it is the only one`() {
        val bytes = mapOf("fast.com" to 0L, "Hetzner-DE" to 150_000L)
        assertEquals("Hetzner-DE", SourceSelector.pick(order, bytes, minBytes, "fast.com"))
    }

    @Test
    fun `ties fall back to the declared order`() {
        val bytes = mapOf("fast.com" to 0L, "Google" to 700_000L, "jsDelivr" to 700_000L)
        assertEquals("Google", SourceSelector.pick(order, bytes, minBytes, "fast.com"))
    }

    @Test
    fun `no bytes anywhere yields no source`() {
        assertNull(SourceSelector.pick(order, emptyMap(), minBytes, "fast.com"))
    }

    @Test
    fun `other sources are detected once they reach the minimum`() {
        assertFalse(
            SourceSelector.anyOtherReached(order, mapOf("Google" to 100_000L), minBytes, "fast.com"),
        )
        assertTrue(
            SourceSelector.anyOtherReached(order, mapOf("Google" to 500_000L), minBytes, "fast.com"),
        )
    }
}
