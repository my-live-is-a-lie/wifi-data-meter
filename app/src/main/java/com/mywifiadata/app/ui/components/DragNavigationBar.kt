package com.mywifiadata.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.core.navigationDragCenter
import com.mywifiadata.app.core.navigationIconScale
import com.mywifiadata.app.core.navigationIndexAt
import com.mywifiadata.app.core.navigationIndicatorContains
import com.mywifiadata.app.core.navigationShortSwipeTarget
import com.mywifiadata.app.core.navigationIndicatorBounds
import com.mywifiadata.app.core.navigationTabCenter
import com.mywifiadata.app.core.navigationTrailImpulse
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class DragNavigationItem(val label: String, val icon: ImageVector)

/**
 * Material's original items retain their click, ripple, keyboard and accessibility semantics.
 * A parent horizontal gesture takes over only after touch slop, cancelling the child's click.
 * The one shared capsule lives above the system inset and behind all icons, never in a page.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DragNavigationBar(
    items: List<DragNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentSelection by rememberUpdatedState(selectedIndex)
    var barSize by remember { mutableStateOf(IntSize.Zero) }
    var barCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val iconCentersY = remember { mutableStateMapOf<Int, Float>() }
    var fingerX by remember { mutableStateOf<Float?>(null) }
    var isBarPressed by remember { mutableStateOf(false) }
    val center = remember { Animatable(0f) }
    var positioned by remember { mutableStateOf(false) }
    var previousGeometry by remember { mutableStateOf<Pair<Float, Boolean>?>(null) }
    val trail = remember { Animatable(0f) }
    var trailJob by remember { mutableStateOf<Job?>(null) }
    val maxTrail = with(density) { 18.dp.toPx() }
    val shortSwipeDistance = with(density) { 16.dp.toPx() }
    val pillWidth = with(density) { 56.dp.toPx() }
    val pillHeight = with(density) { 32.dp.toPx() }
    val fallbackCenterY = with(density) { 22.dp.toPx() }
    val indicatorColor = ShortNavigationBarItemDefaults.colors().selectedIndicatorColor
    val width = barSize.width.toFloat()
    val targetCenter = fingerX ?: navigationTabCenter(selectedIndex, width, items.size, isRtl)
    val dragging = fingerX != null

    // Track each drag position without easing. On release/tap/external navigation, settle to
    // the selected icon. This effect is separate from pointerInput, which must survive tab changes.
    LaunchedEffect(targetCenter, dragging, width, isRtl) {
        if (width > 0f) {
            val geometry = width to isRtl
            val resized = previousGeometry != geometry
            previousGeometry = geometry
            if (!positioned || dragging || resized) center.snapTo(targetCenter)
            else center.animateTo(targetCenter, spring(dampingRatio = 0.82f, stiffness = 480f))
            positioned = true
        }
    }

    fun stretch(delta: Float) {
        if (delta == 0f) return // Don't restart the decay while the finger is stationary.
        val impulse = navigationTrailImpulse(delta, trail.value, maxTrail)
        trailJob?.cancel()
        trailJob = scope.launch {
            trail.snapTo(impulse)
            // Runs even while the finger stays down: stopping movement restores the capsule.
            trail.animateTo(0f, spring(dampingRatio = 1f, stiffness = 220f))
        }
    }

    Surface(modifier = modifier, color = ShortNavigationBarDefaults.containerColor) {
        // Insets sit outside the gesture/drawing area: system Back/Home gestures stay untouched.
        Box(Modifier.windowInsetsPadding(ShortNavigationBarDefaults.windowInsets)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { barSize = it }
                    .onGloballyPositioned { barCoordinates = it }
                    .drawBehind {
                        if (positioned) {
                            val bounds = navigationIndicatorBounds(
                                center = fingerX ?: center.value,
                                pillWidth = pillWidth,
                                stretch = trail.value,
                                barWidth = size.width,
                            )
                            val y = iconCentersY[selectedIndex] ?: fallbackCenterY
                            drawRoundRect(
                                color = indicatorColor,
                                topLeft = Offset(bounds.left, y - pillHeight / 2f),
                                size = Size(bounds.right - bounds.left, pillHeight),
                                cornerRadius = CornerRadius(pillHeight / 2f),
                            )
                        }
                    }
                    // Never key this on selectedIndex, a callback, or page content: a single
                    // uninterrupted pointer stream must be able to visit all four destinations.
                    .pointerInput(items.size, isRtl, barSize, maxTrail, shortSwipeDistance) {
                        try {
                            awaitEachGesture {
                                // Capture the actual press, not the later touch-slop position.
                                // Don't consume it: ordinary clicks and accessibility stay intact.
                                val down = awaitFirstDown(requireUnconsumed = false)
                                isBarPressed = down.position.x in 0f..size.width.toFloat() &&
                                    down.position.y in 0f..size.height.toFloat()
                                try {
                                    val startIndex = currentSelection
                                    val bounds = navigationIndicatorBounds(
                                        center = fingerX ?: center.value,
                                        pillWidth = pillWidth,
                                        stretch = trail.value,
                                        barWidth = size.width.toFloat(),
                                    )
                                    var shortcutAllowed = positioned && navigationIndicatorContains(
                                        down.position.x, down.position.y, bounds,
                                        iconCentersY[startIndex] ?: fallbackCenterY, pillHeight,
                                    )
                                    var lastSelected = startIndex
                                    var crossedTab = false
                                    var lastPosition = down.position

                                    fun moveTo(position: Offset) {
                                        lastPosition = position
                                        // Re-entry may resume scrubbing, but never an accidental shortcut.
                                        if (position.y < 0f || position.y > size.height) {
                                            shortcutAllowed = false
                                            isBarPressed = false
                                            return
                                        }
                                        isBarPressed = true
                                        val index = navigationIndexAt(position.x, size.width.toFloat(), items.size, isRtl)
                                            ?: return
                                        val nextX = navigationDragCenter(position.x, size.width.toFloat(), items.size)
                                        val previousX = fingerX
                                        fingerX = nextX
                                        if (previousX != null) stretch(nextX - previousX)
                                        if (index != startIndex) crossedTab = true
                                        if (index != lastSelected) {
                                            lastSelected = index
                                            currentOnSelect(index)
                                        }
                                    }

                                    val firstDrag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                                        change.consume()
                                    }
                                    if (firstDrag != null) {
                                        if (firstDrag.id != down.id || currentEvent.changes.size > 1) shortcutAllowed = false
                                        trailJob?.cancel()
                                        trailJob = scope.launch { trail.snapTo(0f) }
                                        moveTo(firstDrag.position)
                                        val completed = horizontalDrag(firstDrag.id) { change ->
                                            if (change.id != down.id || currentEvent.changes.size > 1) shortcutAllowed = false
                                            change.consume()
                                            moveTo(change.position)
                                        }
                                        // The final up can carry a last movement not seen in a move event.
                                        val up = currentEvent.changes.firstOrNull { !it.pressed && it.previousPressed }
                                        if (completed && up != null) moveTo(up.position)
                                        val next = navigationShortSwipeTarget(
                                            startIndex = startIndex,
                                            deltaX = lastPosition.x - down.position.x,
                                            count = items.size,
                                            isRtl = isRtl,
                                            threshold = maxOf(shortSwipeDistance, viewConfiguration.touchSlop),
                                            startedOnIndicator = shortcutAllowed,
                                            crossedTab = crossedTab,
                                            completed = completed && up?.id == down.id && currentEvent.changes.size == 1,
                                        )
                                        if (next != null) currentOnSelect(next)
                                        // Release animates the same shared capsule toward the new icon.
                                        // A long scrub or a cancelled drag never receives an extra step.
                                        fingerX = null
                                    }
                                } finally {
                                    // Covers taps (no drag), release, gesture cancellation and disposal.
                                    isBarPressed = false
                                }
                            }
                        } finally {
                            // Also reset if rotation, layout direction or disposal cancels input.
                            fingerX = null
                            isBarPressed = false
                            trailJob?.cancel()
                            scope.launch { trail.snapTo(0f) }
                        }
                    },
            ) {
                ShortNavigationBar(
                    containerColor = Color.Transparent,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    arrangement = ShortNavigationBarArrangement.EqualWeight,
                ) {
                    items.forEachIndexed { index, item ->
                        ShortNavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = { currentOnSelect(index) },
                            colors = ShortNavigationBarItemDefaults.colors(selectedIndicatorColor = Color.Transparent),
                            icon = {
                                val iconScale by animateFloatAsState(
                                    targetValue = navigationIconScale(
                                        indicatorCenter = fingerX ?: center.value,
                                        iconCenter = navigationTabCenter(index, width, items.size, isRtl),
                                        influenceRadius = width / items.size,
                                        pressed = isBarPressed && positioned,
                                    ),
                                    animationSpec = spring(
                                        dampingRatio = 1f,
                                        stiffness = 650f,
                                        visibilityThreshold = 0.001f,
                                    ),
                                    label = "Navigation icon proximity $index",
                                )
                                // Measure the unscaled wrapper: growth must not move the capsule,
                                // labels, tab hit regions or the bar itself.
                                Box(
                                    Modifier.size(24.dp).onGloballyPositioned { icon ->
                                        val bar = barCoordinates
                                        if (bar != null && bar.isAttached && icon.isAttached) {
                                            iconCentersY[index] = bar.localBoundingBoxOf(icon, clipBounds = false).center.y
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp).graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        },
                                    )
                                }
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        }
    }
}
