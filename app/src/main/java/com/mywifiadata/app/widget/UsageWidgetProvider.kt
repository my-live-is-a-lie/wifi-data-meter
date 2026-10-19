package com.mywifiadata.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.widget.RemoteViews
import com.mywifiadata.app.MainActivity
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * أداة الشاشة الرئيسية لعرض استهلاك الواي فاي. تُحفظ آخر قراءة من التطبيق
 * محليًا كي تظهر فور إضافة الأداة حتى إن لم تكن الشاشة الرئيسية مفتوحة.
 */
class UsageWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_WIDGET_PINNED) return

        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val styleIndex = intent.getIntExtra(
            EXTRA_USAGE_CARD_STYLE,
            DEFAULT_USAGE_CARD_STYLE,
        ).coerceIn(0, MAX_USAGE_CARD_STYLE_INDEX)
        context.getSharedPreferences(UsageWidgetUpdater.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(widgetStylePreferenceKey(widgetId), styleIndex)
            .apply()
        updateWidgets(
            context,
            AppWidgetManager.getInstance(context),
            intArrayOf(widgetId),
        )
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        UsageWidgetProvider.updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    @Suppress("UNUSED_PARAMETER")
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        UsageWidgetProvider.updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    companion object {
        const val ACTION_WIDGET_PINNED = "com.mywifiadata.app.widget.ACTION_WIDGET_PINNED"
        const val EXTRA_USAGE_CARD_STYLE = "com.mywifiadata.app.widget.EXTRA_USAGE_CARD_STYLE"
        const val EXTRA_OPEN_USAGE_CARD_STYLE = "com.mywifiadata.app.widget.EXTRA_OPEN_USAGE_CARD_STYLE"
        const val DEFAULT_USAGE_CARD_STYLE = 0
        const val MAX_USAGE_CARD_STYLE_INDEX = 3
        private const val KEY_WIDGET_STYLE_PREFIX = "widget_style_"

        private fun widgetStylePreferenceKey(widgetId: Int): String =
            "$KEY_WIDGET_STYLE_PREFIX$widgetId"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, UsageWidgetProvider::class.java)
            updateWidgets(context, manager, manager.getAppWidgetIds(component))
        }

        internal fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
        ) {
            val preferences = context.getSharedPreferences(
                UsageWidgetUpdater.PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )
            val todayDownload = preferences.getLong(UsageWidgetUpdater.KEY_TODAY_DOWNLOAD, 0L)
            val todayUpload = preferences.getLong(UsageWidgetUpdater.KEY_TODAY_UPLOAD, 0L)
            val cycleUsage = preferences.getLong(UsageWidgetUpdater.KEY_CYCLE_USAGE, 0L)
            val quota = preferences.getLong(UsageWidgetUpdater.KEY_QUOTA, 0L)
            val hasQuota = quota > 0L
            val used = if (hasQuota) cycleUsage else todayDownload + todayUpload
            val gaugeProgress = when {
                hasQuota -> cycleUsage.toDouble() / quota.toDouble()
                cycleUsage > 0L -> used.toDouble() / cycleUsage.toDouble()
                else -> 0.0
            }.coerceIn(0.0, 1.0).toFloat()
            val progressPercent = (gaugeProgress * 100f).roundToInt()

            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_usage)
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val styleFromOptions = if (options.containsKey(EXTRA_USAGE_CARD_STYLE)) {
                    options.getInt(EXTRA_USAGE_CARD_STYLE)
                        .takeIf { it in 0..MAX_USAGE_CARD_STYLE_INDEX }
                } else {
                    null
                }
                val styleIndex = styleFromOptions ?: preferences.getInt(
                    widgetStylePreferenceKey(widgetId),
                    DEFAULT_USAGE_CARD_STYLE,
                ).coerceIn(0, MAX_USAGE_CARD_STYLE_INDEX)
                if (styleFromOptions != null) {
                    preferences.edit()
                        .putInt(widgetStylePreferenceKey(widgetId), styleFromOptions)
                        .apply()
                }
                val widgetWidthDp = options
                    .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220)
                    .takeIf { it > 0 } ?: 220
                val gaugeFrameWidthDp = (widgetWidthDp - 28).coerceAtLeast(140)
                val gaugeWidthDp = (gaugeFrameWidthDp * 0.82f)
                    .roundToInt()
                    .coerceIn(140, 176)
                views.setImageViewBitmap(
                    R.id.widget_gauge,
                    createGaugeBitmap(context, gaugeFrameWidthDp, gaugeWidthDp, gaugeProgress),
                )
                views.setTextViewText(
                    R.id.widget_usage_value,
                    if (hasQuota) {
                        context.getString(R.string.percent_format, progressPercent)
                    } else {
                        Format.bytes(used)
                    },
                )
                views.setTextViewText(
                    R.id.widget_usage_label,
                    context.getString(if (hasQuota) R.string.label_used else R.string.label_today),
                )
                views.setTextViewText(R.id.widget_used_value, Format.bytes(used))
                if (hasQuota) {
                    views.setTextViewText(
                        R.id.widget_remaining,
                        context.getString(
                            R.string.quota_remaining_of,
                            Format.bytes((quota - cycleUsage).coerceAtLeast(0L)),
                            Format.bytes(quota),
                        ),
                    )
                } else {
                    views.setTextViewText(
                        R.id.widget_remaining,
                        context.getString(R.string.quota_not_set),
                    )
                }
                views.setTextViewText(
                    R.id.widget_download,
                    context.getString(
                        R.string.widget_traffic_line,
                        context.getString(R.string.label_download),
                        Format.bytes(todayDownload),
                    ),
                )
                views.setTextViewText(
                    R.id.widget_upload,
                    context.getString(
                        R.string.widget_traffic_line,
                        context.getString(R.string.label_upload),
                        Format.bytes(todayUpload),
                    ),
                )

                val openApp = Intent(context, MainActivity::class.java).apply {
                    putExtra(EXTRA_OPEN_USAGE_CARD_STYLE, styleIndex)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    widgetId,
                    openApp,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}

private fun createGaugeBitmap(
    context: Context,
    frameWidthDp: Int,
    gaugeWidthDp: Int,
    progress: Float,
): Bitmap {
    val density = context.resources.displayMetrics.density
    val frameHeightDp = 176f
    val bitmapWidth = (frameWidthDp * density).roundToInt().coerceAtLeast(1)
    val bitmapHeight = (frameHeightDp * density).roundToInt().coerceAtLeast(1)
    val gaugeDiameter = gaugeWidthDp * density
    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val stroke = 9f * density
    val centerX = bitmapWidth / 2f
    val centerY = bitmapHeight / 2f
    val radius = (gaugeDiameter - stroke) / 2f
    val arcBounds = RectF(
        centerX - radius,
        centerY - radius,
        centerX + radius,
        centerY + radius,
    )
    // اجعل هندسة الأداة مطابقة لقوس DottedUsageGauge داخل التطبيق.
    val startAngle = 145f
    val sweepAngle = 250f
    val dotCount = 31
    val steps = dotCount.coerceAtLeast(2)
    val safeProgress = progress.coerceIn(0f, 1f)
    val progressSweep = sweepAngle * safeProgress
    val indicatorGap = Math.toDegrees((stroke * 0.5f / radius).toDouble()).toFloat()
    val arcSweep = (progressSweep - indicatorGap).coerceAtLeast(0f)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.widget_progress_track)
        alpha = 140
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.widget_progress)
        alpha = 184
        style = Paint.Style.FILL
    }
    val activeDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.widget_surface)
        style = Paint.Style.FILL
    }
    val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.widget_progress)
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.BUTT
    }
    val progressCapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.widget_progress)
        style = Paint.Style.FILL
    }

    canvas.drawArc(arcBounds, startAngle, sweepAngle, false, trackPaint)

    if (arcSweep > 0.001f) {
        // طرف النهاية مسطّح وبداية القوس مستديرة، كما في البطاقة داخل التطبيق.
        canvas.drawArc(arcBounds, startAngle, arcSweep, false, progressPaint)
        val startRadians = Math.toRadians(startAngle.toDouble())
        canvas.drawCircle(
            centerX + cos(startRadians).toFloat() * radius,
            centerY + sin(startRadians).toFloat() * radius,
            stroke / 2f,
            progressCapPaint,
        )
    }

    val dotRadius = 2f * density
    val dotStep = sweepAngle / (steps - 1)
    for (index in 0 until steps) {
        val dotSweep = dotStep * index
        val angle = Math.toRadians((startAngle + dotSweep).toDouble())
        val isActiveDot = index > 0 && dotSweep < arcSweep
        val isPointerNeighbor = safeProgress > 0.001f &&
            abs(dotSweep - progressSweep) <= dotStep * 0.55f
        if (index == 0 && arcSweep > 0.001f) continue
        if (isPointerNeighbor && !isActiveDot) continue

        canvas.drawCircle(
            centerX + cos(angle).toFloat() * radius,
            centerY + sin(angle).toFloat() * radius,
            if (isActiveDot) dotRadius * 0.72f else dotRadius,
            if (isActiveDot) activeDotPaint else dotPaint,
        )
    }

    if (safeProgress > 0.001f) {
        // قاعدة المؤشر مستوية ورأسه الخارجي دائري؛ وتبقى مسافته من القوس صغيرة.
        val pointerAngle = Math.toRadians((startAngle + progressSweep).toDouble())
        val pointerLength = radius * 0.18f
        val pointerRadiusStart = radius - pointerLength / 2f
        val pointerRadiusEnd = radius + pointerLength / 2f
        val pointerX = cos(pointerAngle).toFloat()
        val pointerY = sin(pointerAngle).toFloat()
        val pointerStroke = stroke * 0.40f
        val pointerEndX = centerX + pointerX * pointerRadiusEnd
        val pointerEndY = centerY + pointerY * pointerRadiusEnd
        val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.widget_progress)
            style = Paint.Style.STROKE
            strokeWidth = pointerStroke
            strokeCap = Paint.Cap.BUTT
        }
        canvas.drawLine(
            centerX + pointerX * pointerRadiusStart,
            centerY + pointerY * pointerRadiusStart,
            pointerEndX,
            pointerEndY,
            pointerPaint,
        )
        canvas.drawCircle(
            pointerEndX,
            pointerEndY,
            pointerStroke / 2f,
            progressCapPaint,
        )
    }
    return bitmap
}

/** يخزن آخر قراءة ويحدّث واجهة الأداة على فترات معقولة بدل تحديثها كل ثانية. */
object UsageWidgetUpdater {
    internal const val PREFERENCES_NAME = "usage_widget_data"
    internal const val KEY_TODAY_DOWNLOAD = "today_download"
    internal const val KEY_TODAY_UPLOAD = "today_upload"
    internal const val KEY_CYCLE_USAGE = "cycle_usage"
    internal const val KEY_QUOTA = "quota"

    private const val KEY_LAST_SAVED_AT = "last_saved_at"
    private const val KEY_LAST_RENDERED_AT = "last_rendered_at"
    private const val CACHE_INTERVAL_MILLIS = 30_000L
    private const val WIDGET_REFRESH_INTERVAL_MILLIS = 60_000L

    fun publish(
        context: Context,
        todayDownload: Long,
        todayUpload: Long,
        cycleUsage: Long,
        quota: Long,
    ) {
        val appContext = context.applicationContext
        val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val cachedQuota = preferences.getLong(KEY_QUOTA, Long.MIN_VALUE)
        val quotaChanged = cachedQuota != quota
        val hasChanged = quotaChanged ||
            preferences.getLong(KEY_TODAY_DOWNLOAD, Long.MIN_VALUE) != todayDownload ||
            preferences.getLong(KEY_TODAY_UPLOAD, Long.MIN_VALUE) != todayUpload ||
            preferences.getLong(KEY_CYCLE_USAGE, Long.MIN_VALUE) != cycleUsage
        val cacheIsOld = now - preferences.getLong(KEY_LAST_SAVED_AT, 0L) >= CACHE_INTERVAL_MILLIS

        if (hasChanged && (cacheIsOld || quotaChanged)) {
            preferences.edit()
                .putLong(KEY_TODAY_DOWNLOAD, todayDownload)
                .putLong(KEY_TODAY_UPLOAD, todayUpload)
                .putLong(KEY_CYCLE_USAGE, cycleUsage)
                .putLong(KEY_QUOTA, quota)
                .putLong(KEY_LAST_SAVED_AT, now)
                .apply()
        }

        val renderedAt = preferences.getLong(KEY_LAST_RENDERED_AT, 0L)
        if (hasChanged && (quotaChanged || now - renderedAt >= WIDGET_REFRESH_INTERVAL_MILLIS)) {
            UsageWidgetProvider.updateAll(appContext)
            preferences.edit().putLong(KEY_LAST_RENDERED_AT, now).apply()
        }
    }
}
