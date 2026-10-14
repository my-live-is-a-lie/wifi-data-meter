package com.mywifiadata.app.monitor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat
import java.util.Locale

/** يرسم أيقونة أحادية اللون للنسبة وحلقة تقدمها، لتُستخدم كأيقونة الإشعار الصغيرة. */
internal object QuotaStatusBarIcon {

    private const val SCALE = 4f
    private const val BITMAP_SIZE = 96
    private const val CENTER = 12f
    private const val RADIUS = 10.2f

    /** يعيد null عند عدم وجود باقة، ليستخدم الإشعار أيقونة الواي فاي المعتادة. */
    fun create(usedBytes: Long, quotaBytes: Long, locale: Locale): IconCompat? {
        val progress = QuotaUsageProgressCalculator.calculate(usedBytes, quotaBytes) ?: return null
        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bitmap).apply { scale(SCALE, SCALE) }

        drawTrack(canvas)
        drawProgress(canvas, progress.fraction)
        drawPercent(canvas, progress.percent, locale)
        return IconCompat.createWithBitmap(bitmap)
    }

    private fun drawTrack(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = TRACK_ALPHA
            style = Paint.Style.STROKE
            strokeWidth = TRACK_STROKE
        }
        canvas.drawCircle(CENTER, CENTER, RADIUS, paint)
    }

    private fun drawProgress(canvas: Canvas, fraction: Float) {
        if (fraction <= 0f) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = PROGRESS_STROKE
            strokeCap = Paint.Cap.ROUND
        }
        if (fraction >= 1f) {
            canvas.drawCircle(CENTER, CENTER, RADIUS, paint)
        } else {
            val bounds = RectF(
                CENTER - RADIUS,
                CENTER - RADIUS,
                CENTER + RADIUS,
                CENTER + RADIUS,
            )
            // تبدأ الحلقة من الأعلى وتكتمل باتجاه عقارب الساعة مع استهلاك الباقة.
            canvas.drawArc(bounds, START_ANGLE, FULL_SWEEP * fraction, false, paint)
        }
    }

    private fun drawPercent(canvas: Canvas, percent: Int, locale: Locale) {
        val text = QuotaUsageProgressCalculator.formatDigits(percent, locale)
        val fontSize = when (text.length) {
            1 -> ONE_DIGIT_SIZE
            2 -> TWO_DIGIT_SIZE
            3 -> THREE_DIGIT_SIZE
            else -> FOUR_DIGIT_SIZE
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = fontSize
        }
        // كبّر الرقم مع تقييد عرضه حتى لا يلامس حلقة التقدم في الأرقام العربية واللاتينية.
        val maxTextWidth = 2f * (RADIUS - PROGRESS_STROKE / 2f - TEXT_EDGE_PADDING)
        val measuredTextWidth = paint.measureText(text)
        if (measuredTextWidth > maxTextWidth) {
            paint.textSize *= maxTextWidth / measuredTextWidth
        }
        val metrics = paint.fontMetrics
        val baseline = CENTER - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(text, CENTER, baseline, paint)
    }

    private const val TRACK_ALPHA = 82
    private const val TRACK_STROKE = 1.35f
    private const val PROGRESS_STROKE = 2.0f
    private const val TEXT_EDGE_PADDING = 0.5f
    private const val START_ANGLE = -90f
    private const val FULL_SWEEP = 360f

    // زيادة واضحة لحجم الرقم لتحسين قراءته، مع ضبط العرض تلقائيًا ليلائم الحلقة.
    private const val ONE_DIGIT_SIZE = 15.5f
    private const val TWO_DIGIT_SIZE = 14f
    private const val THREE_DIGIT_SIZE = 10.5f
    private const val FOUR_DIGIT_SIZE = 9f
}
