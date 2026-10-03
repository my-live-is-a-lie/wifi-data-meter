package com.mywifiadata.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * اختبارات حسابيات مخطط الأيام وشريط تمرير اختيار اليوم:
 * أوزان الأعمدة (اتساع العمود المشار إليه وانكماش جيرانه) وتحويل موضع اللمس
 * إلى فهرس اليوم مع مراعاة اتجاه الواجهة من اليمين إلى اليسار.
 */
class DayScrubberMathTest {

    @Test
    fun `all bars share the same weight while the scrubber is idle`() {
        for (index in 0 until 30) {
            assertEquals(1f, barWeight(index = index, activeIndex = null), 0f)
        }
    }

    @Test
    fun `the active bar expands and its neighbours shrink gradually`() {
        val active = 10
        val expanded = barWeight(index = active, activeIndex = active)
        val first = barWeight(index = active + 1, activeIndex = active)
        val second = barWeight(index = active + 2, activeIndex = active)
        val third = barWeight(index = active + 3, activeIndex = active)
        val distant = barWeight(index = active + 4, activeIndex = active)

        assertEquals(1f + BAR_EXPANSION, expanded, 0.0001f)
        // الأقرب أكثر انكماشًا ثم يقل تدريجيًا: عدة أعمدة تنزاح لا عمود واحد.
        assertEquals(1f - BAR_NEIGHBOR_SHRINK, first, 0.0001f)
        assertEquals(true, second < 1f && second > first)
        assertEquals(true, third < 1f && third > second)
        assertEquals(1f, distant, 0f)
    }

    @Test
    fun `both sides of the active bar shift by the same gradient`() {
        val active = 15
        for (distance in 1..3) {
            assertEquals(
                barWeight(index = active - distance, activeIndex = active),
                barWeight(index = active + distance, activeIndex = active),
                0.0001f,
            )
        }
    }

    @Test
    fun `day labels next to the active day are hidden to avoid overlap`() {
        val count = 30
        val stride = 5
        val active = 20
        // تسمية اليوم المشار إليه تظهر دائمًا.
        assertEquals(true, showDayLabel(index = active, count = count, stride = stride, activeIndex = active))
        // جيرانه المباشرون يُخفون حتى لا يتراكبوا مع صندوقه العريض.
        assertEquals(false, showDayLabel(index = active - 1, count = count, stride = stride, activeIndex = active))
        assertEquals(false, showDayLabel(index = active + 1, count = count, stride = stride, activeIndex = active))
        // تسمية منتظمة بعيدة تظهر.
        assertEquals(true, showDayLabel(index = 0, count = count, stride = stride, activeIndex = active))
        // بلا مؤشر: التسميات المنتظمة فقط.
        assertEquals(true, showDayLabel(index = 10, count = count, stride = stride, activeIndex = null))
        assertEquals(false, showDayLabel(index = 11, count = count, stride = stride, activeIndex = null))
        assertEquals(true, showDayLabel(index = count - 1, count = count, stride = stride, activeIndex = null))
    }

    @Test
    fun `active day label is shown even when it is not on a stride position`() {
        assertEquals(true, showDayLabel(index = 17, count = 30, stride = 5, activeIndex = 17))
    }

    @Test
    fun `bar weights never collapse to zero or negative`() {
        for (index in 0 until 30) {
            val weight = barWeight(index = index, activeIndex = 29)
            assertEquals(true, weight > 0f)
        }
    }

    @Test
    fun `touching the far left selects the newest day in a right to left layout`() {
        // في التخطيط العربي (RTL) يبدأ اليوم الأقدم من اليمين، فاليوم الأحدث أقصى اليسار.
        assertEquals(29, scrubIndexAt(xPx = 0f, widthPx = 300f, count = 30, rtl = true))
        assertEquals(0, scrubIndexAt(xPx = 299f, widthPx = 300f, count = 30, rtl = true))
    }

    @Test
    fun `touching the far left selects the oldest day in a left to right layout`() {
        assertEquals(0, scrubIndexAt(xPx = 0f, widthPx = 300f, count = 30, rtl = false))
        assertEquals(29, scrubIndexAt(xPx = 299f, widthPx = 300f, count = 30, rtl = false))
    }

    @Test
    fun `touch positions map to the expected day in a weekly chart`() {
        // سبعة أيام بعرض 350: منتصف كل خانة (٥٠ بكسل) يخص يومها.
        val width = 350f
        val expectedRightToLeft = listOf(6, 5, 4, 3, 2, 1, 0)
        expectedRightToLeft.forEachIndexed { slot, expected ->
            val x = slot * 50f + 25f
            assertEquals(expected, scrubIndexAt(xPx = x, widthPx = width, count = 7, rtl = true))
        }
    }

    @Test
    fun `touch outside the track is clamped to the edge days`() {
        assertEquals(29, scrubIndexAt(xPx = -40f, widthPx = 300f, count = 30, rtl = true))
        assertEquals(0, scrubIndexAt(xPx = 999f, widthPx = 300f, count = 30, rtl = true))
    }

    @Test
    fun `a zero width track never crashes`() {
        assertEquals(0, scrubIndexAt(xPx = 10f, widthPx = 0f, count = 30, rtl = true))
    }

    @Test
    fun `spacing shrinks when many bars are shown`() {
        // ٧ أيام: 6dp بين الأعمدة، ٣٠ يومًا: 3dp — أي أن التباعد يضيق مع الزحام.
        assertEquals(true, chartSpacing(7) > chartSpacing(30))
        assertEquals(6f, chartSpacing(7).value, 0f)
        assertEquals(3f, chartSpacing(30).value, 0f)
    }
}
