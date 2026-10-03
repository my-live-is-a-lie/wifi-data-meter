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
            assertEquals(1f, barWeight(index = index, activeIndex = null, count = 30), 0f)
        }
    }

    @Test
    fun `the active bar expands`() {
        assertEquals(
            ACTIVE_BAR_WEIGHT,
            barWeight(index = 10, activeIndex = 10, count = 30),
            0.0001f,
        )
    }

    @Test
    fun `every other bar shrinks so they all shift, not only the neighbours`() {
        val active = 10
        val count = 30
        val neighbour = barWeight(index = active + 1, activeIndex = active, count = count)
        val far = barWeight(index = 0, activeIndex = active, count = count)
        val farRight = barWeight(index = count - 1, activeIndex = active, count = count)

        // كل الأعمدة غير النشطة تتقلّص بنفس المقدار (فتنزاح جميعها) لا عمود واحد.
        assertEquals(neighbour, far, 0.0001f)
        assertEquals(neighbour, farRight, 0.0001f)
        assertEquals(true, neighbour < 1f)
    }

    @Test
    fun `the total weight stays constant so the chart width never changes`() {
        for (count in listOf(7, 14, 30)) {
            val total = (0 until count).sumOf { index ->
                barWeight(index = index, activeIndex = count / 2, count = count).toDouble()
            }
            assertEquals(count.toDouble(), total, 0.0001)
        }
    }

    @Test
    fun `few bars never collapse to zero width`() {
        for (count in 2..5) {
            for (index in 0 until count) {
                val weight = barWeight(index = index, activeIndex = 0, count = count)
                assertEquals(true, weight >= MIN_OTHERS_WEIGHT)
                assertEquals(true, weight > 0f)
            }
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
            val weight = barWeight(index = index, activeIndex = 29, count = 30)
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
