package com.mywifiadata.app.monitor

/**
 * إطارات تحريك أيقونة بلاطة الإعدادات السريعة.
 *
 * النظام لا يبدأ تلقائيًا أي حركة داخل أيقونة البلاطة (ولا يعرض إلا الأيقونة التي
 * يرسلها التطبيق)، لذا نرسم كل إطار كصورة مستقلة ونحدّث الأيقونة بها إطارًا إطارًا؛
 * بهذا تعمل الحركة على كل نسخ أندرويد وكل واجهات الشركات المصنّعة.
 *
 * الحركة عند التشغيل: الدائرة تتقلص للداخل ثم تعود إلى مكانها (مع تجاوز بسيط)،
 * وشرطات الواي فاي تُرسَم من الشرطة السفلية (الأصغر) إلى الشرطة العليا.
 * الحركة عند الإيقاف: نفس نبضة الدائرة لكن أقصر، والشرطات تُرسَم بالعكس
 * (العليا ثم السفلية ثم النقطة).
 *
 * الأبعاد بوحدات dp داخل مربع 24dp، وهي مطابقة تمامًا لأبعاد الأيقونة المتجهة الثابتة
 * `res/drawable/ic_network_monitor.xml` حتى لا يحدث أي قفز عند انتهاء الحركة.
 */
internal object TileIconFrames {

    /** عدد إطارات حركة التشغيل (14 إطارًا × 30ms ≈ 390ms). */
    const val ACTIVATION_FRAMES = 14

    /** عدد إطارات حركة الإيقاف — أقصر (10 إطارات × 30ms ≈ 270ms). */
    const val DEACTIVATION_FRAMES = 10

    /**
     * المهلة بين إطارين: 30ms ≈ 33 إطارًا في الثانية.
     * أقل من ذلك يرهق النظام بطلبات تحديث متلاحقة للبلاطة بلا فرق يُذكر.
     */
    const val FRAME_DELAY_MS = 30L

    // ——————————————————————————— الأبعاد (dp داخل مربع 24dp) ———————————————————————————

    /** طول ضلع مربع الأيقونة (يستخدم أيضًا لتحويل dp إلى بكسل عند الرسم). */
    const val VIEWPORT = 24f

    const val CENTER_X = 12f
    const val CENTER_Y = 12f
    const val CIRCLE_RADIUS = 10.2f
    const val CIRCLE_STROKE = 1.75f

    /** مركز علامة الواي فاي (النقطة)؛ الأقواس مرسومة حوله. */
    const val GLYPH_CENTER_X = 12f
    const val GLYPH_CENTER_Y = 15.4f
    const val DOT_RADIUS = 1.65f
    const val ARC_INNER_RADIUS = 4.1f
    const val ARC_INNER_STROKE = 2f
    const val ARC_OUTER_RADIUS = 7.5f
    const val ARC_OUTER_STROKE = 2.1f

    /** زاوية القوس الكاملة بالدرجات (100° = ±50° حول الأعلى). */
    const val ARC_SWEEP = 100f

    /** زاوية الأعلى في نظام رسم أندرويد (0° = اليمين، وتزيد مع عقارب الساعة). */
    const val ARC_TOP = 270f

    // ——————————————————————————— إعدادات الحركة ———————————————————————————

    private const val ACTIVATION_SHRINK = 0.26f
    private const val ACTIVATION_SHRINK_END = 0.32f
    private const val DEACTIVATION_SHRINK = 0.15f
    private const val DEACTIVATION_SHRINK_END = 0.30f

    /** نسبة تقلص علامة الواي فاي مقابل تقلص الدائرة حتى تبقى داخلها. */
    private const val GLYPH_FOLLOW = 0.55f

    /** الإطار الساكن: الأيقونة كاملة كما تظهر في البلاطة قبل الحركة وبعدها. */
    val resting: TileIconFrame = TileIconFrame(
        circleScale = 1f,
        glyphScale = 1f,
        dotScale = 1f,
        innerArc = 1f,
        outerArc = 1f,
    )

    /** إطار حركة التشغيل عند التقدّم [progress] (0 → 1). */
    fun activation(progress: Float): TileIconFrame = frame(progress, activating = true)

    /** إطار حركة الإيقاف عند التقدّم [progress] (0 → 1). */
    fun deactivation(progress: Float): TileIconFrame = frame(progress, activating = false)

    private fun frame(progress: Float, activating: Boolean): TileIconFrame {
        val t = progress.coerceIn(0f, 1f)
        val circle = if (activating) {
            pulse(t, ACTIVATION_SHRINK, ACTIVATION_SHRINK_END)
        } else {
            pulse(t, DEACTIVATION_SHRINK, DEACTIVATION_SHRINK_END)
        }
        return TileIconFrame(
            circleScale = circle,
            glyphScale = 1f - GLYPH_FOLLOW * (1f - circle),
            // التشغيل: النقطة ثم الشرطة السفلية ثم العليا — الإيقاف: بالعكس.
            dotScale = reveal(t, if (activating) 0f else 0.58f, if (activating) 0.18f else 0.94f),
            innerArc = reveal(t, if (activating) 0.12f else 0.28f, if (activating) 0.48f else 0.70f),
            outerArc = reveal(t, if (activating) 0.32f else 0.03f, if (activating) 0.86f else 0.42f),
        )
    }

    /**
     * نبضة الدائرة: تتقلص إلى [shrink] من حجمها حتى اللحظة [shrinkEnd]، ثم تعود إلى
     * حجمها الأصلي مع تجاوز بسيط (overshoot) يمنح الإحساس بالارتداد.
     */
    private fun pulse(t: Float, shrink: Float, shrinkEnd: Float): Float {
        val smallest = 1f - shrink
        return if (t <= shrinkEnd) {
            1f - shrink * easeInOut(t / shrinkEnd)
        } else {
            smallest + shrink * easeOutBack((t - shrinkEnd) / (1f - shrinkEnd))
        }
    }

    /** انكشاف تدريجي لقوس أو نقطة بين لحظتين، ينتهي مكتملًا دائمًا. */
    private fun reveal(t: Float, start: Float, end: Float): Float =
        easeInOut(((t - start) / (end - start)).coerceIn(0f, 1f))

    private fun easeInOut(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** انطلاق سريع ثم هدوء مع تجاوز طفيف للنهاية (easeOutBack). */
    private fun easeOutBack(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        val u = t - 1f
        return 1f + 2.70158f * u * u * u + 1.70158f * u * u
    }
}

/** حالة رسم أيقونة البلاطة في لحظة واحدة. */
internal data class TileIconFrame(
    /** مقياس الدائرة (1 = الحجم الطبيعي). */
    val circleScale: Float,
    /** مقياس علامة الواي فاي كاملة. */
    val glyphScale: Float,
    /** مقياس النقطة (0 = مخفية). */
    val dotScale: Float,
    /** نسبة انكشاف الشرطة السفلية (القوس الأدنى) من 0 إلى 1. */
    val innerArc: Float,
    /** نسبة انكشاف الشرطة العليا (القوس الأعلى) من 0 إلى 1. */
    val outerArc: Float,
)
