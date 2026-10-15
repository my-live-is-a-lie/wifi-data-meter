package com.mywifiadata.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.AppScrollRow
import com.mywifiadata.app.core.AppsFastScrollMetrics
import com.mywifiadata.app.core.fastScrollEdgeProgress
import com.mywifiadata.app.core.fastScrollEdgeContains
import com.mywifiadata.app.core.appsFastScrollMetrics

/** Shared app-list thumb: each screen supplies its own list state, pinned header and row count. */
@Composable
internal fun BoxScope.AppsFastScroller(
    listState: LazyListState,
    appCount: Int,
    headerKey: String,
) {
    val metrics by remember(listState, appCount, headerKey) {
        derivedStateOf {
            val layout = listState.layoutInfo
            val header = layout.visibleItemsInfo.firstOrNull { it.key == headerKey }
            if (header == null || layout.totalItemsCount != header.index + 1 + appCount) null
            else appsFastScrollMetrics(
                headerIndex = header.index,
                headerOffset = header.offset,
                headerSize = header.size,
                viewportStart = layout.viewportStartOffset,
                viewportEnd = layout.viewportEndOffset,
                afterPadding = layout.afterContentPadding,
                spacing = layout.mainAxisItemSpacing,
                appCount = appCount,
                rows = layout.visibleItemsInfo.map { AppScrollRow(it.index, it.offset, it.size) },
                canScrollForward = listState.canScrollForward,
            )
        }
    }
    val density = LocalDensity.current
    val activeThumbHeight = 32.dp
    val activeThumbHeightPx = with(density) { activeThumbHeight.toPx() }
    var dragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    // Geometry/visibility changes invalidate a grab, but ordinary scrolling must not do so.
    LaunchedEffect(metrics?.trackTop, metrics?.trackBottom, appCount) {
        dragging = false
    }
    val current = metrics ?: return
    val latestMetrics by rememberUpdatedState(current)
    val trackHeightPx = (current.trackBottom - current.trackTop).toFloat()
    val travel = trackHeightPx - activeThumbHeightPx
    if (travel <= 0f) return

    fun scrollTo(progress: Float, geometry: AppsFastScrollMetrics) {
        val target = geometry.target(progress)
        // One remeasure per pointer update, no queue of animations or scroll coroutines.
        listState.requestScrollToItem(target.index, target.offset)
    }

    // Position follows actual scrolling without animation lag, so grabbing during a fling
    // doesn't jump from an outdated visual location. Only size and opacity are animated.
    val progress = if (dragging) dragProgress else current.progress
    val thumbWidth by animateDpAsState(if (dragging) 5.dp else 2.dp, tween(120), label = "App thumb width")
    val thumbHeight by animateDpAsState(if (dragging) 32.dp else 16.dp, tween(120), label = "App thumb height")
    val alpha by animateFloatAsState(if (dragging) 1f else 0.4f, tween(120), label = "App thumb opacity")
    val tint = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.apps_fast_scroll)
    Box(
        Modifier
            // This must stay on the physical RIGHT in Arabic as well as English.
            .align(AbsoluteAlignment.TopRight)
            .offset { IntOffset(0, current.trackTop) }
            // Only the outermost 24dp below the pinned header is reserved for edge dragging.
            // The rest of the screen retains normal LazyColumn scrolling and app taps.
            .width(24.dp)
            .height(with(density) { trackHeightPx.toDp() })
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(if (dragging) dragProgress else current.progress, 0f..1f)
                setProgress { requested ->
                    if (!requested.isFinite()) false else {
                        scrollTo(requested.coerceIn(0f, 1f), current)
                        true
                    }
                }
            }
            .pointerInput(appCount, headerKey, current.trackTop, current.trackBottom, activeThumbHeightPx) {
                try {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fun inside(x: Float, y: Float) =
                            fastScrollEdgeContains(x, y, size.width.toFloat(), size.height.toFloat())
                        if (!inside(down.position.x, down.position.y)) return@awaitEachGesture
                        // A tap/jitter at the edge must not jump the list. A vertical drag can
                        // begin anywhere on this rail; hitting the tiny thumb isn't required.
                        val start = awaitVerticalTouchSlopOrCancellation(down.id) { change, _ ->
                            change.consume()
                        } ?: return@awaitEachGesture
                        if (start.id != down.id || currentEvent.changes.size != 1 ||
                            !inside(start.position.x, start.position.y)) return@awaitEachGesture
                        val geometry = latestMetrics // Freeze row estimates until release.
                        fun follow(y: Float) {
                            dragProgress = fastScrollEdgeProgress(y, size.height.toFloat(), activeThumbHeightPx)
                            scrollTo(dragProgress, geometry)
                        }
                        try {
                            dragging = true
                            follow(start.position.y) // Jump to the finger, not the old thumb.
                            while (true) {
                                // Read every event, including horizontal-only motion: leaving
                                // the edge must shrink the thumb even with no vertical delta.
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed || change.isConsumed || event.changes.size != 1) break
                                if (!inside(change.position.x, change.position.y)) {
                                    change.consume()
                                    break
                                }
                                change.consume()
                                follow(change.position.y)
                            }
                        } finally {
                            dragging = false
                        }
                        // awaitEachGesture waits for release after an exit; re-entry alone
                        // cannot restart a cancelled grab or open an app underneath it.
                    }
                } finally {
                    dragging = false
                }
            }
            .drawBehind {
                val w = thumbWidth.toPx()
                val h = thumbHeight.toPx()
                drawRoundRect(
                    color = tint.copy(alpha = alpha),
                    topLeft = Offset(
                        size.width - 2.dp.toPx() - w,
                        activeThumbHeightPx / 2f + progress * travel - h / 2f,
                    ),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(w / 2f),
                )
            },
    )
}
