package com.mywifiadata.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mywifiadata.app.core.AppsSortOrder
import com.mywifiadata.app.core.AppsUsageScope
import com.mywifiadata.app.core.UsageNetwork
import com.mywifiadata.app.core.appsCountOptions
import com.mywifiadata.app.core.appsUsageRange
import com.mywifiadata.app.core.filterApps
import com.mywifiadata.app.core.within
import com.mywifiadata.app.core.AppLanguage
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.LauncherIconVisibility
import com.mywifiadata.app.core.SamplingIntervals
import com.mywifiadata.app.core.SpeedTestEngine
import com.mywifiadata.app.core.TimeRange
import com.mywifiadata.app.core.TimeRanges
import com.mywifiadata.app.core.UsagePeriod
import com.mywifiadata.app.data.AppSearchHistory
import com.mywifiadata.app.data.UsageRepository
import com.mywifiadata.app.data.model.AccentColor
import com.mywifiadata.app.data.model.AppSettings
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.AppsFilter
import com.mywifiadata.app.data.model.DayUsage
import com.mywifiadata.app.data.model.DashboardLayout
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
    val session: TrafficTotals = TrafficTotals.Empty,
    val samples: List<SpeedSample> = emptyList(),
    val speedTest: SpeedTestState = SpeedTestState.Idle,
    val speedTestHistory: List<SpeedTestRecord> = emptyList(),
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
    val selectedDayIndex: Int? = null,
    val selectedDayApps: List<AppUsage> = emptyList(),
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

    val historyDays: Int get() = TimeRanges.daysOf(historyPeriod)
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
    private var dailyLoadedAt: Long = 0L
    private var dailyLoadedDays: Int = 0
    private var monthPeakLoadedAt: Long = 0L

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
                speedTestHistory = settings.speedTestHistory,
                wifi = wifi,
                today = today,
                cycle = cycle,
                session = session,
                samples = if (sample == null) state.samples else (state.samples + sample).takeLast(MAX_SAMPLES),
            )
        }
        UsageWidgetUpdater.publish(
            context = repository.context,
            todayDownload = today.rxBytes,
            todayUpload = today.txBytes,
            cycleUsage = cycle.totalBytes,
            quota = settings.quotaBytes,
        )

        val historyDays = TimeRanges.daysOf(_uiState.value.historyPeriod)
        if (hasAccess && (force || now - dailyLoadedAt > DAILY_TTL_MILLIS || dailyLoadedDays != historyDays)) {
            val daily = repository.dailyUsage(historyDays)
            dailyLoadedAt = now
            dailyLoadedDays = historyDays
            _uiState.update { it.copy(daily = daily) }
        }
        loadMonthPeak(hasAccess, force, now)

        if (hasAccess && (force || now - appsLoadedAt > APPS_TTL_MILLIS)) {
            loadApps()
            loadAppsSlices()
        }
    }

    /**
     * يحمّل آخر 30 يومًا ويستخرج أعلى يوم استهلاكًا لعرضه في بطاقة «سجل
     * الاستهلاك» كمرجع للمقارنة، حتى لو كانت الفترة المعروضة يومًا واحدًا
     * (فلا يعود «أعلى يوم» مساويًا لاستهلاك اليوم نفسه).
     *
     * إن كانت الفترة المختارة شهرًا بالفعل نُعيد استخدام بياناتها بلا استعلام جديد.
     */
    private suspend fun loadMonthPeak(hasAccess: Boolean, force: Boolean, now: Long) {
        if (!hasAccess) return
        if (!force && now - monthPeakLoadedAt <= MONTH_PEAK_TTL_MILLIS) return

        val loadedDays = dailyLoadedDays
        val monthDaily = if (loadedDays >= MONTH_DAYS) {
            _uiState.value.daily
        } else {
            repository.dailyUsage(MONTH_DAYS)
        }
        monthPeakLoadedAt = now
        _uiState.update { state ->
            state.copy(monthPeak = monthDaily.maxByOrNull { it.totals.totalBytes })
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

    fun setHistoryPeriod(period: UsagePeriod) {
        if (period == _uiState.value.historyPeriod) return
        val days = TimeRanges.daysOf(period)
        _uiState.update { it.copy(historyPeriod = period, selectedDayIndex = null, selectedDayApps = emptyList()) }
        viewModelScope.launch {
            val daily = repository.dailyUsage(days)
            dailyLoadedDays = days
            dailyLoadedAt = System.currentTimeMillis()
            _uiState.update { it.copy(daily = daily) }
            // عرض «اليوم» = يوم واحد فقط، فنفتح تفاصيله مباشرة ليرى المستخدم استهلاكه وأكثر التطبيقات.
            if (period == UsagePeriod.TODAY && daily.isNotEmpty()) {
                selectDay(daily.lastIndex)
            }
        }
    }

    /** فتح تفاصيل تطبيق واحد (اليوم/الأسبوع/الشهر). */
    fun openAppDetail(app: AppUsage, network: UsageNetwork = UsageNetwork.WIFI) {
        _uiState.update { it.copy(appDetail = AppDetailState(app = app, network = network, isLoading = true)) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val today = repository.appTotals(app.uid, TimeRanges.forPeriod(UsagePeriod.TODAY, now), network)
            val week = repository.appTotals(app.uid, TimeRanges.forPeriod(UsagePeriod.WEEK, now), network)
            val month = repository.appTotals(app.uid, TimeRanges.forPeriod(UsagePeriod.MONTH, now), network)
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

    fun setUsageCardStyle(styleIndex: Int) {
        repository.settingsStore.update {
            it.copy(
                usageCardStyleIndex = styleIndex.coerceIn(
                    UsageWidgetProvider.DEFAULT_USAGE_CARD_STYLE,
                    UsageWidgetProvider.MAX_USAGE_CARD_STYLE_INDEX,
                ),
            )
        }
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

    fun clearData() {
        todayAppsJob?.cancel()
        todayAppsJob = null
        todayAppsLoadedAt = 0L
        todayAppsLoadedRangeStartMillis = 0L
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
        refreshNow()
    }

    private var speedTestJob: Job? = null

    /**
     * بدء اختبار السرعة عبر fast.com (مع رجوع تلقائي إلى Cloudflare إذا حُجب).
     *
     * - قبل البدء: إن لم يكن هناك اتصال تظهر رسالة فورية بلا أي تحميل.
     * - أثناء الاختبار: إن انقطع الاتصال أو مرّ وقت طويل دون نتيجة يوقف
     *   المحرّك التحميل ويصل السبب إلى الواجهة لتظهر رسالته.
     * - بعد النجاح: تُحفظ النتيجة محليًا فتبقى ظاهرة بعد إعادة التشغيل.
     */
    fun startSpeedTest() {
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
