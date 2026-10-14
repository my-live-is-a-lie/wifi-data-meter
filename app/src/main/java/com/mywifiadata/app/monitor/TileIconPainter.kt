package com.mywifiadata.app.monitor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Icon
import kotlin.math.roundToInt

/**
 * رسم إطار أيقونة البلاطة.
 *
 * الأيقونة تُرسَل إلى النظام كصورة نقطية لأن بلاطات الإعدادات السريعة لا تُشغّل
 * الرسوم المتحركة تلقائيًا؛ وكل إطار يُرسم عند طلبه فقط (صورة واحدة في الذاكرة).
 * الألوان كلها أبيض: النظام يلوّن أيقونة البلاطة بلون واحد حسب حالة البلاطة.
 */

/** مضاعف الدقة: نرسم بثلاثة أمثال حجم العرض المتوقع حتى تبدو الخطوط حادة. */
private const val SUPERSAMPLE = 3f

/** يحول الإطار إلى أيقونة تُرسل إلى بلاطة الإعدادات السريعة. */
internal fun TileIconFrame.toIcon(context: Context): Icon {
    val size = iconBitmapSize(context)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    drawOn(Canvas(bitmap), size / TileIconFrames.VIEWPORT)
    return Icon.createWithBitmap(bitmap)
}

private fun iconBitmapSize(context: Context): Int {
    val density = context.resources.displayMetrics.density
    return (TileIconFrames.VIEWPORT * density * SUPERSAMPLE).roundToInt().coerceIn(96, 512)
}

private fun TileIconFrame.drawOn(canvas: Canvas, px: Float) {
    // علامة الواي فاي تصغر حول مركز الأيقونة فتبقى في وسط الدائرة المتقلّصة.
    val glyphX = TileIconFrames.CENTER_X +
        (TileIconFrames.GLYPH_CENTER_X - TileIconFrames.CENTER_X) * glyphScale
    val glyphY = TileIconFrames.CENTER_Y +
        (TileIconFrames.GLYPH_CENTER_Y - TileIconFrames.CENTER_Y) * glyphScale

    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = TileIconFrames.CIRCLE_STROKE * circleScale * px
    }
    canvas.drawCircle(
        TileIconFrames.CENTER_X * px,
        TileIconFrames.CENTER_Y * px,
        TileIconFrames.CIRCLE_RADIUS * circleScale * px,
        ring,
    )

    val arcs = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }
    drawArc(
        canvas = canvas,
        px = px,
        paint = arcs,
        glyphScale = glyphScale,
        centerX = glyphX,
        centerY = glyphY,
        radius = TileIconFrames.ARC_INNER_RADIUS,
        stroke = TileIconFrames.ARC_INNER_STROKE,
        progress = innerArc,
    )
    drawArc(
        canvas = canvas,
        px = px,
        paint = arcs,
        glyphScale = glyphScale,
        centerX = glyphX,
        centerY = glyphY,
        radius = TileIconFrames.ARC_OUTER_RADIUS,
        stroke = TileIconFrames.ARC_OUTER_STROKE,
        progress = outerArc,
    )

    if (dotScale > 0.001f) {
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawCircle(
            glyphX * px,
            glyphY * px,
            TileIconFrames.DOT_RADIUS * dotScale * glyphScale * px,
            dot,
        )
    }
}

/** كل قوس ينكشف من منتصفه إلى طرفيه في نفس الوقت (كانتشار الإشارة). */
private fun drawArc(
    canvas: Canvas,
    px: Float,
    paint: Paint,
    glyphScale: Float,
    centerX: Float,
    centerY: Float,
    radius: Float,
    stroke: Float,
    progress: Float,
) {
    if (progress <= 0.001f) return
    val sweep = TileIconFrames.ARC_SWEEP * progress
    val radiusPx = radius * glyphScale * px
    paint.strokeWidth = stroke * glyphScale * px
    val left = centerX * px - radiusPx
    val top = centerY * px - radiusPx
    val box = RectF(left, top, left + 2f * radiusPx, top + 2f * radiusPx)
    val start = TileIconFrames.ARC_TOP - sweep / 2f
    canvas.drawArc(box, start, sweep, false, paint)
}
