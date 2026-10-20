package com.mywifiadata.app.data

/**
 * منظّم قراءات `NetworkStatsManager` على مستوى العملية.
 *
 * أندرويد لا يحدّث إحصاءات الشبكة إلا عند «فتح جلسة» جديدة، وبشرط أن يمرّ
 * **15 ثانية** على آخر نداء (`POLL_RATE_LIMIT_MS` في `NetworkStatsService`).
 * أي قراءة أسرع من ذلك تعيد ضبط المهلة ولا تفرض تحديثًا، فتعود الأرقام نفسها
 * مهما تكرّرت القراءة — وهذا سبب بقاء العدّادات ثابتة/صفرًا.
 *
 * لذلك تمرّ كل القراءات الدورية في التطبيق (لوحة المعلومات والنافذة العائمة)
 * من هنا: قراءة واحدة كل [MIN_INTERVAL_MILLIS] على الأكثر، ومن تُرفض قراءته
 * يعيد استخدام آخر قيمة معروضة ويعيد المحاولة في الدورة التالية.
 */
object NetworkStatsGate {

    /** 15 ثانية في النظام + ثانية هامش. */
    const val MIN_INTERVAL_MILLIS = 16_000L

    private var lastAcquireMillis = 0L

    @Synchronized
    fun tryAcquire(now: Long = System.currentTimeMillis()): Boolean {
        if (now - lastAcquireMillis < MIN_INTERVAL_MILLIS) return false
        lastAcquireMillis = now
        return true
    }

    /** للاختبارات فقط: تُعيد البوابة إلى حالتها الأولى. */
    internal fun reset() {
        lastAcquireMillis = 0L
    }
}
