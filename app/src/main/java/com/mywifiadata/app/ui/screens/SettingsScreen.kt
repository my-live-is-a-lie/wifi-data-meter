package com.mywifiadata.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.SamplingIntervalUnit
import com.mywifiadata.app.core.SamplingIntervals
import com.mywifiadata.app.data.model.ThemeMode
import com.mywifiadata.app.ui.UiState
import com.mywifiadata.app.ui.components.ExpressiveCard
import com.mywifiadata.app.ui.components.InfoRow
import com.mywifiadata.app.ui.components.SectionHeader
import com.mywifiadata.app.ui.components.SettingSwitchRow
import com.mywifiadata.app.ui.theme.AppButtonShape
import kotlin.math.abs
import kotlin.math.roundToInt
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
    // قسم «المزيد» فارغ حاليًا ومهيأ لإضافة إعدادات مخفية إليه لاحقًا.
    var moreExpanded by rememberSaveable { mutableStateOf(false) }
    val settings = state.settings

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
            ExpressiveCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = stringResource(R.string.settings_section_appearance))
                    ThemeModeSelector(
                        themeMode = settings.themeMode,
                        onThemeModeChange = onThemeModeChange,
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
                    // رأس القسم نفسه هو زر التوسيع/الطيّ، ومحتواه فارغ حاليًا.
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
                            // موضع فارغ لإعدادات إضافية مستقبلًا.
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
