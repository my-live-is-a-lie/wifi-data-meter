package com.mywifiadata.app.core

/**
 * مدة الاحتفاظ باستهلاك التطبيقات المؤرشف (السجل المحلي).
 *
 * القيمة عدد الأشهر، و[UNLIMITED] تعني «لا محدود» أي عدم الحذف. تُحسب المدة بأشهر من 30 يومًا
 * لأن الغرض تقريبي (كم من الوقت يبقى السجل متاحًا)، لا محاسبة تواريخ دقيقة.
 */
object HistoryRetention {
    const val UNLIMITED = 0

    /** الخيار الافتراضي: شهر واحد، وهو أول خيار في القائمة. */
    const val DEFAULT_MONTHS = 1

    private const val MONTH_DAYS = 30L
    private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L

    /** خيارات القائمة بالترتيب: شهر حتى 11 شهرًا، ثم سنوات، ثم بلا حد. */
    val options: List<Int> = listOf(
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        12, 24, 36,
        UNLIMITED,
    )

    /** يعيد قيمة صالحة من القائمة، وأول خيار عند أي قيمة غير معروفة. */
    fun normalize(months: Int): Int = if (months in options) months else DEFAULT_MONTHS

    /** مدة الاحتفاظ بالمللي ثانية، وصفر يعني بلا حد. */
    fun retentionMillis(months: Int): Long {
        val safeMonths = normalize(months)
        if (safeMonths == UNLIMITED) return 0L
        return safeMonths * MONTH_DAYS * DAY_MILLIS
    }
}
