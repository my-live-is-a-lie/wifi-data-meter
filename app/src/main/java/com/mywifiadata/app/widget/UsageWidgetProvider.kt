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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * أداة الشاشة الرئيسية لعرض استهلاك الواي فاي. تُحفظ آخر قراءة من التطبيق
 * محليًا كي تظهر فور إضافة الأداة حتى إن لم تكن الشاشة الرئيسية مفتوحة.
 */
class UsageWidgetProvider : AppWidgetProvider() {

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
                val widgetWidthDp = options
                    .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220)
                    .takeIf { it > 0 } ?: 220
                val gaugeFrameWidthDp = (widgetWidthDp - 24).coerceAtLeast(140)
                val gaugeWidthDp = (gaugeFrameWidthDp * 0.80f)
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

                val openApp = Intent(context, MainActivity::class.java)
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
    val startAngle = 135f
    val sweepAngle = 270f
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
    val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.widget_progress)
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }

    canvas.drawArc(arcBounds, startAngle, sweepAngle, false, trackPaint)
    val dotRadius = 2f * density
    val dotCount = 31
    for (index in 0 until dotCount) {
        val angle = Math.toRadians(
            (startAngle + sweepAngle * index.toFloat() / (dotCount - 1)).toDouble(),
        )
        canvas.drawCircle(
            centerX + cos(angle).toFloat() * radius,
            centerY + sin(angle).toFloat() * radius,
            dotRadius,
            dotPaint,
        )
    }
    if (progress > 0.001f) {
        canvas.drawArc(
            arcBounds,
            startAngle,
            sweepAngle * progress.coerceIn(0f, 1f),
            false,
            progressPaint,
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
