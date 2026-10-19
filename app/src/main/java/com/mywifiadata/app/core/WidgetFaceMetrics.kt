package com.mywifiadata.app.core

/**
 * قياسات وجه أداة الساعة على الشاشة الرئيسية.
 *
 * الأداة ترسم وجهًا واحدًا بنفس نِسب بطاقة «ساعة الاستهلاك» داخل التطبيق
 * (قطر 252dp). يُرسم الوجه في فضاء تصميم ثابت ثم يُكبَّر بنسبة واحدة، فلا
 * تتضخم النقاط وحدها ولا تتراكب النصوص مع الحلقة عند مقاسات الأدوات المختلفة.
 */
object WidgetFaceMetrics {

    /** نصف قطر الوجه في فضاء التصميم؛ يطابق قطر 252dp في بطاقة التطبيق. */
    const val DESIGN_DIAL_RADIUS_DP = 126f

    /** حشوة الأداة من كل جهة (مطابقة لـ widget_usage.xml). */
    const val WIDGET_PADDING_DP = 14

    /** ارتفاع سطر «متبقٍ» وسطر التنزيل/الرفع أسفل القرص (ثابت في التخطيط). */
    const val BOTTOM_TEXT_BLOCK_DP = 46

    /** أقل ارتفاع لمساحة القرص؛ يمنع تصغيرًا يفقد النصوص وضوحها. */
    const val MIN_FRAME_HEIGHT_DP = 176

    /** أعلى ارتفاع للقرص؛ يحمي ذاكرة الصورة النقطية على الشاشات الكبيرة. */
    const val MAX_FRAME_HEIGHT_DP = 420

    /**
     * ارتفاع مساحة القرص بالـ dp انطلاقًا من ارتفاع الأداة وعرضها.
     *
     * الارتفاع الزائد يتّجه للقرص بدل أن يبقى فراغًا أسفل البطاقة، لكنه لا
     * يتجاوز العرض أبدًا: القرص محدود بالعرض أصلًا، وصورة أعرض نسبةً من
     * مساحتها تُصغَّر كاملة عند العرض فبدل أن تكبر تصغر.
     */
    fun frameHeightDp(widgetHeightDp: Int, frameWidthDp: Int): Int {
        val fromHeight = (widgetHeightDp - 2 * WIDGET_PADDING_DP - BOTTOM_TEXT_BLOCK_DP)
            .coerceIn(MIN_FRAME_HEIGHT_DP, MAX_FRAME_HEIGHT_DP)
        return minOf(fromHeight, frameWidthDp.coerceAtLeast(1))
    }

    /**
     * نسبة تكبير فضاء التصميم إلى مقاس القرص المرسوم.
     * تقبل نصف القطر بالبكسل والكثافة، وتتعامل مع القيم غير الصالحة بأمان.
     */
    fun clockScale(dialRadiusPx: Float, density: Float): Float {
        if (density <= 0f || dialRadiusPx <= 0f) return 0f
        return dialRadiusPx / (DESIGN_DIAL_RADIUS_DP * density)
    }

    /** أقصى عرض لصورة الوجه بالـ dp حمايةً لذاكرة الأداة على الشاشات الكبيرة. */
    const val MAX_BITMAP_WIDTH_DP = 480

    /**
     * يقصّ مقاس الصورة إلى حد آمن مع الحفاظ على نسبة الأبعاد نفسها، فتظل
     * الصورة مطابقة لمساحة العرض بعد تكبيرها بـ fitCenter.
     */
    fun boundedBitmapSize(
        widthDp: Int,
        heightDp: Int,
        maxWidthDp: Int = MAX_BITMAP_WIDTH_DP,
    ): Pair<Int, Int> {
        val width = widthDp.coerceAtLeast(1)
        val height = heightDp.coerceAtLeast(1)
        if (width <= maxWidthDp) return width to height
        val ratio = maxWidthDp.toFloat() / width
        return maxWidthDp to (height * ratio).toInt().coerceAtLeast(1)
    }
}
