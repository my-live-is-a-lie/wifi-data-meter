package com.mywifiadata.app.core

import kotlin.math.abs
import kotlin.math.roundToInt

/** نتيجة إفلات النافذة العائمة: إغلاق، التصاق بحافة، أو البقاء حرّة. */
enum class OverlayDrop { NONE, DISMISS, EDGE_LEFT, EDGE_RIGHT }

/**
 * حسابات موضع النافذة العائمة: قصّها داخل الشاشة، ومناطق الإفلات
 * (الإغلاق في أسفل الوسط، والتحوّل إلى لسان عند الحافتين).
 *
 * منفصلة عن أندرويد لتُختبر محليًا بلا جهاز.
 */
object OverlayGeometry {

    /** ارتفاع الشاشة الذي تبدأ عنده منطقة الإغلاق (أسفل الوسط). */
    const val DISMISS_TOP_FRACTION = 0.82f

    /** نصف عرض منطقة الإغلاق حول منتصف الشاشة. */
    const val DISMISS_HALF_WIDTH_FRACTION = 0.24f

    /** نسبة عرض الشاشة عند الحافتين التي تتحوّل عندها النافذة إلى لسان. */
    const val EDGE_FRACTION = 0.10f

    /** أصغر مسافة أفقية داخلية تُرجع اللسان إلى نافذة صغيرة. */
    const val EDGE_RETURN_DP = 36f

    fun clampX(x: Int, width: Int, screenWidth: Int): Int =
        x.coerceIn(0, (screenWidth - width).coerceAtLeast(0))

    fun clampY(y: Int, height: Int, screenHeight: Int): Int =
        y.coerceIn(0, (screenHeight - height).coerceAtLeast(0))

    /** يحوّل مركز النافذة إلى نسبتَي موضع (0..1) تُحفظ في الإعدادات. */
    fun centerFractions(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        screenWidth: Int,
        screenHeight: Int,
    ): Pair<Float, Float> {
        val fractionX = if (screenWidth > 0) (x + width / 2f) / screenWidth else 0.5f
        val fractionY = if (screenHeight > 0) (y + height / 2f) / screenHeight else 0.5f
        return fractionX.coerceIn(0f, 1f) to fractionY.coerceIn(0f, 1f)
    }

    /** موضع أعلى النافذة انطلاقًا من نسبتَي مركزها. */
    fun topLeftFor(
        fractionX: Float,
        fractionY: Float,
        width: Int,
        height: Int,
        screenWidth: Int,
        screenHeight: Int,
    ): Pair<Int, Int> {
        val x = (fractionX.coerceIn(0f, 1f) * screenWidth - width / 2f).roundToInt()
        val y = (fractionY.coerceIn(0f, 1f) * screenHeight - height / 2f).roundToInt()
        return clampX(x, width, screenWidth) to clampY(y, height, screenHeight)
    }

    /**
     * يقرّر مصير النافذة عند الإفلات بحسب مركزها: أسفل الوسط = إغلاق،
     * قريب من الحافة = لسان جانبي، وإلا تبقى في مكانها.
     */
    fun dropTarget(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        screenWidth: Int,
        screenHeight: Int,
    ): OverlayDrop {
        if (screenWidth <= 0 || screenHeight <= 0) return OverlayDrop.NONE
        val centerX = x + width / 2f
        val centerY = y + height / 2f
        val bottomDismiss = centerY >= screenHeight * DISMISS_TOP_FRACTION &&
            abs(centerX - screenWidth / 2f) <= screenWidth * DISMISS_HALF_WIDTH_FRACTION
        if (bottomDismiss) return OverlayDrop.DISMISS
        // تُقاس الحافة بحرف النافذة نفسه لا بمركزها، كي يلتصق اللسان عند سحبها إلى الطرف.
        val edgeMargin = screenWidth * EDGE_FRACTION
        if (x <= edgeMargin) return OverlayDrop.EDGE_LEFT
        if (x + width >= screenWidth - edgeMargin) return OverlayDrop.EDGE_RIGHT
        return OverlayDrop.NONE
    }
}
