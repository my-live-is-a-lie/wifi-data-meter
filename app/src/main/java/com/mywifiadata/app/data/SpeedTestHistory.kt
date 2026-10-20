package com.mywifiadata.app.data

import com.mywifiadata.app.data.model.SpeedTestRecord

/**
 * يحتفظ بأحدث عشر نتائج: النتيجة الأحدث في ملخص البطاقة وتسع نتائج سابقة
 * تظهر في السجل المطوي.
 */
const val SPEED_TEST_HISTORY_LIMIT = 10

/**
 * أدوات السجل منفصلة عن Android لتبقى قواعد الترتيب والتخزين قابلة للاختبار.
 */
internal object SpeedTestHistory {

    /** يضيف الاختبار في موضعه الزمني الأحدث، ويحذف أي نسخة مطابقة والنتائج الأقدم. */
    fun prepend(
        history: List<SpeedTestRecord>,
        record: SpeedTestRecord,
    ): List<SpeedTestRecord> {
        // الوقت نفسه يعني الاختبار نفسه، حتى لو اختلفت حقول القياس بين نسختين
        // (مثل سجل قديم بلا كمية بيانات محفوظة).
        val others = history.filterNot { previous ->
            previous == record || previous.timestampMillis == record.timestampMillis
        }
        return (listOf(record) + others)
            .sortedByDescending(SpeedTestRecord::timestampMillis)
            .take(SPEED_TEST_HISTORY_LIMIT)
    }

    /**
     * ترميز صغير ثابت لا يعتمد على مكتبة JSON: القيم أعداد خام معروفة بلا فواصل،
     * وتُفصل الحقول بفاصلة والسجلات بفاصلة منقوطة.
     *
     * الحقل السادس (كمية البيانات المستهلكة) أُضيف لاحقًا، لذلك يقبل [decode]
     * أيضًا السجلات القديمة المكوّنة من خمسة حقول ويعطيها كمية صفرًا.
     */
    fun encode(history: List<SpeedTestRecord>): String = history
        .take(SPEED_TEST_HISTORY_LIMIT)
        .joinToString(";") { record ->
            listOf(
                record.downloadBitsPerSecond.toRawBits(),
                record.uploadBitsPerSecond.toRawBits(),
                record.timestampMillis,
                if (record.usedFastCom) 1 else 0,
                if (record.uploadMeasured) 1 else 0,
                record.dataBytes.coerceAtLeast(0L),
            ).joinToString(",")
        }

    /** يتجاوز السجلات التالفة ويعيد ترتيب النتائج الأحدث أولًا. */
    fun decode(encoded: String): List<SpeedTestRecord> = encoded
        .split(';')
        .mapNotNull(::decodeRecord)
        .sortedByDescending(SpeedTestRecord::timestampMillis)
        .take(SPEED_TEST_HISTORY_LIMIT)

    private fun decodeRecord(encoded: String): SpeedTestRecord? {
        val values = encoded.split(',')
        if (values.size != 5 && values.size != 6) return null

        val downloadBits = values[0].toLongOrNull() ?: return null
        val uploadBits = values[1].toLongOrNull() ?: return null
        val timestamp = values[2].toLongOrNull()?.takeIf { it > 0L } ?: return null
        val usedFastCom = values[3].toBooleanInt() ?: return null
        val uploadMeasured = values[4].toBooleanInt() ?: return null
        val dataBytes = if (values.size == 6) {
            values[5].toLongOrNull()?.coerceAtLeast(0L) ?: return null
        } else {
            0L
        }
        val download = Double.fromBits(downloadBits)
        val upload = Double.fromBits(uploadBits)
        if (!download.isFinite() || download < 0.0 || !upload.isFinite() || upload < 0.0) return null

        return SpeedTestRecord(
            downloadBitsPerSecond = download,
            uploadBitsPerSecond = upload,
            timestampMillis = timestamp,
            usedFastCom = usedFastCom,
            uploadMeasured = uploadMeasured,
            dataBytes = dataBytes,
        )
    }

    private fun String.toBooleanInt(): Boolean? = when (this) {
        "1" -> true
        "0" -> false
        else -> null
    }
}
