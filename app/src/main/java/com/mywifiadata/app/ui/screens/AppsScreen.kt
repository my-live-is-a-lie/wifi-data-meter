package com.mywifiadata.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.UsageNetwork
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.TimeRange
import com.mywifiadata.app.core.UsagePeriod
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.AppsFilter
import com.mywifiadata.app.data.model.UsageSlice
import com.mywifiadata.app.ui.UiState
import com.mywifiadata.app.ui.components.AppsSearchControls
import com.mywifiadata.app.ui.components.AppUsageRow
import com.mywifiadata.app.ui.components.BarEntry
import com.mywifiadata.app.ui.components.DailyBarChart
import com.mywifiadata.app.ui.components.EmptyState
import com.mywifiadata.app.ui.components.ExpressiveCard
import com.mywifiadata.app.ui.components.PermissionCard
import com.mywifiadata.app.ui.components.SectionHeader
import com.mywifiadata.app.ui.theme.LocalDownloadColor
import com.mywifiadata.app.ui.theme.LocalUploadColor

/**
 * شاشة التطبيقات: ترتيب التطبيقات حسب استهلاك الشبكة والنطاق المختارين،
 * مع تفصيل زمني (ساعات اليوم / أيام الأسبوع / آخر 4 أسابيع) يمكن الضغط عليه
 * لعرض تطبيقات تلك الشريحة، وفلترة حسب نوع التطبيق.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(
    state: UiState,
    onPeriodChange: (UsagePeriod) -> Unit,
    onAppsFilterChange: (AppsFilter) -> Unit,
    onAppsWeekChange: (TimeRange) -> Unit,
    onAppsSliceChange: (TimeRange) -> Unit,
    onOpenUsageAccess: () -> Unit,
    onAppClick: (AppUsage) -> Unit,
    onReverseSortOrder: () -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onChartTypeChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    listState: LazyListState = rememberLazyListState(),
) {
    BackHandler(enabled = state.appsSearchActive) { onSearchActiveChange(false) }
    val apps = state.visibleApps
    val totalBytes = state.filteredApps.sumOf { it.totals.totalBytes }.coerceAtLeast(1L)
    val density = LocalDensity.current
    val rowHeights = remember { mutableStateMapOf<Int, Int>() }
    var toolbarHeight by remember { mutableIntStateOf(0) }
    var searchHintHeight by remember { mutableIntStateOf(0) }
    var emptyHeight by remember(state.appsSearchQuery) { mutableIntStateOf(0) }
    // Short/no-result searches still need enough scroll range to put the toolbar at the top.
    // Longer lists remain fully lazy and receive no extra space.
    val measuredResults = if (apps.isEmpty()) emptyHeight > 0 else apps.all { it.uid in rowHeights }
    val searchBottomSpace = if (state.appsSearchActive && !state.isAppsLoading && measuredResults) {
        val resultHeight = if (apps.isEmpty()) emptyHeight else apps.sumOf { rowHeights[it.uid] ?: 0 }
        val gapsAndPadding = with(density) {
            (12.dp * (apps.size.coerceAtLeast(1) + 2) +
                contentPadding.calculateTopPadding() + contentPadding.calculateBottomPadding()).roundToPx()
        }
        (listState.layoutInfo.viewportSize.height - toolbarHeight - searchHintHeight - resultHeight - gapsAndPadding)
            .coerceAtLeast(0)
    } else 0
    LaunchedEffect(state.appsSearchActive, state.appsSearchQuery, searchBottomSpace) {
        if (state.appsSearchActive) {
            val index = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "apps_search_controls" }?.index
            if (index != null) listState.animateScrollToItem(index)
        }
    }


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
        modifier = modifier.fillMaxWidth().imePadding(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PeriodSelector(period = state.appsPeriod, onPeriodChange = onPeriodChange)
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
                SummaryCard(state = state)
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
                        lineChartEnabled = state.settings.appsLineChartEnabled,
                        onChartTypeChange = onChartTypeChange,
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
                        lineChartEnabled = state.settings.appsLineChartEnabled,
                        onChartTypeChange = onChartTypeChange,
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
                            lineChartEnabled = state.settings.appsLineChartEnabled,
                            onChartTypeChange = onChartTypeChange,
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
                                lineChartEnabled = state.settings.appsLineChartEnabled,
                                onChartTypeChange = onChartTypeChange,
                                onSelect = onAppsSliceChange,
                            )
                        }
                    }
                }
            }

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
                    supportingText = stringResource(R.string.apps_count, apps.size) +
                        if (state.appsSlice == null) "" else " · " + stringResource(R.string.apps_selected_slice),
                )
            }
            stickyHeader(key = "apps_search_controls") {
                Box(modifier = Modifier.onSizeChanged { toolbarHeight = it.height }) {
                    AppsSearchControls(
                        expanded = state.appsSearchActive,
                        query = state.appsSearchQuery,
                        sortOrder = state.appsSortOrder,
                        onExpandedChange = onSearchActiveChange,
                        onQueryChange = onSearchQueryChange,
                        onReverseSortOrder = onReverseSortOrder,
                    )
                }
            }
            if (state.appsSearchActive) {
                item {
                    Text(
                        text = stringResource(R.string.apps_search_ranking_hint),
                        modifier = Modifier.onSizeChanged { searchHintHeight = it.height },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                    ExpressiveCard(modifier = Modifier.onSizeChanged { emptyHeight = it.height }) {
                        EmptyState(
                            icon = Icons.Rounded.Apps,
                            title = stringResource(
                                if (state.appsSearchActive && state.appsSearchQuery.isNotBlank())
                                    R.string.apps_search_empty_title else R.string.apps_empty_title,
                            ),
                            message = stringResource(
                                if (state.appsSearchActive && state.appsSearchQuery.isNotBlank())
                                    R.string.apps_search_empty_body else R.string.apps_empty_body,
                            ),
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = apps,
                    key = { _, app -> app.uid },
                ) { index, app ->
                    AppUsageRow(
                        app = app,
                        modifier = Modifier.onSizeChanged {
                            if (state.appsSearchActive) rowHeights[app.uid] = it.height
                        },
                        share = app.totals.totalBytes.toFloat() / totalBytes.toFloat(),
                        rank = index + 1,
                        onClick = { onAppClick(app) },
                    )
                }
            }
            if (state.appsSearchActive) {
                item(key = "search_bottom_space") {
                    Spacer(modifier = Modifier.height(with(density) { searchBottomSpace.toDp() }))
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
    lineChartEnabled: Boolean,
    onChartTypeChange: (Boolean) -> Unit,
    onSelect: (TimeRange) -> Unit,
) {
    if (slices.isEmpty()) return

    val selectedSlice = slices.find {
        TimeRange(it.startMillis, it.endMillis) == selected
    }
    // تسمية مختصرة تحت الأعمدة: أول تسلسل أرقام من التسمية الكاملة (١٤ / ٢٨ / ٢).
    val shortLabel: (UsageSlice) -> String = { slice ->
        val text = label(slice)
        val firstDigit = text.indexOfFirst { it.isDigit() }
        if (firstDigit < 0) text else text.drop(firstDigit).takeWhile { it.isDigit() }
    }

    ExpressiveCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            // التفاصيل: عند الضغط على عمود تظهر تسميته واستهلاكه في الترويسة.
            SectionHeader(
                title = title,
                supportingText = if (selectedSlice != null) {
                    "${label(selectedSlice)} • ${Format.bytes(selectedSlice.totals.totalBytes)}"
                } else {
                    "${slices.size}"
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            DailyBarChart(
                lineChartEnabled = lineChartEnabled,
                onChartTypeChange = onChartTypeChange,
                entries = slices.map { slice ->
                    BarEntry(
                        label = shortLabel(slice),
                        value = slice.totals.totalBytes,
                        highlight = TimeRange(slice.startMillis, slice.endMillis) == selected,
                    )
                },
                barHeight = 120.dp,
                onEntryClick = { index ->
                    val slice = slices[index]
                    onSelect(TimeRange(slice.startMillis, slice.endMillis))
                },
            )
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
private fun SummaryCard(state: UiState) {
    // بطاقة «الفترة» تعكس الفترة كاملة مهما اختيرت شريحة — لا تتأثر بالأعمدة.
    val periodApps = state.visiblePeriodApps
    val topApp = periodApps.firstOrNull()

    ExpressiveCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = stringResource(R.string.label_period),
                supportingText = stringResource(
                    if (state.appsNetwork == UsageNetwork.WIFI) R.string.apps_network_wifi else R.string.apps_network_mobile,
                ) + " · " + stringResource(R.string.apps_count, periodApps.size),
            )
            Spacer(modifier = Modifier.height(14.dp))
            // التنزيل والرفع والإجمالي في صف أفقي واحد، وتحت كل واحد قيمةه.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PeriodStat(
                    label = stringResource(R.string.label_download),
                    value = Format.bytes(periodApps.sumOf { it.totals.rxBytes }),
                    icon = Icons.Rounded.ArrowDownward,
                    color = LocalDownloadColor.current,
                    modifier = Modifier.weight(1f),
                )
                PeriodStat(
                    label = stringResource(R.string.label_upload),
                    value = Format.bytes(periodApps.sumOf { it.totals.txBytes }),
                    icon = Icons.Rounded.ArrowUpward,
                    color = LocalUploadColor.current,
                    modifier = Modifier.weight(1f),
                )
                PeriodStat(
                    label = stringResource(R.string.label_total),
                    value = Format.bytes(periodApps.sumOf { it.totals.totalBytes }),
                    icon = Icons.Rounded.Apps,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
            if (topApp != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stringResource(R.string.apps_top_consumer, topApp.label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * بلاطة واحدة في صف الفترة: الأيقونة والتسمية في الأعلى، والقيمة تحتهما
 * مباشرة بلون دلالي (التنزيل أزرق، الرفع بنفسجي، الإجمالي بلون النص).
 */
@Composable
private fun PeriodStat(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
