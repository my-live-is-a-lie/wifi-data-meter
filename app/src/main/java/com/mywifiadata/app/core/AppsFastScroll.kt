package com.mywifiadata.app.core

import kotlin.math.floor
import kotlin.math.roundToInt

internal data class AppScrollRow(val index: Int, val offset: Int, val size: Int)
internal data class AppScrollTarget(val index: Int, val offset: Int = 0)

/** Row-based estimate: section cards and the pinned controls never count as app progress. */
internal data class AppsFastScrollMetrics(
    val firstAppIndex: Int,
    val appCount: Int,
    val stride: Float,
    val maxFirstRow: Float,
    val bodyTop: Int,
    val trackTop: Int,
    val trackBottom: Int,
    val progress: Float,
) {
    fun target(progress: Float): AppScrollTarget {
        val fraction = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
        // Let LazyColumn clamp to its real end instead of estimating the final row's height.
        if (fraction == 1f) return AppScrollTarget(firstAppIndex + appCount - 1)
        val row = fraction * maxFirstRow
        val whole = floor(row).toInt().coerceIn(0, appCount - 1)
        return AppScrollTarget(firstAppIndex + whole, ((row - whole) * stride - bodyTop).roundToInt())
    }
}

internal fun appsFastScrollMetrics(
    headerIndex: Int,
    headerOffset: Int,
    headerSize: Int,
    viewportStart: Int,
    viewportEnd: Int,
    afterPadding: Int,
    spacing: Int,
    appCount: Int,
    rows: List<AppScrollRow>,
    canScrollForward: Boolean,
): AppsFastScrollMetrics? {
    if (appCount < 2 || headerSize <= 0 || headerOffset > viewportStart + 1) return null
    val bodyTop = headerOffset + headerSize + spacing
    val bodyEnd = viewportEnd - afterPadding
    val bodyHeight = bodyEnd - bodyTop
    if (bodyHeight <= 0) return null
    val firstApp = headerIndex + 1
    val measured = rows.filter { it.index in firstApp until firstApp + appCount && it.size > 0 }
    val visible = measured.filter { it.offset + it.size > bodyTop && it.offset < bodyEnd }.sortedBy { it.offset }
    val first = visible.firstOrNull() ?: return null
    val stride = measured.map { it.size.toFloat() + spacing }.average().toFloat()
    if (!stride.isFinite() || stride <= 0f) return null
    // The final row has no following item spacing.
    val maxFirstRow = (appCount - (bodyHeight + spacing) / stride).coerceAtLeast(0f)
    if (maxFirstRow <= 0f) return null
    val position = first.index - firstApp + ((bodyTop - first.offset) / stride).coerceIn(0f, 1f)
    return AppsFastScrollMetrics(
        firstAppIndex = firstApp,
        appCount = appCount,
        stride = stride,
        maxFirstRow = maxFirstRow,
        bodyTop = bodyTop,
        // LazyList offsets exclude its before-padding; the overlay uses physical coordinates.
        trackTop = bodyTop - viewportStart,
        trackBottom = bodyEnd - viewportStart,
        progress = if (!canScrollForward) 1f else (position / maxFirstRow).coerceIn(0f, 1f),
    )
}

/** Absolute edge position, independent of where the small indicator was before the grab. */
internal fun fastScrollEdgeProgress(y: Float, trackHeight: Float, thumbHeight: Float): Float {
    if (!y.isFinite() || !trackHeight.isFinite() || !thumbHeight.isFinite() ||
        thumbHeight <= 0f || trackHeight <= thumbHeight) return 0f
    return ((y - thumbHeight / 2f) / (trackHeight - thumbHeight)).coerceIn(0f, 1f)
}

/** Coordinates are local to the physical-right rail, never mirrored by layout direction. */
internal fun fastScrollEdgeContains(x: Float, y: Float, width: Float, height: Float): Boolean =
    x.isFinite() && y.isFinite() && width.isFinite() && height.isFinite() &&
        width > 0f && height > 0f && x in 0f..width && y in 0f..height
