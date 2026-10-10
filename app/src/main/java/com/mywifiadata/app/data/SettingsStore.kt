package com.mywifiadata.app.data

import android.content.Context
import com.mywifiadata.app.core.SamplingIntervals
import com.mywifiadata.app.data.model.AccentColor
import com.mywifiadata.app.data.model.AppSettings
import com.mywifiadata.app.data.model.SpeedTestRecord
import com.mywifiadata.app.data.model.ThemeMode
import com.mywifiadata.app.data.model.TrafficTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * تخزين الإعدادات محليًا باستخدام SharedPreferences مع تعريضها كـ [StateFlow]
 * ليتابعها الـ ViewModel والخدمة.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val current: AppSettings get() = _settings.value

    fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        write(updated)
        _settings.value = updated
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        _settings.value = AppSettings()
    }

    /** يضيف كمية إلى «خط الأساس» للجلسة (يُستخدم عند تصفير الجلسة). */
    fun rebaseSession(baseline: TrafficTotals, startMillis: Long) = update {
        it.copy(sessionBaseline = baseline, sessionStartMillis = startMillis)
    }

    /** يحفظ آخر نتيجة اختبار سرعة لتظهر بعد إعادة تشغيل التطبيق. */
    fun saveSpeedTest(record: SpeedTestRecord) = update {
        it.copy(lastSpeedTest = record)
    }

    private fun read(): AppSettings = AppSettings(
        quotaBytes = prefs.getLong(KEY_QUOTA_BYTES, 0L),
        cycleStartDay = prefs.getInt(KEY_CYCLE_DAY, 1),
        notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true),
        alertThresholdPercent = prefs.getInt(KEY_THRESHOLD, 80),
        sampleIntervalIndex = if (prefs.contains(KEY_INTERVAL_INDEX)) {
            SamplingIntervals.coerceIndex(prefs.getInt(KEY_INTERVAL_INDEX, SamplingIntervals.DEFAULT_INDEX))
        } else {
            val legacyMillis = prefs.getInt(KEY_INTERVAL_LEGACY_SECONDS, 15).toLong() * 1_000L
            SamplingIntervals.closestIndex(legacyMillis)
        },
        dynamicColorEnabled = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
        accentColor = prefs.getString(KEY_ACCENT_COLOR, null)
            ?.let { stored -> AccentColor.entries.firstOrNull { it.name == stored } }
            ?: AccentColor.DEFAULT,
        trackingEnabled = prefs.getBoolean(KEY_TRACKING, false),
        usageCardStyleIndex = prefs.getInt(KEY_USAGE_CARD_STYLE, 0).coerceAtLeast(0),
        historyLineChartEnabled = prefs.getBoolean(KEY_HISTORY_LINE_CHART, false),
        appsLineChartEnabled = prefs.getBoolean(KEY_APPS_LINE_CHART, false),
        themeMode = prefs.getString(KEY_THEME_MODE, null)
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM,
        sessionStartMillis = prefs.getLong(KEY_SESSION_START, System.currentTimeMillis()),
        sessionBaseline = TrafficTotals(
            rxBytes = prefs.getLong(KEY_SESSION_RX, 0L),
            txBytes = prefs.getLong(KEY_SESSION_TX, 0L),
        ),
        alertCycleStartMillis = prefs.getLong(KEY_ALERT_CYCLE, 0L),
        alertLevelReached = prefs.getInt(KEY_ALERT_LEVEL, 0),
        lastSpeedTest = readSpeedTest(),
    )

    /** قراءة آخر نتيجة اختبار سرعة محفوظة، أو null إن لم يُجرَ اختبار بعد. */
    private fun readSpeedTest(): SpeedTestRecord? {
        val timestamp = prefs.getLong(KEY_SPEED_TIME, 0L)
        if (timestamp <= 0L) return null
        return SpeedTestRecord(
            downloadBitsPerSecond = Double.fromBits(prefs.getLong(KEY_SPEED_DOWNLOAD, 0L)),
            uploadBitsPerSecond = Double.fromBits(prefs.getLong(KEY_SPEED_UPLOAD, 0L)),
            timestampMillis = timestamp,
            usedFastCom = prefs.getBoolean(KEY_SPEED_FAST, true),
            uploadMeasured = prefs.getBoolean(KEY_SPEED_UPLOAD_OK, true),
        )
    }

    private fun write(settings: AppSettings) {
        val intervalIndex = SamplingIntervals.coerceIndex(settings.sampleIntervalIndex)
        val intervalMillis = SamplingIntervals.options[intervalIndex].durationMillis
        val legacyIntervalSeconds = listOf(5, 15, 30, 60).minBy { seconds ->
            abs(seconds * 1_000L - intervalMillis)
        }
        prefs.edit()
            .putLong(KEY_QUOTA_BYTES, settings.quotaBytes)
            .putInt(KEY_CYCLE_DAY, settings.cycleStartDay)
            .putBoolean(KEY_NOTIFICATIONS, settings.notificationsEnabled)
            .putInt(KEY_THRESHOLD, settings.alertThresholdPercent)
            .putInt(KEY_INTERVAL_INDEX, intervalIndex)
            .putInt(KEY_INTERVAL_LEGACY_SECONDS, legacyIntervalSeconds)
            .putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColorEnabled)
            .putString(KEY_ACCENT_COLOR, settings.accentColor.name)
            .putBoolean(KEY_TRACKING, settings.trackingEnabled)
            .putInt(KEY_USAGE_CARD_STYLE, settings.usageCardStyleIndex)
            .putBoolean(KEY_HISTORY_LINE_CHART, settings.historyLineChartEnabled)
            .putBoolean(KEY_APPS_LINE_CHART, settings.appsLineChartEnabled)
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putLong(KEY_SESSION_START, settings.sessionStartMillis)
            .putLong(KEY_SESSION_RX, settings.sessionBaseline.rxBytes)
            .putLong(KEY_SESSION_TX, settings.sessionBaseline.txBytes)
            .putLong(KEY_ALERT_CYCLE, settings.alertCycleStartMillis)
            .putInt(KEY_ALERT_LEVEL, settings.alertLevelReached)
            .putLong(KEY_SPEED_DOWNLOAD, settings.lastSpeedTest?.downloadBitsPerSecond?.toRawBits() ?: 0L)
            .putLong(KEY_SPEED_UPLOAD, settings.lastSpeedTest?.uploadBitsPerSecond?.toRawBits() ?: 0L)
            .putLong(KEY_SPEED_TIME, settings.lastSpeedTest?.timestampMillis ?: 0L)
            .putBoolean(KEY_SPEED_FAST, settings.lastSpeedTest?.usedFastCom ?: true)
            .putBoolean(KEY_SPEED_UPLOAD_OK, settings.lastSpeedTest?.uploadMeasured ?: true)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "mywifi_settings"

        private const val KEY_QUOTA_BYTES = "quota_bytes"
        private const val KEY_CYCLE_DAY = "cycle_day"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_THRESHOLD = "alert_threshold"
        private const val KEY_INTERVAL_INDEX = "sample_interval_index"
        private const val KEY_INTERVAL_LEGACY_SECONDS = "sample_interval"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_TRACKING = "tracking_enabled"
        private const val KEY_USAGE_CARD_STYLE = "usage_card_style"
        private const val KEY_HISTORY_LINE_CHART = "history_line_chart_enabled"
        private const val KEY_APPS_LINE_CHART = "apps_line_chart_enabled"
        private const val KEY_SESSION_START = "session_start"
        private const val KEY_SESSION_RX = "session_baseline_rx"
        private const val KEY_SESSION_TX = "session_baseline_tx"
        private const val KEY_ALERT_CYCLE = "alert_cycle"
        private const val KEY_ALERT_LEVEL = "alert_level"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SPEED_DOWNLOAD = "speed_test_download"
        private const val KEY_SPEED_UPLOAD = "speed_test_upload"
        private const val KEY_SPEED_TIME = "speed_test_time"
        private const val KEY_SPEED_FAST = "speed_test_source_fast"
        private const val KEY_SPEED_UPLOAD_OK = "speed_test_upload_ok"
    }
}
