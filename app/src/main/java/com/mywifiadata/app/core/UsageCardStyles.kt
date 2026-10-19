package com.mywifiadata.app.core

/**
 * فهارس أنماط بطاقة الاستهلاك المحفوظة في الإعدادات.
 *
 * الأنماط المتاحة الآن: حلقة كاملة (٠)، حلقة منقّطة (١)، وساعة الاستهلاك (٢).
 * كان الفهرس ٢ سابقًا للنمط «حلقة مع تفاصيل جانبية» الذي أُزيل من قائمة
 * الاختيار، و٣ لنمط الساعة، لذلك تُرحَّل القيم القديمة مرة واحدة عبر [migrate]
 * كي يحتفظ مستخدمو الساعة بنمطهم ويعود مستخدمو النمط المحذوف إلى الحلقة.
 */
object UsageCardStyles {
    const val RING = 0
    const val DOTTED_RING = 1
    const val CLOCK = 2
    const val COUNT = 3

    /** نسخة مخطط الفهارس المحفوظة؛ تزيد عند حذف نمط أو إعادة ترتيبها. */
    const val LATEST_VERSION = 1

    /**
     * يحوّل فهرسًا محفوظًا بنسخة أقدم إلى الفهرس الحالي:
     * الساعة القديمة (٣) تصبح ٢، والنمط الجانبي المحذوف (٢) يعود للحلقة (٠).
     */
    fun migrate(storedIndex: Int, storedVersion: Int): Int {
        if (storedVersion >= LATEST_VERSION) {
            return storedIndex.coerceIn(RING, CLOCK)
        }
        val migrated = when (storedIndex) {
            3 -> CLOCK
            2 -> RING
            else -> storedIndex
        }
        return migrated.coerceIn(RING, CLOCK)
    }
}
