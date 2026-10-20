package com.mywifiadata.app.data

import android.net.TrafficStats
import com.mywifiadata.app.data.model.TrafficTotals

/**
 * قراءة لحظية: [total] إجمالي الجهاز منذ الإقلاع، و[mobile] بيانات الجوال منذ الإقلاع.
 * عند قراءة تطبيق واحد تكون [mobile] قيمتها `null` (العدّاد لا يفصل الواي فاي عن الجوال).
 */
data class LiveReading(
    val total: TrafficTotals,
    val mobile: TrafficTotals?,
)

/**
 * قارئ عدّادات `TrafficStats` المباشرة.
 *
 * بخلاف `NetworkStatsManager` (الذي لا يحدّث الإحصاءات للطرف الثالث أكثر من مرة كل
 * 15 ثانية)، هذه العدّادات تُقرأ من النواة مباشرة وتتغيّر في كل ثانية، ولا تحتاج
 * صلاحية «الوصول إلى الاستخدام». كل نداء خفيف ويصلح للاستدعاء كل ثانية.
 *
 * القيمة `TrafficStats.UNSUPPORTED` (‎-1) تعني أن الجهاز لا يعطي هذا العدّاد.
 */
object LiveTrafficReader {

    /** إجمالي الجهاز + بيانات الجوال؛ `null` إن لم يدعم الجهاز عدّاد الإجمالي. */
    fun readDevice(): LiveReading? {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) return null

        val mobileRx = TrafficStats.getMobileRxBytes()
        val mobileTx = TrafficStats.getMobileTxBytes()
        val mobile = if (
            mobileRx == TrafficStats.UNSUPPORTED.toLong() ||
            mobileTx == TrafficStats.UNSUPPORTED.toLong()
        ) {
            null
        } else {
            TrafficTotals(mobileRx, mobileTx)
        }
        return LiveReading(total = TrafficTotals(rx, tx), mobile = mobile)
    }

    /**
     * عدّاد تطبيق واحد (uid).
     *
     * على أندرويد الحديث قد يمنع النظام قراءة عدّادات تطبيقات أخرى فيعيد
     * `UNSUPPORTED` أو صفرًا ثابتًا؛ نعيد `null` عندها ليعود المتّصل إلى المسار البديل.
     */
    fun readUid(uid: Int): LiveReading? {
        if (uid < 0) return null
        val rx = TrafficStats.getUidRxBytes(uid)
        val tx = TrafficStats.getUidTxBytes(uid)
        if (rx < 0L || tx < 0L) return null
        if (rx + tx == 0L) return null
        return LiveReading(total = TrafficTotals(rx, tx), mobile = null)
    }
}
