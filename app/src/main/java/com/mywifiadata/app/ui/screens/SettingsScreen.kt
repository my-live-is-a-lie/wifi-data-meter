package com.mywifiadata.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.SamplingIntervalUnit
import com.mywifiadata.app.core.SamplingIntervals
import com.mywifiadata.app.data.model.AccentColor
import com.mywifiadata.app.data.model.ThemeMode
import com.mywifiadata.app.ui.UiState
import com.mywifiadata.app.ui.components.ExpressiveCard
import com.mywifiadata.app.ui.components.InfoRow
import com.mywifiadata.app.ui.components.SectionHeader
import com.mywifiadata.app.ui.components.SettingSwitchRow
import com.mywifiadata.app.ui.theme.AppButtonShape
import com.mywifiadata.app.ui.theme.swatchColor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.collect

/**
 * شاشة الإعدادات: الباقة، التنبيهات، المراقبة، البيانات، والمزيد.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    state: UiState,
    onQuotaChange: (Long) -> Unit,
    onCycleDayChange: (Int) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onThresholdChange: (Int) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
    onQuickSettingsTileEnabledChange: (Boolean) -> Unit,
    onAppIconHiddenChange: (Boolean) -> Boolean,
    onTrackingChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    var showQuotaDialog by rememberSaveable { mutableStateOf(false) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    var showHideAppIconDialog by rememberSaveable { mutableStateOf(false) }
    var hideAppIconFailed by rememberSaveable { mutableStateOf(false) }
    var moreExpanded by rememberSaveable { mutableStateOf(false) }
    val settings = state.settings
    val quickSettingsTileReady = settings.quickSettingsTileEnabled && settings.quickSettingsTileAdded

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ExpressiveCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = stringResource(R.string.settings_section_quota),
                        supportingText = stringResource(R.string.settings_quota_hint),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        icon = Icons.Rounded.MonetizationOn,
                        label = stringResource(R.string.label_quota),
                        value = if (state.hasQuota) {
                            Format.bytes(settings.quotaBytes)
                        } else {
                            stringResource(R.string.quota_not_set)
                        },
                    )
                    InfoRow(
                        icon = Icons.Rounded.Timer,
                        label = stringResource(R.string.settings_cycle_day),
                        value = "${settings.cycleStartDay}",
                    )
                    InfoRow(
                        icon = Icons.Rounded.Timer,
                        label = stringResource(R.string.label_remaining),
                        value = if (state.hasQuota) Format.bytes(state.remainingBytes) else "—",
                    )
                    InfoRow(
                        icon = Icons.Rounded.Info,
                        label = stringResource(R.string.label_days),
                        value = "${state.daysLeft}",
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { showQuotaDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppButtonShape,
                    ) {
                        Text(text = stringResource(R.string.quota_set_action))
                    }
                }
            }
        }

        item {
            ExpressiveCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = stringResource(R.string.settings_section_alerts))
                    SettingSwitchRow(
                        icon = Icons.Rounded.Notifications,
                        title = stringResource(R.string.settings_notifications),
                        subtitle = stringResource(R.string.settings_notifications_desc),
                        checked = settings.notificationsEnabled,
                        onCheckedChange = onNotificationsChange,
                    )
                    if (settings.notificationsEnabled) {
                        Text(
                            text = stringResource(R.string.settings_threshold) +
                                " • %${settings.alertThresholdPercent}",
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        AlertThresholdSlider(
                            value = settings.alertThresholdPercent,
                            onValueChange = onThresholdChange,
                        )
                    }
                }
            }
        }

        item {
            ExpressiveCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = stringResource(R.string.settings_section_monitor))
                    IntervalSelector(
                        intervalIndex = settings.sampleIntervalIndex,
                        onIntervalChange = onIntervalChange,
                    )
                    SettingSwitchRow(
                        icon = Icons.Rounded.Tune,
                        title = stringResource(R.string.action_start_tracking),
                        subtitle = if (settings.trackingEnabled) {
                            stringResource(R.string.tracking_running)
                        } else {
                            stringResource(R.string.tracking_stopped)
                        },
                        checked = settings.trackingEnabled,
                        onCheckedChange = onTrackingChange,
                    )
                }
            }
        }

        item {
            ExpressiveCard(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = stringResource(R.string.settings_section_appearance))
                    ThemeModeSelector(
                        themeMode = settings.themeMode,
                        onThemeModeChange = onThemeModeChange,
                    )
                    AccentColorSelector(
                        accentColor = settings.accentColor,
                        dynamicColorEnabled = settings.dynamicColorEnabled,
                        onAccentColorChange = onAccentColorChange,
                    )
                    SettingSwitchRow(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.settings_dynamic_color),
                        subtitle = stringResource(R.string.settings_dynamic_color_desc),
                        checked = settings.dynamicColorEnabled,
                        onCheckedChange = onDynamicColorChange,
                    )
                }
            }
        }

        item {
            ExpressiveCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = stringResource(R.string.settings_section_data))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (state.hasUsageAccess) {
                                Icons.Rounded.Check
                            } else {
                                Icons.Rounded.Info
                            },
                            contentDescription = null,
                            tint = if (state.hasUsageAccess) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_usage_access),
                                style = MaterialTheme.typography.bodyLargeEmphasized,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (state.hasUsageAccess) {
                                    stringResource(R.string.settings_usage_access_granted)
                                } else {
                                    stringResource(R.string.settings_usage_access_missing)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onOpenUsageAccess) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = stringResource(R.string.action_open_usage_access),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppButtonShape,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_clear_data),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_clear_data_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            val chevronRotation by animateFloatAsState(
                targetValue = if (moreExpanded) 180f else 0f,
                label = "moreChevron",
            )
            ExpressiveCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // رأس القسم نفسه هو زر التوسيع/الطيّ.
                    SectionHeader(
                        title = stringResource(R.string.settings_section_more),
                        modifier = Modifier.clickable { moreExpanded = !moreExpanded },
                        trailing = {
                            Icon(
                                imageVector = Icons.Rounded.ExpandMore,
                                contentDescription = stringResource(
                                    if (moreExpanded) {
                                        R.string.settings_more_collapse
                                    } else {
                                        R.string.settings_more_expand
                                    },
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.rotate(chevronRotation),
                            )
                        },
                    )
                    AnimatedVisibility(visible = moreExpanded) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingSwitchRow(
                                icon = Icons.Rounded.GridView,
                                title = stringResource(R.string.settings_quick_settings_tile),
                                subtitle = stringResource(R.string.settings_quick_settings_tile_desc),
                                checked = settings.quickSettingsTileEnabled,
                                onCheckedChange = onQuickSettingsTileEnabledChange,
                            )
                            SettingSwitchRow(
                                icon = Icons.Rounded.VisibilityOff,
                                title = stringResource(R.string.settings_hide_app_icon),
                                subtitle = stringResource(R.string.settings_hide_app_icon_desc),
                                checked = settings.appIconHidden,
                                enabled = quickSettingsTileReady,
                                onCheckedChange = { hide ->
                                    if (hide) {
                                        hideAppIconFailed = false
                                        showHideAppIconDialog = true
                                    } else {
                                        onAppIconHiddenChange(false)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showQuotaDialog) {
        QuotaDialog(
            quotaBytes = settings.quotaBytes,
            cycleStartDay = settings.cycleStartDay,
            onDismiss = { showQuotaDialog = false },
            onConfirm = { gigabytes, cycleDay ->
                onQuotaChange(Format.gigabytesToBytes(gigabytes))
                onCycleDayChange(cycleDay)
                showQuotaDialog = false
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(imageVector = Icons.Rounded.Info, contentDescription = null) },
            title = { Text(text = stringResource(R.string.settings_clear_data)) },
            text = { Text(text = stringResource(R.string.settings_clear_data_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        onClearData()
                    },
                    shape = AppButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(text = stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }, shape = AppButtonShape) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showHideAppIconDialog) {
        AlertDialog(
            onDismissRequest = { showHideAppIconDialog = false },
            icon = { Icon(imageVector = Icons.Rounded.VisibilityOff, contentDescription = null) },
            title = { Text(text = stringResource(R.string.hide_app_icon_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(R.string.hide_app_icon_dialog_intro))
                    HideAppIconGuideIllustration()
                    HideAppIconInstructionStep(
                        number = 1,
                        text = stringResource(R.string.hide_app_icon_step_open_panel),
                    )
                    HideAppIconInstructionStep(
                        number = 2,
                        text = stringResource(R.string.hide_app_icon_step_long_press),
                    )
                    if (hideAppIconFailed) {
                        Text(
                            text = stringResource(R.string.hide_app_icon_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onAppIconHiddenChange(true)) {
                            hideAppIconFailed = false
                            showHideAppIconDialog = false
                        } else {
                            hideAppIconFailed = true
                        }
                    },
                    shape = AppButtonShape,
                ) {
                    Text(text = stringResource(R.string.hide_app_icon_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        hideAppIconFailed = false
                        showHideAppIconDialog = false
                    },
                    shape = AppButtonShape,
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun HideAppIconGuideIllustration() {
    val transition = rememberInfiniteTransition(label = "hide-app-icon-guide")
    val cycleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "guide-cycle-progress",
    )
    val panelProgress = when {
        cycleProgress < 0.06f -> 0f
        cycleProgress < 0.20f -> hideGuideSegment(cycleProgress, 0.06f, 0.20f)
        cycleProgress < 0.70f -> 1f
        cycleProgress < 0.80f -> 1f - hideGuideSegment(cycleProgress, 0.70f, 0.80f)
        else -> 0f
    }
    val isEditing = cycleProgress in 0.32f..0.50f
    val tileAdded = cycleProgress >= 0.50f
    val holdProgress = when {
        cycleProgress < 0.55f || cycleProgress > 0.72f -> 0f
        cycleProgress < 0.60f -> hideGuideSegment(cycleProgress, 0.55f, 0.60f)
        cycleProgress < 0.67f -> 1f
        else -> 1f - hideGuideSegment(cycleProgress, 0.67f, 0.72f)
    }
    val pulse = ((sin(cycleProgress.toDouble() * 2.0 * PI * 10.0) + 1.0) / 2.0).toFloat()
    val captionResource = when {
        cycleProgress < 0.20f -> R.string.hide_app_icon_demo_swipe
        cycleProgress < 0.50f -> R.string.hide_app_icon_demo_add
        cycleProgress < 0.72f -> R.string.hide_app_icon_demo_long_press
        else -> R.string.hide_app_icon_demo_app_open
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.GridView,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.hide_app_icon_guide_panel),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            HideAppIconDemoAppScreen()

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(108.dp)
                    .graphicsLayer {
                        translationY = -size.height * (1f - panelProgress)
                        alpha = panelProgress
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(7.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            if (isEditing) R.string.hide_app_icon_demo_add_tiles
                            else R.string.hide_app_icon_demo_quick_settings,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(
                            if (isEditing) R.string.hide_app_icon_demo_done
                            else R.string.hide_app_icon_demo_edit,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight(),
                    ) {
                        AnimatedContent(
                            targetState = tileAdded,
                            label = "quick-settings-tile-added",
                        ) { added ->
                            HideAppIconGuideTile(
                                selected = added,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (holdProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer { alpha = 0.30f + pulse * 0.45f }
                                    .border(
                                        width = (1f + pulse).dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(10.dp),
                                    ),
                            )
                            Icon(
                                imageVector = Icons.Rounded.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(15.dp)
                                    .graphicsLayer { alpha = holdProgress },
                            )
                        }
                    }
                    HideAppIconGuideTile(
                        selected = false,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    HideAppIconGuideTile(
                        selected = false,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }

                AnimatedVisibility(
                    visible = isEditing,
                    enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 2 },
                    exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { it / 2 },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.hide_app_icon_demo_available_tiles),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        Row(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = 1f + pulse * 0.08f
                                    scaleY = 1f + pulse * 0.08f
                                }
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_stat_usage),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = stringResource(R.string.qs_tile_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (cycleProgress in 0.04f..0.20f) {
                Icon(
                    imageVector = Icons.Rounded.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (hideGuideSegment(cycleProgress, 0.04f, 0.20f) * 54f - 10f).dp)
                        .size(18.dp)
                        .graphicsLayer { alpha = panelProgress },
                )
            }
        }

        Text(
            text = stringResource(captionResource),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            minLines = 1,
        )
    }
}

@Composable
private fun HideAppIconDemoAppScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_stat_usage),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.tab_dashboard),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(R.string.action_start_tracking),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HideAppIconGuideTile(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val foreground = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
            )
            .then(
                if (selected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier,
            )
            .padding(horizontal = 2.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_stat_usage),
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = stringResource(R.string.qs_tile_label),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(17.dp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(foreground.copy(alpha = 0.28f)),
            )
        }
    }
}

private fun hideGuideSegment(value: Float, start: Float, end: Float): Float {
    val fraction = ((value - start) / (end - start)).coerceIn(0f, 1f)
    return fraction * fraction * (3f - 2f * fraction)
}

@Composable
private fun HideAppIconInstructionStep(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IntervalSelector(intervalIndex: Int, onIntervalChange: (Int) -> Unit) {
    val options = SamplingIntervals.options
    val safeIntervalIndex = SamplingIntervals.coerceIndex(intervalIndex)
    val initialCenterVirtualIndex = remember(options.size, safeIntervalIndex) {
        val virtualCycleStart = (Int.MAX_VALUE / 2 / options.size) * options.size
        virtualCycleStart + safeIntervalIndex
    }
    // بلا حشوة: العنصر الأول قبل المختار مباشرة، فيقع المختار دائمًا في الصف الأوسط.
    val initialListIndex = initialCenterVirtualIndex - 1
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialListIndex,
    )
    val centeredVirtualIndex by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportSize.height / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { item ->
                    abs(item.offset + item.size / 2 - viewportCenter)
                }
                ?.index ?: (listState.firstVisibleItemIndex + 1)
        }
    }
    val currentIntervalIndex by rememberUpdatedState(safeIntervalIndex)
    val currentOnIntervalChange by rememberUpdatedState(onIntervalChange)

    // حفظ الخيار بعد الإفلات/اكتمال حركة الالتقاط (snap)، لا مع كل بكسل أثناء السحب.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { isScrolling ->
            if (!isScrolling) {
                val settledIndex = Math.floorMod(centeredVirtualIndex, options.size)
                if (settledIndex != currentIntervalIndex) {
                    currentOnIntervalChange(settledIndex)
                }
            }
        }
    }

    // مزامنة العجلة إذا أعادت الإعدادات ضبط قيمة القياس من خارجها.
    LaunchedEffect(safeIntervalIndex, listState) {
        val currentVirtualIndex = centeredVirtualIndex
        val currentOptionIndex = Math.floorMod(currentVirtualIndex, options.size)
        if (currentOptionIndex != safeIntervalIndex) {
            val forwardDistance = Math.floorMod(safeIntervalIndex - currentOptionIndex, options.size)
            val shortestDistance = if (forwardDistance <= options.size / 2) {
                forwardDistance
            } else {
                forwardDistance - options.size
            }
            // animateScrollToItem يضع الفهرس عند أعلى القائمة؛ نمرر الفهرس السابق كي يتوسط الهدف.
            listState.animateScrollToItem(currentVirtualIndex + shortestDistance - 1)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(INTERVAL_WHEEL_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.settings_interval),
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Box(
            modifier = Modifier
                .width(INTERVAL_WHEEL_WIDTH)
                .height(INTERVAL_WHEEL_HEIGHT),
        ) {
            val selectionShape = RoundedCornerShape(8.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(INTERVAL_WHEEL_ITEM_HEIGHT)
                    .clip(selectionShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                        shape = selectionShape,
                    ),
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            ) {
                items(count = Int.MAX_VALUE) { virtualIndex ->
                    val option = options[Math.floorMod(virtualIndex, options.size)]
                    val distanceFromCenter = abs(virtualIndex - centeredVirtualIndex)
                    val layoutInfo = listState.layoutInfo
                    val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == virtualIndex }
                    val distanceFraction = visibleItem?.let { item ->
                        val itemCenter = item.offset + item.size / 2
                        val viewportCenter = layoutInfo.viewportSize.height / 2
                        (abs(itemCenter - viewportCenter).toFloat() / item.size.coerceAtLeast(1).toFloat())
                            .coerceIn(0f, 1f)
                    } ?: 1f
                    val itemAlpha = 1f - 0.20f * distanceFraction
                    val itemScale = 1f - 0.08f * distanceFraction
                    val unitLabel = stringResource(
                        when (option.unit) {
                            SamplingIntervalUnit.SECOND -> R.string.settings_interval_unit_seconds
                            SamplingIntervalUnit.MINUTE -> R.string.settings_interval_unit_minutes
                            SamplingIntervalUnit.HOUR -> R.string.settings_interval_unit_hours
                        },
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(INTERVAL_WHEEL_ITEM_HEIGHT)
                            .graphicsLayer {
                                alpha = itemAlpha
                                scaleX = itemScale
                                scaleY = itemScale
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${option.amountLabel} $unitLabel",
                            style = if (distanceFromCenter == 0) {
                                MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp)
                            } else {
                                MaterialTheme.typography.titleSmall
                            },
                            color = if (distanceFromCenter == 0) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

private val INTERVAL_WHEEL_ITEM_HEIGHT = 24.dp
private val INTERVAL_WHEEL_HEIGHT = INTERVAL_WHEEL_ITEM_HEIGHT * 3
private val INTERVAL_WHEEL_WIDTH = 68.dp

/**
 * منتقي سمة التطبيق: أربعة خيارات (النظام / فاتح / داكن / مظلم)
 * بنفس نمط [IntervalSelector] مع قصّ النص عند الضيق.
 */
@Composable
private fun ThemeModeSelector(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val labels = listOf(
        stringResource(R.string.theme_system),
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark),
        stringResource(R.string.theme_black),
    )
    val options = remember { ThemeMode.entries.toList() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = themeMode == mode,
                    onClick = { onThemeModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    icon = {},
                    label = {
                        Text(
                            text = labels[index],
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AccentColorSelector(
    accentColor: AccentColor,
    dynamicColorEnabled: Boolean,
    onAccentColorChange: (AccentColor) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val contentLayoutDirection = LocalLayoutDirection.current
    val options = remember { AccentColor.entries.toList() }
    val currentColorLabel = if (dynamicColorEnabled) {
        stringResource(R.string.settings_accent_color_system_active)
    } else {
        stringResource(accentColor.labelResource())
    }
    val currentSwatchColor = if (dynamicColorEnabled) {
        MaterialTheme.colorScheme.primary
    } else {
        accentColor.swatchColor()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { expanded = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_accent_color),
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(currentSwatchColor),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentColorLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (expanded) 180f else 0f),
            )
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 480.dp),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides contentLayoutDirection) {
                    options.forEach { option ->
                        val label = stringResource(option.labelResource())
                        val selected = accentColor == option
                        val swatchColor = if (option == AccentColor.DEFAULT && dynamicColorEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            option.swatchColor()
                        }
                        DropdownMenuItem(
                            text = { Text(text = label, maxLines = 1) },
                            onClick = {
                                expanded = false
                                onAccentColorChange(option)
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                            shape = CircleShape,
                                        ),
                                )
                            },
                            trailingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            } else {
                                null
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun AccentColor.labelResource(): Int = when (this) {
    AccentColor.DEFAULT -> R.string.accent_color_default
    AccentColor.INDIGO -> R.string.accent_color_indigo
    AccentColor.BLUE -> R.string.accent_color_blue
    AccentColor.CYAN -> R.string.accent_color_cyan
    AccentColor.TEAL -> R.string.accent_color_teal
    AccentColor.GREEN -> R.string.accent_color_green
    AccentColor.AMBER -> R.string.accent_color_amber
    AccentColor.ORANGE -> R.string.accent_color_orange
    AccentColor.RED -> R.string.accent_color_red
    AccentColor.PINK -> R.string.accent_color_pink
    AccentColor.PURPLE -> R.string.accent_color_purple
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertThresholdSlider(value: Int, onValueChange: (Int) -> Unit) {
    val sliderState = rememberSliderState(
        value = value.toFloat(),
        steps = 17,
        trackRange = 10f..100f,
    )
    Slider(
        state = sliderState,
        onValueChange = {
            // ملاحظة material3: السحب لا يحدّث state وحده — نحدّثه يدويًا لتحريك المؤشر.
            sliderState.value = it
            onValueChange(it.roundToInt())
        },
    )
}

@Composable
private fun QuotaDialog(
    quotaBytes: Long,
    cycleStartDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Double, Int) -> Unit,
) {
    val initialGigabytes = if (quotaBytes > 0L) {
        String.format(java.util.Locale.US, "%.0f", Format.bytesToGigabytes(quotaBytes))
    } else {
        ""
    }
    var gigabytesText by rememberSaveable(quotaBytes) { mutableStateOf(initialGigabytes) }
    var cycleDay by rememberSaveable(cycleStartDay) { mutableStateOf(cycleStartDay) }
    val parsed = gigabytesText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Rounded.MonetizationOn, contentDescription = null) },
        title = { Text(text = stringResource(R.string.settings_quota_size)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = gigabytesText,
                    onValueChange = { newValue -> gigabytesText = newValue.filter { it.isDigit() || it == '.' } },
                    label = { Text(text = stringResource(R.string.unit_gb)) },
                    supportingText = { Text(text = stringResource(R.string.settings_quota_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.settings_cycle_day),
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    IconButton(
                        onClick = { cycleDay = (cycleDay - 1).coerceAtLeast(1) },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = stringResource(R.string.cd_decrease),
                        )
                    }
                    Text(
                        text = "$cycleDay",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    IconButton(
                        onClick = { cycleDay = (cycleDay + 1).coerceAtMost(28) },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.cd_increase),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settings_cycle_day_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(parsed ?: 0.0, cycleDay) },
                enabled = gigabytesText.isBlank() || (parsed != null && parsed >= 0.0),
                shape = AppButtonShape,
            ) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = AppButtonShape) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}
