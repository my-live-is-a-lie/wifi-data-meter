package com.mywifiadata.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `short hand covered by the center text is extended instead of disappearing`() {
        // نص عريض يغطي العقرب القصير كاملًا عند اتجاه الأسفل، فلا بد أن يظهر
        // طرفه خارج النص بدل أن يختفي أثناء دوران العقرب.
        val wideCenter = HandObstacle(left = -45f, top = -50f, right = 45f, bottom = 50f)
        val segment = ClockHandGeometry.visibleSegment(
            angleDeg = 90f,
            endRadius = 60f,
            obstacles = listOf(wideCenter),
            minLength = 3f,
            maxReach = 150f,
            minVisibleLength = 20f,
        )

        assertNotNull(segment)
        assertEquals(45f, segment!!.first, 0.01f)
        assertEquals(65f, segment.second, 0.01f)
    }

    @Test
    fun `extending the visible part never passes the maximum reach`() {
        val wideCenter = HandObstacle(left = -45f, top = -50f, right = 45f, bottom = 50f)
        val segment = ClockHandGeometry.visibleSegment(
            angleDeg = 90f,
            endRadius = 60f,
            obstacles = listOf(wideCenter),
            minLength = 3f,
            maxReach = 70f,
            minVisibleLength = 40f,
        )

        assertNotNull(segment)
        assertEquals(45f, segment!!.first, 0.01f)
        assertEquals(70f, segment.second, 0.01f)
    }

    @Test
    fun `hand shortened by an outer text is not stretched toward it`() {
        // عقرب يتوقف عند نص علوي قريب؛ لا نمدّه داخل النص بحجة أنه قصير.
        val nearOuterText = HandObstacle(left = -40f, top = -60f, right = 40f, bottom = -45f)
        val segment = ClockHandGeometry.visibleSegment(
            angleDeg = 0f,
            endRadius = 120f,
            obstacles = listOf(centerBlock, nearOuterText),
            minLength = 3f,
            maxReach = 150f,
            minVisibleLength = 30f,
        )

        assertNotNull(segment)
        assertEquals(40f, segment!!.first, 0.01f)
        assertEquals(45f, segment.second, 0.01f)
    }

    @Test
    fun `inflating an obstacle adds the gap on every side`() {
        val inflated = centerBlock.inflated(delta = 6f)

        assertEquals(-66f, inflated.left, 0.01f)
        assertEquals(-46f, inflated.top, 0.01f)
        assertEquals(66f, inflated.right, 0.01f)
        assertEquals(46f, inflated.bottom, 0.01f)
    }

    @Test
    fun `hand spring starts at the allowed length without animation`() {
        val spring = HandSpring()

        assertEquals(120f, spring.advance(120f, nowMillis = 0L), 0.01f)
    }

    @Test
    fun `hand spring contracts gradually instead of jumping`() {
        val spring = HandSpring()
        spring.advance(120f, nowMillis = 0L)

        val firstFrame = spring.advance(40f, nowMillis = 16L)

        // أول إطار من التقلّص يقتطع جزءًا صغيرًا فقط من الطول.
        assertTrue(firstFrame in 40f..120f)
        assertTrue(firstFrame > 100f)
    }

    @Test
    fun `hand spring never grows beyond the allowed length`() {
        val spring = HandSpring()
        spring.advance(20f, nowMillis = 0L)

        var now = 0L
        val lengths = buildList {
            repeat(40) {
                now += 16L
                add(spring.advance(100f, now))
            }
        }

        // يتمدد بسلاسة مع الوقت لكنه لا يتجاوز 100 في أي إطار.
        assertTrue(lengths.first() < 100f)
        assertTrue(lengths.all { it <= 100f })
        assertEquals(100f, lengths.last(), 1f)
    }

    @Test
    fun `hand spring settles back to the allowed length after contracting`() {
        val spring = HandSpring()
        spring.advance(120f, nowMillis = 0L)

        var now = 0L
        repeat(60) {
            now += 16L
            spring.advance(40f, now)
        }

        assertEquals(40f, spring.advance(40f, now + 16L), 1f)
    }

    @Test
    fun `short hand hidden inside the center block is extended outwards`() {
        val centerBlock = HandObstacle(left = -60f, top = -40f, right = 60f, bottom = 40f)

        val segment = ClockHandGeometry.visibleSegment(
            angleDeg = 180f,
            endRadius = 45f,
            obstacles = listOf(centerBlock),
            minLength = 1f,
            maxReach = 105f,
            minVisibleLength = 13.9f,
        )

        assertNotNull(segment)
        // يبدأ بعد النص ثم يُمدّ للخارج حتى يظهر بطول مرئي مقبول.
        assertEquals(40f, segment!!.first, 0.01f)
        assertEquals(53.9f, segment.second, 0.01f)
    }

    @Test
    fun `hand extension never passes the allowed reach`() {
        val centerBlock = HandObstacle(left = -60f, top = -40f, right = 60f, bottom = 40f)

        val segment = ClockHandGeometry.visibleSegment(
            angleDeg = 180f,
            endRadius = 45f,
            obstacles = listOf(centerBlock),
            minLength = 1f,
            maxReach = 60f,
            minVisibleLength = 100f,
        )

        assertEquals(40f, segment!!.first, 0.01f)
        assertEquals(60f, segment.second, 0.01f)
    }

    @Test
    fun `outer text stops the hand without extending it over the text`() {
        val centerBlock = HandObstacle(left = -60f, top = -40f, right = 60f, bottom = 40f)
        val outerText = HandObstacle(left = -30f, top = -100f, right = 30f, bottom = -70f)

        val segment = ClockHandGeometry.visibleSegment(
            angleDeg = 0f,
            endRadius = 120f,
            obstacles = listOf(centerBlock, outerText),
            minLength = 1f,
            maxReach = 140f,
            minVisibleLength = 80f,
        )

        assertEquals(40f, segment!!.first, 0.01f)
        assertEquals(70f, segment.second, 0.01f)
    }

    @Test
    fun `hand fully covered without reach allowance stays hidden as before`() {
        val centerBlock = HandObstacle(left = -60f, top = -40f, right = 60f, bottom = 40f)

        val segment = ClockHandGeometry.visibleSegment(
            angleDeg = 180f,
            endRadius = 35f,
            obstacles = listOf(centerBlock),
            minLength = 1f,
        )

        assertNull(segment)
    }
}
