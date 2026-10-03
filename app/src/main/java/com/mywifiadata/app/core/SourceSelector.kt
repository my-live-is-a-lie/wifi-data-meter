package com.mywifiadata.app.core

/**
 * اختيار أفضل مصدر للقياس من نتائج المعايرة المتوازية.
 *
 * منفصل عن محرّك الشبكة ليكون نقيًا تمامًا وقابلًا لاختبارات الوحدة.
 */
internal object SourceSelector {

    /**
     * المصدر الفائز:
     * 1. المصدر المفضّل (fast.com) إن بلغ الحد الأدنى من البايتات.
     * 2. وإلا المصدر الأكثر بايتات، مع تفضيل ترتيب [order] عند التعادل.
     *
     * @return معرّف المصدر، أو null إن لم يقرأ أي مصدر أي بايت.
     */
    fun pick(
        order: List<String>,
        bytes: Map<String, Long>,
        minBytes: Long,
        preferredId: String? = null,
    ): String? {
        if (preferredId != null && (bytes[preferredId] ?: 0L) >= minBytes) return preferredId
        var best: String? = null
        var bestBytes = 0L
        order.forEach { id ->
            val value = bytes[id] ?: 0L
            if (value > bestBytes) {
                best = id
                bestBytes = value
            }
        }
        return best
    }

    /** هل بلغ أي مصدر غير المفضّل الحد الأدنى (لتحديد لحظة السماح بتخطي المفضّل). */
    fun anyOtherReached(
        order: List<String>,
        bytes: Map<String, Long>,
        minBytes: Long,
        preferredId: String?,
    ): Boolean = order.any { id ->
        id != preferredId && (bytes[id] ?: 0L) >= minBytes
    }
}
