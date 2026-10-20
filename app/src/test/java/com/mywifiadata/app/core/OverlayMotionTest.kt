package com.mywifiadata.app.core

import com.mywifiadata.app.overlay.OverlayMode
import com.mywifiadata.app.overlay.expandsTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حركة النافذة العائمة: مدة التحوّل، تدرّج الإزاحة، واتجاه التكبير/التصغير.
 * هذه حسابات خالصة يمكن التحقق منها بلا جهاز.
 */
class OverlayMotionTest {

    @Test
    fun `expanding takes longer than collapsing but stays responsive`() {
        assertTrue(OverlayMotion.durationMillis(true) > OverlayMotion.durationMillis(false))
        assertTrue(OverlayMotion.durationMillis(true) > 0L)
        assertTrue(OverlayMotion.durationMillis(true) <= OverlayMotion.MAX_MILLIS)
        assertTrue(OverlayMotion.durationMillis(false) <= OverlayMotion.MAX_MILLIS)
    }

    @Test
    fun `ease out starts at zero, ends at one and decelerates`() {
        assertEquals(0f, OverlayMotion.easeOut(0f), 0.0001f)
        assertEquals(1f, OverlayMotion.easeOut(1f), 0.0001f)
        // في أول نصف الزمن يُقطع أكثر من نصف المسافة، ثم تتباطأ الحركة.
        assertTrue(OverlayMotion.easeOut(0.25f) > 0.25f)
        assertTrue(OverlayMotion.easeOut(0.5f) > 0.5f)
        assertTrue(OverlayMotion.easeOut(0.5f) < OverlayMotion.easeOut(0.75f))
    }

    @Test
    fun `ease out clamps values outside the animation range`() {
        assertEquals(0f, OverlayMotion.easeOut(-0.4f), 0.0001f)
        assertEquals(1f, OverlayMotion.easeOut(1.4f), 0.0001f)
    }

    @Test
    fun `lerp moves the window between two positions`() {
        assertEquals(0, OverlayMotion.lerp(0, 400, 0f))
        assertEquals(400, OverlayMotion.lerp(0, 400, 1f))
        assertEquals(200, OverlayMotion.lerp(0, 400, 0.5f))
        // اتجاه سالب (نافذة تتحرك يسارًا أثناء الالتصاق بالحافة اليسرى).
        assertEquals(200, OverlayMotion.lerp(300, 100, 0.5f))
        assertEquals(300, OverlayMotion.lerp(300, 100, -1f))
    }

    @Test
    fun `edge is always a collapse and leaving the edge is always an expand`() {
        assertFalse(OverlayMode.EXPANDED.expandsTo(OverlayMode.EDGE))
        assertFalse(OverlayMode.PILL.expandsTo(OverlayMode.EDGE))
        assertTrue(OverlayMode.EDGE.expandsTo(OverlayMode.PILL))
        assertTrue(OverlayMode.EDGE.expandsTo(OverlayMode.EXPANDED))
        assertTrue(OverlayMode.PILL.expandsTo(OverlayMode.EXPANDED))
        assertFalse(OverlayMode.EXPANDED.expandsTo(OverlayMode.PILL))
    }
}
