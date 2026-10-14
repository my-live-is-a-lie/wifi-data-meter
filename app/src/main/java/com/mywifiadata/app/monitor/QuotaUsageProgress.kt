package com.mywifiadata.app.monitor

import java.util.Locale
import kotlin.math.roundToInt

/** النسبة النصية وكمية امتلاء الحلقة لأيقونة حالة الباقة. */
internal data class QuotaUsageProgress(
    /** النسبة المستهلكة المعروضة داخل الدائرة؛ تُحَدّ عند 999 لتناسب مساحة الأيقونة. */
    val percent: Int,
    /** مقدار امتلاء الحلقة بين 0 و1؛ تصبح كاملة عند بلوغ الباقة أو تجاوزها. */
    val fraction: Float,
)

/** حساب عرض مستقل عن Android لتسهيل التحقق من الحالات الحدّية. */
internal object QuotaUsageProgressCalculator {

    /**
     * يعيد null عندما لا توجد باقة. الاستخدام السالب يعامل كصفر، والحلقة تكتمل
     * عند استهلاك كامل الباقة بينما يظل الرقم يعرض النسبة الفعلية حتى 999.
     */
    fun calculate(usedBytes: Long, quotaBytes: Long): QuotaUsageProgress? {
        if (quotaBytes <= 0L) return null

        val rawFraction = usedBytes.coerceAtLeast(0L).toDouble() / quotaBytes.toDouble()
        val percent = (rawFraction * 100.0).roundToInt().coerceIn(0, MAX_DISPLAYED_PERCENT)
        return QuotaUsageProgress(
            percent = percent,
            fraction = rawFraction.coerceIn(0.0, 1.0).toFloat(),
        )
    }

    /** يستخدم الأرقام العربية في الواجهة العربية واللاتينية في الواجهة الإنجليزية. */
    fun formatDigits(number: Int, locale: Locale): String {
        val digits = number.coerceIn(0, MAX_DISPLAYED_PERCENT).toString()
        if (locale.language != "ar") return digits
        return buildString(digits.length) {
            digits.forEach { digit -> append(('٠'.code + (digit - '0')).toChar()) }
        }
    }

    private const val MAX_DISPLAYED_PERCENT = 999
}
