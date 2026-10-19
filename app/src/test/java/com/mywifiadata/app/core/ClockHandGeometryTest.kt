package com.mywifiadata.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ClockHandGeometryTest {

    /** نص نسبة الاستهلاك حول المركز: من -60 إلى +60 أفقياً ومن -40 إلى +40 رأسياً. */
    private val centerBlock = HandObstacle(
        left = -60f,
        top = -40f,
        right = 60f,
        bottom = 40f,
    )

    /** عدد الأيام أعلى الميناء: من -40 إلى +40 أفقياً ومن -260 إلى -180 رأسياً. */
    private val daysBlock = HandObstacle(
        left = -40f,
        top = -260f,
        right = 40f,
        bottom = -180f,
    )

    @Test
    fun `hand pointing at empty area keeps its full length`() {
        val segment = ClockHandGeometry.visibleSegment(
            angleDeg = 180f,
            endRadius = 200f,
            obstacles = listOf(centerBlock, daysBlock),
            minLength = 3f,
        )

        assertNotNull(segment)
        // يبدأ من حافة النص المحيط بالمركز (النصف الأسفل) وينتهي بطوله الكامل.
        assertEquals(40f, segment!!.first, 0.01f)
        assertEquals(200f, segment.second, 0.01f)
    }

    @Test
    fun `hand pointing at the days label stops before it`() {
        val segment = ClockHandGeometry.visibleSegment(
            angleDeg = 0f,
            endRadius = 200f,
            obstacles = listOf(centerBlock, daysBlock),
            minLength = 3f,
        )

        assertNotNull(segment)
        assertEquals(40f, segment!!.first, 0.01f)
        // يتوقف عند أول حافة من جهة النص (بعد التوسيع) وليس عند نهاية العقرب.
        assertEquals(180f, segment.second, 0.01f)
    }

    @Test
    fun `hand fully hidden behind a text disappears`() {
        val coveringText = HandObstacle(
            left = -200f,
            top = -200f,
            right = 200f,
            bottom = 200f,
        )
        val segment = ClockHandGeometry.visibleSegment(
            angleDeg = 90f,
            endRadius = 150f,
            obstacles = listOf(coveringText),
            minLength = 3f,
        )

        assertNull(segment)
    }

    @Test
    fun `diagonal hand is clipped exactly at the rectangle corner`() {
        // بزاوية 135 درجة (أسفل يمين) يخرج الشعاع من المربع عند 50 ÷ cos45 ≈ 70.7.
        val square = HandObstacle(
            left = -50f,
            top = -50f,
            right = 50f,
            bottom = 50f,
        )
        val segment = ClockHandGeometry.visibleSegment(
            angleDeg = 135f,
            endRadius = 300f,
            obstacles = listOf(square),
            minLength = 3f,
        )

        assertNotNull(segment)
        assertEquals(70.71f, segment!!.first, 0.05f)
        assertEquals(300f, segment.second, 0.01f)
    }

    @Test
    fun `overlapping texts push the hand out instead of drawing over them`() {
        // نص يعلو المركز ونص آخر يتداخل معه: يتوقف العقرب بعد النصين معًا.
        val upperText = HandObstacle(left = -100f, top = -160f, right = 100f, bottom = -30f)
        val segment = ClockHandGeometry.visibleSegment(
            angleDeg = 0f,
            endRadius = 90f,
            obstacles = listOf(centerBlock, upperText),
            minLength = 3f,
        )

        assertNull(segment)
    }

    @Test
    fun `inflating an obstacle adds the gap on every side`() {
        val inflated = centerBlock.inflated(delta = 6f)

        assertEquals(-66f, inflated.left, 0.01f)
        assertEquals(-46f, inflated.top, 0.01f)
        assertEquals(66f, inflated.right, 0.01f)
        assertEquals(46f, inflated.bottom, 0.01f)
    }
}
