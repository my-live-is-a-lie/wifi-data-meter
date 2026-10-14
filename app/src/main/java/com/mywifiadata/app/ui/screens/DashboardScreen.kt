package com.mywifiadata.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.data.model.DashboardLayout
import com.mywifiadata.app.data.model.DashboardLayoutIds
import com.mywifiadata.app.data.model.WifiBand
import com.mywifiadata.app.data.model.WifiStatus
import com.mywifiadata.app.ui.UiState
import com.mywifiadata.app.ui.components.Dot
import com.mywifiadata.app.ui.components.DottedUsageGauge
import com.mywifiadata.app.ui.components.EmptyState
import com.mywifiadata.app.ui.components.ExpressiveCard
import com.mywifiadata.app.ui.components.InfoRow
import com.mywifiadata.app.ui.components.PermissionCard
import com.mywifiadata.app.ui.components.QuotaBar
import com.mywifiadata.app.ui.components.SectionHeader
import com.mywifiadata.app.ui.components.SignalBarsIcon
import com.mywifiadata.app.ui.components.SpeedTestPanel
import com.mywifiadata.app.ui.components.TodayTotalRow
import com.mywifiadata.app.ui.components.TrafficTile
import com.mywifiadata.app.ui.components.UsageRing
import com.mywifiadata.app.ui.theme.AppButtonShape
import com.mywifiadata.app.ui.theme.LocalDownloadColor
import com.mywifiadata.app.ui.theme.LocalUploadColor
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * لوحة المعلومات: نسبة الباقة، حالة الشبكة، إجماليات اليوم والجلسة، والسرعة اللحظية.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DashboardScreen(
    state: UiState,
    onOpenUsageAccess: () -> Unit,
    onOpenQuotaSettings: () -> Unit,
    onRefresh: () -> Unit,
    onResetSession: () -> Unit,
    onToggleTracking: () -> Unit,
    onStartSpeedTest: () -> Unit,
    onStopSpeedTest: () -> Unit,
    onAddUsageWidget: (Int) -> Unit,
    onUsageCardStyleChange: (Int) -> Unit,
    isEditing: Boolean = false,
    dashboardLayout: DashboardLayout = DashboardLayout.Default,
    onDashboardLayoutChange: (DashboardLayout) -> Unit = {},
    onSaveDashboardLayout: () -> Unit = {},
    onCancelDashboardLayout: () -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    var isHeroCompact by rememberSaveable { mutableStateOf(false) }
    var selectedCardStyle by rememberSaveable {
        mutableIntStateOf(
            state.settings.usageCardStyleIndex.coerceIn(0, UsageCardStyle.entries.lastIndex),
        )
    }
    val layout = dashboardLayout.normalized()
    val layoutDirection = LocalLayoutDirection.current
    val horizontalStartPadding = contentPadding.calculateStartPadding(layoutDirection)
    val horizontalEndPadding = contentPadding.calculateEndPadding(layoutDirection)
    val cardBounds = remember { mutableStateMapOf<String, Rect>() }
    val quickActionBounds = remember { mutableStateMapOf<String, Rect>() }
    val todaySectionBounds = remember { mutableStateMapOf<String, Rect>() }
    val trafficTileBounds = remember { mutableStateMapOf<String, Rect>() }
    val cardsToShow = layout.cardOrder.filter { cardId ->
        isEditing || cardId !in layout.hiddenCards
    }.filter { cardId ->
        cardId != DashboardLayoutIds.QUICK_ACTIONS_CARD || isEditing ||
            layout.quickActionOrder.any { it !in layout.hiddenQuickActions }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (isEditing) {
            DashboardEditToolbar(
                modifier = Modifier.padding(
                    start = horizontalStartPadding,
                    end = horizontalEndPadding,
                ),
                onSave = onSaveDashboardLayout,
                onCancel = onCancelDashboardLayout,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            // تُترك مساحة أفقية للبطاقات العادية، بينما تمتد الواجهة المصغّرة حتى حافتي الشاشة.
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.hasUsageAccess) {
                item(key = "dashboard-permission-card") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = horizontalStartPadding, end = horizontalEndPadding),
                    ) {
                        PermissionCard(
                            icon = Icons.Rounded.Info,
                            title = stringResource(R.string.permission_needed_title),
                            message = stringResource(R.string.permission_needed_body),
                            grantLabel = stringResource(R.string.action_open_usage_access),
                            settingsLabel = stringResource(R.string.settings_usage_access),
                            onGrant = onOpenUsageAccess,
                            onOpenSettings = onOpenUsageAccess,
                        )
                    }
                }
            }

            items(items = cardsToShow, key = { "dashboard-card-$it" }) { cardId ->
                val frameModifier = Modifier
                    .fillMaxWidth()
                .then(
                    if (cardId == DashboardLayoutIds.USAGE_CARD && !isEditing) {
                        Modifier
                    } else {
                        Modifier.padding(start = horizontalStartPadding, end = horizontalEndPadding)
                    },
                )
                EditableDashboardItem(
                    id = cardId,
                    isEditing = isEditing,
                    isHidden = cardId in layout.hiddenCards,
                    dragAxis = DragAxis.VERTICAL,
                    positions = cardBounds,
                    onMove = { from, to ->
                        onDashboardLayoutChange(
                            layout.copy(cardOrder = moveItem(layout.cardOrder, from, to)),
                        )
                    },
                    onToggleVisibility = {
                        onDashboardLayoutChange(
                            layout.copy(hiddenCards = toggleId(layout.hiddenCards, cardId)),
                        )
                    },
                    modifier = frameModifier,
                ) {
                    when (cardId) {
                        DashboardLayoutIds.USAGE_CARD -> HeroCard(
                            state = state,
                            isCompact = isHeroCompact && !isEditing,
                            isEditing = isEditing,
                            selectedStyleIndex = selectedCardStyle,
                            onOpenQuotaSettings = onOpenQuotaSettings,
                            horizontalStartPadding = if (isEditing) 0.dp else horizontalStartPadding,
                            horizontalEndPadding = if (isEditing) 0.dp else horizontalEndPadding,
                            onLongPress = { isHeroCompact = true },
                            onRestore = { isHeroCompact = false },
                            onChangeStyle = { direction ->
                                val styleCount = UsageCardStyle.entries.size
                                selectedCardStyle = (selectedCardStyle + direction + styleCount) % styleCount
                                onUsageCardStyleChange(selectedCardStyle)
                            },
                            onAddWidget = onAddUsageWidget,
                        )

                        DashboardLayoutIds.QUICK_ACTIONS_CARD -> QuickActions(
                            onRefresh = onRefresh,
                            onResetSession = onResetSession,
                            trackingEnabled = state.settings.trackingEnabled,
                            onToggleTracking = onToggleTracking,
                            isEditing = isEditing,
                            layout = layout,
                            itemBounds = quickActionBounds,
                            onLayoutChange = onDashboardLayoutChange,
                        )

                        DashboardLayoutIds.WIFI_CARD -> {
                            if (state.wifi.isWifiConnected) {
                                WifiDetailsCard(wifi = state.wifi, isEditing = isEditing)
                            } else {
                                NotConnectedCard(isOnline = state.wifi.isOnline)
                            }
                        }

                        DashboardLayoutIds.TODAY_CARD -> TodayCard(
                            state = state,
                            onStart = onStartSpeedTest,
                            onStop = onStopSpeedTest,
                            isEditing = isEditing,
                            layout = layout,
                            sectionBounds = todaySectionBounds,
                            trafficBounds = trafficTileBounds,
                            onLayoutChange = onDashboardLayoutChange,
                        )

                        DashboardLayoutIds.SESSION_CARD -> SessionCard(
                            state = state,
                            onResetSession = onResetSession,
                            isEditing = isEditing,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardEditToolbar(
    modifier: Modifier = Modifier,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val instructionAlignment = if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        TextAlign.Start
    } else {
        TextAlign.End
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.dashboard_edit_instructions),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = instructionAlignment,
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalButton(
            onClick = onSave,
            shape = AppButtonShape,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(text = stringResource(R.string.action_save))
        }
        TextButton(
            onClick = onCancel,
            shape = AppButtonShape,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(text = stringResource(R.string.action_cancel))
        }
    }
}

private enum class DragAxis { HORIZONTAL, VERTICAL }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EditableDashboardItem(
    id: String,
    isEditing: Boolean,
    isHidden: Boolean,
    dragAxis: DragAxis,
    positions: SnapshotStateMap<String, Rect>,
    onMove: (String, String) -> Unit,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val latestOnMove by rememberUpdatedState(onMove)
    val layoutDirection = LocalLayoutDirection.current
    val badgeAlignment = if (layoutDirection == LayoutDirection.Rtl) {
        Alignment.TopEnd
    } else {
        Alignment.TopStart
    }

    val positionModifier = if (isEditing) {
        Modifier.onGloballyPositioned { coordinates ->
            positions[id] = coordinates.boundsInRoot()
        }
    } else {
        Modifier
    }
    val dragModifier = if (isEditing) {
        Modifier.pointerInput(isEditing, id, dragAxis) {
            var accumulatedDrag = 0f
            var previousTarget: String? = null

            fun handleDrag(change: androidx.compose.ui.input.pointer.PointerInputChange, amount: Float) {
                change.consume()
                val ownBounds = positions[id] ?: return
                val previousCenter = axisCenter(ownBounds, dragAxis) + accumulatedDrag
                val nextCenter = previousCenter + amount
                val crossedTarget = positions.entries
                    .asSequence()
                    .filter { (targetId, bounds) ->
                        targetId != id && bounds.width > 0f && bounds.height > 0f &&
                            (
                                if (amount > 0f) {
                                    previousCenter <= axisCenter(bounds, dragAxis) &&
                                        nextCenter >= axisCenter(bounds, dragAxis)
                                } else if (amount < 0f) {
                                    previousCenter >= axisCenter(bounds, dragAxis) &&
                                        nextCenter <= axisCenter(bounds, dragAxis)
                                } else {
                                    false
                                }
                            )
                    }
                    .minByOrNull { (_, bounds) -> abs(nextCenter - axisCenter(bounds, dragAxis)) }

                accumulatedDrag += amount
                if (crossedTarget != null && crossedTarget.key != previousTarget) {
                    accumulatedDrag += axisStart(ownBounds, dragAxis) -
                        axisStart(crossedTarget.value, dragAxis)
                    latestOnMove(id, crossedTarget.key)
                    previousTarget = crossedTarget.key
                } else if (crossedTarget == null) {
                    previousTarget = null
                }
            }

            when (dragAxis) {
                DragAxis.HORIZONTAL -> detectHorizontalDragGesturesAfterLongPress(
                    onDragStart = {
                        accumulatedDrag = 0f
                        previousTarget = null
                    },
                    onHorizontalDrag = ::handleDrag,
                    onDragEnd = {
                        accumulatedDrag = 0f
                        previousTarget = null
                    },
                    onDragCancel = {
                        accumulatedDrag = 0f
                        previousTarget = null
                    },
                )

                DragAxis.VERTICAL -> detectVerticalDragGesturesAfterLongPress(
                    onDragStart = {
                        accumulatedDrag = 0f
                        previousTarget = null
                    },
                    onVerticalDrag = ::handleDrag,
                    onDragEnd = {
                        accumulatedDrag = 0f
                        previousTarget = null
                    },
                    onDragCancel = {
                        accumulatedDrag = 0f
                        previousTarget = null
                    },
                )
            }
        }
    } else {
        Modifier
    }

    DisposableEffect(isEditing, id, positions) {
        onDispose { positions.remove(id) }
    }

    Box(
        modifier = modifier
            .then(positionModifier)
            .then(dragModifier),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = if (isEditing && isHidden) 0.42f else 1f },
        ) {
            content()
        }

        if (isEditing) {
            Box(
                modifier = Modifier
                    .align(badgeAlignment)
                    .absoluteOffset(x = (-10).dp, y = (-10).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onToggleVisibility),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isHidden) Icons.Rounded.Add else Icons.Rounded.Remove,
                    contentDescription = stringResource(
                        if (isHidden) R.string.dashboard_show_item else R.string.dashboard_hide_item,
                    ),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun axisCenter(rect: Rect, axis: DragAxis): Float = when (axis) {
    DragAxis.HORIZONTAL -> rect.center.x
    DragAxis.VERTICAL -> rect.center.y
}

private fun axisStart(rect: Rect, axis: DragAxis): Float = when (axis) {
    DragAxis.HORIZONTAL -> rect.left
    DragAxis.VERTICAL -> rect.top
}

private fun moveItem(order: List<String>, from: String, to: String): List<String> {
    val fromIndex = order.indexOf(from)
    val toIndex = order.indexOf(to)
    if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return order
    return order.toMutableList().apply {
        val moved = removeAt(fromIndex)
        add(toIndex.coerceIn(0, size), moved)
    }
}

private fun toggleId(ids: Set<String>, id: String): Set<String> =
    if (id in ids) ids - id else ids + id

private enum class UsageCardStyle {
    RING,
    DOTTED_RING,
    SPLIT,
}

@Composable
private fun HeroCard(
    state: UiState,
    isCompact: Boolean,
    isEditing: Boolean,
    selectedStyleIndex: Int,
    onOpenQuotaSettings: () -> Unit,
    horizontalStartPadding: androidx.compose.ui.unit.Dp,
    horizontalEndPadding: androidx.compose.ui.unit.Dp,
    onLongPress: () -> Unit,
    onRestore: () -> Unit,
    onChangeStyle: (Int) -> Unit,
    onAddWidget: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = isCompact,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = {
                val bounce = spring<Float>(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
                (scaleIn(animationSpec = bounce, initialScale = 0.88f) + fadeIn(animationSpec = bounce)) togetherWith
                    (scaleOut(animationSpec = bounce, targetScale = 0.88f) + fadeOut(animationSpec = bounce))
            },
            label = "usage-card-compact-mode",
        ) { compact ->
            if (compact) {
                UsageCardCarousel(
                    state = state,
                    selectedStyleIndex = selectedStyleIndex,
                    horizontalStartPadding = horizontalStartPadding,
                    horizontalEndPadding = horizontalEndPadding,
                    onOpenQuotaSettings = onOpenQuotaSettings,
                    onRestore = onRestore,
                    onChangeStyle = onChangeStyle,
                )
            } else {
                FullUsageCard(
                    state = state,
                    selectedStyleIndex = selectedStyleIndex,
                    onOpenQuotaSettings = if (isEditing) ({}) else onOpenQuotaSettings,
                    modifier = Modifier.padding(
                        start = horizontalStartPadding,
                        end = horizontalEndPadding,
                    ),
                    onLongPress = if (isEditing) null else onLongPress,
                )
            }
        }

        AnimatedVisibility(
            visible = isCompact,
            modifier = Modifier.fillMaxWidth(),
            enter = fadeIn(animationSpec = spring()) +
                expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut(animationSpec = spring()) +
                shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                FilledTonalButton(
                    onClick = { onAddWidget(selectedStyleIndex) },
                    shape = AppButtonShape,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.action_add_widget),
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                    )
                }
            }
        }
    }
}

@Composable
private fun FullUsageCard(
    state: UiState,
    selectedStyleIndex: Int,
    onOpenQuotaSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val hasQuota = state.hasQuota
    val progress = usageCardProgress(state)
    val traffic = if (hasQuota) state.cycle else state.today
    val downloadColor = LocalDownloadColor.current
    val uploadColor = LocalUploadColor.current
    val usageValue = if (hasQuota) {
        stringResource(R.string.percent_format, (state.quotaProgress * 100).roundToInt())
    } else {
        Format.bytes(state.today.totalBytes)
    }
    val usageLabel = stringResource(if (hasQuota) R.string.label_used else R.string.label_today)
    val usedValue = Format.bytes(if (hasQuota) state.cycle.totalBytes else state.today.totalBytes)
    val gaugeStartColor = if (state.quotaExceeded) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val gaugeEndColor = if (state.quotaExceeded) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    ExpressiveCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick,
        onLongClick = onLongPress,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (selectedStyleIndex) {
                UsageCardStyle.DOTTED_RING.ordinal -> {
                    DottedUsageGauge(
                        progress = progress,
                        modifier = Modifier.size(220.dp),
                        strokeWidth = 11.dp,
                        dotRadius = 2.5f.dp,
                        dotCount = 39,
                        dotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                        progressColor = gaugeStartColor,
                        contentDescription = stringResource(R.string.quota_ring_title),
                    ) {
                        FullGaugeCenter(usageValue, usageLabel, usedValue)
                    }
                }

                UsageCardStyle.SPLIT.ordinal -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        UsageRing(
                            progress = progress,
                            modifier = Modifier.size(148.dp),
                            strokeWidth = 16.dp,
                            startColor = gaugeStartColor,
                            endColor = gaugeEndColor,
                            contentDescription = stringResource(R.string.quota_ring_title),
                        ) {
                            Text(
                                text = usageValue,
                                style = MaterialTheme.typography.titleLargeEmphasized,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = usageLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = usedValue,
                                style = MaterialTheme.typography.titleMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                else -> {
                    UsageRing(
                        progress = progress,
                        modifier = Modifier.size(220.dp),
                        strokeWidth = 22.dp,
                        startColor = gaugeStartColor,
                        endColor = gaugeEndColor,
                        contentDescription = stringResource(R.string.quota_ring_title),
                    ) {
                        FullGaugeCenter(usageValue, usageLabel, usedValue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (hasQuota) {
                Text(
                    text = stringResource(
                        R.string.quota_remaining_of,
                        Format.bytes(state.remainingBytes),
                        Format.bytes(state.quotaBytes),
                    ),
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                if (state.quotaExceeded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.quota_exceeded),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.quota_not_set),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                QuotaBar(
                    progress = if (state.cycle.totalBytes > 0L) {
                        state.today.totalBytes.toFloat() / state.cycle.totalBytes.toFloat()
                    } else {
                        0f
                    },
                    startColor = downloadColor,
                    endColor = uploadColor,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.session_since,
                        Format.fullDate(state.settings.sessionStartMillis),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenQuotaSettings, shape = AppButtonShape) {
                    Text(text = stringResource(R.string.quota_set_action))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                LegendItem(
                    color = downloadColor,
                    label = stringResource(R.string.label_download),
                    value = Format.bytes(traffic.rxBytes),
                )
                LegendItem(
                    color = uploadColor,
                    label = stringResource(R.string.label_upload),
                    value = Format.bytes(traffic.txBytes),
                )
            }
        }
    }
}

@Composable
private fun FullGaugeCenter(
    usageValue: String,
    usageLabel: String,
    usedValue: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = usageValue,
            style = MaterialTheme.typography.displaySmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = usageLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = usedValue,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UsageCardCarousel(
    state: UiState,
    selectedStyleIndex: Int,
    horizontalStartPadding: androidx.compose.ui.unit.Dp,
    horizontalEndPadding: androidx.compose.ui.unit.Dp,
    onOpenQuotaSettings: () -> Unit,
    onRestore: () -> Unit,
    onChangeStyle: (Int) -> Unit,
) {
    val dragOffset = androidx.compose.runtime.remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val previousLabel = stringResource(R.string.cd_previous_usage_card)
    val nextLabel = stringResource(R.string.cd_next_usage_card)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val leftEdgeAlignment = if (isRtl) Alignment.CenterEnd else Alignment.CenterStart
    val rightEdgeAlignment = if (isRtl) Alignment.CenterStart else Alignment.CenterEnd

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp)
            .clipToBounds()
            .pointerInput(selectedStyleIndex) {
                var accumulatedDrag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount
                        val maximumDrag = size.width * 0.36f
                        val visibleDrag = accumulatedDrag.coerceIn(-maximumDrag, maximumDrag)
                        coroutineScope.launch { dragOffset.snapTo(visibleDrag) }
                    },
                    onDragEnd = {
                        val threshold = size.width * 0.14f
                        when {
                            accumulatedDrag < -threshold -> onChangeStyle(1)
                            accumulatedDrag > threshold -> onChangeStyle(-1)
                        }
                        accumulatedDrag = 0f
                        coroutineScope.launch {
                            dragOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                    },
                    onDragCancel = {
                        accumulatedDrag = 0f
                        coroutineScope.launch {
                            dragOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            )
                        }
                    },
                )
            },
    ) {
        val cardWidth = (maxWidth - horizontalStartPadding - horizontalEndPadding).coerceAtLeast(1.dp)
        val previewWidth = minOf(maxWidth * 0.60f, 244.dp, cardWidth)
        val previewScale = (previewWidth / cardWidth).coerceIn(0.01f, 1f)
        val step = previewWidth + maxWidth * 0.10f

        UsageCardStyle.entries.forEach { style ->
            val relativeIndex = (style.ordinal - selectedStyleIndex + UsageCardStyle.entries.size) %
                UsageCardStyle.entries.size
            val slot = when (relativeIndex) {
                0 -> 0
                1 -> 1
                else -> -1
            }
            val animatedOffset by animateDpAsState(
                targetValue = step * slot.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "usage-card-carousel-position-${style.name}",
            )
            val animatedScale by animateFloatAsState(
                targetValue = if (slot == 0) 1f else 0.91f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "usage-card-carousel-scale-${style.name}",
            )
            val animatedAlpha by animateFloatAsState(
                targetValue = if (slot == 0) 1f else 0.78f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "usage-card-carousel-alpha-${style.name}",
            )

            FullUsageCard(
                state = state,
                selectedStyleIndex = style.ordinal,
                onOpenQuotaSettings = onOpenQuotaSettings,
                modifier = Modifier
                    .align(Alignment.Center)
                    .absoluteOffset(x = animatedOffset)
                    .graphicsLayer {
                        translationX = dragOffset.value * 0.55f
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = animatedAlpha
                    }
                    .heightIn(min = 244.dp)
                    .scaleCarouselContent(previewScale)
                    .width(cardWidth)
                    .zIndex(if (slot == 0) 1f else 0f),
                onClick = onRestore,
            )
        }

        // مناطق لمس شفافة على طرفي الشاشة: تضغط يمينًا للبطاقة التالية ويسارًا للسابقة.
        Box(
            modifier = Modifier
                .align(leftEdgeAlignment)
                .width(maxWidth * 0.11f)
                .height(260.dp)
                .zIndex(5f)
                .clickable(onClickLabel = previousLabel) { onChangeStyle(-1) },
        )
        Box(
            modifier = Modifier
                .align(rightEdgeAlignment)
                .width(maxWidth * 0.11f)
                .height(260.dp)
                .zIndex(5f)
                .clickable(onClickLabel = nextLabel) { onChangeStyle(1) },
        )
    }
}

private fun Modifier.scaleCarouselContent(scale: Float): Modifier = layout { measurable, constraints ->
    fun unscale(size: Int): Int = if (size == Constraints.Infinity) {
        Constraints.Infinity
    } else {
        (size / scale).roundToInt()
    }

    val placeable = measurable.measure(
        Constraints(
            minWidth = unscale(constraints.minWidth),
            maxWidth = unscale(constraints.maxWidth),
            minHeight = unscale(constraints.minHeight),
            maxHeight = unscale(constraints.maxHeight),
        ),
    )
    val width = (placeable.width * scale).roundToInt()
        .coerceIn(constraints.minWidth, constraints.maxWidth)
    val height = (placeable.height * scale).roundToInt()
        .coerceIn(constraints.minHeight, constraints.maxHeight)

    layout(width, height) {
        placeable.placeWithLayer(0, 0) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}

private fun usageCardProgress(state: UiState): Float = when {
    state.hasQuota -> state.quotaProgress
    state.cycle.totalBytes > 0L -> {
        state.today.totalBytes.toFloat() / state.cycle.totalBytes.toFloat()
    }
    else -> 0f
}

@Composable
private fun LegendItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color = color, size = 10.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun QuickActions(
    onRefresh: () -> Unit,
    onResetSession: () -> Unit,
    trackingEnabled: Boolean,
    onToggleTracking: () -> Unit,
    isEditing: Boolean,
    layout: DashboardLayout,
    itemBounds: SnapshotStateMap<String, Rect>,
    onLayoutChange: (DashboardLayout) -> Unit,
) {
    val actionIds = if (isEditing) {
        layout.quickActionOrder
    } else {
        layout.quickActionOrder.filterNot(layout.hiddenQuickActions::contains)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        actionIds.forEach { actionId ->
            EditableDashboardItem(
                id = actionId,
                isEditing = isEditing,
                isHidden = actionId in layout.hiddenQuickActions,
                dragAxis = DragAxis.HORIZONTAL,
                positions = itemBounds,
                onMove = { from, to ->
                    onLayoutChange(
                        layout.copy(quickActionOrder = moveItem(layout.quickActionOrder, from, to)),
                    )
                },
                onToggleVisibility = {
                    onLayoutChange(
                        layout.copy(hiddenQuickActions = toggleId(layout.hiddenQuickActions, actionId)),
                    )
                },
                modifier = Modifier.weight(1f).heightIn(min = 84.dp),
            ) {
                when (actionId) {
                    DashboardLayoutIds.TRACKING_ACTION -> QuickActionButton(
                        label = stringResource(
                            if (trackingEnabled) R.string.action_stop_tracking else R.string.action_start_tracking,
                        ),
                        icon = if (trackingEnabled) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        onClick = if (isEditing) ({}) else onToggleTracking,
                        active = trackingEnabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp),
                    )

                    DashboardLayoutIds.REFRESH_ACTION -> QuickActionButton(
                        label = stringResource(R.string.action_refresh),
                        icon = Icons.Rounded.Refresh,
                        onClick = if (isEditing) ({}) else onRefresh,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp),
                    )

                    DashboardLayoutIds.RESET_SESSION_ACTION -> QuickActionButton(
                        label = stringResource(R.string.action_reset_session),
                        icon = Icons.Rounded.RestartAlt,
                        onClick = if (isEditing) ({}) else onResetSession,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp),
                    )
                }
            }
        }
    }
}

/**
 * زر إجراء سريع: الأيقونة فوق النص، مع قصّ النص بثلاث نقاط بدل تجاوزه حدود الزر
 * (ButtonGroup كان يقصّ التسميات الطويلة فتظهر الأيقونة فوق النص).
 * [active] تلوّن الزر باللون الأساسي (يُستخدم لحالة المراقبة الجارية).
 */
@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = AppButtonShape,
        colors = if (active) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WifiDetailsCard(wifi: WifiStatus, isEditing: Boolean) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "wifi-details-chevron",
    )
    val unknown = stringResource(R.string.value_unknown)
    // نص الحالة: نمرّر اسم الشبكة داخل القالب، وإلا ظهرت العلامة %1$s كما هي.
    val statusText = when {
        wifi.isOnline && wifi.ssid != null -> stringResource(R.string.status_connected, wifi.ssid)
        wifi.isOnline || wifi.isWifiConnected -> stringResource(R.string.status_connected_unknown)
        else -> stringResource(R.string.status_disconnected)
    }

    ExpressiveCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isEditing) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SignalBarsIcon(
                    level = wifi.signalLevel,
                    modifier = Modifier.size(width = 34.dp, height = 26.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = wifi.ssid ?: stringResource(R.string.status_connected_unknown),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (wifi.isOnline) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = stringResource(R.string.cd_wifi),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.wifi_details_collapse else R.string.wifi_details_expand,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionHeader(title = stringResource(R.string.wifi_card_title))

                    InfoRow(
                        icon = Icons.Rounded.Speed,
                        label = stringResource(R.string.label_link_speed),
                        value = wifi.linkSpeedMbps?.let { "$it ${stringResource(R.string.unit_mbps)}" } ?: unknown,
                    )
                    InfoRow(
                        icon = Icons.Rounded.Router,
                        label = stringResource(R.string.label_band),
                        value = when (wifi.band) {
                            WifiBand.GHZ_2_4 -> stringResource(R.string.band_2_4)
                            WifiBand.GHZ_5 -> stringResource(R.string.band_5)
                            WifiBand.GHZ_6 -> stringResource(R.string.band_6)
                            WifiBand.UNKNOWN -> unknown
                        },
                    )
                    InfoRow(
                        icon = Icons.Rounded.Info,
                        label = stringResource(R.string.label_standard),
                        value = wifi.standard ?: unknown,
                    )
                    InfoRow(
                        icon = Icons.Rounded.Info,
                        label = stringResource(R.string.label_signal),
                        value = wifi.rssiDbm?.let { "$it ${stringResource(R.string.unit_dbm)}" } ?: unknown,
                    )
                    InfoRow(
                        icon = Icons.Rounded.Info,
                        label = stringResource(R.string.label_frequency),
                        value = wifi.frequencyMhz?.let { "$it MHz" } ?: unknown,
                    )
                    InfoRow(
                        icon = Icons.Rounded.Info,
                        label = stringResource(R.string.label_ip),
                        value = wifi.ipv4 ?: unknown,
                    )
                    InfoRow(
                        icon = Icons.Rounded.Info,
                        label = stringResource(R.string.label_gateway),
                        value = wifi.gateway ?: unknown,
                    )
                    if (wifi.dnsServers.isNotEmpty()) {
                        InfoRow(
                            icon = Icons.Rounded.Info,
                            label = "DNS",
                            value = wifi.dnsServers.joinToString(", "),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotConnectedCard(isOnline: Boolean) {
    ExpressiveCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        EmptyState(
            icon = Icons.Rounded.WifiOff,
            title = stringResource(R.string.wifi_not_connected_title),
            message = stringResource(R.string.wifi_not_connected_body),
        )
        if (isOnline) {
            Text(
                text = stringResource(R.string.status_not_wifi),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TodayCard(
    state: UiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    isEditing: Boolean,
    layout: DashboardLayout,
    sectionBounds: SnapshotStateMap<String, Rect>,
    trafficBounds: SnapshotStateMap<String, Rect>,
    onLayoutChange: (DashboardLayout) -> Unit,
) {
    val sectionIds = layout.todaySectionOrder.filter { sectionId ->
        isEditing || sectionId !in layout.hiddenTodaySections
    }.filter { sectionId ->
        sectionId != DashboardLayoutIds.TODAY_TRAFFIC_ROW || isEditing ||
            layout.trafficTileOrder.any { it !in layout.hiddenTrafficTiles }
    }

    ExpressiveCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = stringResource(R.string.label_today),
                supportingText = Format.fullDate(state.now),
            )
            if (sectionIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
            }
            sectionIds.forEachIndexed { index, sectionId ->
                EditableDashboardItem(
                    id = sectionId,
                    isEditing = isEditing,
                    isHidden = sectionId in layout.hiddenTodaySections,
                    dragAxis = DragAxis.VERTICAL,
                    positions = sectionBounds,
                    onMove = { from, to ->
                        onLayoutChange(
                            layout.copy(todaySectionOrder = moveItem(layout.todaySectionOrder, from, to)),
                        )
                    },
                    onToggleVisibility = {
                        onLayoutChange(
                            layout.copy(
                                hiddenTodaySections = toggleId(layout.hiddenTodaySections, sectionId),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    when (sectionId) {
                        DashboardLayoutIds.TODAY_TRAFFIC_ROW -> TrafficTilesRow(
                            state = state,
                            isEditing = isEditing,
                            layout = layout,
                            itemBounds = trafficBounds,
                            onLayoutChange = onLayoutChange,
                        )

                        DashboardLayoutIds.TODAY_TOTAL_ROW -> TodayTotalRow(
                            bytes = state.today.totalBytes,
                            label = stringResource(R.string.label_total),
                            secondaryBytes = state.cycle.totalBytes,
                        )

                        DashboardLayoutIds.TODAY_SPEED_TEST -> SpeedTestPanel(
                            state = state.speedTest,
                            history = state.speedTestHistory,
                            nowMillis = state.now,
                            onStart = if (isEditing) ({}) else onStart,
                            onStop = if (isEditing) ({}) else onStop,
                        )
                    }
                }
                if (index < sectionIds.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun TrafficTilesRow(
    state: UiState,
    isEditing: Boolean,
    layout: DashboardLayout,
    itemBounds: SnapshotStateMap<String, Rect>,
    onLayoutChange: (DashboardLayout) -> Unit,
) {
    val downloadColor = LocalDownloadColor.current
    val uploadColor = LocalUploadColor.current
    val tileIds = if (isEditing) {
        layout.trafficTileOrder
    } else {
        layout.trafficTileOrder.filterNot(layout.hiddenTrafficTiles::contains)
    }

    if (tileIds.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            tileIds.forEach { tileId ->
                EditableDashboardItem(
                    id = tileId,
                    isEditing = isEditing,
                    isHidden = tileId in layout.hiddenTrafficTiles,
                    dragAxis = DragAxis.HORIZONTAL,
                    positions = itemBounds,
                    onMove = { from, to ->
                        onLayoutChange(
                            layout.copy(trafficTileOrder = moveItem(layout.trafficTileOrder, from, to)),
                        )
                    },
                    onToggleVisibility = {
                        onLayoutChange(
                            layout.copy(hiddenTrafficTiles = toggleId(layout.hiddenTrafficTiles, tileId)),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    when (tileId) {
                        DashboardLayoutIds.DOWNLOAD_TILE -> TrafficTile(
                            label = stringResource(R.string.label_download),
                            bytes = state.today.rxBytes,
                            icon = Icons.Rounded.ArrowDownward,
                            accent = downloadColor,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        DashboardLayoutIds.UPLOAD_TILE -> TrafficTile(
                            label = stringResource(R.string.label_upload),
                            bytes = state.today.txBytes,
                            icon = Icons.Rounded.ArrowUpward,
                            accent = uploadColor,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(state: UiState, onResetSession: () -> Unit, isEditing: Boolean) {
    ExpressiveCard(
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 32.dp,
            end = 20.dp,
            bottom = 20.dp,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = stringResource(R.string.label_session),
                supportingText = stringResource(
                    R.string.session_since,
                    Format.clock(state.settings.sessionStartMillis),
                ),
                trailing = {
                    TextButton(
                        onClick = onResetSession,
                        enabled = !isEditing,
                        shape = AppButtonShape,
                    ) {
                        Text(text = stringResource(R.string.action_reset_session))
                    }
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(
                icon = Icons.Rounded.Timer,
                label = stringResource(R.string.label_total),
                value = Format.bytes(state.session.totalBytes),
            )
            InfoRow(
                icon = Icons.Rounded.ShowChart,
                label = stringResource(R.string.label_average),
                value = Format.bytes(state.averagePerDay),
            )
        }
    }
}
