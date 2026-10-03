package com.mywifiadata.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.SpeedTestEngine
import com.mywifiadata.app.core.TimeRange
import com.mywifiadata.app.core.TimeRanges
import com.mywifiadata.app.core.UsagePeriod
import com.mywifiadata.app.data.UsageRepository
import com.mywifiadata.app.data.model.AppSettings
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.AppsFilter
import com.mywifiadata.app.data.model.DayUsage
import com.mywifiadata.app.data.model.SpeedSample
import com.mywifiadata.app.data.model.SpeedTestRecord
import com.mywifiadata.app.data.model.ThemeMode
import com.mywifiadata.app.data.model.TrafficTotals
import com.mywifiadata.app.data.model.UsageSlice
import com.mywifiadata.app.data.model.WifiStatus
import com.mywifiadata.app.monitor.MonitorService
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
    ) : SpeedTestState

    /** فشل الاختبار مع سببه لعرض الرسالة المناسبة. */
    data class Failed(
        val failure: SpeedTestEngine.Failure = SpeedTestEngine.Failure.ERROR,
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
    val daily: List<DayUsage> = emptyList(),
    val apps: List<AppUsage> = emptyList(),
    // تطبيقات الفترة كاملة (لا تتأثر باختيار شريحة) لبطاقة «الفترة».
    val periodApps: List<AppUsage> = emptyList(),
    val isAppsLoading: Boolean = false,
    val appsPeriod: UsagePeriod = UsagePeriod.MONTH,
    val historyPeriod: UsagePeriod = UsagePeriod.MONTH,
    val selectedDayIndex: Int? = null,
    val selectedDayApps: List<AppUsage> = emptyList(),
    val appsFilter: AppsFilter = AppsFilter.ALL,
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

    val visibleApps: List<AppUsage>
        get() = when (appsFilter) {
            AppsFilter.ALL -> apps
            AppsFilter.INSTALLED -> apps.filterNot { it.isSystem }
            AppsFilter.SYSTEM -> apps.filter { it.isSystem }
        }

    val visiblePeriodApps: List<AppUsage>
        get() = when (appsFilter) {
            AppsFilter.ALL -> periodApps
            AppsFilter.INSTALLED -> periodApps.filterNot { it.isSystem }
            AppsFilter.SYSTEM -> periodApps.filter { it.isSystem }
        }

    val historyDays: Int get() = TimeRanges.daysOf(historyPeriod)
}

/**
 * تفاصيل استهلاك تطبيق واحد في الفترات الثلاث (اليوم/الأسبوع/الشهر).
 */
data class AppDetailState(
    val app: AppUsage,
    val isLoading: Boolean = true,
    val today: TrafficTotals = TrafficTotals.Empty,
    val week: TrafficTotals = TrafficTotals.Empty,
    val month: TrafficTotals = TrafficTotals.Empty,
)

/**
 * حامل حالة الشاشة الرئيسية: يحدّث القياسات دوريًا ويوفّر إجراءات المستخدم.
 */
class AppViewModel(private val repository: UsageRepository) : ViewModel() {

    // تُستعاد آخر نتيجة اختبار سرعة محفوظة فور إنشاء الحالة (قبل أي تحديث).
    private val _uiState = MutableStateFlow(
        UiState(speedTest = repository.settingsStore.current.lastSpeedTest.toSpeedTestState()),
    )
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

        if (hasAccess && (force || now - appsLoadedAt > APPS_TTL_MILLIS)) {
            loadApps()
            loadAppsSlices()
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

    /**
     * تحميل التطبيقات للنطاق الحالي: الشريحة المختارة (ساعة/يوم) إن وُجدت،
     * وإلا الفترة الكاملة المختارة.
     */
    private suspend fun loadApps() {
        val period = _uiState.value.appsPeriod
        val slice = _uiState.value.appsSlice
        val periodRange = TimeRanges.forPeriod(period)
        val range = slice ?: periodRange
        _uiState.update { it.copy(isAppsLoading = true) }
        // تُحمَّل فترة كاملة دائمًا (لبطاقة «الفترة» الثابتة) + نطاق الشريحة إن وُجد.
        val periodApps = repository.perAppUsage(periodRange)
        val apps = if (range == periodRange) periodApps else repository.perAppUsage(range)
        appsLoadedAt = System.currentTimeMillis()
        _uiState.update {
            it.copy(apps = apps, periodApps = periodApps, isAppsLoading = false)
        }
    }

    /**
     * تحميل شرائح التفصيل للفترة الحالية: ساعات اليوم (بما فيها الساعة الجارية
     * بحدّها «الآن»)، أيام الأسبوع، أو آخر 4 أسابيع — مع حذف الشرائح التي لا
     * يوجد بها استهلاك حتى لا تُعرض صفوف فارغة.
     */
    private suspend fun loadAppsSlices() {
        // تُحمَّل شرائح اليوم/الأسبوع/الشهر دومًا ليظهر بطاقة أي فترة فورًا
        // عند التبديل بينها (بلا فراغ أو وميض)، مع تجديد الفترة الحالية.
        val hours = TimeRanges.hoursOfToday()
            .map { range -> range.toSlice(repository.deviceTotals(range)) }
            .filter { it.totals.totalBytes > 0L }
        _uiState.update { it.copy(appsHours = hours) }

        val days = TimeRanges.lastDays(7)
            .map { range -> range.toSlice(repository.deviceTotals(range)) }
            .filter { it.totals.totalBytes > 0L }
        _uiState.update { it.copy(appsDays = days) }

        val weeks = TimeRanges.lastWeeks()
            .map { range -> range.toSlice(repository.deviceTotals(range)) }
            .filter { it.totals.totalBytes > 0L }
        _uiState.update { it.copy(appsWeeks = weeks) }

        _uiState.value.appsWeek?.let { loadAppsWeekDays(it) }
    }

    /** أيام الأسبوع المحدد في عرض الشهر (الأحدث أولًا، بدون أيام فارغة). */
    private suspend fun loadAppsWeekDays(week: TimeRange) {
        val days = TimeRanges.daysInRange(week)
            .map { range -> range.toSlice(repository.deviceTotals(range)) }
            .filter { it.totals.totalBytes > 0L }
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
    fun openAppDetail(app: AppUsage) {
        _uiState.update { it.copy(appDetail = AppDetailState(app = app, isLoading = true)) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val today = repository.appTotals(app.uid, TimeRanges.forPeriod(UsagePeriod.TODAY, now))
            val week = repository.appTotals(app.uid, TimeRanges.forPeriod(UsagePeriod.WEEK, now))
            val month = repository.appTotals(app.uid, TimeRanges.forPeriod(UsagePeriod.MONTH, now))
            _uiState.update { state ->
                val detail = state.appDetail
                if (detail == null || detail.app.uid != app.uid) {
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

    fun setThemeMode(mode: ThemeMode) {
        repository.settingsStore.update { it.copy(themeMode = mode) }
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
                )
                repository.settingsStore.saveSpeedTest(record)
                _uiState.update { it.copy(speedTest = record.toSpeedTestState()) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: SpeedTestEngine.SpeedTestException) {
                _uiState.update { it.copy(speedTest = SpeedTestState.Failed(e.failure)) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(speedTest = SpeedTestState.Failed(SpeedTestEngine.Failure.ERROR))
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
        private const val DAILY_TTL_MILLIS = 60_000L
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
    )
}
