package com.mywifiadata.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.East
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.West
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
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.SamplingIntervalUnit
import com.mywifiadata.app.core.SamplingIntervals
import com.mywifiadata.app.core.UsageNetwork
import com.mywifiadata.app.data.model.AccentColor
import com.mywifiadata.app.data.model.AdditionalPlan
import com.mywifiadata.app.data.model.AdditionalPlanDraft
import com.mywifiadata.app.data.model.AppLayoutDirection
import com.mywifiadata.app.data.model.AppSettings
import com.mywifiadata.app.data.model.DataPlan
import com.mywifiadata.app.data.model.MAX_ADDITIONAL_PLANS
import com.mywifiadata.app.data.model.PlanCapacityUnit
import com.mywifiadata.app.data.model.PlanDraft
import com.mywifiadata.app.data.model.PlanDurationUnit
import com.mywifiadata.app.data.model.PlanNetworkType
import com.mywifiadata.app.data.model.ThemeMode
import com.mywifiadata.app.ui.UiState
import com.mywifiadata.app.ui.components.ExpressiveCard
import com.mywifiadata.app.ui.components.InfoRow
import com.mywifiadata.app.ui.components.SectionHeader
import com.mywifiadata.app.ui.components.SettingSwitchRow
import com.mywifiadata.app.ui.theme.AppButtonShape
import com.mywifiadata.app.ui.theme.swatchColor
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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
    onHideStatusBarIconChange: (Boolean) -> Unit,
    onAppIconHiddenChange: (Boolean) -> Boolean,
    onTrackingChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onClearData: () -> Unit,
    onLayoutDirectionChange: (AppLayoutDirection) -> Unit = {},
    onSavePlan: (PlanDraft) -> Unit = { draft -> onQuotaChange(draft.quotaBytes) },
    onActivatePlan: (UsageNetwork) -> Unit = {},
    onAddPlanToSavedList: (PlanDraft) -> Unit = {},
    onUpdateSavedPlan: (UsageNetwork, String, PlanDraft) -> Unit = { _, _, _ -> },
    onSwitchToSavedPlan: (UsageNetwork, String) -> Unit = { _, _ -> },
    onDeletePlan: (UsageNetwork, String) -> Unit = { _, _ -> },
    onAddAdditionalPlan: (UsageNetwork, AdditionalPlanDraft) -> Unit = { _, _ -> },
    onUpdateAdditionalPlan: (UsageNetwork, String, AdditionalPlanDraft) -> Unit = { _, _, _ -> },
    onDeleteAdditionalPlan: (UsageNetwork, String) -> Unit = { _, _ -> },
    onReorderAdditionalPlans: (UsageNetwork, Int, Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    var showQuotaDialog by rememberSaveable { mutableStateOf(false) }
    var showPlansListDialog by rememberSaveable { mutableStateOf(false) }
    var showAddSavedPlanDialog by rememberSaveable { mutableStateOf(false) }
    var editingSavedPlanId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddAdditionalPlanDialog by rememberSaveable { mutableStateOf(false) }
    var editingAdditionalPlanId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteAdditionalPlanId by rememberSaveable { mutableStateOf<String?>(null) }
    var additionalPlansExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedPlanNetwork by rememberSaveable { mutableStateOf(state.activePlanNetwork) }
    var planSwipeDirection by rememberSaveable { mutableStateOf(1) }
    LaunchedEffect(state.activePlanNetwork) {
        selectedPlanNetwork = state.activePlanNetwork
    }
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
            ExpressiveCard(
                modifier = Modifier.pointerInput(selectedPlanNetwork) {
                    var accumulatedDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDrag += dragAmount
                        },
                        onDragEnd = {
                            val threshold = (size.width * 0.12f).coerceAtLeast(36f)
                            when {
                                accumulatedDrag < -threshold -> {
                                    planSwipeDirection = -1
                                    selectedPlanNetwork = if (selectedPlanNetwork == UsageNetwork.WIFI) {
                                        UsageNetwork.MOBILE
                                    } else {
                                        UsageNetwork.WIFI
                                    }
                                }
                                accumulatedDrag > threshold -> {
                                    planSwipeDirection = 1
                                    selectedPlanNetwork = if (selectedPlanNetwork == UsageNetwork.WIFI) {
                                        UsageNetwork.MOBILE
                                    } else {
                                        UsageNetwork.WIFI
                                    }
                                }
                            }
                            accumulatedDrag = 0f
                        },
                        onDragCancel = { accumulatedDrag = 0f },
                    )
                },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_section_quota),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                if (selectedPlanNetwork == UsageNetwork.WIFI) {
                                    R.string.plan_card_network_wifi
                                } else {
                                    R.string.plan_card_network_mobile
                                },
                            ),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    AnimatedContent(
                        targetState = selectedPlanNetwork,
                        transitionSpec = {
                            val dir = planSwipeDirection
                            (slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { width -> -dir * width / 3 } +
                                fadeIn(tween(200))) togetherWith
                                (slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { width -> dir * width / 3 } +
                                    fadeOut(tween(180)))
                        },
                        label = "plan-card-network-page",
                    ) { pageNetwork ->
                        val plan = state.planFor(pageNetwork)
                        val hasPlanQuota = plan.hasQuota
                        val remainingBytes = state.planRemainingBytes(pageNetwork)
                        val daysLeft = state.planDaysLeft(pageNetwork)
                        val totalDays = state.planTotalDays(pageNetwork)
                        val planLogoBitmap = remember(plan.logoPath) {
                            loadPlanLogoBitmap(plan.logoPath)
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (plan.name.isNotBlank() || planLogoBitmap != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    if (plan.name.isNotBlank()) {
                                        Text(
                                            text = plan.name,
                                            style = MaterialTheme.typography.titleMediumEmphasized,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                    if (planLogoBitmap != null) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Image(
                                            bitmap = planLogoBitmap,
                                            contentDescription = stringResource(R.string.plan_logo_label),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                QuotaMetricColumn(
                                    label = stringResource(R.string.label_quota),
                                    value = if (hasPlanQuota) {
                                        Format.bytes(plan.quotaBytes)
                                    } else {
                                        "—"
                                    },
                                    horizontalAlignment = Alignment.Start,
                                    modifier = Modifier.weight(1f),
                                )
                                QuotaMetricColumn(
                                    label = stringResource(R.string.label_remaining),
                                    value = if (hasPlanQuota) Format.bytes(remainingBytes) else "—",
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f),
                                )
                                QuotaMetricColumn(
                                    label = stringResource(R.string.quota_remaining_time),
                                    value = if (hasPlanQuota) {
                                        stringResource(R.string.quota_days_of_total, daysLeft, totalDays)
                                    } else {
                                        "—"
                                    },
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            if (hasPlanQuota && plan.pendingActivation) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Button(
                                        onClick = { onActivatePlan(pageNetwork) },
                                        modifier = Modifier.weight(1f),
                                        shape = AppButtonShape,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.quota_activate_action),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { showQuotaDialog = true },
                                        modifier = Modifier.weight(1f),
                                        shape = AppButtonShape,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.quota_update_action),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = { showQuotaDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = AppButtonShape,
                                ) {
                                    Text(
                                        text = stringResource(
                                            if (hasPlanQuota) R.string.quota_update_action else R.string.quota_set_action,
                                        ),
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = additionalPlansExpanded && plan.additionalPlans.isNotEmpty(),
                            ) {
                                AdditionalPlansSection(
                                    additionalPlans = plan.additionalPlans,
                                    onAddClick = { showAddAdditionalPlanDialog = true },
                                    onEditClick = { item -> editingAdditionalPlanId = item.id },
                                    onRequestDelete = { item -> pendingDeleteAdditionalPlanId = item.id },
                                    onReorder = { fromIdx, toIdx ->
                                        onReorderAdditionalPlans(pageNetwork, fromIdx, toIdx)
                                    },
                                )
                            }

                            if (plan.additionalPlans.isNotEmpty()) {
                                val expandChevronRotation by animateFloatAsState(
                                    targetValue = if (additionalPlansExpanded) 180f else 0f,
                                    label = "additionalPlansChevron",
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable(role = Role.Button) {
                                            additionalPlansExpanded = !additionalPlansExpanded
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ExpandMore,
                                        contentDescription = stringResource(
                                            if (additionalPlansExpanded) {
                                                R.string.additional_plans_collapse_cd
                                            } else {
                                                R.string.additional_plans_expand_cd
                                            },
                                        ),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .rotate(expandChevronRotation),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    PlanCardPageIndicator(
                        selectedNetwork = selectedPlanNetwork,
                        onToggle = {
                            planSwipeDirection = if (selectedPlanNetwork == UsageNetwork.WIFI) -1 else 1
                            selectedPlanNetwork = if (selectedPlanNetwork == UsageNetwork.WIFI) {
                                UsageNetwork.MOBILE
                            } else {
                                UsageNetwork.WIFI
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        }

        item {
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            ExpressiveCard(
                modifier = Modifier.clickable(role = Role.Button) { showPlansListDialog = true },
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.plans_button_title),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = if (isRtl) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.plans_button_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
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
                    LayoutDirectionSelector(
                        layoutDirection = settings.layoutDirection,
                        onLayoutDirectionChange = onLayoutDirectionChange,
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
                                title = stringResource(R.string.settings_hide_status_bar_icon),
                                subtitle = stringResource(R.string.settings_hide_status_bar_icon_desc),
                                checked = settings.hideStatusBarIcon,
                                onCheckedChange = onHideStatusBarIconChange,
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
            settings = settings,
            initialNetwork = selectedPlanNetwork,
            onDismiss = { showQuotaDialog = false },
            onConfirm = { draft ->
                onSavePlan(draft)
                selectedPlanNetwork = when (draft.networkType) {
                    PlanNetworkType.MOBILE -> UsageNetwork.MOBILE
                    PlanNetworkType.WIFI, PlanNetworkType.BOTH -> UsageNetwork.WIFI
                }
                showQuotaDialog = false
            },
        )
    }

    if (showAddSavedPlanDialog) {
        QuotaDialog(
            settings = settings,
            initialNetwork = selectedPlanNetwork,
            isAddingNewPlan = true,
            onDismiss = { showAddSavedPlanDialog = false },
            onConfirm = { draft ->
                onAddPlanToSavedList(draft)
                selectedPlanNetwork = when (draft.networkType) {
                    PlanNetworkType.MOBILE -> UsageNetwork.MOBILE
                    PlanNetworkType.WIFI, PlanNetworkType.BOTH -> UsageNetwork.WIFI
                }
                showAddSavedPlanDialog = false
            },
        )
    }

    val editingSavedPlan = editingSavedPlanId?.let { id ->
        settings.savedPlansFor(selectedPlanNetwork).firstOrNull { it.id == id }
    }
    if (editingSavedPlan != null) {
        QuotaDialog(
            settings = settings,
            initialNetwork = selectedPlanNetwork,
            initialPlanOverride = editingSavedPlan,
            lockNetworkType = true,
            onDismiss = { editingSavedPlanId = null },
            onConfirm = { draft ->
                onUpdateSavedPlan(selectedPlanNetwork, editingSavedPlan.id, draft)
                editingSavedPlanId = null
            },
        )
    }

    if (showPlansListDialog) {
        PlansListDialog(
            network = selectedPlanNetwork,
            activePlan = state.planFor(selectedPlanNetwork),
            savedPlans = settings.savedPlansFor(selectedPlanNetwork),
            onDismiss = { showPlansListDialog = false },
            onActivateCurrentPlan = { onActivatePlan(selectedPlanNetwork) },
            onEditActivePlan = {
                showPlansListDialog = false
                showQuotaDialog = true
            },
            onAddAdditionalPlanToActive = {
                showPlansListDialog = false
                showAddAdditionalPlanDialog = true
            },
            onSwitchToSavedPlan = { planId ->
                onSwitchToSavedPlan(selectedPlanNetwork, planId)
            },
            onEditSavedPlan = { planId ->
                editingSavedPlanId = planId
            },
            onDeletePlan = { planId ->
                onDeletePlan(selectedPlanNetwork, planId)
            },
            onAddNewPlan = {
                showAddSavedPlanDialog = true
            },
        )
    }

    if (showAddAdditionalPlanDialog) {
        AdditionalPlanDialog(
            network = selectedPlanNetwork,
            initialPlan = null,
            onDismiss = { showAddAdditionalPlanDialog = false },
            onConfirm = { draft ->
                onAddAdditionalPlan(selectedPlanNetwork, draft)
                additionalPlansExpanded = true
                showAddAdditionalPlanDialog = false
            },
        )
    }

    val editingAdditionalPlan = editingAdditionalPlanId?.let { id ->
        state.planFor(selectedPlanNetwork).additionalPlans.firstOrNull { it.id == id }
    }
    if (editingAdditionalPlan != null) {
        AdditionalPlanDialog(
            network = selectedPlanNetwork,
            initialPlan = editingAdditionalPlan,
            onDismiss = { editingAdditionalPlanId = null },
            onConfirm = { draft ->
                onUpdateAdditionalPlan(selectedPlanNetwork, editingAdditionalPlan.id, draft)
                editingAdditionalPlanId = null
            },
        )
    }

    val pendingDeleteAdditionalPlan = pendingDeleteAdditionalPlanId?.let { id ->
        state.planFor(selectedPlanNetwork).additionalPlans.firstOrNull { it.id == id }
    }
    if (pendingDeleteAdditionalPlan != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteAdditionalPlanId = null },
            icon = { Icon(imageVector = Icons.Rounded.Delete, contentDescription = null) },
            title = { Text(text = stringResource(R.string.additional_plan_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.additional_plan_delete_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        val id = pendingDeleteAdditionalPlan.id
                        pendingDeleteAdditionalPlanId = null
                        onDeleteAdditionalPlan(selectedPlanNetwork, id)
                    },
                    shape = AppButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(text = stringResource(R.string.plans_action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteAdditionalPlanId = null },
                    shape = AppButtonShape,
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
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
                Column(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
private fun HideAppIconInstructionStep(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
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
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            animation = tween(durationMillis = 27_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "guide-cycle-progress",
    )
    // تبقى الأيقونة ظاهرة أولًا، وتتلاشى خلال 900ms قبل فتح الستارة، ثم تعود فور رجوع الشاشة الرئيسية.
    val launcherAppIconVisible = cycleProgress < 0.055f || cycleProgress >= 0.96f
    val panelProgress = when {
        cycleProgress < 0.09f -> 0f
        cycleProgress < 0.23f -> hideGuideSegment(cycleProgress, 0.09f, 0.23f)
        cycleProgress < 0.82f -> 1f
        cycleProgress < 0.8441f -> 1f - hideGuideSegment(cycleProgress, 0.82f, 0.8441f)
        else -> 0f
    }
    val overflowMenuOpen = cycleProgress in 0.36f..0.44f
    val isEditing = cycleProgress in 0.44f..0.74f
    val tileAdded = cycleProgress >= 0.66f
    val addTileHoldProgress = when {
        cycleProgress < 0.53f || cycleProgress >= 0.59f -> 0f
        cycleProgress < 0.55f -> hideGuideSegment(cycleProgress, 0.53f, 0.55f)
        cycleProgress < 0.57f -> 1f
        else -> 1f - hideGuideSegment(cycleProgress, 0.57f, 0.59f)
    }
    val launchHoldProgress = when {
        cycleProgress < 0.77f || cycleProgress >= 0.82f -> 0f
        cycleProgress < 0.785f -> hideGuideSegment(cycleProgress, 0.77f, 0.785f)
        cycleProgress < 0.805f -> 1f
        else -> 1f - hideGuideSegment(cycleProgress, 0.805f, 0.82f)
    }
    val holdProgress = maxOf(addTileHoldProgress, launchHoldProgress)
    val tileDragProgress = when {
        cycleProgress < 0.59f -> 0f
        cycleProgress < 0.66f -> hideGuideSegment(cycleProgress, 0.59f, 0.66f)
        else -> 0f
    }
    val launchTileShrinkProgress = when {
        cycleProgress < 0.805f -> 0f
        cycleProgress < 0.82f -> hideGuideSegment(cycleProgress, 0.805f, 0.82f)
        else -> 0f
    }
    val tapRippleProgress = hideGuideTapRipple(cycleProgress)
    // بعد اكتمال إغلاق الستارة عند 0.8441، تبقى الشاشة الرئيسية ظاهرة 0.1 ثانية.
    val isAppScreenVisible = cycleProgress >= 0.8478f && cycleProgress < 0.96f
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val fallbackTargets = hideGuideDefaultTargets(isRtl)
    var previewCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var panelCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var moreButtonCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var menuEditCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var doneButtonCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var availableTileCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var addedTileCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var moreButtonPoint by remember { mutableStateOf<HideGuidePoint?>(null) }
    var menuEditPoint by remember { mutableStateOf<HideGuidePoint?>(null) }
    var doneButtonPoint by remember { mutableStateOf<HideGuidePoint?>(null) }
    var availableTilePoint by remember { mutableStateOf<HideGuidePoint?>(null) }
    var addedTilePoint by remember { mutableStateOf<HideGuidePoint?>(null) }

    fun updateGuideTargetPoints() {
        val preview = previewCoordinates ?: return
        val panel = panelCoordinates ?: return
        moreButtonCoordinates?.let { hideGuidePanelPoint(preview, panel, it, panelProgress) }
            ?.let { moreButtonPoint = it }
        menuEditCoordinates?.let { hideGuideMeasuredPoint(preview, it) }?.let { menuEditPoint = it }
        doneButtonCoordinates?.let { hideGuidePanelPoint(preview, panel, it, panelProgress) }
            ?.let { doneButtonPoint = it }
        availableTileCoordinates?.let { hideGuidePanelPoint(preview, panel, it, panelProgress) }
            ?.let { availableTilePoint = it }
        addedTileCoordinates?.let { hideGuidePanelPoint(preview, panel, it, panelProgress) }
            ?.let { addedTilePoint = it }
    }

    val pointerTargets = HideGuideTargets(
        moreButton = previewCoordinates?.let { preview ->
            panelCoordinates?.let { panel ->
                moreButtonCoordinates?.let { hideGuidePanelPoint(preview, panel, it, panelProgress) }
            }
        } ?: moreButtonPoint ?: fallbackTargets.moreButton,
        menuEdit = previewCoordinates?.let { preview ->
            menuEditCoordinates?.let { hideGuideMeasuredPoint(preview, it) }
        } ?: menuEditPoint ?: fallbackTargets.menuEdit,
        availableTile = previewCoordinates?.let { preview ->
            panelCoordinates?.let { panel ->
                availableTileCoordinates?.let { hideGuidePanelPoint(preview, panel, it, panelProgress) }
            }
        } ?: availableTilePoint ?: fallbackTargets.availableTile,
        doneButton = previewCoordinates?.let { preview ->
            panelCoordinates?.let { panel ->
                doneButtonCoordinates?.let { hideGuidePanelPoint(preview, panel, it, panelProgress) }
            }
        } ?: doneButtonPoint ?: fallbackTargets.doneButton,
        addedTile = previewCoordinates?.let { preview ->
            panelCoordinates?.let { panel ->
                addedTileCoordinates?.let { hideGuidePanelPoint(preview, panel, it, panelProgress) }
            }
        } ?: addedTilePoint ?: fallbackTargets.addedTile,
    )
    val pointerPosition = hideGuidePointerPosition(cycleProgress, pointerTargets)
    val captionResource = when {
        cycleProgress < 0.055f || cycleProgress >= 0.96f -> R.string.hide_app_icon_demo_icon_visible
        cycleProgress < 0.09f -> R.string.hide_app_icon_demo_icon_hidden
        cycleProgress < 0.23f -> R.string.hide_app_icon_demo_swipe
        cycleProgress < 0.25f -> R.string.hide_app_icon_demo_panel_open
        cycleProgress < 0.36f -> R.string.hide_app_icon_demo_tap_more
        cycleProgress < 0.44f -> R.string.hide_app_icon_demo_choose_edit
        cycleProgress < 0.66f -> R.string.hide_app_icon_demo_add
        cycleProgress < 0.74f -> R.string.hide_app_icon_demo_finish_edit
        cycleProgress < 0.82f -> R.string.hide_app_icon_demo_long_press
        else -> R.string.hide_app_icon_demo_app_open
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(204.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .onGloballyPositioned {
                    previewCoordinates = it
                    updateGuideTargetPoints()
                },
        ) {
            val tileDragOffsetX =
                maxWidth * (pointerTargets.addedTile.x - pointerTargets.availableTile.x) * tileDragProgress
            val tileDragOffsetY =
                maxHeight * (pointerTargets.addedTile.y - pointerTargets.availableTile.y) * tileDragProgress

            AnimatedContent(
                targetState = isAppScreenVisible,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    if (targetState) {
                        (fadeIn(tween(500)) + scaleIn(initialScale = 0.08f, animationSpec = tween(500))) togetherWith
                            (fadeOut(tween(220)) + scaleOut(targetScale = 0.92f, animationSpec = tween(220)))
                    } else {
                        (fadeIn(tween(260)) + scaleIn(initialScale = 0.92f, animationSpec = tween(260))) togetherWith
                            (fadeOut(tween(260)) + scaleOut(targetScale = 0.08f, animationSpec = tween(260)))
                    }
                },
                label = "home-to-app-screen",
            ) { appIsVisible ->
                if (appIsVisible) {
                    HideAppIconDemoAppScreen()
                } else {
                    HideAppIconDemoHomeScreen(showLauncherAppIcon = launcherAppIconVisible)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(192.dp)
                    .graphicsLayer {
                        translationY = -size.height * (1f - panelProgress)
                        alpha = panelProgress
                    }
                    .onGloballyPositioned {
                        panelCoordinates = it
                        updateGuideTargetPoints()
                    }
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.hide_app_icon_demo_time),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Rounded.Wifi,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .width(17.dp)
                            .height(9.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = RoundedCornerShape(2.dp),
                            )
                            .padding(1.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent),
                            ),
                        ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            if (isEditing) R.string.hide_app_icon_demo_add_tiles
                            else R.string.hide_app_icon_demo_quick_settings,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isEditing) {
                        Text(
                            text = stringResource(R.string.hide_app_icon_demo_done),
                            modifier = Modifier.onGloballyPositioned {
                                doneButtonCoordinates = it
                                updateGuideTargetPoints()
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .onGloballyPositioned {
                                        moreButtonCoordinates = it
                                        updateGuideTargetPoints()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .fillMaxHeight()
                            .align(Alignment.CenterStart),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(16.dp)
                                .shadow(2.dp, CircleShape)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WbSunny,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(10.dp),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HideAppIconDemoTile(
                        title = stringResource(R.string.hide_app_icon_demo_internet),
                        selected = true,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) { tint ->
                        Icon(
                            imageVector = Icons.Rounded.Wifi,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    HideAppIconDemoTile(
                        title = stringResource(R.string.hide_app_icon_demo_bluetooth),
                        selected = false,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) { tint ->
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HideAppIconDemoTile(
                        title = stringResource(R.string.hide_app_icon_demo_notifications),
                        selected = false,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) { tint ->
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .onGloballyPositioned {
                                addedTileCoordinates = it
                                updateGuideTargetPoints()
                            },
                    ) {
                        AnimatedContent(
                            targetState = tileAdded,
                            transitionSpec = {
                                (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 3 }) togetherWith
                                    fadeOut(tween(160))
                            },
                            label = "quick-settings-tile-added",
                        ) { added ->
                            when {
                                added -> HideAppIconDemoTile(
                                    title = stringResource(R.string.qs_tile_label),
                                    selected = true,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = 1f - 0.08f * launchTileShrinkProgress
                                            scaleY = 1f - 0.08f * launchTileShrinkProgress
                                        },
                                ) { tint ->
                                    Icon(
                                        painter = painterResource(R.drawable.ic_network_monitor),
                                        contentDescription = null,
                                        tint = tint,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                isEditing -> Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.45f),
                                        )
                                        .border(
                                            width = 1.dp + tileDragProgress.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.35f + 0.5f * tileDragProgress,
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                else -> Spacer(modifier = Modifier.fillMaxSize())
                            }
                        }
                        if (launchHoldProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer { alpha = 0.28f + launchHoldProgress * 0.28f }
                                    .border(
                                        width = 1.dp + launchHoldProgress.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(12.dp),
                                    ),
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isEditing,
                    enter = fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 2 },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(26.dp)
                                .onGloballyPositioned {
                                    availableTileCoordinates = it
                                    updateGuideTargetPoints()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedContent(
                                targetState = tileAdded,
                                modifier = Modifier.fillMaxSize(),
                                label = "available-tile-selection",
                            ) { added ->
                                if (added) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                } else {
                                    HideAppIconDemoTile(
                                        title = stringResource(R.string.qs_tile_label),
                                        selected = false,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .absoluteOffset(
                                                x = tileDragOffsetX,
                                                y = tileDragOffsetY,
                                            )
                                            .shadow(
                                                elevation = (5f * tileDragProgress).dp,
                                                shape = RoundedCornerShape(12.dp),
                                            )
                                            .graphicsLayer {
                                                val pressScale = 1f - 0.06f * addTileHoldProgress
                                                val dragScale = 1f + 0.04f * tileDragProgress
                                                scaleX = pressScale * dragScale
                                                scaleY = pressScale * dragScale
                                            },
                                    ) { tint ->
                                        Icon(
                                            painter = painterResource(R.drawable.ic_network_monitor),
                                            contentDescription = null,
                                            tint = tint,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = overflowMenuOpen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .absoluteOffset(x = if (isRtl) 9.dp else (-9).dp, y = 52.dp),
                label = "quick-settings-overflow-menu",
            ) { menuVisible ->
                if (menuVisible) {
                    Row(
                        modifier = Modifier
                            .onGloballyPositioned {
                                menuEditCoordinates = it
                                updateGuideTargetPoints()
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.hide_app_icon_demo_edit),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(1.dp).graphicsLayer { alpha = 0f })
                }
            }

            if (cycleProgress >= 0.09f && cycleProgress < 0.82f) {
                val pointerSize = 34.dp
                val longPressPulse = (
                    0.5 + 0.5 * sin(cycleProgress.toDouble() * 2.0 * PI * 24.0)
                ).toFloat()
                Box(
                    modifier = Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset(
                            x = maxWidth * pointerPosition.x - pointerSize / 2,
                            y = maxHeight * pointerPosition.y - pointerSize / 2 + 9.dp,
                        )
                        .size(pointerSize),
                    contentAlignment = Alignment.Center,
                ) {
                    if (tapRippleProgress > 0f) {
                        val tapAlpha = 1f - tapRippleProgress
                        Box(
                            modifier = Modifier
                                .offset(y = (-10).dp)
                                .size((8f + 26f * tapRippleProgress).dp)
                                .shadow(7.dp, CircleShape)
                                .background(Color.White.copy(alpha = 0.32f * tapAlpha), CircleShape)
                                .border(
                                    width = 1.75.dp,
                                    color = Color.White.copy(alpha = 0.88f * tapAlpha),
                                    shape = CircleShape,
                                ),
                        )
                    }
                    if (holdProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .offset(y = (-10).dp)
                                .size((16f + longPressPulse * 14f).dp)
                                .shadow(8.dp, CircleShape)
                                .background(
                                    Color.White.copy(alpha = 0.14f + 0.24f * longPressPulse),
                                    CircleShape,
                                )
                                .border(
                                    width = 1.75.dp,
                                    color = Color.White.copy(alpha = 0.52f + 0.40f * longPressPulse),
                                    shape = CircleShape,
                                ),
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.TouchApp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(27.dp),
                    )
                    Icon(
                        imageVector = Icons.Rounded.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
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
private fun HideAppIconDemoHomeScreen(showLauncherAppIcon: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 28.dp, y = (-28).dp)
                .size(130.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.54f),
                    CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-36).dp, y = 25.dp)
                .size(112.dp)
                .background(
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                    CircleShape,
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.hide_app_icon_demo_time),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.padding(end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height((3 + index * 2).dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(9.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(2.dp),
                        )
                        .padding(1.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(1.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent),
                        ),
                    ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.hide_app_icon_demo_time),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.hide_app_icon_demo_home_date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HideAppIconHomeShortcut(
                    label = stringResource(R.string.hide_app_icon_demo_phone),
                    modifier = Modifier.weight(1f),
                    background = Color(0xFF278A55),
                    foreground = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(23.dp),
                    )
                }
                HideAppIconHomeShortcut(
                    label = stringResource(R.string.hide_app_icon_demo_browser),
                    modifier = Modifier.weight(1f),
                    background = Color(0xFF4285F4),
                    foreground = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(23.dp),
                    )
                }
                HideAppIconHomeShortcut(
                    label = stringResource(R.string.hide_app_icon_demo_messages),
                    modifier = Modifier.weight(1f),
                    background = Color(0xFF168FCE),
                    foreground = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(23.dp),
                    )
                }
                HideAppIconHomeShortcut(
                    label = stringResource(R.string.hide_app_icon_demo_camera),
                    modifier = Modifier.weight(1f),
                    background = Color(0xFF53616B),
                    foreground = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(23.dp),
                    )
                }
                AnimatedVisibility(
                    visible = showLauncherAppIcon,
                    enter = EnterTransition.None,
                    exit = fadeOut(tween(900)) + scaleOut(targetScale = 0.88f, animationSpec = tween(900)),
                ) {
                    HideAppIconHomeShortcut(
                        label = stringResource(R.string.app_name),
                        modifier = Modifier.width(36.dp),
                        background = MaterialTheme.colorScheme.primary,
                        foreground = Color.White,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_stat_usage),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HideAppIconHomeShortcut(
    label: String,
    modifier: Modifier = Modifier,
    background: Color,
    foreground: Color,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(3.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(background)
                .border(
                    width = 0.75.dp,
                    color = Color.White.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HideAppIconDemoAppScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(29.dp)
                    .clip(RoundedCornerShape(10.dp))
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
            Spacer(modifier = Modifier.width(7.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.tab_dashboard),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 9.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.label_total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.label_month),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                )
            }
            Text(
                text = "1.28 GB",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.64f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            HideAppIconUsageMetric(
                label = stringResource(R.string.label_download),
                value = "846 MB",
                icon = Icons.Rounded.ArrowDownward,
                modifier = Modifier.weight(1f),
                tint = MaterialTheme.colorScheme.primary,
            )
            HideAppIconUsageMetric(
                label = stringResource(R.string.label_upload),
                value = "434 MB",
                icon = Icons.Rounded.ArrowUpward,
                modifier = Modifier.weight(1f),
                tint = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun HideAppIconUsageMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    tint: Color,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(17.dp),
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HideAppIconDemoTile(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable (Color) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val foreground = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
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
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(19.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon(foreground)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class HideGuidePoint(val x: Float, val y: Float)

private data class HideGuideTargets(
    val moreButton: HideGuidePoint,
    val menuEdit: HideGuidePoint,
    val availableTile: HideGuidePoint,
    val doneButton: HideGuidePoint,
    val addedTile: HideGuidePoint,
)

private fun hideGuideDefaultTargets(isRtl: Boolean): HideGuideTargets = HideGuideTargets(
    moreButton = HideGuidePoint(x = if (isRtl) 0.10f else 0.90f, y = 0.205f),
    menuEdit = HideGuidePoint(x = if (isRtl) 0.18f else 0.82f, y = 0.33f),
    availableTile = HideGuidePoint(x = if (isRtl) 0.19f else 0.81f, y = 0.80f),
    doneButton = HideGuidePoint(x = if (isRtl) 0.10f else 0.90f, y = 0.205f),
    addedTile = HideGuidePoint(x = if (isRtl) 0.25f else 0.75f, y = 0.61f),
)

private fun hideGuidePanelPoint(
    preview: LayoutCoordinates,
    panel: LayoutCoordinates,
    target: LayoutCoordinates,
    panelProgress: Float,
): HideGuidePoint? {
    if (!preview.isAttached || !panel.isAttached || !target.isAttached) return null
    if (preview.size.width == 0 || preview.size.height == 0 || panel.size.height == 0) return null

    val center = panel.localPositionOf(
        target,
        Offset(x = target.size.width / 2f, y = target.size.height / 2f),
    )
    val panelTop = -panel.size.height * (1f - panelProgress)
    return HideGuidePoint(
        x = (center.x / preview.size.width).coerceIn(0f, 1f),
        y = ((panelTop + center.y) / preview.size.height).coerceIn(0f, 1f),
    )
}

private fun hideGuideMeasuredPoint(
    preview: LayoutCoordinates,
    target: LayoutCoordinates,
): HideGuidePoint? {
    if (!preview.isAttached || !target.isAttached) return null
    if (preview.size.width == 0 || preview.size.height == 0) return null

    val center = preview.localPositionOf(
        target,
        Offset(x = target.size.width / 2f, y = target.size.height / 2f),
    )
    return HideGuidePoint(
        x = (center.x / preview.size.width).coerceIn(0f, 1f),
        y = (center.y / preview.size.height).coerceIn(0f, 1f),
    )
}

private fun hideGuideSegment(value: Float, start: Float, end: Float): Float {
    val fraction = ((value - start) / (end - start)).coerceIn(0f, 1f)
    return FastOutSlowInEasing.transform(fraction)
}

private fun hideGuidePointerPosition(
    value: Float,
    targets: HideGuideTargets,
): HideGuidePoint {
    val homeStart = HideGuidePoint(x = 0.50f, y = 0.015f)
    val swipeEnd = HideGuidePoint(x = 0.50f, y = 0.62f)

    return when {
        value < 0.09f -> homeStart
        value < 0.23f -> hideGuideInterpolate(
            homeStart,
            swipeEnd,
            hideGuideSegment(value, 0.09f, 0.23f),
        )
        value < 0.25f -> swipeEnd
        value < 0.30f -> hideGuideInterpolate(
            swipeEnd,
            targets.moreButton,
            hideGuideSegment(value, 0.25f, 0.30f),
        )
        value < 0.36f -> targets.moreButton
        value < 0.40f -> hideGuideInterpolate(
            targets.moreButton,
            targets.menuEdit,
            hideGuideSegment(value, 0.36f, 0.40f),
        )
        value < 0.44f -> targets.menuEdit
        value < 0.53f -> hideGuideInterpolate(
            targets.menuEdit,
            targets.availableTile,
            hideGuideSegment(value, 0.44f, 0.53f),
        )
        value < 0.59f -> targets.availableTile
        value < 0.66f -> hideGuideInterpolate(
            targets.availableTile,
            targets.addedTile,
            hideGuideSegment(value, 0.59f, 0.66f),
        )
        value < 0.70f -> hideGuideInterpolate(
            targets.addedTile,
            targets.doneButton,
            hideGuideSegment(value, 0.66f, 0.70f),
        )
        value < 0.74f -> targets.doneButton
        value < 0.77f -> hideGuideInterpolate(
            targets.doneButton,
            targets.addedTile,
            hideGuideSegment(value, 0.74f, 0.77f),
        )
        else -> targets.addedTile
    }
}

private fun hideGuideInterpolate(
    start: HideGuidePoint,
    end: HideGuidePoint,
    progress: Float,
): HideGuidePoint = HideGuidePoint(
    x = start.x + (end.x - start.x) * progress,
    y = start.y + (end.y - start.y) * progress,
)

private fun hideGuideTapRipple(value: Float): Float {
    fun progressFrom(start: Float): Float {
        val duration = 0.016f
        if (value < start || value >= start + duration) return 0f
        return ((value - start) / duration).coerceIn(0f, 1f)
    }

    return maxOf(
        progressFrom(0.33f),
        progressFrom(0.41f),
        progressFrom(0.71f),
    )
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
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
                                MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp * 0.90f)
                            } else {
                                MaterialTheme.typography.titleSmall.copy(
                                    fontSize = MaterialTheme.typography.titleSmall.fontSize * 0.85f,
                                )
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

/**
 * سطر «اتجاه العرض» يعرض الاتجاه النشط ويفتح قائمة منسدلة في اليسار
 * بخيارَي (من اليمين لليسار) و(من اليسار لليمين) بنفس أسلوب «لون التمييز».
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LayoutDirectionSelector(
    layoutDirection: AppLayoutDirection,
    onLayoutDirectionChange: (AppLayoutDirection) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val contentLayoutDirection = LocalLayoutDirection.current
    val effectiveDirection = when (layoutDirection) {
        AppLayoutDirection.RTL -> AppLayoutDirection.RTL
        AppLayoutDirection.LTR -> AppLayoutDirection.LTR
        AppLayoutDirection.AUTO -> if (contentLayoutDirection == LayoutDirection.Rtl) {
            AppLayoutDirection.RTL
        } else {
            AppLayoutDirection.LTR
        }
    }
    val options = remember { listOf(AppLayoutDirection.RTL, AppLayoutDirection.LTR) }
    val currentLabel = stringResource(effectiveDirection.labelResource())
    val currentIcon = effectiveDirection.directionIcon()

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
                imageVector = Icons.Rounded.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_layout_direction),
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = currentIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentLabel,
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
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides contentLayoutDirection) {
                    options.forEach { option ->
                        val label = stringResource(option.labelResource())
                        val selected = effectiveDirection == option
                        DropdownMenuItem(
                            text = { Text(text = label, maxLines = 1) },
                            onClick = {
                                expanded = false
                                onLayoutDirectionChange(option)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = option.directionIcon(),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
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

private fun AppLayoutDirection.labelResource(): Int = when (this) {
    AppLayoutDirection.LTR -> R.string.layout_direction_ltr
    AppLayoutDirection.RTL, AppLayoutDirection.AUTO -> R.string.layout_direction_rtl
}

private fun AppLayoutDirection.directionIcon(): ImageVector = when (this) {
    AppLayoutDirection.LTR -> Icons.Rounded.East
    AppLayoutDirection.RTL, AppLayoutDirection.AUTO -> Icons.Rounded.West
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QuotaMetricColumn(
    label: String,
    value: String,
    horizontalAlignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        val valueDirection = if (value.endsWith("B")) {
            LayoutDirection.Ltr
        } else {
            LocalLayoutDirection.current
        }
        CompositionLocalProvider(LocalLayoutDirection provides valueDirection) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * شريط صغير وقصير شفاف أسفل بطاقة الباقة بداخله شرطة قصيرة تتحرك يمينًا ويسارًا
 * لتوضيح البطاقة النشطة (wifi / بيانات الهاتف).
 */
@Composable
private fun PlanCardPageIndicator(
    selectedNetwork: UsageNetwork,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (selectedNetwork == UsageNetwork.WIFI) 14.dp else 1.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "plan-card-indicator-offset",
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .width(28.dp)
                .height(7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                .clickable(role = Role.Button, onClick = onToggle),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .width(13.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun QuotaDialog(
    settings: AppSettings,
    initialNetwork: UsageNetwork = UsageNetwork.WIFI,
    initialPlanOverride: DataPlan? = null,
    isAddingNewPlan: Boolean = false,
    lockNetworkType: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (PlanDraft) -> Unit,
) {
    val context = LocalContext.current
    val initialNetworkType = when (initialNetwork) {
        UsageNetwork.WIFI -> PlanNetworkType.WIFI
        UsageNetwork.MOBILE -> PlanNetworkType.MOBILE
    }
    var networkType by rememberSaveable(initialNetworkType) { mutableStateOf(initialNetworkType) }
    val activePlanNetwork = when (networkType) {
        PlanNetworkType.MOBILE -> UsageNetwork.MOBILE
        PlanNetworkType.WIFI, PlanNetworkType.BOTH -> UsageNetwork.WIFI
    }
    val activePlan = when {
        initialPlanOverride != null -> initialPlanOverride
        isAddingNewPlan -> DataPlan()
        else -> settings.planFor(activePlanNetwork)
    }
    val hasActiveQuota = !isAddingNewPlan && activePlan.hasQuota
    val isEditLocked = hasActiveQuota && activePlan.preventEdit
    val stateKey = initialPlanOverride?.id ?: if (isAddingNewPlan) "new-${activePlanNetwork.name}" else activePlanNetwork.name

    val initialCapacityValue = remember(activePlan.quotaBytes, activePlan.capacityUnit) {
        if (activePlan.quotaBytes > 0L) {
            val raw = activePlan.quotaBytes.toDouble() / activePlan.capacityUnit.bytesFactor.toDouble()
            if (raw == raw.toLong().toDouble()) {
                raw.toLong().toString()
            } else {
                String.format(java.util.Locale.US, "%.2f", raw).trimEnd('0').trimEnd('.')
            }
        } else {
            ""
        }
    }

    var planName by rememberSaveable(stateKey, activePlan.name) { mutableStateOf(activePlan.name) }
    var logoPath by rememberSaveable(stateKey, activePlan.logoPath) { mutableStateOf(activePlan.logoPath) }
    var capacityText by rememberSaveable(stateKey, initialCapacityValue) {
        mutableStateOf(initialCapacityValue)
    }
    var capacityUnit by rememberSaveable(stateKey, activePlan.capacityUnit) {
        mutableStateOf(activePlan.capacityUnit)
    }
    var durationText by rememberSaveable(stateKey, activePlan.durationValue) {
        mutableStateOf(activePlan.durationValue.toString())
    }
    var durationUnit by rememberSaveable(stateKey, activePlan.durationUnit) {
        mutableStateOf(activePlan.durationUnit)
    }
    var preventEdit by rememberSaveable(stateKey, activePlan.preventEdit) {
        mutableStateOf(activePlan.preventEdit)
    }

    val logoBitmap = remember(logoPath) { loadPlanLogoBitmap(logoPath) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            savePickedPlanLogo(context, uri, activePlanNetwork)?.let { savedPath ->
                logoPath = savedPath
            }
        }
    }

    val parsedCapacity = capacityText.toDoubleOrNull()
    val parsedDuration = durationText.toIntOrNull()
    val canSave = isEditLocked || (
        (if (isAddingNewPlan) parsedCapacity != null && parsedCapacity > 0.0 else capacityText.isBlank() || (parsedCapacity != null && parsedCapacity >= 0.0)) &&
            (parsedDuration != null && parsedDuration >= 1)
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (hasActiveQuota) R.string.quota_dialog_title_update else R.string.quota_dialog_title_add,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    TextField(
                        value = planName,
                        onValueChange = { planName = it },
                        placeholder = { Text(text = stringResource(R.string.plan_name_label)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.plan_logo_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.size(48.dp)) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                    .clickable(role = Role.Button) { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (logoBitmap != null) {
                                    Image(
                                        bitmap = logoBitmap,
                                        contentDescription = stringResource(R.string.plan_logo_label),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = stringResource(R.string.plan_logo_label),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (logoBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-6).dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .clickable(role = Role.Button) { logoPath = "" },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.plan_logo_clear),
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = capacityText,
                        onValueChange = { newValue ->
                            capacityText = newValue.filter { it.isDigit() || it == '.' }
                        },
                        label = { Text(text = stringResource(R.string.plan_capacity_label)) },
                        enabled = !isEditLocked,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    PlanDropdownPill(
                        selectedLabel = stringResource(capacityUnit.labelResource()),
                        options = PlanCapacityUnit.entries,
                        optionLabel = { stringResource(it.labelResource()) },
                        selectedOption = capacityUnit,
                        onSelect = { capacityUnit = it },
                        enabled = !isEditLocked,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { newValue ->
                            durationText = newValue.filter { it.isDigit() }
                        },
                        label = { Text(text = stringResource(R.string.plan_duration_label)) },
                        enabled = !isEditLocked,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    PlanDropdownPill(
                        selectedLabel = stringResource(durationUnit.labelResource()),
                        options = PlanDurationUnit.entries,
                        optionLabel = { stringResource(it.labelResource()) },
                        selectedOption = durationUnit,
                        onSelect = { durationUnit = it },
                        enabled = !isEditLocked,
                    )
                }

                if (!lockNetworkType) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.plan_type_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        PlanDropdownPill(
                            selectedLabel = stringResource(networkType.labelResource()),
                            options = PlanNetworkType.entries,
                            optionLabel = { stringResource(it.labelResource()) },
                            selectedOption = networkType,
                            onSelect = { networkType = it },
                            enabled = !isEditLocked,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.plan_prevent_edit_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(
                        checked = preventEdit,
                        onCheckedChange = { preventEdit = it },
                        enabled = !isEditLocked,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        PlanDraft(
                            name = planName,
                            logoPath = logoPath,
                            capacityValue = parsedCapacity ?: 0.0,
                            capacityUnit = capacityUnit,
                            durationValue = (parsedDuration ?: 28).coerceAtLeast(1),
                            durationUnit = durationUnit,
                            networkType = networkType,
                            preventEdit = preventEdit,
                        ),
                    )
                },
                enabled = canSave,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AdditionalPlansSection(
    additionalPlans: List<AdditionalPlan>,
    onAddClick: () -> Unit,
    onEditClick: (AdditionalPlan) -> Unit,
    onRequestDelete: (AdditionalPlan) -> Unit,
    onReorder: (Int, Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val spacingPx = with(density) { 10.dp.toPx() }
    fun itemHeightDp(plan: AdditionalPlan?) = if (plan?.name?.isNotBlank() == true) 88.dp else 66.dp
    val step0To1Px = with(density) { itemHeightDp(additionalPlans.getOrNull(1)).toPx() + spacingPx }
    val step1To0Px = with(density) { itemHeightDp(additionalPlans.getOrNull(0)).toPx() + spacingPx }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var isSettlingSwap by remember { mutableStateOf(false) }
    val dragOffsetY = remember { Animatable(0f) }
    val otherOffsetY = remember { Animatable(0f) }
    var currentOtherTarget by remember { mutableStateOf(0f) }

    fun animateSwapByTap(index: Int) {
        if (additionalPlans.size <= 1 || isSettlingSwap || draggingIndex != null) return
        val targetIndex = if (index == 0) 1 else 0
        val dragTarget = if (index == 0) step0To1Px else -step1To0Px
        val otherTarget = if (index == 0) -step1To0Px else step0To1Px
        draggingIndex = index
        isSettlingSwap = true
        currentOtherTarget = otherTarget
        scope.launch {
            val job1 = launch {
                dragOffsetY.animateTo(
                    targetValue = dragTarget,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
            val job2 = launch {
                otherOffsetY.animateTo(
                    targetValue = otherTarget,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
            job1.join()
            job2.join()
            onReorder(index, targetIndex)
            dragOffsetY.snapTo(0f)
            otherOffsetY.snapTo(0f)
            currentOtherTarget = 0f
            draggingIndex = null
            isSettlingSwap = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.additional_plans_title),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (additionalPlans.size < MAX_ADDITIONAL_PLANS) {
                FilledTonalButton(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp),
                ) {
                    Text(
                        text = stringResource(R.string.plans_action_add),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        val showReorder = additionalPlans.size > 1
        additionalPlans.forEachIndexed { index, item ->
            key(item.id) {
                val isDraggingThis = draggingIndex == index
                val visualOffsetY = when (draggingIndex) {
                    null -> 0f
                    index -> dragOffsetY.value
                    else -> otherOffsetY.value
                }
                val dragTargetStepPx = if (index == 0) step0To1Px else step1To0Px
                val otherTargetStepPx = if (index == 0) -step1To0Px else step0To1Px
                AdditionalPlanRow(
                    plan = item,
                    showReorderHandle = showReorder,
                    isDragging = isDraggingThis && !isSettlingSwap,
                    visualOffsetY = visualOffsetY,
                    onEditClick = { onEditClick(item) },
                    onRequestDelete = { onRequestDelete(item) },
                    onTapSwapOrder = { animateSwapByTap(index) },
                    onVerticalDragStart = {
                        if (showReorder && !isSettlingSwap) {
                            draggingIndex = index
                            currentOtherTarget = 0f
                            scope.launch {
                                dragOffsetY.snapTo(0f)
                                otherOffsetY.snapTo(0f)
                            }
                        }
                    },
                    onVerticalDragDelta = { dy ->
                        if (showReorder && !isSettlingSwap && draggingIndex == index) {
                            val minDrag = if (index == 0) -dragTargetStepPx * 0.18f else -dragTargetStepPx * 1.12f
                            val maxDrag = if (index == 0) dragTargetStepPx * 1.12f else dragTargetStepPx * 0.18f
                            val nextDrag = (dragOffsetY.value + dy).coerceIn(minDrag, maxDrag)
                            scope.launch { dragOffsetY.snapTo(nextDrag) }
                            val willSwap = (index == 0 && nextDrag > dragTargetStepPx * 0.45f) ||
                                (index == 1 && nextDrag < -dragTargetStepPx * 0.45f)
                            val desiredOther = if (willSwap) otherTargetStepPx else 0f
                            if (desiredOther != currentOtherTarget) {
                                currentOtherTarget = desiredOther
                                scope.launch {
                                    otherOffsetY.animateTo(
                                        targetValue = desiredOther,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                }
                            }
                        }
                    },
                    onVerticalDragEnd = {
                        if (showReorder && !isSettlingSwap && draggingIndex == index) {
                            val currentDrag = dragOffsetY.value
                            val willSwap = (index == 0 && currentDrag > dragTargetStepPx * 0.45f) ||
                                (index == 1 && currentDrag < -dragTargetStepPx * 0.45f)
                            isSettlingSwap = true
                            if (willSwap) {
                                val targetIndex = if (index == 0) 1 else 0
                                val finalDrag = if (index == 0) dragTargetStepPx else -dragTargetStepPx
                                val finalOther = otherTargetStepPx
                                currentOtherTarget = finalOther
                                scope.launch {
                                    val job1 = launch {
                                        dragOffsetY.animateTo(
                                            targetValue = finalDrag,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium,
                                            ),
                                        )
                                    }
                                    val job2 = launch {
                                        otherOffsetY.animateTo(
                                            targetValue = finalOther,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium,
                                            ),
                                        )
                                    }
                                    job1.join()
                                    job2.join()
                                    onReorder(index, targetIndex)
                                    dragOffsetY.snapTo(0f)
                                    otherOffsetY.snapTo(0f)
                                    currentOtherTarget = 0f
                                    draggingIndex = null
                                    isSettlingSwap = false
                                }
                            } else {
                                currentOtherTarget = 0f
                                scope.launch {
                                    val job1 = launch {
                                        dragOffsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium,
                                            ),
                                        )
                                    }
                                    val job2 = launch {
                                        otherOffsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium,
                                            ),
                                        )
                                    }
                                    job1.join()
                                    job2.join()
                                    draggingIndex = null
                                    isSettlingSwap = false
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AdditionalPlanRow(
    plan: AdditionalPlan,
    showReorderHandle: Boolean,
    isDragging: Boolean,
    visualOffsetY: Float,
    onEditClick: () -> Unit,
    onRequestDelete: () -> Unit,
    onTapSwapOrder: () -> Unit,
    onVerticalDragStart: () -> Unit,
    onVerticalDragDelta: (Float) -> Unit,
    onVerticalDragEnd: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val maxRevealPx = with(density) { 96.dp.toPx() }
    val deleteThresholdPx = with(density) { 64.dp.toPx() }
    val peekRevealPx = with(density) { 44.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val logoBitmap = remember(plan.logoPath) { loadPlanLogoBitmap(plan.logoPath) }
    val durationLabel = "${plan.durationValue} ${stringResource(plan.durationUnit.labelResource())}"
    val capacityLabel = Format.bytes(plan.quotaBytes)
    val deleteRed = Color(0xFFEF5350)
    val hasPlanName = plan.name.isNotBlank()
    val rowHeight = if (hasPlanName) 88.dp else 66.dp
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "additionalPlanDragScale",
    )
    val currentOnVerticalDragStart by rememberUpdatedState(onVerticalDragStart)
    val currentOnVerticalDragDelta by rememberUpdatedState(onVerticalDragDelta)
    val currentOnVerticalDragEnd by rememberUpdatedState(onVerticalDragEnd)

    // نستخدم تخطيط LTR فيزيائي ثابت حتى يكون الشريط الأحمر دائمًا في أقصى اليسار
    // ويكون السحب لليمين دائمًا لتوسيع الشريط الأحمر وحذف الباقة الإضافية.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val currentOffsetDp = with(density) { offsetX.value.coerceAtLeast(0f).toDp() }
        val redBarWidthDp = 10.dp + currentOffsetDp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .zIndex(if (isDragging || visualOffsetY != 0f) 1f else 0f),
        ) {
            // الطبقة المرئية للبطاقة تتحرك مع إصبع المستخدم عبر graphicsLayer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(plan.id) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val next = (offsetX.value + dragAmount).coerceIn(0f, maxRevealPx * 1.15f)
                                scope.launch { offsetX.snapTo(next) }
                            },
                            onDragEnd = {
                                val triggered = offsetX.value >= deleteThresholdPx
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                }
                                if (triggered) {
                                    onRequestDelete()
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                }
                            },
                        )
                    }
                    .graphicsLayer {
                        translationY = visualOffsetY
                        scaleX = dragScale
                        scaleY = dragScale
                        shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                        shape = RoundedCornerShape(16.dp)
                        clip = true
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                // الشريط الأحمر في أقصى اليسار: رفيع في الحالة العادية، ويتوسع لليمين عند السحب،
                // وعند الضغط عليه مرة واحدة يتحرك قليلًا لليمين ليظهر جزء من سلة المهملات ثم يعود بتأثير مطاطي.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(redBarWidthDp)
                        .fillMaxHeight()
                        .background(deleteRed)
                        .clickable(role = Role.Button) {
                            if (offsetX.value >= deleteThresholdPx * 0.7f) {
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow,
                                        ),
                                    )
                                }
                                onRequestDelete()
                            } else {
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = peekRevealPx,
                                        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                                    )
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow,
                                        ),
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (redBarWidthDp > 18.dp) {
                        val revealAlpha = ((offsetX.value / peekRevealPx).coerceIn(0f, 1f))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.graphicsLayer { alpha = revealAlpha },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.plans_action_delete),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                            if (redBarWidthDp > 48.dp) {
                                Text(
                                    text = stringResource(R.string.plans_action_delete),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                // محتوى صف الباقة الإضافية يتحرك لليمين مع توسع الشريط الأحمر
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = redBarWidthDp + 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.plans_action_edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp),
                        )
                    }

                    if (showReorderHandle) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = stringResource(R.string.additional_plan_reorder_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (logoBitmap != null) {
                                Image(
                                    bitmap = logoBitmap,
                                    contentDescription = stringResource(R.string.plan_logo_label),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                if (hasPlanName) {
                                    Text(
                                        text = plan.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    QuotaMetricColumn(
                                        label = stringResource(R.string.plan_capacity_label),
                                        value = capacityLabel,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f),
                                    )
                                    QuotaMetricColumn(
                                        label = stringResource(R.string.plan_duration_label),
                                        value = durationLabel,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // منطقة لمس ثابتة لزر (=) خارج graphicsLayer حتى تتبع البطاقة حركة الإصبع بسلاسة تامة دون اهتزاز الإحداثيات
            if (showReorderHandle) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = redBarWidthDp + 40.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onTapSwapOrder)
                        .pointerInput(plan.id) {
                            detectVerticalDragGestures(
                                onDragStart = { currentOnVerticalDragStart() },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    currentOnVerticalDragDelta(dragAmount)
                                },
                                onDragEnd = { currentOnVerticalDragEnd() },
                                onDragCancel = { currentOnVerticalDragEnd() },
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun AdditionalPlanDialog(
    network: UsageNetwork,
    initialPlan: AdditionalPlan? = null,
    onDismiss: () -> Unit,
    onConfirm: (AdditionalPlanDraft) -> Unit,
) {
    val context = LocalContext.current
    val isEditing = initialPlan != null
    val isEditLocked = initialPlan?.preventEdit == true
    val stateKey = initialPlan?.id ?: "new-additional-${network.name}"

    val initialCapacityText = remember(initialPlan?.quotaBytes, initialPlan?.capacityUnit) {
        if (initialPlan != null && initialPlan.quotaBytes > 0L) {
            val raw = initialPlan.quotaBytes.toDouble() / initialPlan.capacityUnit.bytesFactor.toDouble()
            if (raw == raw.toLong().toDouble()) {
                raw.toLong().toString()
            } else {
                String.format(java.util.Locale.US, "%.2f", raw).trimEnd('0').trimEnd('.')
            }
        } else {
            ""
        }
    }

    var planName by rememberSaveable(stateKey) { mutableStateOf(initialPlan?.name ?: "") }
    var logoPath by rememberSaveable(stateKey) { mutableStateOf(initialPlan?.logoPath ?: "") }
    var capacityText by rememberSaveable(stateKey) { mutableStateOf(initialCapacityText) }
    var capacityUnit by rememberSaveable(stateKey) {
        mutableStateOf(initialPlan?.capacityUnit ?: PlanCapacityUnit.MB)
    }
    var durationText by rememberSaveable(stateKey) {
        mutableStateOf((initialPlan?.durationValue ?: 7).toString())
    }
    var durationUnit by rememberSaveable(stateKey) {
        mutableStateOf(initialPlan?.durationUnit ?: PlanDurationUnit.DAY)
    }
    var preventEdit by rememberSaveable(stateKey) {
        mutableStateOf(initialPlan?.preventEdit ?: false)
    }

    val logoBitmap = remember(logoPath) { loadPlanLogoBitmap(logoPath) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            savePickedPlanLogo(context, uri, network)?.let { savedPath ->
                logoPath = savedPath
            }
        }
    }

    val parsedCapacity = capacityText.toDoubleOrNull()
    val parsedDuration = durationText.toIntOrNull()
    val canSave = isEditLocked || (
        parsedCapacity != null && parsedCapacity > 0.0 &&
            parsedDuration != null && parsedDuration >= 1
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (isEditing) {
                        R.string.additional_plan_dialog_title_update
                    } else {
                        R.string.additional_plan_dialog_title_add
                    },
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.size(56.dp)) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable(role = Role.Button) { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (logoBitmap != null) {
                                Image(
                                    bitmap = logoBitmap,
                                    contentDescription = stringResource(R.string.plan_logo_label),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.plan_logo_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (logoBitmap != null) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                )
                                .clickable(role = Role.Button) {
                                    if (logoBitmap != null) {
                                        logoPath = ""
                                    } else {
                                        imagePickerLauncher.launch("image/*")
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (logoBitmap != null) Icons.Rounded.Close else Icons.Rounded.CameraAlt,
                                contentDescription = stringResource(R.string.plan_logo_label),
                                tint = if (logoBitmap != null) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                },
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }

                    OutlinedTextField(
                        value = planName,
                        onValueChange = { planName = it },
                        placeholder = { Text(text = stringResource(R.string.additional_plan_name_optional)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    text = stringResource(R.string.plan_capacity_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = capacityText,
                        onValueChange = { newValue ->
                            capacityText = newValue.filter { it.isDigit() || it == '.' }
                        },
                        enabled = !isEditLocked,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    PlanDropdownPill(
                        selectedLabel = stringResource(capacityUnit.labelResource()),
                        options = PlanCapacityUnit.entries,
                        optionLabel = { stringResource(it.labelResource()) },
                        selectedOption = capacityUnit,
                        onSelect = { capacityUnit = it },
                        enabled = !isEditLocked,
                    )
                }

                Text(
                    text = stringResource(R.string.plan_duration_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { newValue ->
                            durationText = newValue.filter { it.isDigit() }
                        },
                        enabled = !isEditLocked,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    PlanDropdownPill(
                        selectedLabel = stringResource(durationUnit.labelResource()),
                        options = PlanDurationUnit.entries,
                        optionLabel = { stringResource(it.labelResource()) },
                        selectedOption = durationUnit,
                        onSelect = { durationUnit = it },
                        enabled = !isEditLocked,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.plan_prevent_edit_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.additional_plan_prevent_edit_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = preventEdit,
                        onCheckedChange = { preventEdit = it },
                        enabled = !isEditLocked,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safeCapacity = if (isEditLocked && initialPlan != null) {
                        initialPlan.quotaBytes.toDouble() / initialPlan.capacityUnit.bytesFactor.toDouble()
                    } else {
                        parsedCapacity ?: 0.0
                    }
                    onConfirm(
                        AdditionalPlanDraft(
                            name = planName,
                            logoPath = logoPath,
                            capacityValue = safeCapacity,
                            capacityUnit = capacityUnit,
                            durationValue = (parsedDuration ?: 7).coerceAtLeast(1),
                            durationUnit = durationUnit,
                            preventEdit = preventEdit,
                        ),
                    )
                },
                enabled = canSave,
                shape = AppButtonShape,
            ) {
                Text(text = stringResource(R.string.quota_update_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = AppButtonShape) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlansListDialog(
    network: UsageNetwork,
    activePlan: DataPlan,
    savedPlans: List<DataPlan>,
    onDismiss: () -> Unit,
    onActivateCurrentPlan: () -> Unit,
    onEditActivePlan: () -> Unit,
    onAddAdditionalPlanToActive: () -> Unit,
    onSwitchToSavedPlan: (String) -> Unit,
    onEditSavedPlan: (String) -> Unit,
    onDeletePlan: (String) -> Unit,
    onAddNewPlan: () -> Unit,
) {
    var pendingSwitchPlanId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeletePlanId by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.plans_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(
                        if (network == UsageNetwork.WIFI) {
                            R.string.plan_card_network_wifi
                        } else {
                            R.string.plan_card_network_mobile
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!activePlan.hasQuota && savedPlans.isEmpty()) {
                    Text(
                        text = stringResource(R.string.plans_empty_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (activePlan.hasQuota) {
                    PlanListItemCard(
                        plan = activePlan,
                        isActivePlan = true,
                        onPrimaryAction = if (activePlan.pendingActivation) onActivateCurrentPlan else null,
                        primaryActionLabel = stringResource(R.string.quota_activate_action),
                        onAddAdditionalClick = if (activePlan.additionalPlans.size < MAX_ADDITIONAL_PLANS) {
                            onAddAdditionalPlanToActive
                        } else {
                            null
                        },
                        onEditClick = onEditActivePlan,
                        onDeleteClick = { pendingDeletePlanId = activePlan.id },
                    )
                }

                if (savedPlans.isNotEmpty()) {
                    savedPlans.forEach { saved ->
                        key(saved.id) {
                            PlanListItemCard(
                                plan = saved,
                                isActivePlan = false,
                                onPrimaryAction = {
                                    if (activePlan.hasQuota) {
                                        pendingSwitchPlanId = saved.id
                                    } else {
                                        onSwitchToSavedPlan(saved.id)
                                    }
                                },
                                primaryActionLabel = stringResource(R.string.plans_action_switch),
                                onAddAdditionalClick = null,
                                onEditClick = { onEditSavedPlan(saved.id) },
                                onDeleteClick = { pendingDeletePlanId = saved.id },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAddNewPlan,
                shape = AppButtonShape,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.plans_action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = AppButtonShape) {
                Text(text = stringResource(R.string.action_close))
            }
        },
    )

    if (pendingSwitchPlanId != null) {
        AlertDialog(
            onDismissRequest = { pendingSwitchPlanId = null },
            icon = { Icon(imageVector = Icons.Rounded.SwapHoriz, contentDescription = null) },
            title = { Text(text = stringResource(R.string.plans_switch_confirm_title)) },
            text = { Text(text = stringResource(R.string.plans_switch_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = pendingSwitchPlanId
                        pendingSwitchPlanId = null
                        if (targetId != null) {
                            onSwitchToSavedPlan(targetId)
                        }
                    },
                    shape = AppButtonShape,
                ) {
                    Text(text = stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingSwitchPlanId = null },
                    shape = AppButtonShape,
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (pendingDeletePlanId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeletePlanId = null },
            icon = { Icon(imageVector = Icons.Rounded.Delete, contentDescription = null) },
            title = { Text(text = stringResource(R.string.plans_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.plans_delete_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = pendingDeletePlanId
                        pendingDeletePlanId = null
                        if (targetId != null) {
                            onDeletePlan(targetId)
                        }
                    },
                    shape = AppButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(text = stringResource(R.string.plans_action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeletePlanId = null },
                    shape = AppButtonShape,
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlanListItemCard(
    plan: DataPlan,
    isActivePlan: Boolean,
    onPrimaryAction: (() -> Unit)?,
    primaryActionLabel: String,
    onAddAdditionalClick: (() -> Unit)?,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val logoBitmap = remember(plan.logoPath) { loadPlanLogoBitmap(plan.logoPath) }
    val durationText = "${plan.durationValue} ${stringResource(plan.durationUnit.labelResource())}"
    val capacityText = Format.bytes(plan.quotaBytes)
    val hasPlanName = plan.name.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isActivePlan) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            )
            .border(
                width = 1.dp,
                color = if (isActivePlan) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(16.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (logoBitmap != null) {
                    Image(
                        bitmap = logoBitmap,
                        contentDescription = stringResource(R.string.plan_logo_label),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (hasPlanName) {
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val infoStyle = if (hasPlanName) {
                            MaterialTheme.typography.bodySmall
                        } else {
                            MaterialTheme.typography.titleSmall
                        }
                        val infoColor = if (hasPlanName) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = capacityText,
                                style = infoStyle,
                                color = infoColor,
                                maxLines = 1,
                            )
                        }
                        Text(
                            text = "•",
                            style = infoStyle,
                            color = infoColor,
                            maxLines = 1,
                        )
                        Text(
                            text = durationText,
                            style = infoStyle,
                            color = infoColor,
                            maxLines = 1,
                        )
                    }
                }
            }

            if (isActivePlan) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (plan.pendingActivation) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (plan.pendingActivation) {
                                R.string.plans_status_pending
                            } else {
                                R.string.plans_status_active
                            },
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (plan.pendingActivation) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (onPrimaryAction != null) {
                Button(
                    onClick = onPrimaryAction,
                    shape = AppButtonShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp),
                ) {
                    Text(
                        text = primaryActionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }

            if (onAddAdditionalClick != null) {
                FilledTonalButton(
                    onClick = onAddAdditionalClick,
                    shape = AppButtonShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.plans_action_add_additional),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.plans_action_edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.plans_action_delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun <T> PlanDropdownPill(
    selectedLabel: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    selectedOption: T,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(
                    width = 1.dp,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    },
                    shape = RoundedCornerShape(50),
                )
                .clickable(enabled = enabled, role = Role.Button) { expanded = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                val label = optionLabel(option)
                DropdownMenuItem(
                    text = { Text(text = label, maxLines = 1) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    trailingIcon = if (option == selectedOption) {
                        { Icon(imageVector = Icons.Rounded.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

private fun PlanCapacityUnit.labelResource(): Int = when (this) {
    PlanCapacityUnit.MB -> R.string.plan_unit_mb
    PlanCapacityUnit.GB -> R.string.plan_unit_gb
    PlanCapacityUnit.TB -> R.string.plan_unit_tb
}

private fun PlanDurationUnit.labelResource(): Int = when (this) {
    PlanDurationUnit.DAY -> R.string.plan_duration_day
    PlanDurationUnit.WEEK -> R.string.plan_duration_week
    PlanDurationUnit.MONTH -> R.string.plan_duration_month
    PlanDurationUnit.YEAR -> R.string.plan_duration_year
}

private fun PlanNetworkType.labelResource(): Int = when (this) {
    PlanNetworkType.WIFI -> R.string.plan_network_wifi
    PlanNetworkType.MOBILE -> R.string.plan_network_mobile
    PlanNetworkType.BOTH -> R.string.plan_network_both
}

private fun loadPlanLogoBitmap(path: String): ImageBitmap? {
    if (path.isBlank()) return null
    return runCatching {
        val file = File(path)
        if (!file.exists()) return null
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    }.getOrNull()
}

private fun savePickedPlanLogo(
    context: Context,
    uri: Uri,
    network: UsageNetwork = UsageNetwork.WIFI,
): String? = runCatching {
    val input = context.contentResolver.openInputStream(uri) ?: return null
    val bitmap = input.use { BitmapFactory.decodeStream(it) } ?: return null
    val maxSide = 256
    val scaled = if (bitmap.width > maxSide || bitmap.height > maxSide) {
        val ratio = minOf(maxSide.toFloat() / bitmap.width, maxSide.toFloat() / bitmap.height)
        val width = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(bitmap, width, height, true)
    } else {
        bitmap
    }
    val dir = File(context.filesDir, "plan_logos").apply { mkdirs() }
    val prefix = "plan_logo_${network.name.lowercase()}_"
    val outFile = File(dir, "$prefix${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}.png")
    FileOutputStream(outFile).use { out ->
        scaled.compress(Bitmap.CompressFormat.PNG, 90, out)
    }
    val existingFiles = dir.listFiles()?.sortedByDescending { it.lastModified() }.orEmpty()
    if (existingFiles.size > 32) {
        existingFiles.drop(32).forEach { it.delete() }
    }
    outFile.absolutePath
}.getOrNull()
