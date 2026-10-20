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
     * نافذة قياس المعدّل اللحظي: متوسط ما سجّله النظام فعلًا خلال آخر 10 ثوانٍ.
     * أطول من فترة القراءة (ثانيتين) كي لا يظهر المعدّل صفرًا بين تحديثات النظام،
     * ويتراجع تدريجيًا إلى الصفر حين تتوقف الحركة فعلًا.
     */
    const val RATE_WINDOW_MILLIS = 10_000L

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
    fun rateWindowStart(samples: List<UsageSample>, nowMillis: Long): UsageSample? {
        if (samples.isEmpty()) return null
        val cutoff = nowMillis - RATE_WINDOW_MILLIS
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
