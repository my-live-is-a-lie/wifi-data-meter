package com.mywifiadata.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** حسابات موضع النافذة العائمة: القصّ داخل الشاشة، والإغلاق، والالتصاق بالحافة. */
class OverlayGeometryTest {

    private val screenWidth = 1080
    private val screenHeight = 2400
    private val pillWidth = 300
    private val pillHeight = 120

    @Test
    fun `dropping the pill over the bottom centre requests dismissal`() {
        val x = (screenWidth - pillWidth) / 2
        val y = screenHeight - pillHeight - 20
        assertEquals(
            OverlayDrop.DISMISS,
            OverlayGeometry.dropTarget(x, y, pillWidth, pillHeight, screenWidth, screenHeight),
        )
    }

    @Test
    fun `dropping near an edge asks for the matching side tab`() {
        assertEquals(
            OverlayDrop.EDGE_LEFT,
            OverlayGeometry.dropTarget(0, 300, pillWidth, pillHeight, screenWidth, screenHeight),
        )
        assertEquals(
            OverlayDrop.EDGE_RIGHT,
            OverlayGeometry.dropTarget(
                screenWidth - pillWidth,
                300,
                pillWidth,
                pillHeight,
                screenWidth,
                screenHeight,
            ),
        )
    }

    @Test
    fun `a pill dropped in the middle stays free`() {
        assertEquals(
            OverlayDrop.NONE,
            OverlayGeometry.dropTarget(400, 600, pillWidth, pillHeight, screenWidth, screenHeight),
        )
    }

    @Test
    fun `bottom corners snap to the edge instead of closing`() {
        // أسفل الشاشة لكن بعيدًا عن الوسط: لا يُغلق، بل يلتصق بالحافة.
        assertEquals(
            OverlayDrop.EDGE_LEFT,
            OverlayGeometry.dropTarget(0, screenHeight - pillHeight, pillWidth, pillHeight, screenWidth, screenHeight),
        )
    }

    @Test
    fun `positions are clamped inside the screen`() {
        assertEquals(0, OverlayGeometry.clampX(-80, pillWidth, screenWidth))
        assertEquals(screenWidth - pillWidth, OverlayGeometry.clampX(screenWidth, pillWidth, screenWidth))
        assertEquals(0, OverlayGeometry.clampY(-10, pillHeight, screenHeight))
        assertEquals(screenHeight - pillHeight, OverlayGeometry.clampY(screenHeight + 50, pillHeight, screenHeight))
    }

    @Test
    fun `window larger than the screen is pinned to the top start`() {
        assertEquals(0, OverlayGeometry.clampX(120, 2000, screenWidth))
        assertEquals(0, OverlayGeometry.clampY(120, 3000, screenHeight))
    }

    @Test
    fun `centre fractions survive the trip back to pixels`() {
        val (fractionX, fractionY) = OverlayGeometry.centerFractions(
            x = 390,
            y = 1140,
            width = pillWidth,
            height = pillHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
        )
        assertEquals(0.5f, fractionX, 0.001f)
        assertEquals(0.5f, fractionY, 0.001f)
        val (x, y) = OverlayGeometry.topLeftFor(
            fractionX,
            fractionY,
            pillWidth,
            pillHeight,
            screenWidth,
            screenHeight,
        )
        assertEquals(390, x)
        assertEquals(1140, y)
    }

    @Test
    fun `the edge tab sticks to its screen side in portrait and in landscape`() {
        // موضع اللسان يُحسب من عرض الشاشة الحالي لا من إحداثي محفوظ، فيبقى على
        // الحافة بعد الدوران بدل أن يبقى عند إحداثي الوضع الرأسي (منتصف الأفقي).
        val tabWidth = 140
        assertEquals(0, OverlayGeometry.edgeX(atLeftEdge = true, tabWidth, screenWidth))
        assertEquals(screenWidth - tabWidth, OverlayGeometry.edgeX(atLeftEdge = false, tabWidth, screenWidth))

        val landscapeWidth = screenHeight // بعد الدوران يتبدّل العرض بالارتفاع
        assertEquals(0, OverlayGeometry.edgeX(atLeftEdge = true, tabWidth, landscapeWidth))
        assertEquals(landscapeWidth - tabWidth, OverlayGeometry.edgeX(atLeftEdge = false, tabWidth, landscapeWidth))
    }

    @Test
    fun `the edge tab never leaves the screen`() {
        assertEquals(0, OverlayGeometry.edgeX(atLeftEdge = false, screenWidth + 400, screenWidth))
        assertEquals(0, OverlayGeometry.edgeX(atLeftEdge = true, screenWidth + 400, screenWidth))
    }

    @Test
    fun `top left placement is clamped near the borders`() {
        val (x, y) = OverlayGeometry.topLeftFor(1f, 1f, pillWidth, pillHeight, screenWidth, screenHeight)
        assertEquals(screenWidth - pillWidth, x)
        assertEquals(screenHeight - pillHeight, y)
    }
}
