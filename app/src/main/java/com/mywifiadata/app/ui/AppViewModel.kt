package com.mywifiadata.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mywifiadata.app.core.AppsSortOrder
import com.mywifiadata.app.core.AppsUsageScope
import com.mywifiadata.app.core.HistoryPlanSortOrder
import com.mywifiadata.app.core.HistorySection
import com.mywifiadata.app.core.HistoryYearMonth
import com.mywifiadata.app.core.UsageNetwork
import com.mywifiadata.app.core.appsCountOptions
import com.mywifiadata.app.core.appsUsageRange
import com.mywifiadata.app.core.availableHistoryMonths
import com.mywifiadata.app.core.availableHistoryYears
import com.mywifiadata.app.core.filterApps
import com.mywifiadata.app.core.isCustomHistoryMonth
import com.mywifiadata.app.core.resolveHistoryMonthForYear
import com.mywifiadata.app.core.within
import com.mywifiadata.app.core.AppLanguage
import com.mywifiadata.app.core.historyApps
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.LauncherIconVisibility
import com.mywifiadata.app.core.SamplingIntervals
import com.mywifiadata.app.core.SpeedTestDataUsage
import com.mywifiadata.app.core.SpeedTestEngine
import com.mywifiadata.app.core.TimeRange
import com.mywifiadata.app.core.TimeRanges
import com.mywifiadata.app.core.UsagePeriod
import com.mywifiadata.app.data.AppSearchHistory
import com.mywifiadata.app.data.UsageRepository
import com.mywifiadata.app.data.model.AccentColor
import com.mywifiadata.app.data.model.AdditionalPlanDraft
import com.mywifiadata.app.data.model.AppLayoutDirection
import com.mywifiadata.app.data.model.AppSettings
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.AppsFilter
import com.mywifiadata.app.data.model.CompletedPlanRecord
import com.mywifiadata.app.data.model.DayUsage
import com.mywifiadata.app.data.model.DashboardLayout
import com.mywifiadata.app.data.model.DataPlan
import com.mywifiadata.app.data.model.PlanDraft
import com.mywifiadata.app.data.model.PlanNetworkType
import com.mywifiadata.app.data.model.SpeedSample
import com.mywifiadata.app.data.model.SpeedTestRecord
import com.mywifiadata.app.data.model.ThemeMode
import com.mywifiadata.app.data.model.TrafficTotals
import com.mywifiadata.app.data.model.UsageSlice
import com.mywifiadata.app.data.model.WifiStatus
import com.mywifiadata.app.monitor.MonitorService
import com.mywifiadata.app.monitor.MonitoringTileService
import com.mywifiadata.app.widget.UsageWidgetProvider
import com.mywifiadata.app.widget.UsageWidgetUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * حالة اختبار السرعة في بطاقة اليوم.
 */
sealed interface SpeedTestState {
    /** لا نتيجة محفوظة بعد (لم يُجرَ أي اختبار). */
    data object Idle : SpeedTestState

    /** الاختبار قيد التشغيل مع مرحلته الحالية وسرعته اللحظية. */
    data class Running(
        val phase: SpeedTestEngine.Phase = SpeedTestEngine.Phase.PREPARING,
        val bitsPerSecond: Double = 0.0,
        val elapsedMillis: Long = 0L,
    ) : SpeedTestState

    /** نتيجة جاهزة (محفوظة محليًا وتظهر حتى بعد إعادة التشغيل). */
    data class Done(
        val downloadBitsPerSecond: Double,
        val uploadBitsPerSecond: Double,
        val timestampMillis: Long = System.currentTimeMillis(),
        val usedFastCom: Boolean = true,
        /** false إن تعذّر قياس الرفع (تُعرض شرطة بدل صفر). */
        val uploadMeasured: Boolean = true,
    ) : SpeedTestState

    /** فشل الاختبار مع سببه وتفاصيل المصادر التي جُرّبت. */
    data class Failed(
        val failure: SpeedTestEngine.Failure = SpeedTestEngine.Failure.ERROR,
        val detail: String? = null,
    ) : SpeedTestState
}

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
    val wifiPlanUsage: TrafficTotals? = null,
    val mobileCycle: TrafficTotals = TrafficTotals.Empty,
    val session: TrafficTotals = TrafficTotals.Empty,
    val samples: List<SpeedSample> = emptyList(),
    val speedTest: SpeedTestState = SpeedTestState.Idle,
    val speedTestHistory: List<SpeedTestRecord> = emptyList(),
    /**
     * تحذير استهلاك بيانات الهاتف قبل بدء اختبار السرعة، أو null إن لم يكن
     * هناك تحذير معروض. يستخدم [SpeedTestDataUsage.estimate] تقديرًا متكيّفًا.
     */
    val speedTestDataWarning: SpeedTestDataUsage.Estimate? = null,
    val daily: List<DayUsage> = emptyList(),
    /**
     * أعلى يوم استهلاكًا خلال آخر 30 يومًا — مرجع ثابت للمقارنة مهما كانت
     * الفترة المختارة في شاشة السجل (تُحمَّل بياناته مستقلة عن [daily]).
     */
    val monthPeak: DayUsage? = null,
    val apps: List<AppUsage> = emptyList(),
    /** تطبيقات اليوم للاستخدام في تفصيل الرفع/التنزيل/الإجمالي داخل لوحة اليوم. */
    val todayApps: List<AppUsage> = emptyList(),
    val isTodayAppsLoading: Boolean = false,
    // تطبيقات الفترة كاملة (لا تتأثر باختيار شريحة) لبطاقة «الفترة».
    val periodApps: List<AppUsage> = emptyList(),
    val isAppsLoading: Boolean = false,
    val appsPeriod: UsagePeriod = UsagePeriod.MONTH,
    val historyPeriod: UsagePeriod = UsagePeriod.MONTH,
    val historySection: HistorySection = HistorySection.NETWORK,
    val historyNetwork: UsageNetwork = UsageNetwork.WIFI,
    val historyPlanNetwork: UsageNetwork = UsageNetwork.WIFI,
    val historyPlanSortOrder: HistoryPlanSortOrder = HistoryPlanSortOrder.NEWEST_FIRST,
    val selectedCompletedPlan: CompletedPlanDetailState? = null,
    val historyYear: Int = TimeRanges.currentYearMonth(now).year,
    val historyMonth: Int = TimeRanges.currentYearMonth(now).month,
    val historyRecordedMonths: List<HistoryYearMonth> = emptyList(),
    val selectedDayIndex: Int? = null,
    val selectedDayApps: List<AppUsage> = emptyList(),
    val isSelectedDayAppsLoading: Boolean = false,
    val historySearchActive: Boolean = false,
    val historySearchQuery: String = "",
    val historyAppsSortOrder: AppsSortOrder = AppsSortOrder.HIGHEST_USAGE,
    val appsFilter: AppsFilter = AppsFilter.ALL,
    val appsNetwork: UsageNetwork = UsageNetwork.WIFI,
    val appsScope: AppsUsageScope = AppsUsageScope.ALL_TIME,
    val appsCountLimit: Int? = null,
    val appsSortOrder: AppsSortOrder = AppsSortOrder.HIGHEST_USAGE,
    val appsSearchActive: Boolean = false,
    val appsSearchQuery: String = "",
    // تفصيل قسم التطبيقات: أسابيع/أيام/ساعات مع الشريحة المختارة حاليًا.
    val appsWeeks: List<UsageSlice> = emptyList(),
    val appsDays: List<UsageSlice> = emptyList(),
    val appsWeekDays: List<UsageSlice> = emptyList(),
    val appsHours: List<UsageSlice> = emptyList(),
    val appsWeek: TimeRange? = null,
    val appsSlice: TimeRange? = null,
    val appDetail: AppDetailState? = null,
) {
    /** الشبكة النشطة لعرض الباقة: الواي فاي عند تفعيله، وبيانات الهاتف عند تفعيلها دون واي فاي. */
    val activePlanNetwork: UsageNetwork
        get() = if (wifi.isMobileConnected && !wifi.isWifiConnected) {
            UsageNetwork.MOBILE
        } else {
            UsageNetwork.WIFI
        }

    val activePlan: DataPlan
        get() = settings.planFor(activePlanNetwork)

    val activeCycle: TrafficTotals
        get() = planUsage(activePlanNetwork)

    val quotaBytes: Long get() = activePlan.quotaBytes

    val hasQuota: Boolean get() = activePlan.hasQuota

    val quotaProgress: Float
        get() = if (hasQuota) {
            (activeCycle.totalBytes.toFloat() / activePlan.quotaBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val quotaExceeded: Boolean get() = hasQuota && activeCycle.totalBytes > activePlan.quotaBytes

    val remainingBytes: Long get() = planRemainingBytes(activePlanNetwork)

    val cycleDays: Int
        get() = planTotalDays(activePlanNetwork)

    val elapsedDays: Int
        get() = TimeRanges.daysBetween(TimeRanges.cycleRange(settings.cycleStartDay, now).startMillis, now)

    val daysLeft: Int
        get() = planDaysLeft(activePlanNetwork)

    private val cycleStartMillis: Long get() = TimeRanges.cycleRange(settings.cycleStartDay, now).startMillis

    private val cycleEndMillis: Long get() = TimeRanges.cycleRange(settings.cycleStartDay, now).endMillis

    fun planFor(network: UsageNetwork): DataPlan = settings.planFor(network)

    fun planUsage(network: UsageNetwork): TrafficTotals = when (network) {
        UsageNetwork.WIFI -> wifiPlanUsage ?: if (settings.planPendingActivation) TrafficTotals.Empty else cycle
        UsageNetwork.MOBILE -> mobileCycle
    }

    fun planRemainingBytes(network: UsageNetwork): Long {
        val plan = settings.planFor(network)
        return (plan.quotaBytes - planUsage(network).totalBytes).coerceAtLeast(0L)
    }

    fun planDaysLeft(network: UsageNetwork): Int {
        val plan = settings.planFor(network)
        return when {
            plan.pendingActivation -> plan.totalDays
            plan.activatedAtMillis > 0L -> plan.daysLeft(now)
            network == UsageNetwork.MOBILE -> plan.daysLeft(now)
            else -> TimeRanges.daysLeftInCycle(settings.cycleStartDay, now)
        }
    }

    fun planTotalDays(network: UsageNetwork): Int {
        val plan = settings.planFor(network)
        return if (plan.pendingActivation || plan.activatedAtMillis > 0L || network == UsageNetwork.MOBILE) {
            plan.totalDays
        } else {
            TimeRanges.daysBetween(cycleStartMillis, cycleEndMillis)
        }
    }

    val averagePerDay: Long get() = cycle.totalBytes / elapsedDays.coerceAtLeast(1)

    val projectedBytes: Long get() = averagePerDay * TimeRanges.daysBetween(cycleStartMillis, cycleEndMillis)

    val currentSpeed: SpeedSample? get() = samples.lastOrNull()

    val sessionDurationMillis: Long get() = (now - settings.sessionStartMillis).coerceAtLeast(0L)

    val filteredApps: List<AppUsage> get() = filterApps(apps, appsFilter)

    val visibleApps: List<AppUsage>
        get() {
            val sorted = filterApps(apps, appsFilter, sortOrder = appsSortOrder)
            val matching = if (appsSearchActive) {
                AppSearchHistory.search(sorted, appsSearchQuery, settings.appSearchHistory)
            } else sorted
            return appsCountLimit?.let { matching.take(it) } ?: matching
        }

    // The top-N control limits rows only, never the full-period summary or percentages.
    val visiblePeriodApps: List<AppUsage> get() = filterApps(periodApps, appsFilter)

    fun appsRange(now: Long = this.now): TimeRange =
        appsUsageRange(appsNetwork, appsScope, settings.cycleStartDay, now)

    /** Period buttons always control the summary and list; Wi-Fi scope only narrows that period. */
    fun appsPeriodRange(now: Long = this.now): TimeRange =
        TimeRanges.forPeriod(appsPeriod, now).within(appsRange(now))

    val visibleHistoryApps: List<AppUsage>
        get() = historyApps(selectedDayApps, historySearchActive, historySearchQuery, historyAppsSortOrder)

    val historyDays: Int get() = TimeRanges.daysOf(historyPeriod)

    val currentHistoryYearMonth: HistoryYearMonth
        get() = TimeRanges.currentYearMonth(now)

    val isCustomHistoryDate: Boolean
        get() = isCustomHistoryMonth(historyYear, historyMonth, currentHistoryYearMonth)

    val availableHistoryYears: List<Int>
        get() = availableHistoryYears(historyRecordedMonths)

    val availableHistoryMonths: List<Int>
        get() = availableHistoryMonths(historyRecordedMonths, historyYear)

    val visibleCompletedPlans: List<CompletedPlanRecord>
        get() {
            val list = settings.completedPlansFor(historyPlanNetwork)
            return when (historyPlanSortOrder) {
                HistoryPlanSortOrder.NEWEST_FIRST -> list.sortedByDescending { it.endMillis }
                HistoryPlanSortOrder.OLDEST_FIRST -> list.sortedBy { it.endMillis }
            }
        }
}

/**
 * حالة صفحة إحصائيات استهلاك التطبيقات لباقة منتهية محددة.
 */
data class CompletedPlanDetailState(
    val record: CompletedPlanRecord,
    val network: UsageNetwork = UsageNetwork.WIFI,
    val isLoading: Boolean = true,
    val totals: TrafficTotals = TrafficTotals.Empty,
    val apps: List<AppUsage> = emptyList(),
    val searchActive: Boolean = false,
    val searchQuery: String = "",
    val sortOrder: AppsSortOrder = AppsSortOrder.HIGHEST_USAGE,
) {
    val visibleApps: List<AppUsage>
        get() = historyApps(apps, searchActive, searchQuery, sortOrder)
}

/**
 * تفاصيل استهلاك تطبيق واحد في الفترات الثلاث (اليوم/الأسبوع/الشهر).
 */
data class AppDetailState(
    val app: AppUsage,
    val network: UsageNetwork = UsageNetwork.WIFI,
    val isLoading: Boolean = true,
    val today: TrafficTotals = TrafficTotals.Empty,
    val week: TrafficTotals = TrafficTotals.Empty,
    val month: TrafficTotals = TrafficTotals.Empty,
)

/**
 * حامل حالة الشاشة الرئيسية: يحدّث القياسات دوريًا ويوفّر إجراءات المستخدم.
 */
class AppViewModel(private val repository: UsageRepository) : ViewModel() {

    // تُستعاد الإعدادات وآخر نتيجة اختبار سرعة فور إنشاء الحالة (قبل أي تحديث).
    private val initialSettings = repository.settingsStore.current
    private val _uiState = MutableStateFlow(
        UiState(
            settings = initialSettings,
            speedTest = initialSettings.lastSpeedTest.toSpeedTestState(),
            speedTestHistory = initialSettings.speedTestHistory,
        ),
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var lastTotals: TrafficTotals = TrafficTotals.Empty
    private var lastSampleMillis: Long = 0L
    private var appsLoadedAt: Long = 0L
    private var appsRequestId = 0L
    private var slicesRequestId = 0L
    private var weekDaysRequestId = 0L
    private var todayAppsLoadedAt: Long = 0L
    private var todayAppsLoadedRangeStartMillis: Long = 0L
    private var todayAppsJob: Job? = null
    private var selectedDayAppsJob: Job? = null
    private var historyRequestId = 0L
    private var dailyLoadedAt: Long = 0L
    private var dailyLoadedNetwork: UsageNetwork? = null
    private var dailyLoadedYear: Int = 0
    private var dailyLoadedMonth: Int = 0

    init {
        viewModelScope.launch {
            repository.settingsStore.settings.collect { settings ->
                _uiState.update {
                    it.copy(settings = settings, speedTestHistory = settings.speedTestHistory)
                }
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
        var settings = repository.settingsStore.current
        val hasAccess = repository.hasUsageAccess()

        val wifi = repository.wifiStatus()
        val today = if (hasAccess) repository.todayTotals(now) else TrafficTotals.Empty
        val cycle = if (hasAccess) {
            repository.cycleTotals(settings.cycleStartDay, now)
        } else {
            TrafficTotals.Empty
        }
        var wifiPlanUsage = if (hasAccess) {
            repository.planTotals(settings, now)
        } else {
            TrafficTotals.Empty
        }
        var mobileCycle = if (hasAccess) {
            repository.mobilePlanTotals(settings, now)
        } else {
            TrafficTotals.Empty
        }

        val promotedSettings = settings.promoteExpiredPlansIfNeeded(
            wifiUsedBytes = wifiPlanUsage.totalBytes,
            mobileUsedBytes = mobileCycle.totalBytes,
            now = now,
        )
        if (promotedSettings != settings) {
            repository.settingsStore.update { promotedSettings }
            settings = repository.settingsStore.current
            wifiPlanUsage = if (hasAccess) repository.planTotals(settings, now) else TrafficTotals.Empty
            mobileCycle = if (hasAccess) repository.mobilePlanTotals(settings, now) else TrafficTotals.Empty
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
            val currentYm = TimeRanges.currentYearMonth(now)
            val keepCustomDate = state.isCustomHistoryDate
            state.copy(
                now = now,
                isLoading = false,
                hasUsageAccess = hasAccess,
                settings = settings,
                speedTestHistory = settings.speedTestHistory,
                wifi = wifi,
                today = today,
                cycle = cycle,
                wifiPlanUsage = wifiPlanUsage,
                mobileCycle = mobileCycle,
                session = session,
                historyYear = if (keepCustomDate) state.historyYear else currentYm.year,
                historyMonth = if (keepCustomDate) state.historyMonth else currentYm.month,
                samples = if (sample == null) state.samples else (state.samples + sample).takeLast(MAX_SAMPLES),
            )
        }
        val currentState = _uiState.value
        UsageWidgetUpdater.publish(
            context = repository.context,
            todayDownload = today.rxBytes,
            todayUpload = today.txBytes,
            cycleUsage = currentState.activeCycle.totalBytes,
            quota = currentState.quotaBytes,
            daysLeft = currentState.daysLeft,
            styleIndex = settings.usageCardStyleIndex,
        )

        val sameHistorySelection = dailyLoadedNetwork == currentState.historyNetwork &&
            dailyLoadedYear == currentState.historyYear &&
            dailyLoadedMonth == currentState.historyMonth
        if (hasAccess && (force || now - dailyLoadedAt > DAILY_TTL_MILLIS || !sameHistorySelection)) {
            loadHistory(now = now)
        }

        if (hasAccess && (force || now - appsLoadedAt > APPS_TTL_MILLIS)) {
            loadApps()
            loadAppsSlices()
            repository.captureUsageArchive(now)
        }
    }

    /**
     * يحمّل إحصاءات أيام الشهر والسنة المحددين في شاشة السجل مع الأشهر والسنوات
     * التي سُجّل فيها استهلاك فعلي للشبكة المختارة.
     */
    private suspend fun loadHistory(
        now: Long = System.currentTimeMillis(),
        adjustSelectionToRecorded: Boolean = false,
    ) {
        val requestId = ++historyRequestId
        val selection = _uiState.value
        val network = selection.historyNetwork
        val recorded = repository.recordedHistoryMonths(network, now)
        val currentYm = TimeRanges.currentYearMonth(now)
        var targetYear = selection.historyYear
        var targetMonth = selection.historyMonth

        if (adjustSelectionToRecorded && isCustomHistoryMonth(targetYear, targetMonth, currentYm)) {
            if (HistoryYearMonth(targetYear, targetMonth) !in recorded) {
                targetYear = currentYm.year
                targetMonth = currentYm.month
            }
        }

        val daily = repository.dailyUsageForMonth(targetYear, targetMonth, network, now)
        val targetYm = HistoryYearMonth(targetYear, targetMonth)
        val finalRecorded = if (daily.any { it.totals.totalBytes > 0L } && targetYm !in recorded) {
            (recorded + targetYm).sorted()
        } else {
            recorded
        }

        if (requestId != historyRequestId || _uiState.value.historyNetwork != network) return
        dailyLoadedAt = System.currentTimeMillis()
        dailyLoadedNetwork = network
        dailyLoadedYear = targetYear
        dailyLoadedMonth = targetMonth
        _uiState.update { state ->
            state.copy(
                historyYear = targetYear,
                historyMonth = targetMonth,
                historyRecordedMonths = finalRecorded,
                daily = daily,
                monthPeak = daily.maxByOrNull { it.totals.totalBytes },
            )
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

    private fun UiState.hasSameAppsSelection(other: UiState): Boolean =
        appsNetwork == other.appsNetwork && appsScope == other.appsScope &&
            settings.cycleStartDay == other.settings.cycleStartDay &&
            appsPeriod == other.appsPeriod && appsWeek == other.appsWeek && appsSlice == other.appsSlice

    /**
     * تحميل التطبيقات للنطاق الحالي: الشريحة المختارة (ساعة/يوم) إن وُجدت،
     * وإلا الفترة الكاملة المختارة.
     */
    private suspend fun loadApps() {
        val requestId = ++appsRequestId
        val selection = _uiState.value
        val periodRange = selection.appsPeriodRange(System.currentTimeMillis())
        val range = selection.appsSlice?.within(periodRange) ?: periodRange
        _uiState.update { it.copy(isAppsLoading = true) }
        val periodApps = repository.perAppUsage(periodRange, selection.appsNetwork)
        val apps = if (range == periodRange) periodApps else repository.perAppUsage(range, selection.appsNetwork)
        if (requestId != appsRequestId || !_uiState.value.hasSameAppsSelection(selection)) return
        appsLoadedAt = System.currentTimeMillis()
        _uiState.update {
            it.copy(apps = apps, periodApps = periodApps, isAppsLoading = false)
        }
    }

    /** Charts use the same network and cycle bounds as the app list. */
    private suspend fun loadAppsSlices() {
        val requestId = ++slicesRequestId
        val selection = _uiState.value
        val now = System.currentTimeMillis()
        val bounds = selection.appsRange(now)
        suspend fun slices(ranges: List<TimeRange>): List<UsageSlice> = ranges
            .map { it.within(bounds) }
            .filter { it.endMillis > it.startMillis }
            .map { it.toSlice(repository.deviceTotals(it, selection.appsNetwork)) }
            .filter { it.totals.totalBytes > 0L }

        val hours = slices(TimeRanges.hoursOfToday(now))
        val days = slices(TimeRanges.lastDays(7, now))
        val weeks = slices(TimeRanges.lastWeeks(now = now))
        if (requestId != slicesRequestId || !_uiState.value.hasSameAppsSelection(selection)) return
        _uiState.update { it.copy(appsHours = hours, appsDays = days, appsWeeks = weeks) }
        selection.appsWeek?.let { loadAppsWeekDays(it) }
    }

    private suspend fun loadAppsWeekDays(week: TimeRange) {
        val requestId = ++weekDaysRequestId
        val selection = _uiState.value
        val bounds = selection.appsPeriodRange(System.currentTimeMillis())
        val days = TimeRanges.daysInRange(week)
            .map { it.within(bounds) }
            .filter { it.endMillis > it.startMillis }
            .map { it.toSlice(repository.deviceTotals(it, selection.appsNetwork)) }
            .filter { it.totals.totalBytes > 0L }
        if (requestId != weekDaysRequestId || !_uiState.value.hasSameAppsSelection(selection)) return
        _uiState.update { it.copy(appsWeekDays = days) }
    }

    private fun TimeRange.toSlice(totals: TrafficTotals) =
        UsageSlice(startMillis = startMillis, endMillis = endMillis, totals = totals)

    // ————————— إجراءات المستخدم —————————

    fun setAppsPeriod(period: UsagePeriod) {
        if (period == _uiState.value.appsPeriod) return
        // نُبقي القوائم والشرائح الحالية ظاهرة حتى تصل بيانات الفترة الجديدة
        // (بلا مسح يسبّب دائرة التحميل والوميض)؛ يُصفَّر الاختيار فقط.
        _uiState.update {
            it.copy(
                appsPeriod = period,
                appsWeek = null,
                appsSlice = null,
            )
        }
        viewModelScope.launch {
            loadApps()
            loadAppsSlices()
        }
    }

    /** اختيار/إلغاء اختيار أسبوع في عرض الشهر، مع تحميل أيامه — بدون مسح
     *  القوائم الحالية حتى لا تومض الشاشة أثناء التحميل. */
    fun selectAppsWeek(week: TimeRange) {
        val selected = if (_uiState.value.appsWeek == week) null else week
        _uiState.update { it.copy(appsWeek = selected, appsSlice = null) }
        viewModelScope.launch {
            selected?.let { loadAppsWeekDays(it) }
            loadApps()
        }
    }

    /** اختيار/إلغاء اختيار شريحة (يوم أو ساعة) لعرض تطبيقاتها — بدون مسح
     *  القائمة الحالية حتى لا تومض الشاشة أو تظهر دائرة تحميل عند كل ضغطة. */
    fun selectAppsSlice(slice: TimeRange) {
        val selected = if (_uiState.value.appsSlice == slice) null else slice
        _uiState.update { it.copy(appsSlice = selected) }
        viewModelScope.launch { loadApps() }
    }

    fun setAppsFilter(filter: AppsFilter) = _uiState.update { it.copy(appsFilter = filter) }

    fun setAppsNetwork(network: UsageNetwork) {
        if (network == _uiState.value.appsNetwork) return
        reloadAppsSelection { it.copy(appsNetwork = network) }
    }

    fun setAppsScope(scope: AppsUsageScope) {
        if (scope == _uiState.value.appsScope) return
        reloadAppsSelection { it.copy(appsScope = scope) }
    }

    fun setAppsSearchActive(active: Boolean) {
        _uiState.update { it.copy(appsSearchActive = active, appsSearchQuery = if (active) it.appsSearchQuery else "") }
    }

    fun setAppsSearchQuery(query: String) {
        _uiState.update { it.copy(appsSearchQuery = query) }
    }

    /** Called only by rows on the apps page, not dashboard/history detail navigation. */
    fun openAppsListDetail(app: AppUsage) {
        val state = _uiState.value
        if (state.appsSearchActive) {
            repository.settingsStore.update {
                it.copy(appSearchHistory = AppSearchHistory.record(it.appSearchHistory, app, System.currentTimeMillis()))
            }
            _uiState.update { it.copy(settings = repository.settingsStore.current) }
        }
        openAppDetail(app, state.appsNetwork)
    }

    fun setAppsSortOrder(sortOrder: AppsSortOrder) {
        _uiState.update { it.copy(appsSortOrder = sortOrder) }
    }

    fun reverseAppsSortOrder() {
        _uiState.update { it.copy(appsSortOrder = it.appsSortOrder.reversed()) }
    }

    fun setAppsCountLimit(limit: Int?) {
        if (limit !in appsCountOptions) return
        _uiState.update { it.copy(appsCountLimit = limit) }
    }

    private fun reloadAppsSelection(change: (UiState) -> UiState) {
        _uiState.update {
            change(it).copy(
                apps = emptyList(), periodApps = emptyList(), isAppsLoading = true,
                appsWeek = null, appsSlice = null, appsWeeks = emptyList(),
                appsDays = emptyList(), appsWeekDays = emptyList(), appsHours = emptyList(),
            )
        }
        viewModelScope.launch {
            loadApps()
            loadAppsSlices()
        }
    }

    fun setHistorySection(section: HistorySection) {
        if (section == _uiState.value.historySection) return
        _uiState.update {
            it.copy(
                historySection = section,
                selectedCompletedPlan = if (section == HistorySection.PLAN) it.selectedCompletedPlan else null,
                historySearchActive = if (section == HistorySection.NETWORK) it.historySearchActive else false,
                historySearchQuery = if (section == HistorySection.NETWORK) it.historySearchQuery else "",
            )
        }
    }

    fun setHistoryPlanNetwork(network: UsageNetwork) {
        if (network == _uiState.value.historyPlanNetwork) return
        _uiState.update {
            it.copy(
                historyPlanNetwork = network,
                selectedCompletedPlan = null,
            )
        }
    }

    fun setHistoryPlanSortOrder(sortOrder: HistoryPlanSortOrder) {
        if (sortOrder == _uiState.value.historyPlanSortOrder) return
        _uiState.update {
            it.copy(historyPlanSortOrder = sortOrder)
        }
    }

    fun openCompletedPlanDetail(record: CompletedPlanRecord) {
        val network = _uiState.value.historyPlanNetwork
        _uiState.update {
            it.copy(
                selectedCompletedPlan = CompletedPlanDetailState(
                    record = record,
                    network = network,
                    isLoading = true,
                ),
            )
        }
        viewModelScope.launch {
            val range = TimeRange(
                startMillis = record.startMillis,
                endMillis = record.endMillis.coerceAtLeast(record.startMillis),
            )
            val totals = if (range.endMillis > range.startMillis) {
                repository.deviceTotals(range, network)
            } else {
                TrafficTotals.Empty
            }
            val apps = if (range.endMillis > range.startMillis) {
                repository.perAppUsage(range, network)
            } else {
                emptyList()
            }
            _uiState.update { state ->
                val current = state.selectedCompletedPlan
                if (current == null || current.record.id != record.id || current.network != network) {
                    state
                } else {
                    state.copy(
                        selectedCompletedPlan = current.copy(
                            isLoading = false,
                            totals = totals,
                            apps = apps,
                        ),
                    )
                }
            }
        }
    }

    fun closeCompletedPlanDetail() {
        _uiState.update { it.copy(selectedCompletedPlan = null) }
    }

    fun setCompletedPlanSearchActive(active: Boolean) {
        _uiState.update { state ->
            val current = state.selectedCompletedPlan ?: return@update state
            state.copy(
                selectedCompletedPlan = current.copy(
                    searchActive = active,
                    searchQuery = if (active) current.searchQuery else "",
                ),
            )
        }
    }

    fun setCompletedPlanSearchQuery(query: String) {
        _uiState.update { state ->
            val current = state.selectedCompletedPlan ?: return@update state
            state.copy(
                selectedCompletedPlan = current.copy(searchQuery = query),
            )
        }
    }

    fun reverseCompletedPlanAppsSortOrder() {
        _uiState.update { state ->
            val current = state.selectedCompletedPlan ?: return@update state
            state.copy(
                selectedCompletedPlan = current.copy(
                    sortOrder = current.sortOrder.reversed(),
                ),
            )
        }
    }

    fun setHistoryNetwork(network: UsageNetwork) {
        if (network == _uiState.value.historyNetwork) return
        selectedDayAppsJob?.cancel()
        _uiState.update {
            it.copy(
                historyNetwork = network,
                selectedDayIndex = null,
                selectedDayApps = emptyList(),
                isSelectedDayAppsLoading = false,
                historySearchActive = false,
                historySearchQuery = "",
            )
        }
        viewModelScope.launch {
            loadHistory(adjustSelectionToRecorded = true)
        }
    }

    fun setHistoryMonth(month: Int) {
        val safeMonth = month.coerceIn(1, 12)
        if (safeMonth == _uiState.value.historyMonth) return
        selectedDayAppsJob?.cancel()
        _uiState.update {
            it.copy(
                historyMonth = safeMonth,
                selectedDayIndex = null,
                selectedDayApps = emptyList(),
                isSelectedDayAppsLoading = false,
                historySearchActive = false,
                historySearchQuery = "",
            )
        }
        viewModelScope.launch {
            loadHistory()
        }
    }

    fun setHistoryYear(year: Int) {
        if (year == _uiState.value.historyYear) return
        selectedDayAppsJob?.cancel()
        val targetMonth = resolveHistoryMonthForYear(
            recordedMonths = _uiState.value.historyRecordedMonths,
            targetYear = year,
            currentSelectedMonth = _uiState.value.historyMonth,
        )
        _uiState.update {
            it.copy(
                historyYear = year,
                historyMonth = targetMonth,
                selectedDayIndex = null,
                selectedDayApps = emptyList(),
                isSelectedDayAppsLoading = false,
                historySearchActive = false,
                historySearchQuery = "",
            )
        }
        viewModelScope.launch {
            loadHistory()
        }
    }

    fun resetHistoryDate() {
        val currentYm = TimeRanges.currentYearMonth(System.currentTimeMillis())
        if (_uiState.value.historyYear == currentYm.year && _uiState.value.historyMonth == currentYm.month) return
        selectedDayAppsJob?.cancel()
        _uiState.update {
            it.copy(
                historyYear = currentYm.year,
                historyMonth = currentYm.month,
                selectedDayIndex = null,
                selectedDayApps = emptyList(),
                isSelectedDayAppsLoading = false,
                historySearchActive = false,
                historySearchQuery = "",
            )
        }
        viewModelScope.launch {
            loadHistory()
        }
    }

    fun setHistoryPeriod(period: UsagePeriod) {
        if (period == _uiState.value.historyPeriod) return
        selectedDayAppsJob?.cancel()
        _uiState.update {
            it.copy(
                historyPeriod = period, selectedDayIndex = null, selectedDayApps = emptyList(),
                isSelectedDayAppsLoading = false, historySearchActive = false, historySearchQuery = "",
            )
        }
        viewModelScope.launch {
            loadHistory()
        }
    }

    /** فتح تفاصيل تطبيق واحد (اليوم/الأسبوع/الشهر). */
    fun openAppDetail(app: AppUsage, network: UsageNetwork = UsageNetwork.WIFI) {
        _uiState.update { it.copy(appDetail = AppDetailState(app = app, network = network, isLoading = true)) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val today = repository.appTotals(app.uid, TimeRanges.forPeriod(UsagePeriod.TODAY, now), network, app.packageName)
            val week = repository.appTotals(app.uid, TimeRanges.forPeriod(UsagePeriod.WEEK, now), network, app.packageName)
            val month = repository.appTotals(app.uid, TimeRanges.forPeriod(UsagePeriod.MONTH, now), network, app.packageName)
            _uiState.update { state ->
                val detail = state.appDetail
                if (detail == null || detail.app.uid != app.uid || detail.network != network) {
                    state
                } else {
                    state.copy(
                        appDetail = detail.copy(
                            isLoading = false,
                            today = today,
                            week = week,
                            month = month,
                        ),
                    )
                }
            }
        }
    }

    /** إغلاق تفاصيل التطبيق. */
    fun closeAppDetail() {
        _uiState.update { it.copy(appDetail = null) }
    }

    fun setHistorySearchActive(active: Boolean) {
        _uiState.update {
            val enabled = active && it.selectedDayIndex != null
            it.copy(historySearchActive = enabled, historySearchQuery = if (enabled) it.historySearchQuery else "")
        }
    }

    fun setHistorySearchQuery(query: String) {
        _uiState.update { it.copy(historySearchQuery = query) }
    }

    fun reverseHistoryAppsSortOrder() {
        _uiState.update { it.copy(historyAppsSortOrder = it.historyAppsSortOrder.reversed()) }
    }

    fun selectDay(index: Int?) {
        val stateSnapshot = _uiState.value
        val day = index?.let { stateSnapshot.daily.getOrNull(it) }
        val network = stateSnapshot.historyNetwork
        selectedDayAppsJob?.cancel()
        // Never search the previous day's rows while the new selection is still loading.
        _uiState.update {
            it.copy(
                selectedDayIndex = index.takeIf { day != null }, selectedDayApps = emptyList(),
                isSelectedDayAppsLoading = day != null, historySearchActive = false, historySearchQuery = "",
            )
        }
        if (day == null) return
        selectedDayAppsJob = viewModelScope.launch {
            val apps = repository.perAppUsage(
                com.mywifiadata.app.core.TimeRange(day.startMillis, day.endMillis),
                network,
            )
            _uiState.update { state ->
                val currentDay = state.selectedDayIndex?.let { state.daily.getOrNull(it) }
                if (state.historyNetwork == network &&
                    currentDay?.startMillis == day.startMillis &&
                    currentDay.endMillis == day.endMillis
                ) {
                    state.copy(selectedDayApps = apps, isSelectedDayAppsLoading = false)
                } else state
            }
        }
    }

    /** تحميل استهلاك تطبيقات اليوم عند فتح أحد تفاصيل بطاقة «اليوم». */
    fun loadTodayApps() {
        if (!_uiState.value.hasUsageAccess || todayAppsJob?.isActive == true) return

        val now = System.currentTimeMillis()
        val range = TimeRanges.forPeriod(UsagePeriod.TODAY, now)
        if (
            range.startMillis == todayAppsLoadedRangeStartMillis &&
            now - todayAppsLoadedAt <= TODAY_APPS_TTL_MILLIS
        ) {
            return
        }

        todayAppsJob = viewModelScope.launch {
            _uiState.update { it.copy(isTodayAppsLoading = true) }
            val loadedAt = System.currentTimeMillis()
            val todayRange = TimeRanges.forPeriod(UsagePeriod.TODAY, loadedAt)
            val apps = try {
                repository.perAppUsage(todayRange)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            todayAppsLoadedAt = loadedAt
            todayAppsLoadedRangeStartMillis = todayRange.startMillis
            _uiState.update { it.copy(todayApps = apps, isTodayAppsLoading = false) }
        }
    }

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
        MonitoringTileService.requestRefresh(repository.context)
    }

    fun stopTracking() {
        repository.settingsStore.update { it.copy(trackingEnabled = false) }
        MonitorService.stop(repository.context)
        MonitoringTileService.requestRefresh(repository.context)
    }

    fun setQuickSettingsTileEnabled(enabled: Boolean) {
        val settingsStore = repository.settingsStore
        if (!enabled && settingsStore.current.appIconHidden) {
            if (!LauncherIconVisibility.setVisible(repository.context, visible = true)) return
        }
        settingsStore.update {
            it.copy(
                quickSettingsTileEnabled = enabled,
                appIconHidden = if (enabled) it.appIconHidden else false,
            )
        }
        MonitoringTileService.requestRefresh(repository.context)
    }

    fun setQuickSettingsTileAdded(added: Boolean) {
        repository.settingsStore.update { it.copy(quickSettingsTileAdded = added) }
        MonitoringTileService.requestRefresh(repository.context)
    }

    fun setAppLanguage(languageTag: String) {
        val language = AppLanguage.normalize(languageTag)
        repository.settingsStore.update { it.copy(appLanguage = language) }
        // نصوص الأداة بلغة التطبيق أيضًا، فنعيد رسمها فور تغيير اللغة.
        UsageWidgetProvider.updateAll(repository.context)
    }

    /** Changes the launcher alias only after the recovery tile has actually been added. */
    fun setAppIconHidden(hidden: Boolean): Boolean {
        val settings = repository.settingsStore.current
        if (hidden && (!settings.quickSettingsTileEnabled || !settings.quickSettingsTileAdded)) {
            return false
        }
        if (!LauncherIconVisibility.setVisible(repository.context, visible = !hidden)) return false
        repository.settingsStore.update { it.copy(appIconHidden = hidden) }
        return true
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

    fun savePlan(draft: PlanDraft) {
        val targetNetworks = when (draft.networkType) {
            PlanNetworkType.WIFI -> listOf(UsageNetwork.WIFI)
            PlanNetworkType.MOBILE -> listOf(UsageNetwork.MOBILE)
            PlanNetworkType.BOTH -> listOf(UsageNetwork.WIFI, UsageNetwork.MOBILE)
        }
        repository.settingsStore.update { settings ->
            var updated = settings.copy(planNetworkType = draft.networkType)
            for (network in targetNetworks) {
                val currentPlan = updated.planFor(network)
                val isLockedUpdate = currentPlan.hasQuota && currentPlan.preventEdit
                val nextPlan = if (isLockedUpdate) {
                    currentPlan.copy(
                        name = draft.name.trim(),
                        logoPath = draft.logoPath,
                    )
                } else {
                    val newQuotaBytes = draft.quotaBytes
                    val safeDuration = draft.durationValue.coerceAtLeast(1)
                    val isNewOrChangedPlan = !currentPlan.hasQuota ||
                        currentPlan.quotaBytes != newQuotaBytes ||
                        currentPlan.durationValue != safeDuration ||
                        currentPlan.durationUnit != draft.durationUnit
                    DataPlan(
                        id = currentPlan.id.ifBlank { java.util.UUID.randomUUID().toString() },
                        name = draft.name.trim(),
                        logoPath = draft.logoPath,
                        quotaBytes = newQuotaBytes,
                        capacityUnit = draft.capacityUnit,
                        durationValue = safeDuration,
                        durationUnit = draft.durationUnit,
                        preventEdit = if (newQuotaBytes > 0L) draft.preventEdit else false,
                        pendingActivation = when {
                            newQuotaBytes <= 0L -> false
                            isNewOrChangedPlan -> true
                            else -> currentPlan.pendingActivation
                        },
                        activatedAtMillis = when {
                            newQuotaBytes <= 0L || isNewOrChangedPlan -> 0L
                            else -> currentPlan.activatedAtMillis
                        },
                        additionalPlans = if (newQuotaBytes > 0L) currentPlan.additionalPlans else emptyList(),
                    )
                }
                updated = updated.withPlanFor(network, nextPlan)
            }
            updated
        }
        refreshNow()
    }

    fun activatePlan(network: UsageNetwork = UsageNetwork.WIFI) {
        val now = System.currentTimeMillis()
        repository.settingsStore.update { settings ->
            val currentPlan = settings.planFor(network)
            settings.withPlanFor(
                network = network,
                plan = currentPlan.copy(
                    pendingActivation = false,
                    activatedAtMillis = now,
                ),
            )
        }
        refreshNow()
    }

    fun addPlanToSavedList(draft: PlanDraft) {
        repository.settingsStore.update { settings ->
            settings.addOrSavePlanFromList(draft)
        }
        refreshNow()
    }

    fun updateSavedPlan(network: UsageNetwork, planId: String, draft: PlanDraft) {
        repository.settingsStore.update { settings ->
            settings.updateSavedPlan(network, planId, draft)
        }
        refreshNow()
    }

    fun switchToSavedPlan(network: UsageNetwork, planId: String) {
        val now = System.currentTimeMillis()
        repository.settingsStore.update { settings ->
            settings.switchToSavedPlan(network, planId, now)
        }
        refreshNow()
    }

    fun deletePlan(network: UsageNetwork, planId: String) {
        repository.settingsStore.update { settings ->
            settings.deletePlan(network, planId)
        }
        refreshNow()
    }

    fun addAdditionalPlan(network: UsageNetwork, draft: AdditionalPlanDraft) {
        repository.settingsStore.update { settings ->
            settings.addAdditionalPlan(network, draft)
        }
        refreshNow()
    }

    fun updateAdditionalPlan(
        network: UsageNetwork,
        additionalPlanId: String,
        draft: AdditionalPlanDraft,
    ) {
        repository.settingsStore.update { settings ->
            settings.updateAdditionalPlan(network, additionalPlanId, draft)
        }
        refreshNow()
    }

    fun deleteAdditionalPlan(network: UsageNetwork, additionalPlanId: String) {
        repository.settingsStore.update { settings ->
            settings.deleteAdditionalPlan(network, additionalPlanId)
        }
        refreshNow()
    }

    fun reorderAdditionalPlans(network: UsageNetwork, fromIndex: Int, toIndex: Int) {
        repository.settingsStore.update { settings ->
            settings.reorderAdditionalPlans(network, fromIndex, toIndex)
        }
    }

    fun reorderSavedPlans(network: UsageNetwork, fromIndex: Int, toIndex: Int) {
        repository.settingsStore.update { settings ->
            settings.reorderSavedPlans(network, fromIndex, toIndex)
        }
    }

    fun resetActivePlan(network: UsageNetwork) {
        repository.settingsStore.update { settings ->
            settings.resetActivePlan(network)
        }
        refreshNow()
    }

    fun resetSavedPlan(network: UsageNetwork, planId: String) {
        repository.settingsStore.update { settings ->
            settings.resetSavedPlan(network, planId)
        }
        refreshNow()
    }

    fun resetAdditionalPlan(network: UsageNetwork, additionalPlanId: String) {
        repository.settingsStore.update { settings ->
            settings.resetAdditionalPlan(network, additionalPlanId)
        }
        refreshNow()
    }

    fun setUsageCardStyle(styleIndex: Int) {
        val safeStyle = styleIndex.coerceIn(
            UsageWidgetProvider.DEFAULT_USAGE_CARD_STYLE,
            UsageWidgetProvider.MAX_USAGE_CARD_STYLE_INDEX,
        )
        repository.settingsStore.update { it.copy(usageCardStyleIndex = safeStyle) }
        // الأداة تتبع شكل البطاقة المختار، فتتغيّر كل الأدوات المضافة فورًا.
        UsageWidgetUpdater.syncStyle(repository.context, safeStyle)
    }

    fun setDashboardLayout(layout: DashboardLayout) {
        repository.settingsStore.update { it.copy(dashboardLayout = layout.normalized()) }
    }

    fun setHistoryLineChartEnabled(enabled: Boolean) {
        repository.settingsStore.update { it.copy(historyLineChartEnabled = enabled) }
    }

    fun setAppsLineChartEnabled(enabled: Boolean) {
        repository.settingsStore.update { it.copy(appsLineChartEnabled = enabled) }
    }

    fun setCycleStartDay(day: Int) {
        repository.settingsStore.update { it.copy(cycleStartDay = day.coerceIn(1, 28)) }
        refreshNow()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.settingsStore.update { it.copy(notificationsEnabled = enabled) }
    }

    fun setHideStatusBarIcon(hidden: Boolean) {
        repository.settingsStore.update { it.copy(hideStatusBarIcon = hidden) }
    }

    fun setAlertThreshold(percent: Int) {
        repository.settingsStore.update { it.copy(alertThresholdPercent = percent.coerceIn(10, 100)) }
    }

    fun setSampleInterval(index: Int) {
        repository.settingsStore.update {
            it.copy(sampleIntervalIndex = SamplingIntervals.coerceIndex(index))
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        repository.settingsStore.update {
            it.copy(
                dynamicColorEnabled = enabled,
                // عند الرجوع إلى ألوان النظام نعيد الخيار الافتراضي بدل إبقاء لون يدوي غير فعّال.
                accentColor = if (enabled) AccentColor.DEFAULT else it.accentColor,
            )
        }
    }

    fun setAccentColor(color: AccentColor) {
        repository.settingsStore.update {
            it.copy(accentColor = color, dynamicColorEnabled = false)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        repository.settingsStore.update { it.copy(themeMode = mode) }
    }

    fun setLayoutDirection(direction: AppLayoutDirection) {
        repository.settingsStore.update { it.copy(layoutDirection = direction) }
    }

    fun clearData() {
        selectedDayAppsJob?.cancel()
        selectedDayAppsJob = null
        todayAppsJob?.cancel()
        todayAppsJob = null
        todayAppsLoadedAt = 0L
        todayAppsLoadedRangeStartMillis = 0L
        dailyLoadedAt = 0L
        dailyLoadedNetwork = null
        dailyLoadedYear = 0
        dailyLoadedMonth = 0
        LauncherIconVisibility.setVisible(repository.context, visible = true)
        stopTracking()
        repository.settingsStore.clearAll()
        MonitoringTileService.requestRefresh(repository.context)
        _uiState.update {
            UiState(
                isLoading = false,
                hasUsageAccess = repository.hasUsageAccess(),
                settings = repository.settingsStore.current,
            )
        }
        viewModelScope.launch {
            repository.clearAppIdentityCache()
            refreshNow()
        }
    }

    private var speedTestJob: Job? = null

    /**
     * بدء اختبار السرعة عبر fast.com (مع رجوع تلقائي إلى Cloudflare إذا حُجب).
     *
     * - قبل البدء: إن لم يكن هناك اتصال تظهر رسالة فورية بلا أي تحميل.
     * - على بيانات الهاتف: يظهر أولًا تحذير بكمية الاستهلاك التقديرية، ويبدأ
     *   الاختبار بعد اختيار «متابعة» فقط، ما لم يكن المستخدم قد اختار تجاهل
     *   التحذير خلال آخر يوم كامل.
     * - أثناء الاختبار: إن انقطع الاتصال أو مرّ وقت طويل دون نتيجة يوقف
     *   المحرّك التحميل ويصل السبب إلى الواجهة لتظهر رسالته.
     * - بعد النجاح: تُحفظ النتيجة محليًا فتبقى ظاهرة بعد إعادة التشغيل.
     */
    fun startSpeedTest() {
        if (_uiState.value.speedTest is SpeedTestState.Running) return
        if (_uiState.value.speedTestDataWarning != null) return

        val wifi = repository.wifiStatus()
        if (!wifi.isOnline) {
            _uiState.update {
                it.copy(speedTest = SpeedTestState.Failed(SpeedTestEngine.Failure.NO_CONNECTION))
            }
            return
        }

        val onMobileData = wifi.isMobileConnected && !wifi.isWifiConnected
        val settings = repository.settingsStore.current
        if (onMobileData && SpeedTestDataUsage.shouldWarn(settings.speedTestWarningSnoozedUntilMillis)) {
            _uiState.update { state ->
                state.copy(
                    speedTestDataWarning = SpeedTestDataUsage.estimate(settings.speedTestHistory),
                )
            }
            return
        }

        launchSpeedTest()
    }

    /** «متابعة الاختبار» من تحذير بيانات الهاتف: يغلق التحذير ويبدأ القياس. */
    fun continueSpeedTestAfterWarning() {
        if (_uiState.value.speedTestDataWarning == null) return
        _uiState.update { it.copy(speedTestDataWarning = null) }
        launchSpeedTest()
    }

    /** «إلغاء» من تحذير بيانات الهاتف: لا يبدأ أي تحميل ولا يُستهلك أي بيانات. */
    fun cancelSpeedTestWarning() {
        _uiState.update { it.copy(speedTestDataWarning = null) }
    }

    /**
     * «تجاهل لمدة يوم»: يبدأ الاختبار الآن ويُخفى التحذير ليوم كامل، ثم يعود
     * تلقائيًا في الاختبار التالي بعد انقضاء المدة.
     */
    fun ignoreSpeedTestWarningForDay() {
        if (_uiState.value.speedTestDataWarning == null) return
        repository.settingsStore.snoozeSpeedTestWarning()
        _uiState.update { state ->
            state.copy(
                speedTestDataWarning = null,
                settings = repository.settingsStore.current,
            )
        }
        launchSpeedTest()
    }

    /** تشغيل الاختبار فعليًا بعد اجتياز فحوص الاتصال والتحذير. */
    private fun launchSpeedTest() {
        if (_uiState.value.speedTest is SpeedTestState.Running) return

        if (!repository.wifiStatus().isOnline) {
            _uiState.update {
                it.copy(speedTest = SpeedTestState.Failed(SpeedTestEngine.Failure.NO_CONNECTION))
            }
            return
        }

        speedTestJob = viewModelScope.launch {
            _uiState.update { it.copy(speedTest = SpeedTestState.Running()) }
            try {
                val result = SpeedTestEngine.run(
                    isOnline = { repository.wifiStatus().isOnline },
                    onProgress = { progress ->
                        _uiState.update { state ->
                            state.copy(
                                speedTest = SpeedTestState.Running(
                                    phase = progress.phase,
                                    bitsPerSecond = progress.bitsPerSecond,
                                    elapsedMillis = progress.elapsedMillis,
                                ),
                            )
                        }
                    },
                )
                val record = SpeedTestRecord(
                    downloadBitsPerSecond = result.downloadBitsPerSecond,
                    uploadBitsPerSecond = result.uploadBitsPerSecond,
                    timestampMillis = System.currentTimeMillis(),
                    usedFastCom = result.usedFastCom,
                    uploadMeasured = result.uploadMeasured,
                    // كمية الاستهلاك الفعلية تُغذّي تقدير الاختبار القادم على بيانات الهاتف.
                    dataBytes = result.dataBytes,
                )
                repository.settingsStore.saveSpeedTest(record)
                _uiState.update {
                    it.copy(
                        speedTest = record.toSpeedTestState(),
                        speedTestHistory = repository.settingsStore.current.speedTestHistory,
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: SpeedTestEngine.SpeedTestException) {
                _uiState.update {
                    it.copy(speedTest = SpeedTestState.Failed(e.failure, e.detail))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        speedTest = SpeedTestState.Failed(
                            failure = SpeedTestEngine.Failure.ERROR,
                            detail = e.message,
                        ),
                    )
                }
            }
        }
    }

    /** إيقاف الاختبار الجاري وإغلاق التحميل، مع الرجوع إلى آخر نتيجة محفوظة. */
    fun stopSpeedTest() {
        speedTestJob?.cancel()
        speedTestJob = null
        val saved = repository.settingsStore.current.lastSpeedTest
        _uiState.update { it.copy(speedTest = saved.toSpeedTestState()) }
    }

    /** نص سرعة جاهز للعرض في الشريط العلوي. */
    fun speedText(): String = Format.speed(_uiState.value.currentSpeed?.totalBytesPerSecond ?: 0.0)

    companion object {
        // تحديث كل ثانية ليتحرك مقياس السرعة اللحظي باستمرار لا كل 4 ثوانٍ؛
        // أعمال التحميل الثقيلة داخل refresh() محمية بمواعيد TTL منفصلة.
        private const val REFRESH_INTERVAL_MILLIS = 1_000L
        private const val APPS_TTL_MILLIS = 30_000L
        private const val TODAY_APPS_TTL_MILLIS = 30_000L
        private const val DAILY_TTL_MILLIS = 60_000L
        private const val MONTH_PEAK_TTL_MILLIS = 120_000L
        private const val MONTH_DAYS = 30
        private const val MAX_SAMPLES = 30

        fun factory(repository: UsageRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { AppViewModel(repository) }
        }
    }
}

/**
 * تحويل نتيجة اختبار السرعة المحفوظة إلى حالة واجهة، أو [SpeedTestState.Idle]
 * إن لم يُجرَ أي اختبار بعد.
 */
private fun SpeedTestRecord?.toSpeedTestState(): SpeedTestState {
    val record = this ?: return SpeedTestState.Idle
    return SpeedTestState.Done(
        downloadBitsPerSecond = record.downloadBitsPerSecond,
        uploadBitsPerSecond = record.uploadBitsPerSecond,
        timestampMillis = record.timestampMillis,
        usedFastCom = record.usedFastCom,
        uploadMeasured = record.uploadMeasured,
    )
}
