package com.mywifiadata.app.core

import com.mywifiadata.app.data.model.SpeedTestRecord

/**
 * تقدير كمية البيانات التي يستهلكها اختبار السرعة، وقواعد عرض تحذير الاستهلاك
 * عند إجراء الاختبار على بيانات الهاتف.
 *
 * الاختبار يحمّل ويرفع بيانات حقيقية لقياس السرعة، لذلك قد يستهلك عشرات
 * الميجابايتات في الاختبار الواحد. يعرض التطبيق تحذيرًا قبل البدء حين تكون
 * الشبكة النشطة هي بيانات الهاتف، ويذكر تقديرًا متكيّفًا يُحسب من الاستهلاك
 * الفعلي للاختبارات السابقة بدل رقم ثابت.
 */
object SpeedTestDataUsage {

    /** عدد الاختبارات السابقة التي يدخل استهلاكها في التقدير (الأحدث أولًا). */
    const val SAMPLE_LIMIT = 5

    /** هامش أمان فوق متوسط الاستهلاك السابق لأن ظروف الشبكة قد تتحسن. */
    private const val SAFETY_MARGIN = 1.15

    /** تقدير أولي (60 ميجابايت) قبل إجراء أي اختبار على هذا الجهاز. */
    const val DEFAULT_ESTIMATE_BYTES = 60_000_000L

    /** حدود معقولة حتى لا يظهر تقدير صغير جدًا أو ضخم جدًا. */
    private const val MIN_ESTIMATE_BYTES = 5_000_000L
    private const val MAX_ESTIMATE_BYTES = 500_000_000L

    /** تقريب لأعلى إلى مضاعفات 5 ميجابايت ليكون الرقم سهل القراءة. */
    private const val ROUNDING_STEP_BYTES = 5_000_000L

    /**
     * مدة القياس المفترضة (ثوانٍ) عند تقدير استهلاك سجل قديم لا يحفظ كمية
     * البيانات: المعايرة + القياس الفعلي للتنزيل، ثم الرفع.
     */
    private const val DOWNLOAD_SECONDS = 15.0
    private const val UPLOAD_SECONDS = 6.0

    /** اتصالات التجهيز وجلب الروابط والبروتوكولات، تُضاف لكل سجل قديم. */
    private const val SETUP_BYTES = 2_000_000L

    /** مدة تجاهل التحذير بعد اختيار «تجاهل لمدة يوم»: يوم كامل ثم يعود. */
    const val WARNING_SNOOZE_MILLIS = 24L * 60L * 60L * 1_000L

    /**
     * تقدير الاستهلاك المتوقّع لاختبار واحد.
     *
     * @property bytes الكمية التقديرية بالبايت (مقرّبة لأعلى إلى 5 ميجابايت).
     * @property basedOnTests عدد الاختبارات السابقة التي بُني عليها التقدير،
     *  وصفر يعني أن [bytes] تقدير أولي لم يُضبط بعد.
     */
    data class Estimate(
        val bytes: Long,
        val basedOnTests: Int,
    ) {
        val isBasedOnHistory: Boolean get() = basedOnTests > 0
    }

    /**
     * يحسب التقدير من استهلاك أحدث [SAMPLE_LIMIT] اختبارات، مع وزن أعلى
     * للنتائج الأحدث ليتكيّف الرقم مع تغيّر سرعة الشبكة أو إعدادات الاختبار.
     *
     * السجلات التي لا تحمل كمية بيانات محفوظة (سُجّلت قبل إضافة هذا القياس)
     * يُقدَّر استهلاكها من سرعتي التنزيل والرفع المسجلتين فيها.
     */
    fun estimate(history: List<SpeedTestRecord>): Estimate {
        val samples = history
            .asSequence()
            .filter { it.timestampMillis > 0L }
            .sortedByDescending(SpeedTestRecord::timestampMillis)
            .take(SAMPLE_LIMIT)
            .map(::bytesFor)
            .filter { it > 0L }
            .toList()

        if (samples.isEmpty()) return Estimate(DEFAULT_ESTIMATE_BYTES, 0)

        var weightedBytes = 0.0
        var totalWeight = 0.0
        samples.forEachIndexed { index, bytes ->
            // الأحدث وزن أكبر: n، n-1، …، 1.
            val weight = (samples.size - index).toDouble()
            weightedBytes += bytes * weight
            totalWeight += weight
        }
        val averageWithMargin = weightedBytes / totalWeight * SAFETY_MARGIN
        return Estimate(normalize(averageWithMargin.toLong()), samples.size)
    }

    /** استهلاك سجل واحد: المقيس فعليًا إن وُجد، وإلا تقدير من سرعتيه. */
    private fun bytesFor(record: SpeedTestRecord): Long {
        if (record.dataBytes > 0L) return record.dataBytes

        val download = record.downloadBitsPerSecond / 8.0 * DOWNLOAD_SECONDS
        val upload = if (record.uploadMeasured) {
            record.uploadBitsPerSecond / 8.0 * UPLOAD_SECONDS
        } else {
            0.0
        }
        return (download + upload).toLong() + SETUP_BYTES
    }

    /** تقريب لأعلى إلى مضاعفات 5 ميجابايت داخل الحدود المسموحة. */
    private fun normalize(bytes: Long): Long {
        val clamped = bytes.coerceIn(MIN_ESTIMATE_BYTES, MAX_ESTIMATE_BYTES)
        return ((clamped + ROUNDING_STEP_BYTES - 1L) / ROUNDING_STEP_BYTES) * ROUNDING_STEP_BYTES
    }

    /**
     * هل يجب عرض التحذير الآن؟ يعود التحذير تلقائيًا بعد انقضاء اليوم الكامل
     * الذي اختاره المستخدم عند «التجاهل»، دون الحاجة إلى أي مؤقت في الخلفية.
     */
    fun shouldWarn(
        snoozedUntilMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean = snoozedUntilMillis <= nowMillis

    /** نهاية فترة تجاهل تبدأ الآن وتستمر يومًا كاملًا. */
    fun snoozeUntil(nowMillis: Long = System.currentTimeMillis()): Long =
        nowMillis + WARNING_SNOOZE_MILLIS
}
