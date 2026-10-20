package com.mywifiadata.app.core

import kotlin.math.roundToInt

/**
 * حركة النافذة العائمة: مُدد التحوّل بين الأشكال وتدرّجها.
 *
 * التكبير أبطأ من التصغير قصدًا: العين تتابع النمو خطوة بخطوة، أما التصغير
 * السريع فيجعل «الطيّ» إحساسًا فوريًا لا اختفاءً مفاجئًا.
 *
 * الأرقام هنا مشتركة بين طبقة العرض (Compose) والخدمة (WindowManager) كي يتحرّك
 * القياس والإزاحة في وقت واحد، فلا يسبق أحدهما الآخر فيبدو التحوّل مرتبكًا.
 */
object OverlayMotion {

    /** مدة تكبير الشكل: كبسولة ← بطاقة، أو لسان ← كبسولة. */
    const val EXPAND_MILLIS = 300L

    /** مدة تصغير الشكل: بطاقة ← كبسولة، أو كبسولة ← لسان. */
    const val COLLAPSE_MILLIS = 220L

    /** سقف المدة كي يبقى التحوّل إحساسًا فوريًا لا انتظارًا. */
    const val MAX_MILLIS = 400L

    /** المدة المناسبة بحسب اتجاه التحوّل. */
    fun durationMillis(expanding: Boolean): Long = if (expanding) EXPAND_MILLIS else COLLAPSE_MILLIS

    /**
     * تدرّج «تباطؤ»: يبدأ سريعًا ثم يهدأ قرب الهدف.
     * يُطبَّق على الإزاحة فقط؛ القياس يتدرّج بمنحنى Compose نفسه.
     */
    fun easeOut(fraction: Float): Float {
        val clamped = fraction.coerceIn(0f, 1f)
        val inverse = 1f - clamped
        return 1f - inverse * inverse * inverse
    }

    /** إزاحة خطية بين موضعين بالبكسل تُحسب في كل إطار من الانميشن. */
    fun lerp(start: Int, end: Int, fraction: Float): Int =
        (start + (end - start) * fraction.coerceIn(0f, 1f)).roundToInt()
}
