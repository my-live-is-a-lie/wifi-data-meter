package com.mywifiadata.app.core

import com.mywifiadata.app.data.model.TrafficTotals

/** قراءة واحدة من إحصاءات الشبكة مع وقت أخذها (زمن النظام المحلي). */
data class UsageSample(
    val millis: Long,
    val totals: TrafficTotals,
)

/**
 * حساب استهلاك جلسة النافذة العائمة من قراءات **حقيقية** لإحصاءات النظام.
 *
 * لا تُولَّد أي أرقام هنا ولا يُضاف شيء كل ثانية: كل ما تفعله الدوال طرح قراءة
 * تراكمية من أخرى، وقسمة الفرق على الزمن الفعلي الذي مرّ بينهما. فصل الحساب عن
 * الخدمة يجعله قابلًا للاختبار بلا جهاز.
 */
object OverlaySampling {

    /**
     * نافذة قياس المعدّل: متوسط ما سجّله النظام فعلًا خلال آخر قراءتين (16 ثانية).
     * أندرويد لا يحدّث الإحصاءات للطرف الثالث أكثر من مرة كل 15 ثانية، فالمعدّل
     * الحقيقي أقرب ما يكون لمتوسط ما بين قراءتين، ويتراجع إلى الصفر عند توقف الحركة.
     */
    const val RATE_WINDOW_MILLIS = 16_000L

    /**
     * نافذة المعدّل اللحظي عند القراءة المباشرة من `TrafficStats` (كل ثانية):
     * متوسط آخر 3 ثوانٍ، فيتحرك الرقم كل ثانية دون أن يقفز بعنف.
     */
    const val LIVE_RATE_WINDOW_MILLIS = 3_000L

    /** أقل مدة تُقبل لحساب معدّل معقول (بايت/ثانية). */
    private const val MIN_RATE_MILLIS = 1_000L

    /**
     * استهلاك المقطع الحالي = القراءة الحالية − قراءة بداية المقطع.
     *
     * لا يقبل سالبًا: إحصاءات النظام قد تُعاد ضبطها (إعادة تشغيل الجهاز مثلًا)
     * فتُعامل القراءة الأصغر كبداية جديدة بدل عرض أرقام سالبة.
     */
    fun sessionTotals(startTotals: TrafficTotals, currentTotals: TrafficTotals): TrafficTotals =
        TrafficTotals(
            rxBytes = (currentTotals.rxBytes - startTotals.rxBytes).coerceAtLeast(0L),
            txBytes = (currentTotals.txBytes - startTotals.txBytes).coerceAtLeast(0L),
        )

    /**
     * الفرق بين قراءتين تراكميتين من العدّاد نفسه.
     * التراجع (إعادة تشغيل الجهاز أو تصفير العدّاد) يُعامل كصفر لا كرقم سالب.
     */
    fun delta(previous: TrafficTotals, current: TrafficTotals): TrafficTotals =
        TrafficTotals(
            rxBytes = (current.rxBytes - previous.rxBytes).coerceAtLeast(0L),
            txBytes = (current.txBytes - previous.txBytes).coerceAtLeast(0L),
        )

    /**
     * فرق استهلاك الواي فاي فقط = فرق إجمالي الجهاز − فرق بيانات الجوال.
     * نطرح **الفروق** لا القراءتين المطلقتين كي لا تنتج أرقام سالبة من اختلاف لحظة القراءة.
     */
    fun wifiDelta(
        previousTotal: TrafficTotals,
        currentTotal: TrafficTotals,
        previousMobile: TrafficTotals,
        currentMobile: TrafficTotals,
    ): TrafficTotals {
        val total = delta(previousTotal, currentTotal)
        val mobile = delta(previousMobile, currentMobile)
        return TrafficTotals(
            rxBytes = (total.rxBytes - mobile.rxBytes).coerceAtLeast(0L),
            txBytes = (total.txBytes - mobile.txBytes).coerceAtLeast(0L),
        )
    }

    /** بايت/ثانية بين قراءتين فعليتين؛ صفر إذا لم يمرّ وقت كافٍ أو تراجعت القراءة. */
    fun rate(first: UsageSample, last: UsageSample): TrafficTotalsRate {
        val elapsedMillis = last.millis - first.millis
        if (elapsedMillis < MIN_RATE_MILLIS) return TrafficTotalsRate()
        val seconds = elapsedMillis / 1000.0
        return TrafficTotalsRate(
            rxBytesPerSecond = (last.totals.rxBytes - first.totals.rxBytes).coerceAtLeast(0L) / seconds,
            txBytesPerSecond = (last.totals.txBytes - first.totals.txBytes).coerceAtLeast(0L) / seconds,
        )
    }

    /**
     * أقدم قراءة ما زالت داخل نافذة [RATE_WINDOW_MILLIS]، مع الاحتفاظ بقراءة أقدم
     * منها بقليل كي تُغطّى النافذة كاملة بدل أن تضيق مع كل قراءة جديدة.
     *
     * [samples] مرتّبة من الأقدم إلى الأحدث.
     */
    fun rateWindowStart(
        samples: List<UsageSample>,
        nowMillis: Long,
        windowMillis: Long = RATE_WINDOW_MILLIS,
    ): UsageSample? {
        if (samples.isEmpty()) return null
        val cutoff = nowMillis - windowMillis
        val index = samples.indexOfLast { it.millis <= cutoff }
        return when {
            index < 0 -> samples.first()
            index >= samples.lastIndex -> samples.last()
            else -> samples[index]
        }
    }
}

/** معدّل لحظي بالبايت في الثانية. */
data class TrafficTotalsRate(
    val rxBytesPerSecond: Double = 0.0,
    val txBytesPerSecond: Double = 0.0,
) {
    val totalBytesPerSecond: Double get() = rxBytesPerSecond + txBytesPerSecond
}
