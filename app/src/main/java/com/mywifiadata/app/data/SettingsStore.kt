package com.mywifiadata.app.data

import android.content.Context
import com.mywifiadata.app.core.AppLanguage
import com.mywifiadata.app.core.HistoryRetention
import com.mywifiadata.app.core.SamplingIntervals
import com.mywifiadata.app.core.SpeedTestDataUsage
import com.mywifiadata.app.core.UsageCardStyles
import com.mywifiadata.app.data.model.AccentColor
import com.mywifiadata.app.data.model.AppLayoutDirection
import com.mywifiadata.app.data.model.AppSettings
import com.mywifiadata.app.data.model.AppsUsageFilterMode
import com.mywifiadata.app.data.model.DashboardLayout
import com.mywifiadata.app.data.model.MonitorTarget
import com.mywifiadata.app.data.model.OverlayEdge
import com.mywifiadata.app.data.model.DataPlan
import com.mywifiadata.app.data.model.PlanCapacityUnit
import com.mywifiadata.app.data.model.PlanDurationUnit
import com.mywifiadata.app.data.model.PlanNetworkType
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

    /**
     * يقرأ فهرس شكل البطاقة ويرحّل القيم المحفوظة قبل إزالة النمط الجانبي،
     * ثم يكتب الفهرس المرحّل والنسخة الجديدة مرة واحدة.
     */
    private fun readUsageCardStyle(): Int {
        val storedVersion = prefs.getInt(KEY_USAGE_CARD_STYLE_VERSION, 0)
        val storedStyle = prefs.getInt(KEY_USAGE_CARD_STYLE, UsageCardStyles.RING)
        val migrated = UsageCardStyles.migrate(storedStyle, storedVersion)
        if (storedVersion < UsageCardStyles.LATEST_VERSION || storedStyle != migrated) {
            prefs.edit()
                .putInt(KEY_USAGE_CARD_STYLE, migrated)
                .putInt(KEY_USAGE_CARD_STYLE_VERSION, UsageCardStyles.LATEST_VERSION)
                .apply()
        }
        return migrated
    }

    fun clearAll() {
        // يحتفظ بحالة البلاطة المثبتة ولغة الواجهة؛ فهما لا تمثلان بيانات القياس المحلية.
        val current = _settings.value
        prefs.edit()
            .clear()
            .putBoolean(KEY_QUICK_SETTINGS_TILE_ADDED, current.quickSettingsTileAdded)
            .putString(AppLanguage.PREFERENCE_KEY, current.appLanguage)
            .apply()
        _settings.value = AppSettings(
            quickSettingsTileAdded = current.quickSettingsTileAdded,
            appLanguage = current.appLanguage,
        )
    }

    /** يضيف كمية إلى «خط الأساس» للجلسة (يُستخدم عند تصفير الجلسة). */
    fun rebaseSession(baseline: TrafficTotals, startMillis: Long) = update {
        it.copy(sessionBaseline = baseline, sessionStartMillis = startMillis)
    }

    /** يحفظ نتيجة الاختبار الأخيرة ويضيفها إلى سجل آخر عشرة اختبارات. */
    fun saveSpeedTest(record: SpeedTestRecord) = update { settings ->
        settings.copy(
            lastSpeedTest = record,
            speedTestHistory = SpeedTestHistory.prepend(settings.speedTestHistory, record),
        )
    }

    /**
     * يتجاهل تحذير استهلاك بيانات الهاتف في اختبار السرعة ليوم كامل يبدأ من
     * [nowMillis]؛ بعد انقضاء المدة يعود التحذير تلقائيًا.
     */
    fun snoozeSpeedTestWarning(nowMillis: Long = System.currentTimeMillis()) = update { settings ->
        settings.copy(
            speedTestWarningSnoozedUntilMillis = SpeedTestDataUsage.snoozeUntil(nowMillis),
        )
    }

    private fun read(): AppSettings = AppSettings(
        quotaBytes = prefs.getLong(KEY_QUOTA_BYTES, 0L),
        cycleStartDay = prefs.getInt(KEY_CYCLE_DAY, 1),
        planId = prefs.getString(KEY_PLAN_ID, "wifi-active")?.ifBlank { "wifi-active" } ?: "wifi-active",
        planName = prefs.getString(KEY_PLAN_NAME, "") ?: "",
        planLogoPath = prefs.getString(KEY_PLAN_LOGO_PATH, "") ?: "",
        planCapacityUnit = prefs.getString(KEY_PLAN_CAPACITY_UNIT, null)
            ?.let { stored -> PlanCapacityUnit.entries.firstOrNull { it.name == stored } }
            ?: PlanCapacityUnit.GB,
        planDurationValue = prefs.getInt(KEY_PLAN_DURATION_VALUE, 28).coerceAtLeast(1),
        planDurationUnit = prefs.getString(KEY_PLAN_DURATION_UNIT, null)
            ?.let { stored -> PlanDurationUnit.entries.firstOrNull { it.name == stored } }
            ?: PlanDurationUnit.DAY,
        planNetworkType = prefs.getString(KEY_PLAN_NETWORK_TYPE, null)
            ?.let { stored -> PlanNetworkType.entries.firstOrNull { it.name == stored } }
            ?: PlanNetworkType.WIFI,
        planPreventEdit = prefs.getBoolean(KEY_PLAN_PREVENT_EDIT, false),
        planPendingActivation = prefs.getBoolean(KEY_PLAN_PENDING_ACTIVATION, false),
        planActivatedAtMillis = prefs.getLong(KEY_PLAN_ACTIVATED_AT, 0L),
        planAdditionalPlans = PlanStorageCodec.decodeAdditionalPlans(
            prefs.getString(KEY_PLAN_ADDITIONAL_PLANS, null),
        ),
        mobilePlan = DataPlan(
            id = prefs.getString(KEY_MOBILE_PLAN_ID, "mobile-active")?.ifBlank { "mobile-active" } ?: "mobile-active",
            name = prefs.getString(KEY_MOBILE_PLAN_NAME, "") ?: "",
            logoPath = prefs.getString(KEY_MOBILE_PLAN_LOGO_PATH, "") ?: "",
            quotaBytes = prefs.getLong(KEY_MOBILE_QUOTA_BYTES, 0L),
            capacityUnit = prefs.getString(KEY_MOBILE_PLAN_CAPACITY_UNIT, null)
                ?.let { stored -> PlanCapacityUnit.entries.firstOrNull { it.name == stored } }
                ?: PlanCapacityUnit.GB,
            durationValue = prefs.getInt(KEY_MOBILE_PLAN_DURATION_VALUE, 28).coerceAtLeast(1),
            durationUnit = prefs.getString(KEY_MOBILE_PLAN_DURATION_UNIT, null)
                ?.let { stored -> PlanDurationUnit.entries.firstOrNull { it.name == stored } }
                ?: PlanDurationUnit.DAY,
            preventEdit = prefs.getBoolean(KEY_MOBILE_PLAN_PREVENT_EDIT, false),
            pendingActivation = prefs.getBoolean(KEY_MOBILE_PLAN_PENDING_ACTIVATION, false),
            activatedAtMillis = prefs.getLong(KEY_MOBILE_PLAN_ACTIVATED_AT, 0L),
            additionalPlans = PlanStorageCodec.decodeAdditionalPlans(
                prefs.getString(KEY_MOBILE_PLAN_ADDITIONAL_PLANS, null),
            ),
        ),
        savedWifiPlans = PlanStorageCodec.decodeSavedPlans(
            prefs.getString(KEY_SAVED_WIFI_PLANS, null),
        ),
        savedMobilePlans = PlanStorageCodec.decodeSavedPlans(
            prefs.getString(KEY_SAVED_MOBILE_PLANS, null),
        ),
        wifiActivePlanIndex = prefs.getInt(KEY_WIFI_ACTIVE_PLAN_INDEX, 0).coerceAtLeast(0),
        mobileActivePlanIndex = prefs.getInt(KEY_MOBILE_ACTIVE_PLAN_INDEX, 0).coerceAtLeast(0),
        completedWifiPlans = PlanStorageCodec.decodeCompletedPlans(
            prefs.getString(KEY_COMPLETED_WIFI_PLANS, null),
        ),
        completedMobilePlans = PlanStorageCodec.decodeCompletedPlans(
            prefs.getString(KEY_COMPLETED_MOBILE_PLANS, null),
        ),
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
        quickSettingsTileEnabled = prefs.getBoolean(KEY_QUICK_SETTINGS_TILE, false),
        quickSettingsTileAdded = prefs.getBoolean(KEY_QUICK_SETTINGS_TILE_ADDED, false),
        appIconHidden = prefs.getBoolean(KEY_APP_ICON_HIDDEN, false),
        appLanguage = AppLanguage.normalize(
            prefs.getString(AppLanguage.PREFERENCE_KEY, AppLanguage.SYSTEM),
        ),
        trackingEnabled = prefs.getBoolean(KEY_TRACKING, false),
        hideStatusBarIcon = prefs.getBoolean(KEY_HIDE_STATUS_BAR_ICON, false),
        usageCardStyleIndex = readUsageCardStyle(),
        historyRetentionMonths = HistoryRetention.normalize(
            prefs.getInt(KEY_HISTORY_RETENTION, HistoryRetention.DEFAULT_MONTHS),
        ),
        dashboardLayout = DashboardLayout.decode(prefs.getString(KEY_DASHBOARD_LAYOUT, null)),
        historyLineChartEnabled = prefs.getBoolean(KEY_HISTORY_LINE_CHART, false),
        appsLineChartEnabled = prefs.getBoolean(KEY_APPS_LINE_CHART, false),
        appsUsageFilterEnabled = prefs.getBoolean(KEY_APPS_USAGE_FILTER_ENABLED, false),
        appsUsageFilterMode = prefs.getString(KEY_APPS_USAGE_FILTER_MODE, null)
            ?.let { stored -> AppsUsageFilterMode.entries.firstOrNull { it.name == stored } }
            ?: AppsUsageFilterMode.AMOUNT,
        appsUsageFilterAmount = prefs.getInt(KEY_APPS_USAGE_FILTER_AMOUNT, 5).coerceAtLeast(0),
        appsUsageFilterUnit = prefs.getString(KEY_APPS_USAGE_FILTER_UNIT, null)
            ?.let { stored -> PlanCapacityUnit.entries.firstOrNull { it.name == stored } }
            ?: PlanCapacityUnit.MB,
        appsUsageFilterPercent = prefs.getInt(KEY_APPS_USAGE_FILTER_PERCENT, 3).coerceIn(1, 30),
        appSearchHistory = AppSearchHistory.decode(prefs.getString(KEY_APP_SEARCH_HISTORY, null)),
        monitorTarget = MonitorTarget(
            uid = prefs.getInt(KEY_MONITOR_TARGET_UID, MonitorTarget.NO_APP),
            packageName = prefs.getString(KEY_MONITOR_TARGET_PACKAGE, null).orEmpty(),
            label = prefs.getString(KEY_MONITOR_TARGET_LABEL, null).orEmpty(),
            iconPath = prefs.getString(KEY_MONITOR_TARGET_ICON, null).orEmpty(),
        ).let { target -> if (target.uid < 0) MonitorTarget.AllApps else target },
        monitorTargetHistory = AppSearchHistory.decode(prefs.getString(KEY_MONITOR_TARGET_HISTORY, null)),
        overlayAnchorX = prefs.getFloat(KEY_OVERLAY_ANCHOR_X, 0.5f),
        overlayAnchorY = prefs.getFloat(KEY_OVERLAY_ANCHOR_Y, 0.24f),
        overlayEdge = prefs.getString(KEY_OVERLAY_EDGE, null)
            ?.let { stored -> OverlayEdge.entries.firstOrNull { it.name == stored } }
            ?: OverlayEdge.LEFT,
        themeMode = prefs.getString(KEY_THEME_MODE, null)
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM,
        layoutDirection = prefs.getString(KEY_LAYOUT_DIRECTION, null)
            ?.let { stored -> AppLayoutDirection.entries.firstOrNull { it.name == stored } }
            ?: AppLayoutDirection.AUTO,
        sessionStartMillis = prefs.getLong(KEY_SESSION_START, System.currentTimeMillis()),
        sessionBaseline = TrafficTotals(
            rxBytes = prefs.getLong(KEY_SESSION_RX, 0L),
            txBytes = prefs.getLong(KEY_SESSION_TX, 0L),
        ),
        alertCycleStartMillis = prefs.getLong(KEY_ALERT_CYCLE, 0L),
        alertLevelReached = prefs.getInt(KEY_ALERT_LEVEL, 0),
        speedTestHistory = readSpeedTestHistory(),
        lastSpeedTest = readSpeedTest(),
        speedTestWarningSnoozedUntilMillis = prefs.getLong(KEY_SPEED_WARNING_SNOOZED_UNTIL, 0L),
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
            dataBytes = prefs.getLong(KEY_SPEED_DATA_BYTES, 0L).coerceAtLeast(0L),
        )
    }

    /** يقرأ السجل الجديد، أو يرحّل آخر نتيجة القديمة إلى أول عنصر عند التحديث. */
    private fun readSpeedTestHistory(): List<SpeedTestRecord> {
        val last = readSpeedTest()
        val encoded = prefs.getString(KEY_SPEED_HISTORY, null)
        val history = encoded
            ?.takeIf(String::isNotBlank)
            ?.let(SpeedTestHistory::decode)
            .orEmpty()

        if (history.isEmpty()) return listOfNotNull(last)
        return if (last != null && history.none { it == last }) {
            SpeedTestHistory.prepend(history, last)
        } else {
            history
        }
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
            .putString(KEY_PLAN_ID, settings.planId)
            .putString(KEY_PLAN_NAME, settings.planName)
            .putString(KEY_PLAN_LOGO_PATH, settings.planLogoPath)
            .putString(KEY_PLAN_CAPACITY_UNIT, settings.planCapacityUnit.name)
            .putInt(KEY_PLAN_DURATION_VALUE, settings.planDurationValue.coerceAtLeast(1))
            .putString(KEY_PLAN_DURATION_UNIT, settings.planDurationUnit.name)
            .putString(KEY_PLAN_NETWORK_TYPE, settings.planNetworkType.name)
            .putBoolean(KEY_PLAN_PREVENT_EDIT, settings.planPreventEdit)
            .putBoolean(KEY_PLAN_PENDING_ACTIVATION, settings.planPendingActivation)
            .putLong(KEY_PLAN_ACTIVATED_AT, settings.planActivatedAtMillis)
            .putString(
                KEY_PLAN_ADDITIONAL_PLANS,
                PlanStorageCodec.encodeAdditionalPlans(settings.planAdditionalPlans),
            )
            .putString(KEY_MOBILE_PLAN_ID, settings.mobilePlan.id)
            .putString(KEY_MOBILE_PLAN_NAME, settings.mobilePlan.name)
            .putString(KEY_MOBILE_PLAN_LOGO_PATH, settings.mobilePlan.logoPath)
            .putLong(KEY_MOBILE_QUOTA_BYTES, settings.mobilePlan.quotaBytes)
            .putString(KEY_MOBILE_PLAN_CAPACITY_UNIT, settings.mobilePlan.capacityUnit.name)
            .putInt(KEY_MOBILE_PLAN_DURATION_VALUE, settings.mobilePlan.durationValue.coerceAtLeast(1))
            .putString(KEY_MOBILE_PLAN_DURATION_UNIT, settings.mobilePlan.durationUnit.name)
            .putBoolean(KEY_MOBILE_PLAN_PREVENT_EDIT, settings.mobilePlan.preventEdit)
            .putBoolean(KEY_MOBILE_PLAN_PENDING_ACTIVATION, settings.mobilePlan.pendingActivation)
            .putLong(KEY_MOBILE_PLAN_ACTIVATED_AT, settings.mobilePlan.activatedAtMillis)
            .putString(
                KEY_MOBILE_PLAN_ADDITIONAL_PLANS,
                PlanStorageCodec.encodeAdditionalPlans(settings.mobilePlan.additionalPlans),
            )
            .putString(KEY_SAVED_WIFI_PLANS, PlanStorageCodec.encodeSavedPlans(settings.savedWifiPlans))
            .putString(KEY_SAVED_MOBILE_PLANS, PlanStorageCodec.encodeSavedPlans(settings.savedMobilePlans))
            .putInt(KEY_WIFI_ACTIVE_PLAN_INDEX, settings.wifiActivePlanIndex.coerceAtLeast(0))
            .putInt(KEY_MOBILE_ACTIVE_PLAN_INDEX, settings.mobileActivePlanIndex.coerceAtLeast(0))
            .putString(
                KEY_COMPLETED_WIFI_PLANS,
                PlanStorageCodec.encodeCompletedPlans(settings.completedWifiPlans),
            )
            .putString(
                KEY_COMPLETED_MOBILE_PLANS,
                PlanStorageCodec.encodeCompletedPlans(settings.completedMobilePlans),
            )
            .putBoolean(KEY_NOTIFICATIONS, settings.notificationsEnabled)
            .putInt(KEY_THRESHOLD, settings.alertThresholdPercent)
            .putInt(KEY_INTERVAL_INDEX, intervalIndex)
            .putInt(KEY_INTERVAL_LEGACY_SECONDS, legacyIntervalSeconds)
            .putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColorEnabled)
            .putString(KEY_ACCENT_COLOR, settings.accentColor.name)
            .putBoolean(KEY_QUICK_SETTINGS_TILE, settings.quickSettingsTileEnabled)
            .putBoolean(KEY_QUICK_SETTINGS_TILE_ADDED, settings.quickSettingsTileAdded)
            .putBoolean(KEY_APP_ICON_HIDDEN, settings.appIconHidden)
            .putString(AppLanguage.PREFERENCE_KEY, AppLanguage.normalize(settings.appLanguage))
            .putBoolean(KEY_TRACKING, settings.trackingEnabled)
            .putBoolean(KEY_HIDE_STATUS_BAR_ICON, settings.hideStatusBarIcon)
            .putInt(KEY_USAGE_CARD_STYLE, settings.usageCardStyleIndex)
            .putInt(
                KEY_HISTORY_RETENTION,
                HistoryRetention.normalize(settings.historyRetentionMonths),
            )
            .putInt(KEY_USAGE_CARD_STYLE_VERSION, UsageCardStyles.LATEST_VERSION)
            .putString(KEY_DASHBOARD_LAYOUT, settings.dashboardLayout.encode())
            .putBoolean(KEY_HISTORY_LINE_CHART, settings.historyLineChartEnabled)
            .putBoolean(KEY_APPS_LINE_CHART, settings.appsLineChartEnabled)
            .putBoolean(KEY_APPS_USAGE_FILTER_ENABLED, settings.appsUsageFilterEnabled)
            .putString(KEY_APPS_USAGE_FILTER_MODE, settings.appsUsageFilterMode.name)
            .putInt(KEY_APPS_USAGE_FILTER_AMOUNT, settings.appsUsageFilterAmount.coerceAtLeast(0))
            .putString(KEY_APPS_USAGE_FILTER_UNIT, settings.appsUsageFilterUnit.name)
            .putInt(KEY_APPS_USAGE_FILTER_PERCENT, settings.appsUsageFilterPercent.coerceIn(1, 30))
            .putString(KEY_APP_SEARCH_HISTORY, AppSearchHistory.encode(settings.appSearchHistory))
            .putInt(KEY_MONITOR_TARGET_UID, settings.monitorTarget.uid)
            .putString(KEY_MONITOR_TARGET_PACKAGE, settings.monitorTarget.packageName)
            .putString(KEY_MONITOR_TARGET_LABEL, settings.monitorTarget.label)
            .putString(KEY_MONITOR_TARGET_ICON, settings.monitorTarget.iconPath)
            .putString(
                KEY_MONITOR_TARGET_HISTORY,
                AppSearchHistory.encode(settings.monitorTargetHistory),
            )
            .putFloat(KEY_OVERLAY_ANCHOR_X, settings.overlayAnchorX)
            .putFloat(KEY_OVERLAY_ANCHOR_Y, settings.overlayAnchorY)
            .putString(KEY_OVERLAY_EDGE, settings.overlayEdge.name)
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putString(KEY_LAYOUT_DIRECTION, settings.layoutDirection.name)
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
            .putLong(KEY_SPEED_DATA_BYTES, settings.lastSpeedTest?.dataBytes ?: 0L)
            .putLong(
                KEY_SPEED_WARNING_SNOOZED_UNTIL,
                settings.speedTestWarningSnoozedUntilMillis,
            )
            .putString(KEY_SPEED_HISTORY, SpeedTestHistory.encode(settings.speedTestHistory))
            .apply()
    }

    companion object {
        const val PREFS_NAME = AppLanguage.PREFERENCES_NAME

        private const val KEY_QUOTA_BYTES = "quota_bytes"
        private const val KEY_CYCLE_DAY = "cycle_day"
        private const val KEY_PLAN_ID = "plan_id"
        private const val KEY_PLAN_NAME = "plan_name"
        private const val KEY_PLAN_LOGO_PATH = "plan_logo_path"
        private const val KEY_PLAN_CAPACITY_UNIT = "plan_capacity_unit"
        private const val KEY_PLAN_DURATION_VALUE = "plan_duration_value"
        private const val KEY_PLAN_DURATION_UNIT = "plan_duration_unit"
        private const val KEY_PLAN_NETWORK_TYPE = "plan_network_type"
        private const val KEY_PLAN_PREVENT_EDIT = "plan_prevent_edit"
        private const val KEY_PLAN_PENDING_ACTIVATION = "plan_pending_activation"
        private const val KEY_PLAN_ACTIVATED_AT = "plan_activated_at"
        private const val KEY_PLAN_ADDITIONAL_PLANS = "plan_additional_plans"
        private const val KEY_MOBILE_PLAN_ID = "mobile_plan_id"
        private const val KEY_MOBILE_PLAN_NAME = "mobile_plan_name"
        private const val KEY_MOBILE_PLAN_LOGO_PATH = "mobile_plan_logo_path"
        private const val KEY_MOBILE_QUOTA_BYTES = "mobile_quota_bytes"
        private const val KEY_MOBILE_PLAN_CAPACITY_UNIT = "mobile_plan_capacity_unit"
        private const val KEY_MOBILE_PLAN_DURATION_VALUE = "mobile_plan_duration_value"
        private const val KEY_MOBILE_PLAN_DURATION_UNIT = "mobile_plan_duration_unit"
        private const val KEY_MOBILE_PLAN_PREVENT_EDIT = "mobile_plan_prevent_edit"
        private const val KEY_MOBILE_PLAN_PENDING_ACTIVATION = "mobile_plan_pending_activation"
        private const val KEY_MOBILE_PLAN_ACTIVATED_AT = "mobile_plan_activated_at"
        private const val KEY_MOBILE_PLAN_ADDITIONAL_PLANS = "mobile_plan_additional_plans"
        private const val KEY_SAVED_WIFI_PLANS = "saved_wifi_plans"
        private const val KEY_SAVED_MOBILE_PLANS = "saved_mobile_plans"
        private const val KEY_WIFI_ACTIVE_PLAN_INDEX = "wifi_active_plan_index"
        private const val KEY_MOBILE_ACTIVE_PLAN_INDEX = "mobile_active_plan_index"
        private const val KEY_COMPLETED_WIFI_PLANS = "completed_wifi_plans"
        private const val KEY_COMPLETED_MOBILE_PLANS = "completed_mobile_plans"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_THRESHOLD = "alert_threshold"
        private const val KEY_INTERVAL_INDEX = "sample_interval_index"
        private const val KEY_INTERVAL_LEGACY_SECONDS = "sample_interval"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_QUICK_SETTINGS_TILE = "quick_settings_tile_enabled"
        private const val KEY_QUICK_SETTINGS_TILE_ADDED = "quick_settings_tile_added"
        private const val KEY_APP_ICON_HIDDEN = "app_icon_hidden"
        private const val KEY_TRACKING = "tracking_enabled"
        private const val KEY_HIDE_STATUS_BAR_ICON = "hide_status_bar_icon"
        private const val KEY_USAGE_CARD_STYLE = "usage_card_style"
        private const val KEY_HISTORY_RETENTION = "history_retention_months"
        private const val KEY_USAGE_CARD_STYLE_VERSION = "usage_card_style_version"
        private const val KEY_DASHBOARD_LAYOUT = "dashboard_layout"
        private const val KEY_HISTORY_LINE_CHART = "history_line_chart_enabled"
        private const val KEY_APPS_LINE_CHART = "apps_line_chart_enabled"
        private const val KEY_APPS_USAGE_FILTER_ENABLED = "apps_usage_filter_enabled"
        private const val KEY_APPS_USAGE_FILTER_MODE = "apps_usage_filter_mode"
        private const val KEY_APPS_USAGE_FILTER_AMOUNT = "apps_usage_filter_amount"
        private const val KEY_APPS_USAGE_FILTER_UNIT = "apps_usage_filter_unit"
        private const val KEY_APPS_USAGE_FILTER_PERCENT = "apps_usage_filter_percent"
        private const val KEY_APP_SEARCH_HISTORY = "app_search_selection_history"
        private const val KEY_MONITOR_TARGET_UID = "monitor_target_uid"
        private const val KEY_MONITOR_TARGET_PACKAGE = "monitor_target_package"
        private const val KEY_MONITOR_TARGET_LABEL = "monitor_target_label"
        private const val KEY_MONITOR_TARGET_ICON = "monitor_target_icon"
        private const val KEY_MONITOR_TARGET_HISTORY = "monitor_target_history"
        private const val KEY_OVERLAY_ANCHOR_X = "overlay_anchor_x"
        private const val KEY_OVERLAY_ANCHOR_Y = "overlay_anchor_y"
        private const val KEY_OVERLAY_EDGE = "overlay_edge"
        private const val KEY_SESSION_START = "session_start"
        private const val KEY_SESSION_RX = "session_baseline_rx"
        private const val KEY_SESSION_TX = "session_baseline_tx"
        private const val KEY_ALERT_CYCLE = "alert_cycle"
        private const val KEY_ALERT_LEVEL = "alert_level"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LAYOUT_DIRECTION = "layout_direction"
        private const val KEY_SPEED_DOWNLOAD = "speed_test_download"
        private const val KEY_SPEED_UPLOAD = "speed_test_upload"
        private const val KEY_SPEED_TIME = "speed_test_time"
        private const val KEY_SPEED_FAST = "speed_test_source_fast"
        private const val KEY_SPEED_UPLOAD_OK = "speed_test_upload_ok"
        private const val KEY_SPEED_DATA_BYTES = "speed_test_data_bytes"
        private const val KEY_SPEED_WARNING_SNOOZED_UNTIL = "speed_test_warning_snoozed_until"
        private const val KEY_SPEED_HISTORY = "speed_test_history"
    }
}
