package com.mywifiadata.app.core

/** وحدة فترة أخذ عينة المراقبة. */
enum class SamplingIntervalUnit(val millisecondsPerUnit: Long) {
    SECOND(1_000L),
    MINUTE(60_000L),
    HOUR(3_600_000L),
}

/** خيار واحد في عجلة فترات القياس؛ [halfUnits] يسمح بأنصاف الوحدات. */
data class SamplingIntervalOption(
    val halfUnits: Int,
    val unit: SamplingIntervalUnit,
) {
    val durationMillis: Long
        get() = (unit.millisecondsPerUnit / 2L) * halfUnits

    val amountLabel: String
        get() = if (halfUnits % 2 == 0) {
            (halfUnits / 2).toString()
        } else {
            "${halfUnits / 2}.5"
        }
}

/**
 * الخيارات مرتبة كما تظهر في العجلة: ثوانٍ، ثم دقائق، ثم ساعات، وتُعاد من
 * البداية بعد آخر خيار. يخزَّن الفهرس بدل المدة حتى تبقى التسميات المكررة
 * مثل «60 ث» و«1 د» اختيارين مستقلين.
 */
object SamplingIntervals {
    private val secondHalfUnits = listOf(1, 2, 3, 4, 5, 6, 8, 10, 20, 30, 40, 50, 60, 80, 100, 120)
    private val minuteHalfUnits = listOf(2, 3, 4, 5, 6, 8, 10, 20, 30, 40, 50, 60, 80, 100, 120)
    private val hourHalfUnits = listOf(2, 3, 4, 5, 6)

    val options: List<SamplingIntervalOption> = buildList {
        secondHalfUnits.forEach { add(SamplingIntervalOption(it, SamplingIntervalUnit.SECOND)) }
        minuteHalfUnits.forEach { add(SamplingIntervalOption(it, SamplingIntervalUnit.MINUTE)) }
        hourHalfUnits.forEach { add(SamplingIntervalOption(it, SamplingIntervalUnit.HOUR)) }
    }

    /** 15 ثانية، حفاظًا على الإعداد الافتراضي السابق للتطبيق. */
    const val DEFAULT_INDEX = 9

    fun coerceIndex(index: Int): Int = index.coerceIn(0, options.lastIndex)

    /** أقرب خيار للترقية من إعداد الفاصل القديم المخزّن بالثواني. */
    fun closestIndex(durationMillis: Long): Int = options.indices.minByOrNull { index ->
        kotlin.math.abs(options[index].durationMillis - durationMillis)
    } ?: DEFAULT_INDEX
}
