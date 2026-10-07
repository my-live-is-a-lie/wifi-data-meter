package com.mywifiadata.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.mywifiadata.app.MainActivity
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
import kotlin.math.roundToInt

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
            val progress = if (hasQuota) {
                (cycleUsage.toDouble() / quota.toDouble() * 100.0)
                    .roundToInt()
                    .coerceIn(0, 100)
            } else {
                0
            }

            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_usage)
                views.setTextViewText(
                    R.id.widget_title,
                    context.getString(R.string.widget_title),
                )
                views.setTextViewText(
                    R.id.widget_usage_value,
                    if (hasQuota) {
                        context.getString(R.string.percent_format, progress)
                    } else {
                        Format.bytes(used)
                    },
                )
                views.setTextViewText(
                    R.id.widget_usage_label,
                    context.getString(if (hasQuota) R.string.label_used else R.string.label_today),
                )
                views.setViewVisibility(
                    R.id.widget_progress,
                    if (hasQuota) View.VISIBLE else View.GONE,
                )
                if (hasQuota) {
                    views.setProgressBar(R.id.widget_progress, 100, progress, false)
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
                        Format.bytesShort(todayDownload),
                    ),
                )
                views.setTextViewText(
                    R.id.widget_upload,
                    context.getString(
                        R.string.widget_traffic_line,
                        context.getString(R.string.label_upload),
                        Format.bytesShort(todayUpload),
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
