package com.mywifiadata.app.monitor

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.mywifiadata.app.MyWifiApplication
import com.mywifiadata.app.R
import com.mywifiadata.app.core.AppLanguage
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.SamplingIntervals
import com.mywifiadata.app.core.TimeRange
import com.mywifiadata.app.core.TimeRanges
import com.mywifiadata.app.data.UsageRepository
import com.mywifiadata.app.data.model.AppSettings
import com.mywifiadata.app.widget.UsageWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * خدمة أمامية تراقب استهلاك الواي فاي وتُحدّث الإشعار الدائم،
 * وتُطلق تنبيهات عند اقتراب انتهاء الباقة.
 */
class MonitorService : Service() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.localizedContext(newBase))
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    private lateinit var repository: UsageRepository
    private lateinit var notifier: Notifier

    override fun onCreate() {
        super.onCreate()
        val app = application as MyWifiApplication
        repository = app.repository
        notifier = Notifier(this)
        notifier.ensureChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            repository.settingsStore.update { it.copy(trackingEnabled = false) }
            MonitoringTileService.requestRefresh(this)
            stopSelfSafely()
            return START_NOT_STICKY
        }

        try {
            startAsForeground()
        } catch (exception: RuntimeException) {
            Log.e(TAG, "Could not promote the monitoring service to foreground", exception)
            repository.settingsStore.update { it.copy(trackingEnabled = false) }
            MonitoringTileService.requestRefresh(this)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (loopJob?.isActive != true) {
            loopJob = scope.launch { monitorLoop() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * على أندرويد 15+ يوقف النظام خدمة `dataSync` بعد مدة محدودة،
     * فنُغلق المراقبة بهدوء ونُخبر المستخدم.
     */
    override fun onTimeout(startId: Int) {
        repository.settingsStore.update { it.copy(trackingEnabled = false) }
        MonitoringTileService.requestRefresh(this)
        notifier.notifyAlert(
            Notifier.ALERT_TIMEOUT_ID,
            getString(R.string.notification_timeout_title),
            getString(R.string.notification_timeout_text),
        )
        stopSelfSafely()
        super.onTimeout(startId)
    }

    private fun startAsForeground() {
        val notification = notifier.buildOngoingNotification(
            title = getString(R.string.app_name),
            text = getString(R.string.tracking_running),
            hideStatusBarIcon = repository.settingsStore.current.hideStatusBarIcon,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                Notifier.ONGOING_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(Notifier.ONGOING_ID, notification)
        }
    }

    private suspend fun monitorLoop() {
        while (currentCoroutineContext().isActive) {
            val settings = repository.settingsStore.current
            if (!settings.trackingEnabled) {
                stopSelfSafely()
                return
            }

            runCatching { sample(settings) }

            val intervalIndex = SamplingIntervals.coerceIndex(settings.sampleIntervalIndex)
            val intervalMillis = SamplingIntervals.options[intervalIndex].durationMillis
            // تغيّر الإعداد أو إيقاف المراقبة يوقظ الحلقة فورًا بدل انتظار مدة القياس القديمة.
            withTimeoutOrNull(intervalMillis) {
                repository.settingsStore.settings.first {
                    !it.trackingEnabled ||
                        it.sampleIntervalIndex != intervalIndex ||
                        it.hideStatusBarIcon != settings.hideStatusBarIcon
                }
            }
        }
    }

    private suspend fun sample(settings: AppSettings) {
        if (!repository.hasUsageAccess()) {
            notifier.updateOngoing(
                notifier.buildOngoingNotification(
                    title = getString(R.string.app_name),
                    text = getString(R.string.permission_needed_title),
                    hideStatusBarIcon = settings.hideStatusBarIcon,
                ),
            )
            return
        }

        val now = System.currentTimeMillis()
        val today = repository.todayTotals(now)
        val cycle = repository.cycleTotals(settings.cycleStartDay, now)
        val session = repository.deviceTotals(TimeRange(settings.sessionStartMillis, now))
        UsageWidgetUpdater.publish(
            context = this,
            todayDownload = today.rxBytes,
            todayUpload = today.txBytes,
            cycleUsage = cycle.totalBytes,
            quota = settings.quotaBytes,
        )

        // لا نعرض سرعة لحظية في الإشعار: قياسها من إجمالي الدورة غير دقيق ومشتّت،
        // والسرعة الفعلية متاحة داخل التطبيق ولوحة اختبار السرعة.
        val title = getString(R.string.notification_monitor_title, Format.bytes(today.totalBytes))
        val text = if (settings.quotaBytes > 0L) {
            val remaining = (settings.quotaBytes - cycle.totalBytes).coerceAtLeast(0L)
            getString(R.string.notification_monitor_text, Format.bytes(remaining))
        } else {
            getString(R.string.notification_monitor_text_no_quota, Format.bytes(session.totalBytes))
        }
        notifier.updateOngoing(
            notifier.buildOngoingNotification(
                title = title,
                text = text,
                quotaUsedBytes = cycle.totalBytes,
                quotaBytes = settings.quotaBytes,
                hideStatusBarIcon = settings.hideStatusBarIcon,
            ),
        )

        checkQuotaAlerts(settings, cycle.totalBytes, now)
    }

    private fun checkQuotaAlerts(settings: AppSettings, cycleBytes: Long, now: Long) {
        if (!settings.notificationsEnabled || settings.quotaBytes <= 0L) return

        val cycleKey = TimeRanges.cycleRange(settings.cycleStartDay, now).startMillis
        val percentage = cycleBytes * 100.0 / settings.quotaBytes

        val level = when {
            percentage >= 100.0 -> 100
            percentage >= settings.alertThresholdPercent -> settings.alertThresholdPercent
            else -> 0
        }

        // إعادة ضبط حالة التنبيه عند بداية دورة فاتورة جديدة
        val current = repository.settingsStore.current
        if (current.alertCycleStartMillis != cycleKey) {
            repository.settingsStore.update {
                it.copy(alertCycleStartMillis = cycleKey, alertLevelReached = 0)
            }
        }

        val updated = repository.settingsStore.current
        if (level <= 0 || updated.alertLevelReached >= level) return

        if (level >= 100) {
            notifier.notifyAlert(
                Notifier.ALERT_EXCEEDED_ID,
                getString(R.string.notification_alert_exceeded_title),
                getString(
                    R.string.notification_alert_exceeded_text,
                    Format.bytes(cycleBytes),
                    Format.bytes(settings.quotaBytes),
                ),
            )
        } else {
            notifier.notifyAlert(
                Notifier.ALERT_THRESHOLD_ID,
                getString(R.string.notification_alert_threshold_title),
                getString(
                    R.string.notification_alert_threshold_text,
                    level.toString(),
                    Format.bytes(settings.quotaBytes),
                ),
            )
        }

        repository.settingsStore.update { it.copy(alertLevelReached = level) }
    }

    private fun stopSelfSafely() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val TAG = "MonitorService"
        const val ACTION_STOP = "com.mywifiadata.app.action.STOP_MONITOR"

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }
}
