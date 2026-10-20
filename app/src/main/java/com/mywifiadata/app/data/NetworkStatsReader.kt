package com.mywifiadata.app.data

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import com.mywifiadata.app.core.UsageNetwork
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
    fun wifiDeviceTotals(startMillis: Long, endMillis: Long): TrafficTotals =
        deviceTotals(startMillis, endMillis, UsageNetwork.WIFI)

    fun deviceTotals(startMillis: Long, endMillis: Long, network: UsageNetwork): TrafficTotals =
        deviceTotalsOrNull(startMillis, endMillis, network) ?: TrafficTotals.Empty

    /**
     * مثل [deviceTotals] لكنها تعيد `null` عند تعذّر القراءة بدل صفر.
     *
     * يفرّق هذا بين «لا استهلاك» و«فشل الاستعلام»؛ النافذة العائمة تحتاج الفرق كي لا
     * تتخذ صفرًا كخط أساس فتظهر قفزة وهمية عند أول قراءة ناجحة.
     */
    fun deviceTotalsOrNull(startMillis: Long, endMillis: Long, network: UsageNetwork): TrafficTotals? {
        val statsManager = manager ?: return null
        if (endMillis <= startMillis) return null
        return try {
            val bucket = statsManager.querySummaryForDevice(
                if (network == UsageNetwork.WIFI) ConnectivityManager.TYPE_WIFI else ConnectivityManager.TYPE_MOBILE,
                null,
                startMillis,
                endMillis,
            )
            if (bucket == null) null else TrafficTotals(bucket.rxBytes, bucket.txBytes)
        } catch (securityException: SecurityException) {
            null
        } catch (exception: Exception) {
            null
        }
    }

    /** استهلاك كل تطبيق (uid) على الشبكة المختارة؛ null يجمع اشتراكات الهاتف المتاحة. */
    fun perUidTotals(startMillis: Long, endMillis: Long, network: UsageNetwork): Map<Int, TrafficTotals> =
        perUidTotalsOrNull(startMillis, endMillis, network) ?: emptyMap()

    /**
     * مثل [perUidTotals] لكنها تعيد `null` عند تعذّر القراءة بدل خريطة فارغة.
     *
     * تعتمد `querySummary` لأنها تُوزّع الحِزمة الزمنية الجارية نسبيًا (interpolation)
     * فتظهر الحركة الحالية، بخلاف `queryDetailsForUid` التي تتجاهل أي حزمة جزئية
     * «Since bucket length is in the order of hours, this method cannot be used to
     * measure data usage on a fine grained time scale» — لذلك لا تصلح لعدّاد مباشر.
     */
    fun perUidTotalsOrNull(startMillis: Long, endMillis: Long, network: UsageNetwork): Map<Int, TrafficTotals>? {
        val statsManager = manager ?: return null
        if (endMillis <= startMillis) return null

        val result = HashMap<Int, TrafficTotals>()
        var stats: NetworkStats? = null
        try {
            stats = statsManager.querySummary(
                if (network == UsageNetwork.WIFI) ConnectivityManager.TYPE_WIFI else ConnectivityManager.TYPE_MOBILE,
                null,
                startMillis,
                endMillis,
            ) ?: return null

            val bucket = NetworkStats.Bucket()
            while (stats.getNextBucket(bucket)) {
                val uid = bucket.uid
                if (uid < 0) continue // UID_ALL / UID_TETHERING / UID_REMOVED
                val totals = TrafficTotals(bucket.rxBytes, bucket.txBytes)
                result[uid] = result[uid]?.plus(totals) ?: totals
            }
        } catch (securityException: SecurityException) {
            return null
        } catch (exception: Exception) {
            return null
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
