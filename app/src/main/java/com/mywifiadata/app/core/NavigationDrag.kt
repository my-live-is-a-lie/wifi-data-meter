package com.mywifiadata.app.core

import kotlin.math.abs

/** Physical screen coordinates: RTL reverses slots, never the gesture's direction. */
internal fun navigationIndexAt(x: Float, width: Float, count: Int, isRtl: Boolean): Int? {
    if (!x.isFinite() || !width.isFinite() || width <= 0f || count <= 0) return null
    val slot = ((x.coerceIn(0f, width) / width) * count).toInt().coerceIn(0, count - 1)
    return if (isRtl) count - 1 - slot else slot
}

internal fun navigationTabCenter(index: Int, width: Float, count: Int, isRtl: Boolean): Float {
    if (!width.isFinite() || width <= 0f || count <= 0) return 0f
    val slot = index.coerceIn(0, count - 1).let { if (isRtl) count - 1 - it else it }
    return (slot + 0.5f) * width / count
}

/** Keep the capsule inside the two end icons while the finger can reach the full bar width. */
internal fun navigationDragCenter(x: Float, width: Float, count: Int): Float {
    if (!x.isFinite() || !width.isFinite() || width <= 0f || count <= 0) return 0f
    val halfSlot = width / count / 2f
    return x.coerceIn(halfSlot, width - halfSlot)
}

/** Signed stretch: positive extends LEFT when moving right, negative extends RIGHT. */
internal fun navigationTrailImpulse(delta: Float, previous: Float, limit: Float): Float {
    if (!delta.isFinite() || !previous.isFinite() || !limit.isFinite() || limit <= 0f) return 0f
    if (delta == 0f) return previous.coerceIn(-limit, limit)
    // Reversal immediately swaps the trailing edge instead of stretching ahead of the finger.
    val carry = if ((delta > 0f) == (previous > 0f)) abs(previous) * 0.55f else 0f
    val extent = (abs(delta) * 1.8f + carry).coerceAtMost(limit)
    return if (delta > 0f) extent else -extent
}

internal data class NavigationIndicatorBounds(val left: Float, val right: Float)

internal fun navigationIndicatorBounds(
    center: Float,
    pillWidth: Float,
    stretch: Float,
    barWidth: Float,
): NavigationIndicatorBounds {
    val half = pillWidth / 2f
    return NavigationIndicatorBounds(
        left = (center - half - stretch.coerceAtLeast(0f)).coerceIn(0f, barWidth),
        right = (center + half - stretch.coerceAtMost(0f)).coerceIn(0f, barWidth),
    )
}
