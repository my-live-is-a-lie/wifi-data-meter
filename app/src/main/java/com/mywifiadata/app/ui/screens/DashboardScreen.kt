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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
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
    onAddUsageWidget: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    var isHeroCompact by rememberSaveable { mutableStateOf(false) }
    var selectedCardStyle by rememberSaveable { mutableIntStateOf(0) }
    val layoutDirection = LocalLayoutDirection.current
    val horizontalStartPadding = contentPadding.calculateStartPadding(layoutDirection)
    val horizontalEndPadding = contentPadding.calculateEndPadding(layoutDirection)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        // تُترك مساحة أفقية للبطاقات العادية، بينما تمتد الواجهة المصغّرة حتى حافتي الشاشة.
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroCard(
                state = state,
                isCompact = isHeroCompact,
                selectedStyleIndex = selectedCardStyle,
                onOpenQuotaSettings = onOpenQuotaSettings,
                horizontalStartPadding = horizontalStartPadding,
                horizontalEndPadding = horizontalEndPadding,
                onLongPress = { isHeroCompact = true },
                onRestore = { isHeroCompact = false },
                onChangeStyle = { direction ->
                    val styleCount = UsageCardStyle.entries.size
                    selectedCardStyle = (selectedCardStyle + direction + styleCount) % styleCount
                },
                onAddWidget = onAddUsageWidget,
            )
        }

        if (!state.hasUsageAccess) {
            item {
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

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = horizontalStartPadding, end = horizontalEndPadding),
            ) {
                QuickActions(
                    onRefresh = onRefresh,
                    onResetSession = onResetSession,
                    onOpenQuotaSettings = onOpenQuotaSettings,
                    trackingEnabled = state.settings.trackingEnabled,
                    onToggleTracking = onToggleTracking,
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = horizontalStartPadding, end = horizontalEndPadding),
            ) {
                if (state.wifi.isWifiConnected) {
                    WifiDetailsCard(wifi = state.wifi)
                } else {
                    NotConnectedCard(isOnline = state.wifi.isOnline)
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = horizontalStartPadding, end = horizontalEndPadding),
            ) {
                TodayCard(
                    state = state,
                    onStart = onStartSpeedTest,
                    onStop = onStopSpeedTest,
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = horizontalStartPadding, end = horizontalEndPadding),
            ) {
                SessionCard(state = state, onResetSession = onResetSession)
            }
        }
    }
}

private enum class UsageCardStyle {
    RING,
    DOTTED_RING,
    SPLIT,
}

@Composable
private fun HeroCard(
    state: UiState,
    isCompact: Boolean,
    selectedStyleIndex: Int,
    onOpenQuotaSettings: () -> Unit,
    horizontalStartPadding: androidx.compose.ui.unit.Dp,
    horizontalEndPadding: androidx.compose.ui.unit.Dp,
    onLongPress: () -> Unit,
    onRestore: () -> Unit,
    onChangeStyle: (Int) -> Unit,
    onAddWidget: () -> Unit,
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
                    onRestore = onRestore,
                    onChangeStyle = onChangeStyle,
                )
            } else {
                FullUsageCard(
                    state = state,
                    selectedStyleIndex = selectedStyleIndex,
                    horizontalStartPadding = horizontalStartPadding,
                    horizontalEndPadding = horizontalEndPadding,
                    onOpenQuotaSettings = onOpenQuotaSettings,
                    onLongPress = onLongPress,
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
                    onClick = onAddWidget,
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
    horizontalStartPadding: androidx.compose.ui.unit.Dp,
    horizontalEndPadding: androidx.compose.ui.unit.Dp,
    onOpenQuotaSettings: () -> Unit,
    onLongPress: () -> Unit,
) {
    val hasQuota = state.hasQuota
    val progress = usageCardProgress(state)
    val downloadColor = LocalDownloadColor.current
    val uploadColor = LocalUploadColor.current
    val usageValue = if (hasQuota) {
        stringResource(R.string.percent_format, (state.quotaProgress * 100).roundToInt())
    } else {
        Format.bytes(state.today.totalBytes)
    }
    val usageLabel = stringResource(if (hasQuota) R.string.label_used else R.string.label_today)
    val usedValue = Format.bytes(state.cycle.totalBytes)
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
        modifier = Modifier.padding(start = horizontalStartPadding, end = horizontalEndPadding),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        onLongClick = onLongPress,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (selectedStyleIndex == UsageCardStyle.DOTTED_RING.ordinal) {
                DottedUsageGauge(
                    progress = progress,
                    modifier = Modifier.size(width = 280.dp, height = 234.dp),
                    strokeWidth = 14.dp,
                    dotRadius = 3.dp,
                    dotCount = 46,
                    dotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                    progressColor = gaugeStartColor,
                    contentDescription = stringResource(R.string.quota_ring_title),
                ) {
                    FullGaugeCenter(usageValue, usageLabel, usedValue, dotted = true)
                }
            } else {
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
                    value = Format.bytes(state.today.rxBytes),
                )
                LegendItem(
                    color = uploadColor,
                    label = stringResource(R.string.label_upload),
                    value = Format.bytes(state.today.txBytes),
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
    dotted: Boolean = false,
) {
    Column(
        modifier = if (dotted) Modifier.offset(y = (-44).dp) else Modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!dotted) Spacer(modifier = Modifier.height(24.dp))
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
            .height(260.dp)
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
        val cardWidth = minOf(maxWidth * 0.60f, 244.dp)
        val cardSpacing = maxWidth * 0.10f
        val step = cardWidth + cardSpacing

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

            ExpressiveCard(
                modifier = Modifier
                    .width(cardWidth)
                    .height(244.dp)
                    .align(Alignment.Center)
                    .absoluteOffset(x = animatedOffset)
                    .graphicsLayer {
                        translationX = dragOffset.value * 0.55f
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = animatedAlpha
                    }
                    .zIndex(if (slot == 0) 1f else 0f),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                onClick = onRestore,
                contentPadding = PaddingValues(14.dp),
            ) {
                CompactUsageCardContent(state = state, style = style)
            }
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

@Composable
private fun CompactUsageCardContent(state: UiState, style: UsageCardStyle) {
    val progress = usageCardProgress(state)
    val downloadColor = LocalDownloadColor.current
    val uploadColor = LocalUploadColor.current
    val usageValue = if (state.hasQuota) {
        stringResource(R.string.percent_format, (state.quotaProgress * 100).roundToInt())
    } else {
        Format.bytesShort(state.today.totalBytes)
    }
    val usedBytes = if (state.hasQuota) state.cycle.totalBytes else state.today.totalBytes
    val usedLabel = stringResource(if (state.hasQuota) R.string.label_used else R.string.label_today)
    val remainingText = if (state.hasQuota) {
        stringResource(
            R.string.quota_remaining_of,
            Format.bytes(state.remainingBytes),
            Format.bytes(state.quotaBytes),
        )
    } else {
        stringResource(R.string.quota_not_set)
    }

    when (style) {
        UsageCardStyle.RING, UsageCardStyle.DOTTED_RING -> {
            CompactCircularUsageCardContent(
                state = state,
                progress = progress,
                usageValue = usageValue,
                usedBytes = usedBytes,
                usedLabel = usedLabel,
                remainingText = remainingText,
                downloadColor = downloadColor,
                uploadColor = uploadColor,
                dotted = style == UsageCardStyle.DOTTED_RING,
            )
        }

        UsageCardStyle.SPLIT -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UsageRing(
                        progress = progress,
                        modifier = Modifier.size(94.dp),
                        strokeWidth = 10.dp,
                        startColor = if (state.quotaExceeded) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        endColor = if (state.quotaExceeded) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                        contentDescription = stringResource(R.string.quota_ring_title),
                    ) {
                        Text(
                            text = usageValue,
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = usedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = Format.bytes(usedBytes),
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = remainingText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                CompactTrafficSummary(state, downloadColor, uploadColor)
            }
        }
    }
}

@Composable
private fun CompactCircularUsageCardContent(
    state: UiState,
    progress: Float,
    usageValue: String,
    usedBytes: Long,
    usedLabel: String,
    remainingText: String,
    downloadColor: Color,
    uploadColor: Color,
    dotted: Boolean,
) {
    val startColor = if (state.quotaExceeded) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val endColor = if (state.quotaExceeded) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (dotted) {
            BoxWithConstraints(contentAlignment = Alignment.Center) {
                val gaugeWidth = minOf(maxWidth, 188.dp)
                val gaugeHeight = gaugeWidth * (158f / 188f)
                DottedUsageGauge(
                    progress = progress,
                    modifier = Modifier.size(width = gaugeWidth, height = gaugeHeight),
                    strokeWidth = 9.dp,
                    dotRadius = 2.dp,
                    dotCount = 31,
                    dotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                    progressColor = startColor,
                    contentDescription = stringResource(R.string.quota_ring_title),
                ) {
                    Box(modifier = Modifier.offset(y = (-30).dp)) {
                        CompactGaugeCenter(usageValue, usedLabel, usedBytes)
                    }
                }
            }
        } else {
            UsageRing(
                progress = progress,
                modifier = Modifier.size(148.dp),
                strokeWidth = 14.dp,
                startColor = startColor,
                endColor = endColor,
                contentDescription = stringResource(R.string.quota_ring_title),
            ) {
                CompactGaugeCenter(usageValue, usedLabel, usedBytes)
            }
        }

        if (dotted) {
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.height(7.dp))
        }
        Text(
            text = remainingText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(7.dp))
        CompactTrafficSummary(state, downloadColor, uploadColor)
    }
}

@Composable
private fun CompactGaugeCenter(usageValue: String, usedLabel: String, usedBytes: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = usageValue,
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = usedLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = Format.bytes(usedBytes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun CompactTrafficSummary(
    state: UiState,
    downloadColor: Color,
    uploadColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactTrafficItem(
            label = stringResource(R.string.label_download),
            value = Format.bytes(state.today.rxBytes),
            color = downloadColor,
        )
        CompactTrafficItem(
            label = stringResource(R.string.label_upload),
            value = Format.bytes(state.today.txBytes),
            color = uploadColor,
        )
    }
}

@Composable
private fun CompactTrafficItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color = color, size = 6.dp)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    onOpenQuotaSettings: () -> Unit,
    trackingEnabled: Boolean,
    onToggleTracking: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ارتفاع أدنى موحّد حتى يتساوى زر المراقبة (سطر واحد) مع بقية الأزرار.
        QuickActionButton(
            label = stringResource(
                if (trackingEnabled) R.string.action_stop_tracking else R.string.action_start_tracking,
            ),
            icon = if (trackingEnabled) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            onClick = onToggleTracking,
            active = trackingEnabled,
            modifier = Modifier.weight(1f).heightIn(min = 84.dp),
        )
        QuickActionButton(
            label = stringResource(R.string.action_refresh),
            icon = Icons.Rounded.Refresh,
            onClick = onRefresh,
            modifier = Modifier.weight(1f).heightIn(min = 84.dp),
        )
        QuickActionButton(
            label = stringResource(R.string.action_reset_session),
            icon = Icons.Rounded.RestartAlt,
            onClick = onResetSession,
            modifier = Modifier.weight(1f).heightIn(min = 84.dp),
        )
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
private fun WifiDetailsCard(wifi: WifiStatus) {
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
                    .clickable { expanded = !expanded },
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
) {
    val downloadColor = LocalDownloadColor.current
    val uploadColor = LocalUploadColor.current

    ExpressiveCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = stringResource(R.string.label_today),
                supportingText = Format.fullDate(state.now),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TrafficTile(
                    label = stringResource(R.string.label_download),
                    bytes = state.today.rxBytes,
                    icon = Icons.Rounded.ArrowDownward,
                    accent = downloadColor,
                    modifier = Modifier.weight(1f),
                )
                TrafficTile(
                    label = stringResource(R.string.label_upload),
                    bytes = state.today.txBytes,
                    icon = Icons.Rounded.ArrowUpward,
                    accent = uploadColor,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            TodayTotalRow(
                bytes = state.today.totalBytes,
                label = stringResource(R.string.label_total),
                secondaryBytes = state.cycle.totalBytes,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SpeedTestPanel(
                state = state.speedTest,
                onStart = onStart,
                onStop = onStop,
            )
        }
    }
}

@Composable
private fun SessionCard(state: UiState, onResetSession: () -> Unit) {
    ExpressiveCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = stringResource(R.string.label_session),
                supportingText = stringResource(
                    R.string.session_since,
                    Format.clock(state.settings.sessionStartMillis),
                ),
                trailing = {
                    TextButton(onClick = onResetSession, shape = AppButtonShape) {
                        Text(text = stringResource(R.string.action_reset_session))
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
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
