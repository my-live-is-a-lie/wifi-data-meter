package com.mywifiadata.app.data

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import com.mywifiadata.app.data.model.TrafficTotals

/**
 * قارئ إحصاءات الشبكة من [NetworkStatsManager].
 *
 * ملاحظات مهمة:
 * - كل الدوال هنا يجب أن تُستدعى من خيط خلفي فقط (`@WorkerThread` في النظام).
 * - قراءة الإجمالي للجهاز تتطلب صلاحية «الوصول إلى الاستخدام» (PACKAGE_USAGE_STATS)،
 *   والقراءة حسب التطبيق تتطلب نفس الصلاحية أيضًا، لذلك نتعامل مع الأخطاء بأمان.
 * - `null` أو `SecurityException` تعني عادةً أن الصلاحية غير ممنوحة.
 */
class NetworkStatsReader(private val context: Context) {

    private val manager: NetworkStatsManager? =
        context.getSystemService(NetworkStatsManager::class.java)

    /** هل يملك التطبيق صلاحية «الوصول إلى الاستخدام»؟ */
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            }
        } catch (securityException: SecurityException) {
            return false
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** إجمالي استهلاك الواي فاي للجهاز في فترة زمنية. */
    fun wifiDeviceTotals(startMillis: Long, endMillis: Long): TrafficTotals {
        val statsManager = manager ?: return TrafficTotals.Empty
        if (endMillis <= startMillis) return TrafficTotals.Empty
        return try {
            val bucket = statsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_WIFI,
                null,
                startMillis,
                endMillis,
            )
            if (bucket == null) TrafficTotals.Empty else TrafficTotals(bucket.rxBytes, bucket.txBytes)
        } catch (securityException: SecurityException) {
            TrafficTotals.Empty
        } catch (exception: Exception) {
            TrafficTotals.Empty
        }
    }

    /** استهلاك كل تطبيق (uid) على الواي فاي في فترة زمنية. */
    fun wifiPerUidTotals(startMillis: Long, endMillis: Long): Map<Int, TrafficTotals> {
        val statsManager = manager ?: return emptyMap()
        if (endMillis <= startMillis) return emptyMap()

        val result = HashMap<Int, TrafficTotals>()
        var stats: NetworkStats? = null
        try {
            stats = statsManager.querySummary(
                ConnectivityManager.TYPE_WIFI,
                null,
                startMillis,
                endMillis,
            ) ?: return emptyMap()

            val bucket = NetworkStats.Bucket()
            while (stats.getNextBucket(bucket)) {
                val uid = bucket.uid
                if (uid < 0) continue // UID_ALL / UID_TETHERING / UID_REMOVED
                val totals = TrafficTotals(bucket.rxBytes, bucket.txBytes)
                result[uid] = result[uid]?.plus(totals) ?: totals
            }
        } catch (securityException: SecurityException) {
            return emptyMap()
        } catch (exception: Exception) {
            return emptyMap()
        } finally {
            try {
                stats?.close()
            } catch (ignored: Exception) {
                // تجاهل: إغلاق المؤشر ليس حرجًا
            }
        }
        return result
    }
}
