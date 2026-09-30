package com.mywifiadata.app.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.mywifiadata.app.R
import com.mywifiadata.app.core.TimeRange
import com.mywifiadata.app.core.TimeRanges
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.DayUsage
import com.mywifiadata.app.data.model.TrafficTotals
import com.mywifiadata.app.data.model.WifiStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * مصدر البيانات الموحّد للتطبيق: يجمع بين قارئ إحصاءات النظام وقارئ شبكة الواي فاي.
 */
class UsageRepository(
    val context: Context,
    val settingsStore: SettingsStore,
) {

    private val statsReader = NetworkStatsReader(context)
    private val wifiReader = WifiNetworkReader(context)
    private val packageManager: PackageManager = context.packageManager

    fun hasUsageAccess(): Boolean = statsReader.hasUsageAccess()

    fun wifiStatus(): WifiStatus = wifiReader.read()

    suspend fun deviceTotals(range: TimeRange): TrafficTotals = withContext(Dispatchers.IO) {
        statsReader.wifiDeviceTotals(range.startMillis, range.endMillis)
    }

    suspend fun todayTotals(now: Long = System.currentTimeMillis()): TrafficTotals =
        deviceTotals(TimeRange(TimeRanges.startOfToday(now), now))

    suspend fun cycleTotals(
        cycleStartDay: Int,
        now: Long = System.currentTimeMillis(),
    ): TrafficTotals = deviceTotals(TimeRanges.cycleRange(cycleStartDay, now))

    /** استهلاك التطبيقات مرتّبًا من الأكثر استهلاكًا. */
    suspend fun perAppUsage(range: TimeRange): List<AppUsage> = withContext(Dispatchers.IO) {
        if (!statsReader.hasUsageAccess()) return@withContext emptyList()
        val perUid = statsReader.wifiPerUidTotals(range.startMillis, range.endMillis)
        perUid.entries
            .filter { it.value.totalBytes > 0L }
            .map { (uid, totals) -> resolveApp(uid, totals) }
            .sortedByDescending { it.totals.totalBytes }
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

    private fun resolveApp(uid: Int, totals: TrafficTotals): AppUsage {
        val packageName = runCatching { packageManager.getPackagesForUid(uid)?.firstOrNull() }
            .getOrNull()

        if (packageName == null) {
            return AppUsage(
                uid = uid,
                packageName = null,
                label = context.getString(R.string.unknown_app_label),
                isSystem = true,
                totals = totals,
            )
        }

        val appInfo = runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull()
        val label = appInfo?.let { info ->
            runCatching { packageManager.getApplicationLabel(info).toString() }.getOrNull()
        } ?: packageName

        val isSystem = appInfo?.let { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 } ?: false

        return AppUsage(
            uid = uid,
            packageName = packageName,
            label = label,
            isSystem = isSystem,
            totals = totals,
        )
    }
}
