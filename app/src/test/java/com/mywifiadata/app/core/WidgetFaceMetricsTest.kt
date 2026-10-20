package com.mywifiadata.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetFaceMetricsTest {

    @Test
    fun `default widget keeps the minimum dial height`() {
        // 3×4 خلايا في الوضع الطولي: عرض داخلي 192dp وارتفاع 250dp.
        assertEquals(
            WidgetFaceMetrics.MIN_FRAME_HEIGHT_DP,
            WidgetFaceMetrics.frameHeightDp(widgetHeightDp = 250, frameWidthDp = 192),
        )
    }

    @Test
    fun `taller widgets grow the dial instead of leaving empty space`() {
        // 239×288dp كما في لقطة الهاتف: القرص يتّسع من 176dp إلى 211dp.
        assertEquals(211, WidgetFaceMetrics.frameHeightDp(widgetHeightDp = 288, frameWidthDp = 211))
        assertTrue(
            WidgetFaceMetrics.frameHeightDp(288, 211) > WidgetFaceMetrics.frameHeightDp(250, 192),
        )
    }

    @Test
    fun `dial height never exceeds the widget width`() {
        // القرص محدود بالعرض، وارتفاع أكبر من العرض لا يكبّر الصورة بل يصغّرها.
        assertEquals(220, WidgetFaceMetrics.frameHeightDp(widgetHeightDp = 900, frameWidthDp = 220))
        assertEquals(140, WidgetFaceMetrics.frameHeightDp(widgetHeightDp = 250, frameWidthDp = 140))
    }

    @Test
    fun `dial height stays inside the allowed range`() {
        assertEquals(
            WidgetFaceMetrics.MIN_FRAME_HEIGHT_DP,
            WidgetFaceMetrics.frameHeightDp(100, 960),
        )
        assertEquals(
            WidgetFaceMetrics.MAX_FRAME_HEIGHT_DP,
            WidgetFaceMetrics.frameHeightDp(2_000, 960),
        )
    }

    @Test
    fun `clock scale matches the in-app card design`() {
        // قطر 252dp بكثافة 3 يعطي نسبة 1 تمامًا.
        assertEquals(1f, WidgetFaceMetrics.clockScale(378f, 3f), 0.0001f)
        // القرص 211dp على شاشة كثافتها 3 يعطي 211 ÷ 252.
        assertEquals(0.837f, WidgetFaceMetrics.clockScale(316.5f, 3f), 0.001f)
    }

    @Test
    fun `oversized widgets keep their aspect ratio inside the bitmap limit`() {
        val (width, height) = WidgetFaceMetrics.boundedBitmapSize(600, 300)

        assertEquals(WidgetFaceMetrics.MAX_BITMAP_WIDTH_DP, width)
        assertEquals(240, height)
        // الصورة الناتجة تحمل النسبة نفسها فتظهر بلا تشويه بعد fitCenter.
        assertEquals(2f, width.toFloat() / height, 0.01f)
    }

    @Test
    fun `normal widget sizes are left unchanged`() {
        assertEquals(211 to 211, WidgetFaceMetrics.boundedBitmapSize(211, 211))
    }

    @Test
    fun `invalid inputs never produce a broken scale`() {
        assertEquals(0f, WidgetFaceMetrics.clockScale(0f, 3f), 0f)
        assertEquals(0f, WidgetFaceMetrics.clockScale(300f, 0f), 0f)
        assertEquals(0f, WidgetFaceMetrics.clockScale(-5f, 3f), 0f)
    }
}
