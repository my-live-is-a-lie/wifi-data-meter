package com.mywifiadata.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import com.mywifiadata.app.core.navigationIndexAt
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
    val center = remember { Animatable(0f) }
    var positioned by remember { mutableStateOf(false) }
    var previousGeometry by remember { mutableStateOf<Pair<Float, Boolean>?>(null) }
    val trail = remember { Animatable(0f) }
    var trailJob by remember { mutableStateOf<Job?>(null) }
    val maxTrail = with(density) { 18.dp.toPx() }
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
                    .pointerInput(items.size, isRtl, barSize, maxTrail) {
                        var lastSelected = currentSelection
                        fun moveTo(position: Offset) {
                            // Leaving vertically suspends switching; re-entering resumes it.
                            if (position.y < 0f || position.y > size.height) return
                            val index = navigationIndexAt(position.x, size.width.toFloat(), items.size, isRtl)
                                ?: return
                            val nextX = navigationDragCenter(position.x, size.width.toFloat(), items.size)
                            val previousX = fingerX
                            fingerX = nextX
                            // The first position may be far from the old selection. Only actual
                            // finger movement determines which edge trails, not that initial jump.
                            if (previousX != null) stretch(nextX - previousX)
                            if (index != lastSelected) {
                                lastSelected = index
                                currentOnSelect(index)
                            }
                        }
                        try {
                            detectHorizontalDragGestures(
                                onDragStart = { position ->
                                    lastSelected = currentSelection
                                    trailJob?.cancel()
                                    trailJob = scope.launch { trail.snapTo(0f) }
                                    moveTo(position)
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    moveTo(change.position)
                                },
                                onDragEnd = { fingerX = null },
                                onDragCancel = { fingerX = null },
                            )
                        } finally {
                            // Also reset if rotation, layout direction or disposal cancels input.
                            fingerX = null
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
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).onGloballyPositioned { icon ->
                                        val bar = barCoordinates
                                        if (bar != null && bar.isAttached && icon.isAttached) {
                                            iconCentersY[index] = bar.localBoundingBoxOf(icon, clipBounds = false).center.y
                                        }
                                    },
                                )
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        }
    }
}
