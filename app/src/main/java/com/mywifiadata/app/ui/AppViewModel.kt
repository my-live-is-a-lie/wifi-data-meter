package com.mywifiadata.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.TimeRanges
import com.mywifiadata.app.core.UsagePeriod
import com.mywifiadata.app.data.UsageRepository
import com.mywifiadata.app.data.model.AppSettings
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.DayUsage
import com.mywifiadata.app.data.model.SpeedSample
import com.mywifiadata.app.data.model.TrafficTotals
import com.mywifiadata.app.data.model.WifiStatus
import com.mywifiadata.app.monitor.MonitorService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * حالة الواجهة الكاملة.
 */
data class UiState(
    val now: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val wifi: WifiStatus = WifiStatus(),
    val today: TrafficTotals = TrafficTotals.Empty,
    val cycle: TrafficTotals = TrafficTotals.Empty,
    val session: TrafficTotals = TrafficTotals.Empty,
    val samples: List<SpeedSample> = emptyList(),
    val daily: List<DayUsage> = emptyList(),
    val apps: List<AppUsage> = emptyList(),
    val isAppsLoading: Boolean = false,
    val appsPeriod: UsagePeriod = UsagePeriod.MONTH,
    val historyPeriod: UsagePeriod = UsagePeriod.MONTH,
    val selectedDayIndex: Int? = null,
    val selectedDayApps: List<AppUsage> = emptyList(),
    val showSystemApps: Boolean = false,
) {
    val quotaBytes: Long get() = settings.quotaBytes

    val hasQuota: Boolean get() = settings.quotaBytes > 0L

    val quotaProgress: Float
        get() = if (hasQuota) {
            (cycle.totalBytes.toFloat() / settings.quotaBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val quotaExceeded: Boolean get() = hasQuota && cycle.totalBytes > settings.quotaBytes

    val remainingBytes: Long get() = (settings.quotaBytes - cycle.totalBytes).coerceAtLeast(0L)

    val cycleDays: Int get() = TimeRanges.daysBetween(cycleStartMillis, cycleEndMillis)

    val elapsedDays: Int
        get() = TimeRanges.daysBetween(TimeRanges.cycleRange(settings.cycleStartDay, now).startMillis, now)

    val daysLeft: Int get() = TimeRanges.daysLeftInCycle(settings.cycleStartDay, now)

    private val cycleStartMillis: Long get() = TimeRanges.cycleRange(settings.cycleStartDay, now).startMillis

    private val cycleEndMillis: Long get() = TimeRanges.cycleRange(settings.cycleStartDay, now).endMillis

    val averagePerDay: Long get() = cycle.totalBytes / elapsedDays.coerceAtLeast(1)

    val projectedBytes: Long get() = averagePerDay * cycleDays

    val currentSpeed: SpeedSample? get() = samples.lastOrNull()

    val sessionDurationMillis: Long get() = (now - settings.sessionStartMillis).coerceAtLeast(0L)

    val visibleApps: List<AppUsage>
        get() = if (showSystemApps) apps else apps.filterNot { it.isSystem }

    val historyDays: Int get() = TimeRanges.daysOf(historyPeriod)
}

/**
 * حامل حالة الشاشة الرئيسية: يحدّث القياسات دوريًا ويوفّر إجراءات المستخدم.
 */
class AppViewModel(private val repository: UsageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var lastTotals: TrafficTotals = TrafficTotals.Empty
    private var lastSampleMillis: Long = 0L
    private var appsLoadedAt: Long = 0L
    private var dailyLoadedAt: Long = 0L
    private var dailyLoadedDays: Int = 0

    init {
        viewModelScope.launch {
            repository.settingsStore.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch { refreshLoop() }
    }

    private suspend fun refreshLoop() {
        while (viewModelScope.isActive) {
            runCatching { refresh() }
            delay(REFRESH_INTERVAL_MILLIS)
        }
    }

    /** تحديث القراءات (الإجماليات، الشبكة، الحالة). */
    suspend fun refresh(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val settings = repository.settingsStore.current
        val hasAccess = repository.hasUsageAccess()

        val wifi = repository.wifiStatus()
        val today = if (hasAccess) repository.todayTotals(now) else TrafficTotals.Empty
        val cycle = if (hasAccess) {
            repository.cycleTotals(settings.cycleStartDay, now)
        } else {
            TrafficTotals.Empty
        }
        val session = if (hasAccess) {
            repository.deviceTotals(
                com.mywifiadata.app.core.TimeRange(settings.sessionStartMillis, now),
            )
        } else {
            TrafficTotals.Empty
        }

        val sample = buildSample(now, today)

        _uiState.update { state ->
            state.copy(
                now = now,
                isLoading = false,
                hasUsageAccess = hasAccess,
                settings = settings,
                wifi = wifi,
                today = today,
                cycle = cycle,
                session = session,
                samples = if (sample == null) state.samples else (state.samples + sample).takeLast(MAX_SAMPLES),
            )
        }

        val historyDays = TimeRanges.daysOf(_uiState.value.historyPeriod)
        if (hasAccess && (force || now - dailyLoadedAt > DAILY_TTL_MILLIS || dailyLoadedDays != historyDays)) {
            val daily = repository.dailyUsage(historyDays)
            dailyLoadedAt = now
            dailyLoadedDays = historyDays
            _uiState.update { it.copy(daily = daily) }
        }

        val appsPeriod = _uiState.value.appsPeriod
        if (hasAccess && (force || now - appsLoadedAt > APPS_TTL_MILLIS)) {
            loadApps(appsPeriod)
        }
    }

    private fun buildSample(now: Long, totals: TrafficTotals): SpeedSample? {
        val previousMillis = lastSampleMillis
        val previousTotals = lastTotals
        lastSampleMillis = now
        lastTotals = totals
        if (previousMillis <= 0L || now <= previousMillis) return null
        val seconds = (now - previousMillis) / 1000.0
        if (seconds <= 0.0) return null
        val rx = ((totals.rxBytes - previousTotals.rxBytes).coerceAtLeast(0L)) / seconds
        val tx = ((totals.txBytes - previousTotals.txBytes).coerceAtLeast(0L)) / seconds
        return SpeedSample(timestampMillis = now, rxBytesPerSecond = rx, txBytesPerSecond = tx)
    }

    private suspend fun loadApps(period: UsagePeriod) {
        _uiState.update { it.copy(isAppsLoading = true) }
        val range = TimeRanges.forPeriod(period)
        val apps = repository.perAppUsage(range)
        appsLoadedAt = System.currentTimeMillis()
        _uiState.update { it.copy(apps = apps, isAppsLoading = false) }
    }

    // ————————— إجراءات المستخدم —————————

    fun setAppsPeriod(period: UsagePeriod) {
        if (period == _uiState.value.appsPeriod) return
        _uiState.update { it.copy(appsPeriod = period, apps = emptyList()) }
        viewModelScope.launch { loadApps(period) }
    }

    fun setHistoryPeriod(period: UsagePeriod) {
        if (period == _uiState.value.historyPeriod) return
        _uiState.update { it.copy(historyPeriod = period, selectedDayIndex = null, selectedDayApps = emptyList()) }
        viewModelScope.launch {
            val daily = repository.dailyUsage(TimeRanges.daysOf(period))
            dailyLoadedDays = TimeRanges.daysOf(period)
            dailyLoadedAt = System.currentTimeMillis()
            _uiState.update { it.copy(daily = daily) }
        }
    }

    fun selectDay(index: Int?) {
        _uiState.update { it.copy(selectedDayIndex = index) }
        if (index == null) {
            _uiState.update { it.copy(selectedDayApps = emptyList()) }
            return
        }
        val day = _uiState.value.daily.getOrNull(index) ?: return
        viewModelScope.launch {
            val apps = repository.perAppUsage(
                com.mywifiadata.app.core.TimeRange(day.startMillis, day.endMillis),
            )
            _uiState.update { state ->
                if (state.selectedDayIndex == index) state.copy(selectedDayApps = apps) else state
            }
        }
    }

    fun setShowSystemApps(show: Boolean) = _uiState.update { it.copy(showSystemApps = show) }

    fun refreshNow() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            runCatching { refresh(force = true) }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun startTracking() {
        repository.settingsStore.update { it.copy(trackingEnabled = true) }
        MonitorService.start(repository.context)
    }

    fun stopTracking() {
        repository.settingsStore.update { it.copy(trackingEnabled = false) }
        MonitorService.stop(repository.context)
    }

    fun resetSession() {
        val now = System.currentTimeMillis()
        val baseline = _uiState.value.today
        repository.settingsStore.rebaseSession(baseline = baseline, startMillis = now)
        lastTotals = TrafficTotals.Empty
        lastSampleMillis = 0L
        _uiState.update { it.copy(samples = emptyList(), now = now) }
        refreshNow()
    }

    fun setQuotaBytes(bytes: Long) {
        repository.settingsStore.update { it.copy(quotaBytes = bytes.coerceAtLeast(0L)) }
        refreshNow()
    }

    fun setCycleStartDay(day: Int) {
        repository.settingsStore.update { it.copy(cycleStartDay = day.coerceIn(1, 28)) }
        refreshNow()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.settingsStore.update { it.copy(notificationsEnabled = enabled) }
    }

    fun setAlertThreshold(percent: Int) {
        repository.settingsStore.update { it.copy(alertThresholdPercent = percent.coerceIn(10, 100)) }
    }

    fun setSampleInterval(seconds: Int) {
        repository.settingsStore.update { it.copy(sampleIntervalSeconds = seconds.coerceIn(5, 300)) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        repository.settingsStore.update { it.copy(dynamicColorEnabled = enabled) }
    }

    fun clearData() {
        stopTracking()
        repository.settingsStore.clearAll()
        _uiState.update {
            UiState(
                isLoading = false,
                hasUsageAccess = repository.hasUsageAccess(),
                settings = repository.settingsStore.current,
            )
        }
        refreshNow()
    }

    /** نص سرعة جاهز للعرض في الشريط العلوي. */
    fun speedText(): String = Format.speed(_uiState.value.currentSpeed?.totalBytesPerSecond ?: 0.0)

    companion object {
        private const val REFRESH_INTERVAL_MILLIS = 4_000L
        private const val APPS_TTL_MILLIS = 30_000L
        private const val DAILY_TTL_MILLIS = 60_000L
        private const val MAX_SAMPLES = 30

        fun factory(repository: UsageRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { AppViewModel(repository) }
        }
    }
}
