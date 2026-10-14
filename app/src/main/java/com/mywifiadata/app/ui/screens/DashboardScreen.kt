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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.verticalDrag
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.DashboardLayout
import com.mywifiadata.app.data.model.DashboardLayoutIds
import com.mywifiadata.app.data.model.WifiBand
import com.mywifiadata.app.data.model.WifiStatus
import com.mywifiadata.app.ui.UiState
import com.mywifiadata.app.ui.components.AppIcon
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class DashboardDragState {
    val activeLongPressIds = mutableStateMapOf<String, Int>()

    var draggedItemId by mutableStateOf<String?>(null)
        private set
    var draggedPointerRoot by mutableStateOf(Offset.Zero)
        private set
    var draggedAnchorOffset by mutableStateOf(Offset.Zero)
        private set
    var settlingItemId by mutableStateOf<String?>(null)
        private set
    var settlingStartOffset by mutableStateOf(Offset.Zero)
        private set
    var settlingOffset by mutableStateOf(Offset.Zero)
        private set
    var edgeScrollDirection by mutableIntStateOf(0)
        private set

    var viewportBounds: Rect = Rect.Zero
    var edgeScrollZonePx: Float = 0f

    val selectedItemId: String?
        get() = draggedItemId
            ?: settlingItemId
            ?: activeLongPressIds.entries.maxByOrNull { it.value }?.key

    fun beginLongPress(id: String, priority: Int) {
        activeLongPressIds[id] = priority
    }

    fun endLongPress(id: String) {
        activeLongPressIds.remove(id)
    }

    fun startDrag(id: String, pointerRoot: Offset, itemBounds: Rect) {
        settlingItemId = null
        settlingStartOffset = Offset.Zero
        settlingOffset = Offset.Zero
        draggedItemId = id
        draggedPointerRoot = pointerRoot
        draggedAnchorOffset = pointerRoot - Offset(itemBounds.left, itemBounds.top)
        edgeScrollDirection = 0
    }

    fun updateDraggedPointer(id: String, pointerRoot: Offset) {
        if (draggedItemId == id) draggedPointerRoot = pointerRoot
    }

    fun draggedOffsetFor(id: String, itemBounds: Rect, axis: DragAxis): Offset {
        if (draggedItemId != id) return Offset.Zero
        val rawOffset = draggedPointerRoot - draggedAnchorOffset - Offset(itemBounds.left, itemBounds.top)
        return when (axis) {
            DragAxis.HORIZONTAL -> Offset(rawOffset.x, 0f)
            DragAxis.VERTICAL -> Offset(0f, rawOffset.y)
        }
    }

    fun draggedAnchorFor(id: String): Offset =
        if (draggedItemId == id) draggedAnchorOffset else Offset.Zero

    fun updateAutoScroll(pointerY: Float) {
        val bounds = viewportBounds
        edgeScrollDirection = when {
            bounds.height <= 0f || edgeScrollZonePx <= 0f -> 0
            pointerY <= bounds.top + edgeScrollZonePx -> -1
            pointerY >= bounds.bottom - edgeScrollZonePx -> 1
            else -> 0
        }
    }

    fun finishDrag(id: String, finalOffset: Offset) {
        if (draggedItemId != id) return
        draggedItemId = null
        draggedPointerRoot = Offset.Zero
        draggedAnchorOffset = Offset.Zero
        edgeScrollDirection = 0
        if (finalOffset != Offset.Zero) {
            settlingItemId = id
            settlingStartOffset = finalOffset
            settlingOffset = finalOffset
        }
    }

    fun updateSettlingOffset(offset: Offset) {
        settlingOffset = offset
    }

    fun completeSettlement(id: String) {
        if (settlingItemId == id) {
            settlingItemId = null
            settlingStartOffset = Offset.Zero
            settlingOffset = Offset.Zero
        }
    }

    fun disposeItem(id: String) {
        endLongPress(id)
        if (draggedItemId == id) {
            draggedItemId = null
            draggedPointerRoot = Offset.Zero
            draggedAnchorOffset = Offset.Zero
            edgeScrollDirection = 0
        }
        completeSettlement(id)
    }

    fun reset() {
        activeLongPressIds.clear()
        draggedItemId = null
        draggedPointerRoot = Offset.Zero
        draggedAnchorOffset = Offset.Zero
        settlingItemId = null
        settlingStartOffset = Offset.Zero
        settlingOffset = Offset.Zero
        edgeScrollDirection = 0
    }
}

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
    onLoadTodayApps: () -> Unit = {},
    dashboardListState: LazyListState = rememberLazyListState(),
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
    val todayHeaderBounds = remember { mutableStateMapOf<String, Rect>() }
    val trafficTileBounds = remember { mutableStateMapOf<String, Rect>() }
    val dragState = remember { DashboardDragState() }
    val density = LocalDensity.current
    val autoScrollEdgeZonePx = with(density) { 72.dp.toPx() }
    val autoScrollStepPx = with(density) { 9.dp.toPx() }
    val settleX = remember { Animatable(0f) }
    val settleY = remember { Animatable(0f) }
    dragState.edgeScrollZonePx = autoScrollEdgeZonePx

    LaunchedEffect(isEditing) {
        if (!isEditing) dragState.reset()
    }
    LaunchedEffect(dragState.settlingItemId) {
        val settlingId = dragState.settlingItemId ?: return@LaunchedEffect
        val startOffset = dragState.settlingStartOffset
        settleX.snapTo(startOffset.x)
        settleY.snapTo(startOffset.y)
        dragState.updateSettlingOffset(startOffset)
        coroutineScope {
            launch {
                settleX.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                ) {
                    dragState.updateSettlingOffset(Offset(value, settleY.value))
                }
            }
            launch {
                settleY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                ) {
                    dragState.updateSettlingOffset(Offset(settleX.value, value))
                }
            }
        }
        dragState.completeSettlement(settlingId)
    }
    LaunchedEffect(dragState.draggedItemId, dragState.edgeScrollDirection) {
        val draggedId = dragState.draggedItemId ?: return@LaunchedEffect
        val direction = dragState.edgeScrollDirection
        if (direction == 0) return@LaunchedEffect
        while (dragState.draggedItemId == draggedId && dragState.edgeScrollDirection == direction) {
            dashboardListState.scrollBy(direction * autoScrollStepPx)
            delay(16)
        }
    }

    val cardsToShow = layout.cardOrder.filter { cardId ->
        isEditing || cardId !in layout.hiddenCards
    }.filter { cardId ->
        when (cardId) {
            DashboardLayoutIds.QUICK_ACTIONS_CARD -> isEditing ||
                layout.quickActionOrder.any { it !in layout.hiddenQuickActions }

            DashboardLayoutIds.TODAY_CARD -> isEditing ||
                layout.todaySectionOrder.any { sectionId ->
                    sectionId !in layout.hiddenTodaySections &&
                        (sectionId != DashboardLayoutIds.TODAY_TRAFFIC_ROW ||
                            layout.trafficTileOrder.any { it !in layout.hiddenTrafficTiles })
                }

            else -> true
        }
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

        // Preserve normal list scrolling; once a long press is recognized, reserve vertical motion for reordering.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onGloballyPositioned { coordinates ->
                    dragState.viewportBounds = coordinates.boundsInRoot()
                },
            state = dashboardListState,
            userScrollEnabled = !isEditing || dragState.activeLongPressIds.isEmpty(),
            // تُترك مساحة أفقية للبطاقات العادية، بينما تمتد الواجهة المصغّرة حتى حافتي الشاشة.
            contentPadding = PaddingValues(
                top = if (isEditing) 0.dp else contentPadding.calculateTopPadding(),
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
                        if (dragState.draggedItemId == cardId || dragState.settlingItemId == cardId) {
                            Modifier
                        } else {
                            Modifier.animateItem()
                        },
                    )
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
                    dragState = dragState,
                    canStartDragAt = { rootPosition ->
                        cardId != DashboardLayoutIds.TODAY_CARD ||
                            todayHeaderBounds[DashboardLayoutIds.TODAY_CARD]?.contains(rootPosition) == true
                    },
                    showVisibilityButton = cardId !in DashboardLayoutIds.COMPOSITE_CARD_IDS,
                    dragShield = cardId == DashboardLayoutIds.USAGE_CARD ||
                        cardId == DashboardLayoutIds.WIFI_CARD ||
                        cardId == DashboardLayoutIds.SPEED_TEST_CARD ||
                        cardId == DashboardLayoutIds.SESSION_CARD,
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
                            dragState = dragState,
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
                            onLoadTodayApps = onLoadTodayApps,
                            listState = dashboardListState,
                            isEditing = isEditing,
                            layout = layout,
                            cardBounds = cardBounds,
                            sectionBounds = todaySectionBounds,
                            headerBounds = todayHeaderBounds,
                            trafficBounds = trafficTileBounds,
                            onLayoutChange = onDashboardLayoutChange,
                            dragState = dragState,
                        )

                        DashboardLayoutIds.SPEED_TEST_CARD -> SpeedTestPanel(
                            state = state.speedTest,
                            history = state.speedTestHistory,
                            nowMillis = state.now,
                            onStart = if (isEditing) ({}) else onStartSpeedTest,
                            onStop = if (isEditing) ({}) else onStopSpeedTest,
                        )

                        DashboardLayoutIds.SESSION_CARD -> SessionCard(
                            state = state,
                            onResetSession = onResetSession,
                            isEditing = isEditing,
                        )
                    }
                }
            }

            if (isEditing) {
                item(key = "dashboard-reset-layout") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = horizontalStartPadding, end = horizontalEndPadding),
                    ) {
                        FilledTonalButton(
                            onClick = { onDashboardLayoutChange(DashboardLayout.Default) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppButtonShape,
                        ) {
                            Icon(imageVector = Icons.Rounded.RestartAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.dashboard_reset_layout))
                        }
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
            .padding(bottom = 4.dp),
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
        Row(
            modifier = Modifier.absoluteOffset(x = (-8).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
    dragState: DashboardDragState,
    onMove: (String, String) -> Unit,
    onToggleVisibility: () -> Unit,
    dragPriority: Int = 0,
    showVisibilityButton: Boolean = true,
    dragEnabled: Boolean = true,
    dragShield: Boolean = false,
    canStartDragAt: (Offset) -> Boolean = { true },
    onEdgeMoveTarget: (previousCenter: Float, nextCenter: Float) -> String? = { _, _ -> null },
    onEdgeMove: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val latestOnMove by rememberUpdatedState(onMove)
    val latestCanStartDragAt by rememberUpdatedState(canStartDragAt)
    val latestOnEdgeMoveTarget by rememberUpdatedState(onEdgeMoveTarget)
    val latestOnEdgeMove by rememberUpdatedState(onEdgeMove)
    val layoutDirection = LocalLayoutDirection.current
    val badgeAlignment = if (layoutDirection == LayoutDirection.Rtl) {
        Alignment.TopEnd
    } else {
        Alignment.TopStart
    }
    val isSelected = isEditing && dragState.selectedItemId == id
    val isBeingDragged = isEditing && dragState.draggedItemId == id
    val isSettling = isEditing && dragState.settlingItemId == id
    val itemBounds = positions[id]
    // Badges protrude left, so the rightmost sibling must draw above the item beside it.
    val itemDrawOrder = when {
        isSelected || isBeingDragged || isSettling -> 5f
        isEditing && showVisibilityButton ->
            1f + (itemBounds?.left?.coerceAtLeast(0f) ?: 0f) / 1000f
        else -> 0f
    }
    val visualOffset = when {
        isBeingDragged && itemBounds != null -> dragState.draggedOffsetFor(id, itemBounds, dragAxis)
        isSettling -> dragState.settlingOffset
        else -> Offset.Zero
    }
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "dashboard-item-selection-scale",
    )
    val coordinatesRef = remember(id) { arrayOfNulls<LayoutCoordinates>(1) }

    val positionModifier = if (isEditing) {
        Modifier.onGloballyPositioned { coordinates ->
            positions[id] = coordinates.boundsInRoot()
            coordinatesRef[0] = coordinates
        }
    } else {
        Modifier
    }
    val dragModifier = if (isEditing && dragEnabled) {
        Modifier.pointerInput(isEditing, id, dragAxis, dragPriority) {
            var previousTarget: String? = null
            var previousEdgeTarget: String? = null
            var previousPointerRootPosition: Offset? = null

            fun updateAutoScroll(change: PointerInputChange) {
                if (dragAxis == DragAxis.VERTICAL) {
                    coordinatesRef[0]?.localToRoot(change.position)?.y?.let(dragState::updateAutoScroll)
                }
            }

            fun finishDrag() {
                val finalOffset = positions[id]?.let { dragState.draggedOffsetFor(id, it, dragAxis) } ?: Offset.Zero
                dragState.finishDrag(id, finalOffset)
            }

            fun handleDrag(change: PointerInputChange, amount: Float) {
                change.consume()
                val previousRootPosition = previousPointerRootPosition
                val currentRootPosition = coordinatesRef[0]?.localToRoot(change.position)
                val pointerDelta = if (currentRootPosition != null && previousRootPosition != null) {
                    axisOffset(currentRootPosition - previousRootPosition, dragAxis)
                } else {
                    amount
                }
                val effectiveCurrentRootPosition = currentRootPosition
                    ?: previousRootPosition?.plus(axisVector(dragAxis, pointerDelta))
                if (effectiveCurrentRootPosition != null) {
                    previousPointerRootPosition = effectiveCurrentRootPosition
                    dragState.updateDraggedPointer(id, effectiveCurrentRootPosition)
                }

                val ownBounds = positions[id]
                if (ownBounds == null || effectiveCurrentRootPosition == null) {
                    updateAutoScroll(change)
                    return
                }

                val anchor = dragState.draggedAnchorFor(id)
                val itemCenterOffset = axisExtent(ownBounds, dragAxis) / 2f - axisOffset(anchor, dragAxis)
                val previousCenter = axisPosition(previousRootPosition ?: effectiveCurrentRootPosition, dragAxis) +
                    itemCenterOffset
                val nextCenter = axisPosition(effectiveCurrentRootPosition, dragAxis) + itemCenterOffset

                val edgeTarget = latestOnEdgeMoveTarget(previousCenter, nextCenter)
                if (edgeTarget != null && edgeTarget != previousEdgeTarget) {
                    latestOnEdgeMove(edgeTarget)
                    previousEdgeTarget = edgeTarget
                } else if (edgeTarget == null) {
                    previousEdgeTarget = null
                }

                val crossedTarget = positions.entries
                    .asSequence()
                    .filter { (targetId, bounds) ->
                        targetId != id && bounds.width > 0f && bounds.height > 0f &&
                            (
                                if (pointerDelta > 0f) {
                                    previousCenter <= axisCenter(bounds, dragAxis) &&
                                        nextCenter >= axisCenter(bounds, dragAxis)
                                } else if (pointerDelta < 0f) {
                                    previousCenter >= axisCenter(bounds, dragAxis) &&
                                        nextCenter <= axisCenter(bounds, dragAxis)
                                } else {
                                    false
                                }
                            )
                    }
                    .minByOrNull { (_, bounds) -> abs(nextCenter - axisCenter(bounds, dragAxis)) }

                if (crossedTarget != null && crossedTarget.key != previousTarget) {
                    latestOnMove(id, crossedTarget.key)
                    previousTarget = crossedTarget.key
                } else if (crossedTarget == null) {
                    previousTarget = null
                }

                updateAutoScroll(change)
            }

            detectAxisDragGesturesAfterLongPress(
                axis = dragAxis,
                canStartDrag = { localPosition ->
                    val rootPosition = coordinatesRef[0]?.localToRoot(localPosition) ?: localPosition
                    latestCanStartDragAt(rootPosition)
                },
                onLongPress = { dragState.beginLongPress(id, dragPriority) },
                onDragStart = { pressPosition ->
                    previousTarget = null
                    previousEdgeTarget = null
                    val pointerRootPosition = coordinatesRef[0]?.localToRoot(pressPosition) ?: pressPosition
                    previousPointerRootPosition = pointerRootPosition
                    dragState.startDrag(id, pointerRootPosition, positions[id] ?: Rect.Zero)
                },
                onDrag = ::handleDrag,
                onDragEnd = ::finishDrag,
                onDragCancel = ::finishDrag,
                onLongPressEnd = { dragState.endLongPress(id) },
            )
        }
    } else {
        Modifier
    }
    val dragShieldModifier = if (isEditing && dragShield) {
        Modifier.pointerInput(isEditing, id) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                awaitLongPressOrCancellation(down.id)
            }
        }
    } else {
        Modifier
    }

    DisposableEffect(isEditing, id, positions, dragState) {
        onDispose {
            positions.remove(id)
            dragState.disposeItem(id)
        }
    }

    Box(
        modifier = modifier
            .then(positionModifier)
            .then(if (isEditing && dragEnabled) dragModifier else Modifier)
            .zIndex(itemDrawOrder),
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                translationX = visualOffset.x
                translationY = visualOffset.y
                scaleX = selectionScale
                scaleY = selectionScale
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = if (isEditing && isHidden) 0.42f else 1f },
            ) {
                content()
            }

            if (isEditing && dragShield) {
                // This transparent overlay blocks nested clickables while the outer item handles the drag.
                Box(modifier = Modifier.matchParentSize().then(dragShieldModifier))
            }

            if (isEditing && showVisibilityButton) {
                Box(
                    modifier = Modifier
                        .align(badgeAlignment)
                        .absoluteOffset(x = (-16).dp, y = (-8).dp)
                        .size(32.dp)
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
}

@Composable
private fun rememberReorderPlacementModifier(id: String, enabled: Boolean): Modifier {
    val animatedX = remember(id) { Animatable(0f) }
    val animatedY = remember(id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var previousPosition by remember(id) { mutableStateOf<Offset?>(null) }

    return Modifier
        .onGloballyPositioned { coordinates ->
            val currentPosition = coordinates.positionInParent()
            val previous = previousPosition
            if (enabled && previous != null && previous != currentPosition) {
                val displacement = previous - currentPosition
                scope.launch {
                    animatedX.snapTo(displacement.x)
                    animatedY.snapTo(displacement.y)
                    coroutineScope {
                        launch {
                            animatedX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.86f,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                        launch {
                            animatedY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.86f,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                    }
                }
            }
            previousPosition = currentPosition
        }
        .then(
            if (enabled) {
                Modifier.graphicsLayer {
                    translationX = animatedX.value
                    translationY = animatedY.value
                }
            } else {
                Modifier
            },
        )
}

@OptIn(ExperimentalFoundationApi::class)
private suspend fun PointerInputScope.detectAxisDragGesturesAfterLongPress(
    axis: DragAxis,
    canStartDrag: (Offset) -> Boolean = { true },
    onLongPress: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (PointerInputChange, Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onLongPressEnd: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!canStartDrag(down.position)) return@awaitEachGesture
        var longPressRecognized = false
        var dragStarted = false
        var dragFinished = false
        try {
            val longPress = awaitLongPressOrCancellation(down.id)
            if (longPress != null) {
                longPressRecognized = true
                onLongPress()
                var overSlop = 0f
                val drag = when (axis) {
                    DragAxis.HORIZONTAL -> awaitHorizontalTouchSlopOrCancellation(
                        longPress.id,
                    ) { change, amount ->
                        change.consume()
                        overSlop = amount
                    }

                    DragAxis.VERTICAL -> awaitVerticalTouchSlopOrCancellation(
                        longPress.id,
                    ) { change, amount ->
                        change.consume()
                        overSlop = amount
                    }
                }

                if (drag == null) {
                    onDragCancel()
                    dragFinished = true
                } else {
                    dragStarted = true
                    onDragStart(longPress.position)
                    onDrag(drag, overSlop)
                    val completed = when (axis) {
                        DragAxis.HORIZONTAL -> horizontalDrag(drag.id) { change ->
                            onDrag(change, change.positionChange().x)
                            change.consume()
                        }

                        DragAxis.VERTICAL -> verticalDrag(drag.id) { change ->
                            onDrag(change, change.positionChange().y)
                            change.consume()
                        }
                    }
                    if (completed) onDragEnd() else onDragCancel()
                    dragFinished = true
                }
            }
        } finally {
            if (dragStarted && !dragFinished) onDragCancel()
            if (longPressRecognized) onLongPressEnd()
        }
    }
}

private fun axisCenter(rect: Rect, axis: DragAxis): Float = when (axis) {
    DragAxis.HORIZONTAL -> rect.center.x
    DragAxis.VERTICAL -> rect.center.y
}

private fun axisOffset(offset: Offset, axis: DragAxis): Float = when (axis) {
    DragAxis.HORIZONTAL -> offset.x
    DragAxis.VERTICAL -> offset.y
}

private fun axisPosition(offset: Offset, axis: DragAxis): Float = when (axis) {
    DragAxis.HORIZONTAL -> offset.x
    DragAxis.VERTICAL -> offset.y
}

private fun axisExtent(rect: Rect, axis: DragAxis): Float = when (axis) {
    DragAxis.HORIZONTAL -> rect.width
    DragAxis.VERTICAL -> rect.height
}

private fun axisVector(axis: DragAxis, amount: Float): Offset = when (axis) {
    DragAxis.HORIZONTAL -> Offset(amount, 0f)
    DragAxis.VERTICAL -> Offset(0f, amount)
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
    dragState: DashboardDragState,
) {
    val actionIds = if (isEditing) {
        layout.quickActionOrder
    } else {
        layout.quickActionOrder.filterNot(layout.hiddenQuickActions::contains)
    }
    if (actionIds.isNotEmpty()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val itemSpacing = 10.dp
            val itemWidth = (maxWidth - itemSpacing * (actionIds.size - 1).toFloat()) /
                actionIds.size.toFloat()
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            ) {
                items(items = actionIds, key = { it }) { actionId ->
                    val placementModifier = if (
                        dragState.draggedItemId == actionId || dragState.settlingItemId == actionId
                    ) {
                        Modifier
                    } else {
                        Modifier.animateItem()
                    }
                    EditableDashboardItem(
                        id = actionId,
                        isEditing = isEditing,
                        isHidden = actionId in layout.hiddenQuickActions,
                        dragAxis = DragAxis.HORIZONTAL,
                        positions = itemBounds,
                        dragState = dragState,
                        dragPriority = 1,
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
                        dragShield = true,
                        modifier = Modifier
                            .width(itemWidth)
                            .then(placementModifier)
                            .heightIn(min = 84.dp),
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
    isEditing: Boolean,
    onLoadTodayApps: () -> Unit,
    listState: LazyListState,
    layout: DashboardLayout,
    cardBounds: SnapshotStateMap<String, Rect>,
    sectionBounds: SnapshotStateMap<String, Rect>,
    headerBounds: SnapshotStateMap<String, Rect>,
    trafficBounds: SnapshotStateMap<String, Rect>,
    onLayoutChange: (DashboardLayout) -> Unit,
    dragState: DashboardDragState,
) {
    val sectionIds = layout.todaySectionOrder.filter { sectionId ->
        isEditing || sectionId !in layout.hiddenTodaySections
    }.filter { sectionId ->
        sectionId != DashboardLayoutIds.TODAY_TRAFFIC_ROW || isEditing ||
            layout.trafficTileOrder.any { it !in layout.hiddenTrafficTiles }
    }
    var expandedMetric by rememberSaveable { mutableStateOf<TodayUsageMetric?>(null) }
    var expandsAbove by rememberSaveable { mutableStateOf(false) }
    var pendingMetricAnchor by remember { mutableStateOf<Pair<TodayUsageMetric, Float>?>(null) }
    var expandedMetricAnchor by remember { mutableStateOf<Pair<TodayUsageMetric, Float>?>(null) }
    var isCompensatingMetricScroll by remember { mutableStateOf(false) }
    val estimatedAppsPanelHeightPx = with(LocalDensity.current) { TODAY_APPS_PANEL_ESTIMATED_HEIGHT.toPx() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isEditing) {
        if (isEditing) {
            expandedMetric = null
            pendingMetricAnchor = null
            expandedMetricAnchor = null
        }
    }

    LaunchedEffect(expandedMetric, expandsAbove, state.isTodayAppsLoading) {
        val anchorMetric = expandedMetric
        if (anchorMetric != null && expandsAbove && !state.isTodayAppsLoading) {
            delay(250)
            if (expandedMetric == anchorMetric && expandsAbove && !state.isTodayAppsLoading) {
                expandedMetricAnchor = null
            }
        }
    }

    fun compensateMetricScroll(scrollDelta: Float) {
        if (abs(scrollDelta) <= 1f) return
        scope.launch {
            isCompensatingMetricScroll = true
            try {
                listState.scrollBy(scrollDelta)
            } finally {
                isCompensatingMetricScroll = false
            }
        }
    }

    fun updateMetricPosition(metric: TodayUsageMetric, bounds: Rect) {
        if (listState.isScrollInProgress && !isCompensatingMetricScroll) {
            expandedMetricAnchor = null
            pendingMetricAnchor = null
            return
        }

        val expandedAnchor = expandedMetricAnchor
        if (expandedAnchor?.first == metric && expandedMetric == metric && expandsAbove) {
            val scrollDelta = bounds.top - expandedAnchor.second
            compensateMetricScroll(scrollDelta)
            if (!state.isTodayAppsLoading) expandedMetricAnchor = null
            return
        }

        val pendingAnchor = pendingMetricAnchor ?: return
        if (pendingAnchor.first != metric) return
        pendingMetricAnchor = null
        val scrollDelta = bounds.top - pendingAnchor.second
        compensateMetricScroll(scrollDelta)
    }

    fun toggleMetric(metric: TodayUsageMetric, metricBounds: Rect) {
        if (expandedMetric == metric) {
            expandedMetricAnchor = null
            pendingMetricAnchor = if (expandsAbove) metric to metricBounds.top else null
            expandedMetric = null
            return
        }

        val previousExpandedMetric = expandedMetric
        val viewport = dragState.viewportBounds
        val availableAbove = (metricBounds.top - viewport.top).coerceAtLeast(0f)
        val availableBelow = (viewport.bottom - metricBounds.bottom).coerceAtLeast(0f)
        val shouldExpandAbove = when {
            metricBounds.height <= 0f || viewport.height <= 0f -> false
            availableBelow >= estimatedAppsPanelHeightPx -> false
            availableAbove >= estimatedAppsPanelHeightPx -> true
            else -> availableAbove > availableBelow
        }
        expandsAbove = shouldExpandAbove
        expandedMetricAnchor = if (shouldExpandAbove) metric to metricBounds.top else null
        pendingMetricAnchor = if (!shouldExpandAbove && previousExpandedMetric != null) {
            metric to metricBounds.top
        } else {
            null
        }
        expandedMetric = metric
        onLoadTodayApps()
    }

    ExpressiveCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = stringResource(R.string.label_today),
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    headerBounds[DashboardLayoutIds.TODAY_CARD] = coordinates.boundsInRoot()
                },
                supportingText = Format.fullDate(state.now),
            )
            if (sectionIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
            }
            sectionIds.forEachIndexed { index, sectionId ->
                key(sectionId) {
                    val placementModifier = rememberReorderPlacementModifier(
                        id = sectionId,
                        enabled = isEditing && dragState.draggedItemId != sectionId &&
                            dragState.settlingItemId != sectionId,
                    )
                    EditableDashboardItem(
                        id = sectionId,
                        isEditing = isEditing,
                        isHidden = sectionId in layout.hiddenTodaySections,
                        dragAxis = DragAxis.VERTICAL,
                        positions = sectionBounds,
                        dragState = dragState,
                        dragPriority = 1,
                        onMove = { from, to ->
                            onLayoutChange(
                                layout.copy(todaySectionOrder = moveItem(layout.todaySectionOrder, from, to)),
                            )
                        },
                        onEdgeMoveTarget = { previousCenter, nextCenter ->
                            // Crossing the Today card's own edge moves the group, while interior drags reorder sections.
                            val direction = when {
                                nextCenter < previousCenter -> -1
                                nextCenter > previousCenter -> 1
                                else -> 0
                            }
                            val sectionIndex = layout.todaySectionOrder.indexOf(sectionId)
                            val todayBounds = cardBounds[DashboardLayoutIds.TODAY_CARD]
                            val sectionBoundsForMove = sectionBounds[sectionId]
                            val sectionHalfHeight = (sectionBoundsForMove?.height ?: 0f) / 2f
                            val crossedCardEdge = todayBounds != null && sectionBoundsForMove != null &&
                                when (direction) {
                                    -1 -> previousCenter - sectionHalfHeight >= todayBounds.top &&
                                        nextCenter - sectionHalfHeight < todayBounds.top

                                    1 -> previousCenter + sectionHalfHeight <= todayBounds.bottom &&
                                        nextCenter + sectionHalfHeight > todayBounds.bottom

                                    else -> false
                                }
                            val isBoundarySection = (direction < 0 && sectionIndex == 0) ||
                                (direction > 0 && sectionIndex == layout.todaySectionOrder.lastIndex)
                            val neighborIndex = when {
                                direction < 0 -> layout.cardOrder.indexOf(DashboardLayoutIds.TODAY_CARD) - 1
                                direction > 0 -> layout.cardOrder.indexOf(DashboardLayoutIds.TODAY_CARD) + 1
                                else -> -1
                            }
                            val neighborCardId = layout.cardOrder.getOrNull(neighborIndex)
                            val neighborBounds = neighborCardId?.let { cardBounds[it] }
                            if (
                                !isEditing || !isBoundarySection || !crossedCardEdge ||
                                neighborCardId == null || neighborBounds == null || neighborBounds.height <= 0f
                            ) {
                                null
                            } else {
                                neighborCardId
                            }
                        },
                        onEdgeMove = { targetCardId ->
                            onLayoutChange(
                                layout.copy(
                                    cardOrder = moveItem(
                                        layout.cardOrder,
                                        DashboardLayoutIds.TODAY_CARD,
                                        targetCardId,
                                    ),
                                ),
                            )
                        },
                        onToggleVisibility = {
                            onLayoutChange(
                                layout.copy(
                                    hiddenTodaySections = toggleId(layout.hiddenTodaySections, sectionId),
                                ),
                            )
                        },
                        showVisibilityButton = sectionId != DashboardLayoutIds.TODAY_TRAFFIC_ROW,
                        dragEnabled = true,
                        dragShield = sectionId == DashboardLayoutIds.TODAY_TOTAL_ROW,
                        modifier = placementModifier.fillMaxWidth(),
                    ) {
                        when (sectionId) {
                            DashboardLayoutIds.TODAY_TRAFFIC_ROW -> TrafficTilesRow(
                                state = state,
                                isEditing = isEditing,
                                layout = layout,
                                itemBounds = trafficBounds,
                                onLayoutChange = onLayoutChange,
                                dragState = dragState,
                                expandedMetric = expandedMetric,
                                expandsAbove = expandsAbove,
                                onMetricClick = ::toggleMetric,
                                onMetricBoundsChanged = ::updateMetricPosition,
                            )

                            DashboardLayoutIds.TODAY_TOTAL_ROW -> ExpandableTodayMetric(
                                metric = TodayUsageMetric.TOTAL,
                                expandedMetric = expandedMetric,
                                expandsAbove = expandsAbove,
                                apps = state.todayApps,
                                isAppsLoading = state.isTodayAppsLoading,
                                hasUsageAccess = state.hasUsageAccess,
                                isEditing = isEditing,
                                onMetricClick = ::toggleMetric,
                                onMetricBoundsChanged = ::updateMetricPosition,
                            ) { metricModifier ->
                                TodayTotalRow(
                                    bytes = state.today.totalBytes,
                                    label = stringResource(R.string.label_total),
                                    secondaryBytes = state.cycle.totalBytes,
                                    modifier = metricModifier,
                                )
                            }
                        }
                    }
                }
                if (index < sectionIds.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

private enum class TodayUsageMetric(val labelRes: Int) {
    DOWNLOAD(R.string.label_download) {
        override fun bytes(app: AppUsage): Long = app.totals.rxBytes
    },
    UPLOAD(R.string.label_upload) {
        override fun bytes(app: AppUsage): Long = app.totals.txBytes
    },
    TOTAL(R.string.label_total) {
        override fun bytes(app: AppUsage): Long = app.totals.totalBytes
    };

    abstract fun bytes(app: AppUsage): Long
}

private val TODAY_APPS_PANEL_ESTIMATED_HEIGHT = 360.dp

@Composable
private fun ExpandableTodayMetric(
    metric: TodayUsageMetric,
    expandedMetric: TodayUsageMetric?,
    expandsAbove: Boolean,
    apps: List<AppUsage>,
    isAppsLoading: Boolean,
    hasUsageAccess: Boolean,
    isEditing: Boolean,
    onMetricClick: (TodayUsageMetric, Rect) -> Unit,
    onMetricBoundsChanged: (TodayUsageMetric, Rect) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val isExpanded = !isEditing && expandedMetric == metric
    var metricBounds by remember(metric) { mutableStateOf(Rect.Zero) }
    val metricContentModifier = Modifier
        .fillMaxWidth()
        .onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInRoot()
            metricBounds = bounds
            onMetricBoundsChanged(metric, bounds)
        }
        .then(
            if (isEditing) Modifier else Modifier.clickable {
                onMetricClick(metric, metricBounds)
            },
        )

    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = isExpanded && expandsAbove,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column {
                TodayTopAppsPanel(
                    metric = metric,
                    apps = apps,
                    isAppsLoading = isAppsLoading,
                    hasUsageAccess = hasUsageAccess,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        content(metricContentModifier)

        AnimatedVisibility(
            visible = isExpanded && !expandsAbove,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                TodayTopAppsPanel(
                    metric = metric,
                    apps = apps,
                    isAppsLoading = isAppsLoading,
                    hasUsageAccess = hasUsageAccess,
                )
            }
        }
    }
}

@Composable
private fun TodayTopAppsPanel(
    metric: TodayUsageMetric,
    apps: List<AppUsage>,
    isAppsLoading: Boolean,
    hasUsageAccess: Boolean,
) {
    val topApps = remember(metric, apps) {
        apps.asSequence()
            .filter { metric.bytes(it) > 0L }
            .sortedByDescending { metric.bytes(it) }
            .take(5)
            .toList()
    }
    val valueColor = when (metric) {
        TodayUsageMetric.DOWNLOAD -> LocalDownloadColor.current
        TodayUsageMetric.UPLOAD -> LocalUploadColor.current
        TodayUsageMetric.TOTAL -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = stringResource(R.string.dashboard_today_top_apps_title, stringResource(metric.labelRes)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))

            when {
                !hasUsageAccess -> Text(
                    text = stringResource(R.string.permission_needed_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                topApps.isEmpty() && isAppsLoading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.dashboard_today_apps_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                topApps.isEmpty() -> Text(
                    text = stringResource(R.string.dashboard_today_apps_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> {
                    if (isAppsLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isCompactLayout = maxWidth < 260.dp
                        Column(modifier = Modifier.fillMaxWidth()) {
                            topApps.forEachIndexed { index, app ->
                                TodayTopAppItem(
                                    index = index,
                                    app = app,
                                    metric = metric,
                                    valueColor = valueColor,
                                    compact = isCompactLayout,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayTopAppItem(
    index: Int,
    app: AppUsage,
    metric: TodayUsageMetric,
    valueColor: Color,
    compact: Boolean,
) {
    val value = Format.bytes(metric.bytes(app))
    if (compact) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = (index + 1).toString(),
                    modifier = Modifier.width(22.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (app.packageName != null) {
                    AppIcon(packageName = app.packageName, size = 30.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = app.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = value,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = valueColor,
                maxLines = 1,
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = (index + 1).toString(),
                modifier = Modifier.width(22.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (app.packageName != null) {
                AppIcon(packageName = app.packageName, size = 34.dp)
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = app.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = valueColor,
                maxLines = 1,
            )
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
    dragState: DashboardDragState,
    expandedMetric: TodayUsageMetric?,
    expandsAbove: Boolean,
    onMetricClick: (TodayUsageMetric, Rect) -> Unit,
    onMetricBoundsChanged: (TodayUsageMetric, Rect) -> Unit,
) {
    val downloadColor = LocalDownloadColor.current
    val uploadColor = LocalUploadColor.current
    val tileIds = if (isEditing) {
        layout.trafficTileOrder
    } else {
        layout.trafficTileOrder.filterNot(layout.hiddenTrafficTiles::contains)
    }
    if (tileIds.isNotEmpty()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tileSpacing = 12.dp
            val tileWidth = (maxWidth - tileSpacing * (tileIds.size - 1).toFloat()) /
                tileIds.size.toFloat()
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(tileSpacing),
            ) {
                items(items = tileIds, key = { it }) { tileId ->
                    val placementModifier = if (
                        dragState.draggedItemId == tileId || dragState.settlingItemId == tileId
                    ) {
                        Modifier
                    } else {
                        Modifier.animateItem()
                    }
                    EditableDashboardItem(
                        id = tileId,
                        isEditing = isEditing,
                        isHidden = tileId in layout.hiddenTrafficTiles,
                        dragAxis = DragAxis.HORIZONTAL,
                        positions = itemBounds,
                        dragState = dragState,
                        dragPriority = 2,
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
                        dragShield = true,
                        modifier = Modifier
                            .width(tileWidth)
                            .then(placementModifier),
                    ) {
                        when (tileId) {
                            DashboardLayoutIds.DOWNLOAD_TILE -> ExpandableTodayMetric(
                                metric = TodayUsageMetric.DOWNLOAD,
                                expandedMetric = expandedMetric,
                                expandsAbove = expandsAbove,
                                apps = state.todayApps,
                                isAppsLoading = state.isTodayAppsLoading,
                                hasUsageAccess = state.hasUsageAccess,
                                isEditing = isEditing,
                                onMetricClick = onMetricClick,
                                onMetricBoundsChanged = onMetricBoundsChanged,
                            ) { metricModifier ->
                                TrafficTile(
                                    label = stringResource(R.string.label_download),
                                    bytes = state.today.rxBytes,
                                    icon = Icons.Rounded.ArrowDownward,
                                    accent = downloadColor,
                                    modifier = metricModifier.fillMaxWidth(),
                                )
                            }

                            DashboardLayoutIds.UPLOAD_TILE -> ExpandableTodayMetric(
                                metric = TodayUsageMetric.UPLOAD,
                                expandedMetric = expandedMetric,
                                expandsAbove = expandsAbove,
                                apps = state.todayApps,
                                isAppsLoading = state.isTodayAppsLoading,
                                hasUsageAccess = state.hasUsageAccess,
                                isEditing = isEditing,
                                onMetricClick = onMetricClick,
                                onMetricBoundsChanged = onMetricBoundsChanged,
                            ) { metricModifier ->
                                TrafficTile(
                                    label = stringResource(R.string.label_upload),
                                    bytes = state.today.txBytes,
                                    icon = Icons.Rounded.ArrowUpward,
                                    accent = uploadColor,
                                    modifier = metricModifier.fillMaxWidth(),
                                )
                            }
                        }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_session),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            R.string.session_since,
                            Format.clock(state.settings.sessionStartMillis),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(
                    onClick = onResetSession,
                    enabled = !isEditing,
                    shape = AppButtonShape,
                    modifier = Modifier.absoluteOffset(x = (-8).dp),
                ) {
                    Text(text = stringResource(R.string.action_reset_session))
                }
            }
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
