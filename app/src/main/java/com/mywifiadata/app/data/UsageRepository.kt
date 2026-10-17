package com.mywifiadata.app.data

import android.content.Context
import com.mywifiadata.app.core.HistoryYearMonth
import com.mywifiadata.app.core.UsageNetwork
import com.mywifiadata.app.core.TimeRange
import com.mywifiadata.app.core.TimeRanges
import com.mywifiadata.app.data.model.AppSettings
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.DayUsage
import com.mywifiadata.app.data.model.PlanNetworkType
import com.mywifiadata.app.data.model.TrafficTotals
import com.mywifiadata.app.data.model.WifiStatus
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * مصدر البيانات الموحّد للتطبيق: يجمع بين قارئ إحصاءات النظام وقارئ شبكة الواي فاي.
 */
class UsageRepository(
    val context: Context,
    val settingsStore: SettingsStore,
) {

    private val statsReader = NetworkStatsReader(context)
    private val wifiReader = WifiNetworkReader(context)
    val appIdentities = AppIdentityCache(context.applicationContext)
    private val usageArchive = AppUsageArchive(File(context.noBackupFilesDir, "app-usage-archive"))
    private val archiveRefreshMutex = Mutex()
    private val recordedMonthsMutex = Mutex()
    private val completedMonthsUsageCache = mutableMapOf<Pair<UsageNetwork, HistoryYearMonth>, Boolean>()
    private var archivedDayStart = -1L
    private var lastArchiveRefresh = 0L

    suspend fun clearAppIdentityCache() = withContext(Dispatchers.IO) {
        archiveRefreshMutex.withLock {
            usageArchive.clear()
            appIdentities.clear()
            archivedDayStart = -1L
            lastArchiveRefresh = 0L
        }
        recordedMonthsMutex.withLock {
            completedMonthsUsageCache.clear()
        }
    }

    fun hasUsageAccess(): Boolean = statsReader.hasUsageAccess()

    fun wifiStatus(): WifiStatus = wifiReader.read()

    suspend fun deviceTotals(
        range: TimeRange,
        network: UsageNetwork = UsageNetwork.WIFI,
    ): TrafficTotals = withContext(Dispatchers.IO) {
        statsReader.deviceTotals(range.startMillis, range.endMillis, network)
    }

    suspend fun todayTotals(now: Long = System.currentTimeMillis()): TrafficTotals =
        deviceTotals(TimeRange(TimeRanges.startOfToday(now), now))

    suspend fun cycleTotals(
        cycleStartDay: Int,
        now: Long = System.currentTimeMillis(),
    ): TrafficTotals = deviceTotals(TimeRanges.cycleRange(cycleStartDay, now))

    suspend fun planTotals(
        settings: AppSettings,
        now: Long = System.currentTimeMillis(),
    ): TrafficTotals {
        if (settings.planPendingActivation) return TrafficTotals.Empty
        val range = if (settings.planActivatedAtMillis > 0L) {
            val endMillis = settings.planActivatedAtMillis + settings.planTotalDays.toLong() * 86_400_000L
            TimeRange(
                startMillis = settings.planActivatedAtMillis,
                endMillis = maxOf(settings.planActivatedAtMillis, minOf(now, endMillis)),
            )
        } else {
            TimeRanges.cycleRange(settings.cycleStartDay, now)
        }
        return when (settings.planNetworkType) {
            PlanNetworkType.WIFI -> deviceTotals(range, UsageNetwork.WIFI)
            PlanNetworkType.MOBILE -> deviceTotals(range, UsageNetwork.MOBILE)
            PlanNetworkType.BOTH -> deviceTotals(range, UsageNetwork.WIFI) + deviceTotals(range, UsageNetwork.MOBILE)
        }
    }

    /** استهلاك التطبيقات مرتّبًا من الأكثر استهلاكًا. */
    suspend fun perAppUsage(
        range: TimeRange,
        network: UsageNetwork = UsageNetwork.WIFI,
    ): List<AppUsage> = withContext(Dispatchers.IO) {
        if (!statsReader.hasUsageAccess()) return@withContext emptyList()
        val now = System.currentTimeMillis()
        val bounded = TimeRange(range.startMillis, minOf(range.endMillis, now))
        if (bounded.endMillis <= bounded.startMillis) return@withContext emptyList()
        val perUid = statsReader.perUidTotals(bounded.startMillis, bounded.endMillis, network)
        val live = perUid.entries
            .filter { it.value.totalBytes > 0L }
            .map { (uid, totals) -> resolveApp(uid, totals) }
        usageArchive.record(bounded, network, live, now)
        val removed = usageArchive.restoreMissing(perUid.keys, bounded, network, ::resolveApp)
        (live + removed).sortedByDescending { it.totals.totalBytes }
    }

    /** Details use the same live/archive merge as the list, not an unrelated reused UID. */
    suspend fun appTotals(
        uid: Int,
        range: TimeRange,
        network: UsageNetwork = UsageNetwork.WIFI,
        expectedPackageName: String? = null,
    ): TrafficTotals = perAppUsage(range, network).firstOrNull {
        it.uid == uid && (expectedPackageName == null || it.packageName == expectedPackageName)
    }?.totals ?: TrafficTotals.Empty

    /**
     * Save daily observations for both networks so a later period change can still show a
     * removed app. Completed days are reused; today's reading is refreshed at most once a minute.
     * These are observed totals, not a reconstruction of Android's merged removed-UID bucket.
     */
    suspend fun captureUsageArchive(now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        if (!hasUsageAccess()) return@withContext
        archiveRefreshMutex.withLock {
            if (now - lastArchiveRefresh in 0L until 60_000L) return@withLock
            val todayStart = TimeRanges.startOfToday(now)
            val days = if (archivedDayStart != todayStart) TimeRanges.lastDays(30, now)
                else listOf(TimeRange(todayStart, now))
            val identities = mutableMapOf<Int, AppUsage>()
            for (network in UsageNetwork.entries) {
                for (day in days) {
                    currentCoroutineContext().ensureActive()
                    val range = TimeRange(day.startMillis, minOf(day.endMillis, now))
                    if (range.endMillis <= range.startMillis) continue
                    if (range.endMillis <= todayStart && usageArchive.hasCompleteDay(range, network)) continue
                    val apps = statsReader.perUidTotals(range.startMillis, range.endMillis, network).entries
                        .filter { it.value.totalBytes > 0L }
                        .map { (uid, totals) ->
                            identities.getOrPut(uid) { resolveApp(uid, TrafficTotals.Empty) }.copy(totals = totals)
                        }
                    usageArchive.record(range, network, apps, now)
                }
            }
            archivedDayStart = todayStart
            lastArchiveRefresh = now
        }
    }

    /** استهلاك الأيام الأخيرة (الأقدم أولاً). */
    suspend fun dailyUsage(days: Int): List<DayUsage> = withContext(Dispatchers.IO) {
        TimeRanges.lastDays(days).map { day ->
            DayUsage(
                startMillis = day.startMillis,
                endMillis = day.endMillis,
                totals = statsReader.wifiDeviceTotals(day.startMillis, day.endMillis),
            )
        }
    }

    /** استهلاك أيام الشهر الميلادي المحدد للشبكة المختارة (الأقدم أولاً). */
    suspend fun dailyUsageForMonth(
        year: Int,
        month: Int,
        network: UsageNetwork = UsageNetwork.WIFI,
        now: Long = System.currentTimeMillis(),
    ): List<DayUsage> = withContext(Dispatchers.IO) {
        TimeRanges.daysOfMonth(year, month, now).map { day ->
            DayUsage(
                startMillis = day.startMillis,
                endMillis = day.endMillis,
                totals = statsReader.deviceTotals(day.startMillis, day.endMillis, network),
            )
        }
    }

    /**
     * الأشهر والسنوات التي سُجّل فيها استهلاك بيانات فعلي (> 0 بايت) للشبكة المحددة.
     */
    suspend fun recordedHistoryMonths(
        network: UsageNetwork = UsageNetwork.WIFI,
        now: Long = System.currentTimeMillis(),
    ): List<HistoryYearMonth> = withContext(Dispatchers.IO) {
        if (!statsReader.hasUsageAccess()) return@withContext emptyList()
        val currentYm = TimeRanges.currentYearMonth(now)
        val oldestYear = currentYm.year - HISTORY_SCAN_YEARS_BACK
        val result = mutableListOf<HistoryYearMonth>()

        recordedMonthsMutex.withLock {
            for (year in oldestYear..currentYm.year) {
                currentCoroutineContext().ensureActive()
                val yearRange = TimeRanges.yearRange(year, now)
                if (yearRange.endMillis <= yearRange.startMillis) continue
                val maxMonth = if (year == currentYm.year) currentYm.month else 12
                val allCached = year < currentYm.year && (1..maxMonth).all { month ->
                    completedMonthsUsageCache.containsKey(network to HistoryYearMonth(year, month))
                }
                if (!allCached) {
                    val yearTotals = statsReader.deviceTotals(yearRange.startMillis, yearRange.endMillis, network)
                    if (yearTotals.totalBytes <= 0L) {
                        if (year < currentYm.year) {
                            for (month in 1..maxMonth) {
                                completedMonthsUsageCache[network to HistoryYearMonth(year, month)] = false
                            }
                        }
                        continue
                    }
                }
                for (month in 1..maxMonth) {
                    currentCoroutineContext().ensureActive()
                    val ym = HistoryYearMonth(year, month)
                    val isCompletedMonth = ym < currentYm
                    val hasUsage = if (isCompletedMonth) {
                        completedMonthsUsageCache.getOrPut(network to ym) {
                            val range = TimeRanges.monthRange(year, month, now)
                            range.endMillis > range.startMillis &&
                                statsReader.deviceTotals(range.startMillis, range.endMillis, network).totalBytes > 0L
                        }
                    } else {
                        val range = TimeRanges.monthRange(year, month, now)
                        range.endMillis > range.startMillis &&
                            statsReader.deviceTotals(range.startMillis, range.endMillis, network).totalBytes > 0L
                    }
                    if (hasUsage) {
                        result += ym
                    }
                }
            }
        }
        result
    }

    private fun resolveApp(uid: Int, totals: TrafficTotals): AppUsage =
        appIdentities.resolve(uid, totals)

    companion object {
        private const val HISTORY_SCAN_YEARS_BACK = 4
    }
}
