package com.mywifiadata.app.core

import kotlin.math.abs

/**
 * مستطيل حول نص داخل ميناء الساعة، بإحداثيات نسبية إلى مركز الميناء
 * (اليمين موجب واليسار سالب، والأسفل موجب والأعلى سالب).
 */
data class HandObstacle(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    /** يوسّع المستطيل من كل الجهات ليبقى بين النص والعقرب فراغ صغير. */
    fun inflated(delta: Float): HandObstacle = HandObstacle(
        left = left - delta,
        top = top - delta,
        right = right + delta,
        bottom = bottom + delta,
    )
}

/**
 * هندسة عقارب الساعة: تحسب الجزء المرئي من عقرب يمتد من المركز إلى الخارج،
 * بحيث يتقلّص عند النصوص التي تعترضه بدل أن يمر فوقها.
 */
object ClockHandGeometry {

    /**
     * يحسب الجزء المرئي من عقرب يمتد من المركز في اتجاه [angleDeg]
     * (صفر عند الساعة 12 واتجاه عقارب الساعة) حتى [endRadius].
     *
     * - العقرب يبدأ من حافة أي نص يحيط بالمركز (مثل نسبة الاستهلاك).
     * - إن اعترضه نص آخر في طريقه (مثل عدد الأيام المتبقية) يتوقف طرفه قبله.
     *
     * يعيد المسافتين [البداية، النهاية] من المركز، أو null إذا لم يبق
     * جزء مرئي أطول من [minLength].
     */
    fun visibleSegment(
        angleDeg: Float,
        endRadius: Float,
        obstacles: List<HandObstacle>,
        minLength: Float,
    ): Pair<Float, Float>? {
        val radians = Math.toRadians(angleDeg.toDouble() - 90.0)
        val dx = kotlin.math.cos(radians).toFloat()
        val dy = kotlin.math.sin(radians).toFloat()

        // فترات تقاطع الشعاع مع النصوص، مدمجة عند تراكبها، ومرتّبة من المركز للخارج.
        val intervals = mergeIntervals(
            obstacles.mapNotNull { rayRectInterval(dx, dy, it) }
                .filter { it.second > 0f && it.first < endRadius }
                .map { maxOf(it.first, 0f) to it.second },
        )

        var visibleStart = 0f
        var visibleEnd = endRadius
        for ((enter, exit) in intervals) {
            if (enter <= visibleStart) {
                // النص يغطي بداية العقرب، فيظهر العقرب بعد نهايته.
                visibleStart = maxOf(visibleStart, exit)
            } else {
                // أول نص أمام العقرب يقلّص طرفه من جهته.
                visibleEnd = minOf(visibleEnd, enter)
                break
            }
        }

        return if (visibleEnd - visibleStart >= minLength) visibleStart to visibleEnd else null
    }

    private fun mergeIntervals(intervals: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        if (intervals.size <= 1) return intervals
        val sorted = intervals.sortedBy { it.first }
        val merged = mutableListOf(sorted.first())
        sorted.drop(1).forEach { (enter, exit) ->
            val last = merged.last()
            if (enter <= last.second) {
                merged[merged.lastIndex] = last.first to maxOf(last.second, exit)
            } else {
                merged.add(enter to exit)
            }
        }
        return merged
    }

    /**
     * فترة تقاطع شعاع (يبدأ من المركز باتجاه [dx], [dy]) مع مستطيل:
     * أقرب مسافة وأبعد مسافة. تعيد null إذا لم يمر الشعاع بالمستطيل.
     */
    private fun rayRectInterval(
        dx: Float,
        dy: Float,
        rect: HandObstacle,
    ): Pair<Float, Float>? {
        var enter = -Float.MAX_VALUE
        var exit = Float.MAX_VALUE

        if (abs(dx) < 1e-5f) {
            if (0f < rect.left || 0f > rect.right) return null
        } else {
            val first = rect.left / dx
            val second = rect.right / dx
            enter = maxOf(enter, minOf(first, second))
            exit = minOf(exit, maxOf(first, second))
        }

        if (abs(dy) < 1e-5f) {
            if (0f < rect.top || 0f > rect.bottom) return null
        } else {
            val first = rect.top / dy
            val second = rect.bottom / dy
            enter = maxOf(enter, minOf(first, second))
            exit = minOf(exit, maxOf(first, second))
        }

        return if (enter <= exit) enter to exit else null
    }
}
