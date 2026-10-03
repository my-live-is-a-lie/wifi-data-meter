package com.mywifiadata.app.core

/**
 * مرصد زمني لاختبار السرعة: يحوّل «انقطاع الاتصال» و«مرور وقت طويل دون أي نتيجة»
 * إلى سبب فشل واضح، فيُغلق التحميل فورًا وتظهر رسالة مفهومة للمستخدم.
 *
 * المنطق نقي تمامًا (لا أندرويد ولا شبكة) ليعمل في اختبارات الوحدة.
 */
class SpeedTestWatchdog(
    private val startedAtMillis: Long,
    private val overallTimeoutMillis: Long = DEFAULT_OVERALL_TIMEOUT_MILLIS,
    private val stallTimeoutMillis: Long = DEFAULT_STALL_TIMEOUT_MILLIS,
) {

    /** لحظة آخر بيانات وصلت (تُحدَّث مع كل قراءة أو كتابة ناجحة). */
    var lastProgressMillis: Long = startedAtMillis
        private set

    /** تُنادى عند وصول بيانات جديدة. */
    fun onProgress(nowMillis: Long) {
        if (nowMillis > lastProgressMillis) lastProgressMillis = nowMillis
    }

    /**
     * سبب التوقف في هذه اللحظة، أو `null` إن كان الاختبار ما زال سليمًا.
     *
     * الترتيب مقصود: انقطاع الاتصال أولًا، ثم توقّف وصول البيانات (لا نتيجة
     * رغم أن الاتصال قائم)، ثم تجاوز السقف الزمني الكامل للاختبار.
     */
    fun check(nowMillis: Long, isOnline: Boolean): SpeedTestEngine.Failure? = when {
        !isOnline -> SpeedTestEngine.Failure.CONNECTION_LOST
        nowMillis - lastProgressMillis >= stallTimeoutMillis -> SpeedTestEngine.Failure.STALLED
        nowMillis - startedAtMillis >= overallTimeoutMillis -> SpeedTestEngine.Failure.TIMEOUT
        else -> null
    }

    companion object {
        /** إن لم تصل أي بيانات خلال هذه المدة يُعدّ التحميل متوقفًا. */
        const val DEFAULT_STALL_TIMEOUT_MILLIS = 12_000L

        /** سقف زمني كامل للاختبار؛ بعده يُغلق التحميل بلا نتيجة. */
        const val DEFAULT_OVERALL_TIMEOUT_MILLIS = 45_000L
    }
}
