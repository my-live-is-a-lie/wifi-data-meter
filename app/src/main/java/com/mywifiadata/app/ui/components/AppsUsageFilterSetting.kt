package com.mywifiadata.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.appsUsageFilterRepeatIntervalMillis
import com.mywifiadata.app.core.stepAppsUsageFilterPercent
import com.mywifiadata.app.data.model.AppsUsageFilterMode
import com.mywifiadata.app.data.model.PlanCapacityUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Expandable settings row for hiding low-usage apps from the Apps tab.
 * The master switch controls filtering; the chevron independently opens the controls.
 */
@Composable
fun AppsUsageFilterSetting(
    enabled: Boolean,
    mode: AppsUsageFilterMode,
    amount: Int,
    unit: PlanCapacityUnit,
    percentage: Int,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (AppsUsageFilterMode) -> Unit,
    onAmountChange: (Int) -> Unit,
    onUnitChange: (PlanCapacityUnit) -> Unit,
    onPercentageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else 90f,
        label = "appsUsageFilterChevron",
    )
    val title = stringResource(R.string.apps_usage_filter_title)
    val expandCollapseDescription = stringResource(
        if (expanded) R.string.apps_usage_filter_collapse else R.string.apps_usage_filter_expand,
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .padding(vertical = 2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.apps_usage_filter_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = expandCollapseDescription },
            ) {
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.semantics { contentDescription = title },
            )
        }

        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = if (mode == AppsUsageFilterMode.PERCENTAGE) {
                    Alignment.CenterVertically
                } else {
                    Alignment.Top
                },
                horizontalArrangement = if (mode == AppsUsageFilterMode.PERCENTAGE) {
                    Arrangement.SpaceBetween
                } else {
                    Arrangement.spacedBy(4.dp)
                },
            ) {
                FilterModeDropdown(
                    mode = mode,
                    onModeChange = onModeChange,
                    modifier = Modifier.width(112.dp),
                )
                when (mode) {
                    AppsUsageFilterMode.AMOUNT -> AmountFilterControls(
                        amount = amount,
                        unit = unit,
                        onAmountChange = onAmountChange,
                        onUnitChange = onUnitChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 25.dp),
                    )
                    AppsUsageFilterMode.PERCENTAGE -> PercentageFilterControls(
                        percentage = percentage.coerceIn(1, 30),
                        onPercentageChange = onPercentageChange,
                        modifier = Modifier.width(106.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterModeDropdown(
    mode: AppsUsageFilterMode,
    onModeChange: (AppsUsageFilterMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modeOptions = listOf(
        AppsUsageFilterMode.AMOUNT to stringResource(R.string.apps_usage_filter_mode_amount),
        AppsUsageFilterMode.PERCENTAGE to stringResource(R.string.apps_usage_filter_mode_percentage),
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.apps_usage_filter_mode_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(6.dp))
        FilterOptionPill(
            selectedLabel = modeOptions.first { it.first == mode }.second,
            options = modeOptions,
            selectedOption = mode,
            onSelect = onModeChange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AmountFilterControls(
    amount: Int,
    unit: PlanCapacityUnit,
    onAmountChange: (Int) -> Unit,
    onUnitChange: (PlanCapacityUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var amountText by rememberSaveable { mutableStateOf(amount.toString()) }
    val amountAccessibilityLabel = stringResource(R.string.apps_usage_filter_amount_accessibility)

    LaunchedEffect(amount) {
        if (amountText.toIntOrNull() != amount) amountText = amount.toString()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = amountText,
            onValueChange = { entered: String ->
                val digits = entered.filter(Char::isDigit).take(10)
                amountText = digits
                digits.toIntOrNull()?.let(onAmountChange)
            },
            modifier = Modifier
                .width(62.dp)
                .heightIn(min = 44.dp)
                .semantics { contentDescription = amountAccessibilityLabel },
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        FilterOptionPill(
            selectedLabel = stringResource(unit.labelResource()),
            options = PlanCapacityUnit.entries.map { it to stringResource(it.labelResource()) },
            selectedOption = unit,
            onSelect = onUnitChange,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PercentageFilterControls(
    percentage: Int,
    onPercentageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val decreaseLabel = stringResource(R.string.apps_usage_filter_decrease)
    val increaseLabel = stringResource(R.string.apps_usage_filter_increase)
    val darkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val percentageBadgeColor = if (darkSurface) Color(0xFF1D202A) else MaterialTheme.colorScheme.surfaceContainerHigh

    // Keep the number/track/buttons in the compact proportions of the supplied design.
    // The inner layout is LTR in Arabic too: right increases and left decreases.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.width(68.dp)) {
                Box(
                    modifier = Modifier
                        .width(68.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(percentageBadgeColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.apps_usage_filter_percent_value, percentage),
                        style = MaterialTheme.typography.bodyLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                AutoRepeatPercentSlider(
                    value = percentage,
                    onValueChange = onPercentageChange,
                    decreaseLabel = decreaseLabel,
                    increaseLabel = increaseLabel,
                )
            }
            Column(
                modifier = Modifier.width(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PercentStepButton(
                    icon = Icons.Rounded.Add,
                    label = increaseLabel,
                    onClick = { onPercentageChange(stepAppsUsageFilterPercent(percentage, increase = true)) },
                )
                PercentStepButton(
                    icon = Icons.Rounded.Remove,
                    label = decreaseLabel,
                    onClick = { onPercentageChange(stepAppsUsageFilterPercent(percentage, increase = false)) },
                )
            }
        }
    }
}

@Composable
private fun PercentStepButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(
                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF3F455A)
                else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun AutoRepeatPercentSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    decreaseLabel: String,
    increaseLabel: String,
) {
    var widthPx by remember { mutableIntStateOf(0) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var lastStepDirection by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val thumbRadiusPx = with(density) { 12.dp.toPx() }
    val trackInsetPx = with(density) { 3.dp.toPx() }
    val centerDeadZonePx = with(density) { 3.dp.toPx() }
    val edgeThresholdPx = with(density) { 4.dp.toPx() }
    val currentValue by rememberUpdatedState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val description = stringResource(R.string.apps_usage_filter_slider_description)
    val valueDescription = stringResource(R.string.apps_usage_filter_percent_value, value)

    // Holding the handle away from center repeats one step at a controlled, distance-based pace.
    // The center dead zone pauses immediately; the modest 450–900 ms interval keeps adjustment precise.
    LaunchedEffect(widthPx, thumbRadiusPx, trackInsetPx, centerDeadZonePx) {
        val maxOffset = (widthPx / 2f - trackInsetPx - thumbRadiusPx).coerceAtLeast(0f)
        val activeRange = (maxOffset - centerDeadZonePx).coerceAtLeast(0f)
        val tickMillis = 50L
        var elapsedMillis = 0L
        var previousIncrease: Boolean? = null

        while (true) {
            val offset = dragOffsetPx
            val offsetDistance = abs(offset)
            if (activeRange <= 0f || offsetDistance <= centerDeadZonePx) {
                elapsedMillis = 0L
                previousIncrease = null
                lastStepDirection = 0
                delay(tickMillis)
                continue
            }

            val increase = offset < 0f
            if (previousIncrease != increase) {
                elapsedMillis = 0L
                previousIncrease = increase
            }
            val distanceFraction = ((offsetDistance - centerDeadZonePx) / activeRange).coerceIn(0f, 1f)
            val intervalMillis = appsUsageFilterRepeatIntervalMillis(distanceFraction)

            elapsedMillis += tickMillis
            if (elapsedMillis >= intervalMillis) {
                val currentOffset = dragOffsetPx
                val currentIncrease = currentOffset < 0f
                if (abs(currentOffset) <= centerDeadZonePx || currentIncrease != previousIncrease) {
                    elapsedMillis = 0L
                    previousIncrease = if (abs(currentOffset) <= centerDeadZonePx) null else currentIncrease
                } else {
                    elapsedMillis -= intervalMillis
                    lastStepDirection = if (currentIncrease) 1 else -1
                    latestOnValueChange(stepAppsUsageFilterPercent(currentValue, increase = currentIncrease))
                }
            }
            delay(tickMillis)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(widthPx, thumbRadiusPx, trackInsetPx, edgeThresholdPx) {
                detectDragGestures(
                    onDragStart = { touch ->
                        lastStepDirection = 0
                        val maxOffset = (size.width / 2f - trackInsetPx - thumbRadiusPx).coerceAtLeast(0f)
                        dragOffsetPx = (touch.x - size.width / 2f).coerceIn(-maxOffset, maxOffset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val maxOffset = (size.width / 2f - trackInsetPx - thumbRadiusPx).coerceAtLeast(0f)
                        dragOffsetPx = (change.position.x - size.width / 2f).coerceIn(-maxOffset, maxOffset)
                    },
                    onDragEnd = {
                        val maxOffset = (size.width / 2f - trackInsetPx - thumbRadiusPx).coerceAtLeast(0f)
                        val releasedOffset = dragOffsetPx
                        val reachedExtreme = maxOffset > edgeThresholdPx &&
                            abs(releasedOffset) >= maxOffset - edgeThresholdPx
                        val releaseIncrease = releasedOffset < 0f
                        val releaseDirection = if (releaseIncrease) 1 else -1
                        if (reachedExtreme && lastStepDirection != releaseDirection) {
                            latestOnValueChange(
                                stepAppsUsageFilterPercent(currentValue, increase = releaseIncrease),
                            )
                        }
                        dragOffsetPx = 0f
                        lastStepDirection = 0
                    },
                    onDragCancel = {
                        dragOffsetPx = 0f
                        lastStepDirection = 0
                    },
                )
            }
            .semantics {
                contentDescription = description
                stateDescription = valueDescription
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 1f..30f, 0)
                customActions = listOf(
                    CustomAccessibilityAction(decreaseLabel) {
                        latestOnValueChange(stepAppsUsageFilterPercent(currentValue, increase = false))
                        true
                    },
                    CustomAccessibilityAction(increaseLabel) {
                        latestOnValueChange(stepAppsUsageFilterPercent(currentValue, increase = true))
                        true
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 3.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF3F455A)
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(dragOffsetPx.roundToInt(), 0) }
                .size(width = 24.dp, height = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFDDE4FF)
                    else MaterialTheme.colorScheme.primary,
                ),
        )
    }
}

@Composable
private fun <T> FilterOptionPill(
    selectedLabel: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f), shape)
                .clickable(role = Role.Button) { expanded = true }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodyLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (option, label) ->
                DropdownMenuItem(
                    text = { Text(text = label) },
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
