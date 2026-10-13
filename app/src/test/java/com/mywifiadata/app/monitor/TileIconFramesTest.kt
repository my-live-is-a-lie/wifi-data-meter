package com.mywifiadata.app.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * اختبارات إطارات تحريك أيقونة البلاطة:
 * الدائرة تتقلص للداخل ثم تعود إلى مكانها، وشرطات الواي فاي تُرسَم من الشرطة السفلية
 * إلى العليا عند التشغيل، وبالعكس (العليا ثم السفلية) عند الإيقاف.
 */
class TileIconFramesTest {

    private val activation = List(TileIconFrames.ACTIVATION_FRAMES) { index ->
        TileIconFrames.activation(index.toFloat() / (TileIconFrames.ACTIVATION_FRAMES - 1))
    }

    private val deactivation = List(TileIconFrames.DEACTIVATION_FRAMES) { index ->
        TileIconFrames.deactivation(index.toFloat() / (TileIconFrames.DEACTIVATION_FRAMES - 1))
    }

    @Test
    fun `activation starts with the wifi bars hidden so they are redrawn`() {
        val first = TileIconFrames.activation(0f)
        assertEquals(1f, first.circleScale, EPSILON)
        assertEquals(0f, first.dotScale, EPSILON)
        assertEquals(0f, first.innerArc, EPSILON)
        assertEquals(0f, first.outerArc, EPSILON)
    }

    @Test
    fun `activation draws the lower bar then the upper one`() {
        val midway = TileIconFrames.activation(0.45f)
        assertTrue("النقطة تظهر قبل الشرطات", midway.dotScale > midway.innerArc)
        assertTrue("الشرطة السفلية تسبق العليا", midway.innerArc > midway.outerArc)

        val late = TileIconFrames.activation(0.6f)
        assertEquals(1f, late.innerArc, EPSILON)
        assertTrue(late.outerArc < 1f)
    }

    @Test
    fun `deactivation draws the upper bar first and the dot last`() {
        val midway = TileIconFrames.deactivation(0.35f)
        assertTrue("الشرطة العليا تسبق السفلية", midway.outerArc > midway.innerArc)
        assertTrue("النقطة آخر ما يظهر", midway.innerArc > midway.dotScale)
        assertEquals(0f, midway.dotScale, EPSILON)
    }

    @Test
    fun `circle shrinks inward then returns to its place with a small overshoot`() {
        val scales = activation.map { it.circleScale }
        val smallest = scales.min()
        val smallestIndex = scales.indexOf(smallest)

        assertTrue("تتقلص الدائرة للداخل", smallest < 0.8f)
        assertTrue("التقلص في النصف الأول من الحركة", smallestIndex in 1..(scales.size / 2))
        assertTrue("تتجاوز حجمها قليلًا قبل أن تستقر", scales.max() > 1f)
        assertEquals("تعود إلى مكانها", 1f, scales.last(), EPSILON)
    }

    @Test
    fun `deactivation is shorter and gentler than activation`() {
        assertTrue(TileIconFrames.DEACTIVATION_FRAMES < TileIconFrames.ACTIVATION_FRAMES)
        assertTrue(
            deactivation.minOf { it.circleScale } > activation.minOf { it.circleScale },
        )
    }

    @Test
    fun `animations are short enough to feel responsive`() {
        val activationMs = (TileIconFrames.ACTIVATION_FRAMES - 1) * TileIconFrames.FRAME_DELAY_MS
        val deactivationMs = (TileIconFrames.DEACTIVATION_FRAMES - 1) * TileIconFrames.FRAME_DELAY_MS
        assertTrue("حركة التشغيل أقل من نصف ثانية ($activationMs)", activationMs <= 500L)
        assertTrue("حركة الإيقاف أقصر ($deactivationMs)", deactivationMs <= 350L)
        assertTrue("إطار واحد على الأقل كل 24ms", TileIconFrames.FRAME_DELAY_MS >= 24L)
    }

    @Test
    fun `both animations end with the complete icon`() {
        assertFrameEquals(TileIconFrames.resting, TileIconFrames.activation(1f))
        assertFrameEquals(TileIconFrames.resting, TileIconFrames.deactivation(1f))
        assertFrameEquals(TileIconFrames.resting, activation.last())
        assertFrameEquals(TileIconFrames.resting, deactivation.last())
    }

    @Test
    fun `wifi mark stays inside the circle in every frame`() {
        (activation + deactivation).forEach { frame ->
            val circleInnerEdge =
                frame.circleScale * (TileIconFrames.CIRCLE_RADIUS - TileIconFrames.CIRCLE_STROKE / 2f)
            assertTrue(
                "علامة الواي فاي داخل الدائرة (${wifiExtent() * frame.glyphScale} < $circleInnerEdge)",
                wifiExtent() * frame.glyphScale < circleInnerEdge,
            )
        }
    }

    @Test
    fun `out of range progress is clamped to the animation bounds`() {
        assertFrameEquals(TileIconFrames.activation(0f), TileIconFrames.activation(-2f))
        assertFrameEquals(TileIconFrames.resting, TileIconFrames.activation(4f))
    }

    /** أبعد نقطة في علامة الواي فاي عن مركز الأيقونة (طرف القوس الأعلى مع الاستدارة). */
    private fun wifiExtent(): Float {
        val outerEdge = TileIconFrames.ARC_OUTER_RADIUS + TileIconFrames.ARC_OUTER_STROKE / 2f
        val halfSweep = Math.toRadians(TileIconFrames.ARC_SWEEP.toDouble() / 2f)
        val x = outerEdge * sin(halfSweep).toFloat()
        val y = TileIconFrames.GLYPH_CENTER_Y - outerEdge * cos(halfSweep).toFloat()
        return hypot(x, y - TileIconFrames.CENTER_Y)
    }

    private fun assertFrameEquals(expected: TileIconFrame, actual: TileIconFrame) {
        assertEquals(expected.circleScale, actual.circleScale, EPSILON)
        assertEquals(expected.glyphScale, actual.glyphScale, EPSILON)
        assertEquals(expected.dotScale, actual.dotScale, EPSILON)
        assertEquals(expected.innerArc, actual.innerArc, EPSILON)
        assertEquals(expected.outerArc, actual.outerArc, EPSILON)
    }

    private companion object {
        const val EPSILON = 0.0001f
    }
}
