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

/** Hit-test the visible rounded capsule, not the whole tab or its label. */
internal fun navigationIndicatorContains(
    x: Float,
    y: Float,
    bounds: NavigationIndicatorBounds,
    centerY: Float,
    height: Float,
): Boolean {
    if (!x.isFinite() || !y.isFinite() || !centerY.isFinite() || !height.isFinite() ||
        !bounds.left.isFinite() || !bounds.right.isFinite() || height <= 0f || bounds.right <= bounds.left) return false
    val halfHeight = height / 2f
    if (y < centerY - halfHeight || y > centerY + halfHeight || x < bounds.left || x > bounds.right) return false
    val radius = minOf(halfHeight, (bounds.right - bounds.left) / 2f)
    val cornerX = x.coerceIn(bounds.left + radius, bounds.right - radius)
    val cornerY = y.coerceIn(centerY - halfHeight + radius, centerY + halfHeight - radius)
    val dx = x - cornerX
    val dy = y - cornerY
    return dx * dx + dy * dy <= radius * radius
}

/** A short swipe supplements scrubbing; it must never add another step to a completed scrub. */
internal fun navigationShortSwipeTarget(
    startIndex: Int,
    deltaX: Float,
    count: Int,
    isRtl: Boolean,
    threshold: Float,
    startedOnIndicator: Boolean,
    crossedTab: Boolean,
    completed: Boolean,
): Int? {
    if (!completed || !startedOnIndicator || crossedTab || count <= 1 || startIndex !in 0 until count) return null
    if (!deltaX.isFinite() || !threshold.isFinite() || threshold <= 0f || abs(deltaX) < threshold) return null
    val physicalStep = if (deltaX > 0f) 1 else -1
    val logicalStep = if (isRtl) -physicalStep else physicalStep
    // No wraparound from either end of the bar.
    return (startIndex + logicalStep).takeIf { it in 0 until count }
}

/** Scale is driven by proximity to the capsule's center, not selection or its elastic tail. */
internal fun navigationIconScale(
    indicatorCenter: Float,
    iconCenter: Float,
    influenceRadius: Float,
    pressed: Boolean,
): Float {
    if (!pressed || !indicatorCenter.isFinite() || !iconCenter.isFinite() ||
        !influenceRadius.isFinite() || influenceRadius <= 0f) return 1f
    val proximity = (1f - abs(indicatorCenter - iconCenter) / influenceRadius).coerceIn(0f, 1f)
    // Smoothstep avoids a sudden growth/shrink at either end; a held center stays at 120%.
    val eased = proximity * proximity * (3f - 2f * proximity)
    return 1f + 0.2f * eased
}
