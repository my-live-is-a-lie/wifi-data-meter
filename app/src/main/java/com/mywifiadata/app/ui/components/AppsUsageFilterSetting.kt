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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                        modifier = Modifier.weight(1f),
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
        val amountFieldShape = RoundedCornerShape(8.dp)
        Box(
            modifier = Modifier
                .width(58.dp)
                .height(40.dp)
                .clip(amountFieldShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outline, amountFieldShape)
                .semantics { contentDescription = amountAccessibilityLabel },
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                value = amountText,
                onValueChange = { entered ->
                    val digits = entered.filter(Char::isDigit).take(10)
                    amountText = digits
                    digits.toIntOrNull()?.let(onAmountChange)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                textStyle = MaterialTheme.typography.titleMediumEmphasized.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        innerTextField()
                    }
                },
            )
        }
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

    // The gesture controls are directional (left lowers, right raises) in both app languages.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(percentageBadgeColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.apps_usage_filter_percent_value, percentage),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                AcceleratingPercentSlider(
                    value = percentage,
                    onValueChange = onPercentageChange,
                    decreaseLabel = decreaseLabel,
                    increaseLabel = increaseLabel,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PercentStepButton(
                    icon = Icons.Rounded.Remove,
                    label = decreaseLabel,
                    onClick = { onPercentageChange((percentage - 1).coerceAtLeast(1)) },
                )
                PercentStepButton(
                    icon = Icons.Rounded.Add,
                    label = increaseLabel,
                    onClick = { onPercentageChange((percentage + 1).coerceAtMost(30)) },
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
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .semantics { contentDescription = label },
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF3F455A)
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AcceleratingPercentSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    decreaseLabel: String,
    increaseLabel: String,
) {
    var widthPx by remember { mutableIntStateOf(0) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var dragStartValue by remember { mutableIntStateOf(value) }
    val density = LocalDensity.current
    val thumbRadiusPx = with(density) { 12.dp.toPx() }
    val currentValue by rememberUpdatedState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val description = stringResource(R.string.apps_usage_filter_slider_description)
    val valueDescription = stringResource(R.string.apps_usage_filter_percent_value, value)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(widthPx, thumbRadiusPx) {
                detectDragGestures(
                    onDragStart = { touch ->
                        dragStartValue = currentValue
                        val maxOffset = (size.width / 2f - thumbRadiusPx).coerceAtLeast(0f)
                        dragOffsetPx = (touch.x - size.width / 2f).coerceIn(-maxOffset, maxOffset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val maxOffset = (size.width / 2f - thumbRadiusPx).coerceAtLeast(0f)
                        val offset = (change.position.x - size.width / 2f).coerceIn(-maxOffset, maxOffset)
                        dragOffsetPx = offset
                        val fraction = if (maxOffset > 0f) (abs(offset) / maxOffset).coerceIn(0f, 1f) else 0f
                        val movingRight = offset >= 0f
                        val availableSteps = if (movingRight) 30 - dragStartValue else dragStartValue - 1
                        val steps = (availableSteps * fraction * fraction).roundToInt()
                        val next = if (movingRight) dragStartValue + steps else dragStartValue - steps
                        latestOnValueChange(next.coerceIn(1, 30))
                    },
                    onDragEnd = { dragOffsetPx = 0f },
                    onDragCancel = { dragOffsetPx = 0f },
                )
            }
            .semantics {
                contentDescription = description
                stateDescription = valueDescription
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 1f..30f, 0)
                customActions = listOf(
                    CustomAccessibilityAction(decreaseLabel) {
                        if (currentValue <= 1) false else {
                            latestOnValueChange(currentValue - 1)
                            true
                        }
                    },
                    CustomAccessibilityAction(increaseLabel) {
                        if (currentValue >= 30) false else {
                            latestOnValueChange(currentValue + 1)
                            true
                        }
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
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
