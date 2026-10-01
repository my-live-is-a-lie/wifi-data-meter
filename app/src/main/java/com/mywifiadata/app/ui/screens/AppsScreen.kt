package com.mywifiadata.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.TimeRange
import com.mywifiadata.app.core.UsagePeriod
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.AppsFilter
import com.mywifiadata.app.data.model.UsageSlice
import com.mywifiadata.app.ui.UiState
import com.mywifiadata.app.ui.components.AppUsageRow
import com.mywifiadata.app.ui.components.EmptyState
import com.mywifiadata.app.ui.components.ExpressiveCard
import com.mywifiadata.app.ui.components.InfoRow
import com.mywifiadata.app.ui.components.PermissionCard
import com.mywifiadata.app.ui.components.SectionHeader
import com.mywifiadata.app.ui.theme.LocalDownloadColor
import com.mywifiadata.app.ui.theme.LocalUploadColor

/**
 * شاشة التطبيقات: ترتيب التطبيقات حسب استهلاك الواي فاي في الفترة المختارة،
 * مع تفصيل زمني (ساعات اليوم / أيام الأسبوع / آخر 4 أسابيع) يمكن الضغط عليه
 * لعرض تطبيقات تلك الشريحة، وفلترة حسب نوع التطبيق.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppsScreen(
    state: UiState,
    onPeriodChange: (UsagePeriod) -> Unit,
    onAppsFilterChange: (AppsFilter) -> Unit,
    onAppsWeekChange: (TimeRange) -> Unit,
    onAppsSliceChange: (TimeRange) -> Unit,
    onOpenUsageAccess: () -> Unit,
    onAppClick: (AppUsage) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val apps = state.visibleApps
    val totalBytes = apps.sumOf { it.totals.totalBytes }.coerceAtLeast(1L)

    // تسميات الشرائح: ساعة "14:00 – 15:00"، يوم "الأحد، 28 سبتمبر"، أسبوع "21 – 27 سبتمبر".
    val hourLabel: (UsageSlice) -> String = { slice ->
        "${Format.clock(slice.startMillis)} – ${Format.clock(slice.endMillis)}"
    }
    val dayLabel: (UsageSlice) -> String = { slice ->
        "${Format.weekday(slice.startMillis)}، ${Format.shortDate(slice.startMillis)}"
    }
    val weekLabel: (UsageSlice) -> String = { slice ->
        "${Format.shortDate(slice.startMillis)} – ${Format.shortDate(slice.endMillis - 1)}"
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PeriodSelector(
                period = state.appsPeriod,
                onPeriodChange = onPeriodChange,
            )
        }

        if (!state.hasUsageAccess) {
            item {
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
        } else {
            item {
                SummaryCard(
                    state = state,
                    totalBytes = apps.sumOf { it.totals.totalBytes },
                    appsCount = apps.size,
                )
            }

            // التفصيل الزمني حسب الفترة: ساعات اليوم، أيام الأسبوع، أو أسابيع الشهر.
            when (state.appsPeriod) {
                UsagePeriod.TODAY -> item {
                    SliceCard(
                        title = stringResource(R.string.apps_slices_hours),
                        icon = Icons.Rounded.Schedule,
                        slices = state.appsHours,
                        label = hourLabel,
                        selected = state.appsSlice,
                        onSelect = onAppsSliceChange,
                    )
                }

                UsagePeriod.WEEK -> item {
                    SliceCard(
                        title = stringResource(R.string.apps_slices_days),
                        icon = Icons.Rounded.DateRange,
                        slices = state.appsDays,
                        label = dayLabel,
                        selected = state.appsSlice,
                        onSelect = onAppsSliceChange,
                    )
                }

                UsagePeriod.MONTH -> {
                    item {
                        SliceCard(
                            title = stringResource(R.string.apps_slices_weeks),
                            icon = Icons.Rounded.CalendarMonth,
                            slices = state.appsWeeks,
                            label = weekLabel,
                            selected = state.appsWeek,
                            onSelect = onAppsWeekChange,
                        )
                    }
                    if (state.appsWeek != null) {
                        item {
                            SliceCard(
                                title = stringResource(R.string.apps_slices_days),
                                icon = Icons.Rounded.DateRange,
                                slices = state.appsWeekDays,
                                label = dayLabel,
                                selected = state.appsSlice,
                                onSelect = onAppsSliceChange,
                            )
                        }
                    }
                }
            }

            if (state.isAppsLoading && apps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator(
                            modifier = Modifier.size(56.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            } else if (apps.isEmpty()) {
                item {
                    ExpressiveCard {
                        EmptyState(
                            icon = Icons.Rounded.Apps,
                            title = stringResource(R.string.apps_empty_title),
                            message = stringResource(R.string.apps_empty_body),
                        )
                    }
                }
            } else {
                // فلتر نوع التطبيقات: الكل (الافتراضي) أو المثبتة أو النظام — فوق قائمة التطبيقات.
                item {
                    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                        Text(
                            text = stringResource(R.string.apps_filter_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        AppsFilterRow(
                            filter = state.appsFilter,
                            onFilterChange = onAppsFilterChange,
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = stringResource(R.string.tab_apps),
                        supportingText = stringResource(R.string.apps_count, apps.size),
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.app_detail_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                itemsIndexed(
                    items = apps,
                    key = { _, app -> app.uid },
                ) { index, app ->
                    AppUsageRow(
                        app = app,
                        share = app.totals.totalBytes.toFloat() / totalBytes.toFloat(),
                        rank = index + 1,
                        onClick = { onAppClick(app) },
                    )
                }
            }
        }
    }
}

/** صفوف فلتر نوع التطبيقات: الكل / المثبتة / النظام — بدون اختيار يعرض الكل. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppsFilterRow(
    filter: AppsFilter,
    onFilterChange: (AppsFilter) -> Unit,
) {
    val options = remember { AppsFilter.entries.toList() }
    val labels = listOf(
        stringResource(R.string.apps_filter_all),
        stringResource(R.string.apps_filter_installed),
        stringResource(R.string.apps_filter_system),
    )

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = filter == option,
                onClick = { onFilterChange(option) },
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

/**
 * بطاقة شرائح زمنية (ساعات/أيام/أسابيع) مع إجمالي كل شريحة؛
 * الضغط على شريحة يحدّد/يلغي تحديد تطبيقاتها (الصف المحدد يظهر مظلّلًا).
 */
@Composable
private fun SliceCard(
    title: String,
    icon: ImageVector,
    slices: List<UsageSlice>,
    label: (UsageSlice) -> String,
    selected: TimeRange?,
    onSelect: (TimeRange) -> Unit,
) {
    if (slices.isEmpty()) return

    ExpressiveCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(title = title, supportingText = "${slices.size}")
            slices.forEach { slice ->
                val range = TimeRange(slice.startMillis, slice.endMillis)
                val isSelected = selected == range
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            shape = RoundedCornerShape(10.dp),
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(range) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.Check else icon,
                        contentDescription = null,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label(slice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = Format.bytes(slice.totals.totalBytes),
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PeriodSelector(
    period: UsagePeriod,
    onPeriodChange: (UsagePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayLabel = stringResource(R.string.label_today)
    val weekLabel = stringResource(R.string.label_week)
    val monthLabel = stringResource(R.string.label_month)

    ButtonGroup(
        overflowIndicator = { menuState ->
            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        toggleableItem(
            checked = period == UsagePeriod.TODAY,
            label = todayLabel,
            onCheckedChange = { onPeriodChange(UsagePeriod.TODAY) },
            weight = 1f,
        )
        toggleableItem(
            checked = period == UsagePeriod.WEEK,
            label = weekLabel,
            onCheckedChange = { onPeriodChange(UsagePeriod.WEEK) },
            weight = 1f,
        )
        toggleableItem(
            checked = period == UsagePeriod.MONTH,
            label = monthLabel,
            onCheckedChange = { onPeriodChange(UsagePeriod.MONTH) },
            weight = 1f,
        )
    }
}

@Composable
private fun SummaryCard(state: UiState, totalBytes: Long, appsCount: Int) {
    val topApp = state.visibleApps.firstOrNull()

    ExpressiveCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = stringResource(R.string.label_period),
                supportingText = stringResource(R.string.apps_count, appsCount),
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(
                icon = Icons.Rounded.ArrowDownward,
                label = stringResource(R.string.label_download),
                value = Format.bytes(state.visibleApps.sumOf { it.totals.rxBytes }),
                valueColor = LocalDownloadColor.current,
            )
            InfoRow(
                icon = Icons.Rounded.ArrowUpward,
                label = stringResource(R.string.label_upload),
                value = Format.bytes(state.visibleApps.sumOf { it.totals.txBytes }),
                valueColor = LocalUploadColor.current,
            )
            InfoRow(
                icon = Icons.Rounded.Apps,
                label = stringResource(R.string.label_total),
                value = Format.bytes(totalBytes),
            )
            if (topApp != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${stringResource(R.string.apps_screen_title)}: ${topApp.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
