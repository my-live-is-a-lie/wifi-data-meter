package com.mywifiadata.app.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.mywifiadata.app.MainActivity
import com.mywifiadata.app.R
import com.mywifiadata.app.overlay.UsageOverlayService

/**
 * إنشاء قنوات الإشعارات وبنائها (إشعار المراقبة الدائم + تنبيهات الباقة).
 */
class Notifier(private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)
    private val hiddenStatusBarIcon by lazy(LazyThreadSafetyMode.NONE) {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
        }
        IconCompat.createWithBitmap(bitmap)
    }

    fun ensureChannels() {
        val notificationManager = manager ?: return
        val monitorChannel = NotificationChannel(
            CHANNEL_MONITOR,
            context.getString(R.string.channel_monitor_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_monitor_desc)
            setShowBadge(false)
        }
        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS,
            context.getString(R.string.channel_alerts_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_alerts_desc)
        }
        val overlayChannel = NotificationChannel(
            CHANNEL_OVERLAY,
            context.getString(R.string.channel_overlay_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_overlay_desc)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannels(
            listOf(monitorChannel, alertsChannel, overlayChannel),
        )
    }

    /** يبقي إشعار الخدمة ظاهرًا، مع إمكانية إخفاء أيقونته من شريط الحالة. */
    fun buildOngoingNotification(
        title: String,
        text: String,
        quotaUsedBytes: Long? = null,
        quotaBytes: Long = 0L,
        hideStatusBarIcon: Boolean = false,
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, MonitorService::class.java).setAction(MonitorService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val quotaIcon = if (hideStatusBarIcon) {
            null
        } else {
            quotaUsedBytes?.let { usedBytes ->
                QuotaStatusBarIcon.create(
                    usedBytes = usedBytes,
                    quotaBytes = quotaBytes,
                    locale = context.resources.configuration.locales[0],
                )
            }
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_MONITOR)
        when {
            hideStatusBarIcon -> builder.setSmallIcon(hiddenStatusBarIcon)
            quotaIcon != null -> builder.setSmallIcon(quotaIcon)
            else -> builder.setSmallIcon(R.drawable.ic_network_monitor)
        }

        return builder
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.action_stop_tracking), stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /** إشعار دائم يبقي خدمة النافذة العائمة حيّة ويمكن إغلاقها منه. */
    fun buildOverlayNotification(targetLabel: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            3,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val closeIntent = PendingIntent.getService(
            context,
            4,
            Intent(context, UsageOverlayService::class.java)
                .setAction(UsageOverlayService.ACTION_CLOSE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_OVERLAY)
            .setSmallIcon(R.drawable.ic_stat_usage)
            .setContentTitle(context.getString(R.string.overlay_notification_title))
            .setContentText(targetLabel)
            .setStyle(NotificationCompat.BigTextStyle().bigText(targetLabel))
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.action_close), closeIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateOverlay(notification: Notification) {
        runCatching { manager?.notify(OVERLAY_ID, notification) }
    }

    fun updateOngoing(notification: Notification) {
        runCatching {
            manager?.notify(ONGOING_ID, notification)
        }
    }

    fun notifyAlert(id: Int, title: String, text: String) {
        ensureChannels()
        val contentIntent = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_usage)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    fun cancel(id: Int) {
        runCatching { manager?.cancel(id) }
    }

    companion object {
        const val CHANNEL_MONITOR = "monitor"
        const val CHANNEL_ALERTS = "alerts"
        const val CHANNEL_OVERLAY = "overlay"
        const val ONGOING_ID = 1001
        const val OVERLAY_ID = 1005
        const val ALERT_THRESHOLD_ID = 1002
        const val ALERT_EXCEEDED_ID = 1003
        const val ALERT_TIMEOUT_ID = 1004
    }
}
