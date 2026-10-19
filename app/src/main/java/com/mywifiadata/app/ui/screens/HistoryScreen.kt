package com.mywifiadata.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.AppsSortOrder
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.HistorySection
import com.mywifiadata.app.core.UsageNetwork
import com.mywifiadata.app.core.UsagePeriod
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.CompletedPlanRecord
import com.mywifiadata.app.data.model.DayUsage
import com.mywifiadata.app.ui.CompletedPlanDetailState
import com.mywifiadata.app.ui.UiState
import com.mywifiadata.app.ui.components.AppsFastScroller
import com.mywifiadata.app.ui.components.AppsSearchControls
import com.mywifiadata.app.ui.components.AppUsageRow
import com.mywifiadata.app.ui.components.BarEntry
import com.mywifiadata.app.ui.components.DailyBarChart
import com.mywifiadata.app.ui.components.DayScrubber
import com.mywifiadata.app.ui.components.chartSpacing
import com.mywifiadata.app.ui.components.EmptyState
import com.mywifiadata.app.ui.components.ExpressiveCard
import kotlin.math.abs
import kotlinx.coroutines.launch
import com.mywifiadata.app.ui.components.InfoRow
import com.mywifiadata.app.ui.components.PermissionCard
import com.mywifiadata.app.ui.components.SectionHeader
import com.mywifiadata.app.ui.theme.LocalDownloadColor
import com.mywifiadata.app.ui.theme.LocalUploadColor

/**
 * أزرار التبديل بين «الشبكة» و«الباقة» بجانب عنوان شاشة السجل.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HistorySectionSelector(
    section: HistorySection,
    onSectionChange: (HistorySection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        HistorySection.NETWORK to stringResource(R.string.history_section_network),
        HistorySection.PLAN to stringResource(R.string.history_section_plan),
    )

    ButtonGroup(
        overflowIndicator = { menuState ->
            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
        },
        modifier = modifier,
    ) {
        options.forEach { (itemSection, label) ->
            val checked = section == itemSection
            customItem(
                buttonGroupContent = {
                    val interactionSource = remember { MutableInteractionSource() }
                    ToggleButton(
                        checked = checked,
                        onCheckedChange = { onSectionChange(itemSection) },
                        modifier = Modifier
                            .weight(1f)
                            .animateWidth(interactionSource = interactionSource),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        interactionSource = interactionSource,
                    ) {
                        Text(
                            text = label,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSectionChange(itemSection)
                            menuState.dismiss()
                        },
                    )
                },
            )
        }
    }
}

/**
 * شاشة السجلّ: مخطط استهلاك الأيام وتفاصيل اليوم المحدد.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    state: UiState,
    onPeriodChange: (UsagePeriod) -> Unit = {},
    onSelectDay: (Int?) -> Unit,
    onOpenUsageAccess: () -> Unit,
    onAppClick: (AppUsage) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onReverseSortOrder: () -> Unit,
    onResetHistoryDate: () -> Unit = {},
    onChartTypeChange: (Boolean) -> Unit = {},
    onCompletedPlanClick: (CompletedPlanRecord) -> Unit = {},
    onCloseCompletedPlan: () -> Unit = {},
    onCompletedPlanSearchActiveChange: (Boolean) -> Unit = {},
    onCompletedPlanSearchQueryChange: (String) -> Unit = {},
    onReverseCompletedPlanSortOrder: () -> Unit = {},
    onCompletedPlanAppClick: (AppUsage, UsageNetwork) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    listState: LazyListState = rememberLazyListState(),
    searchListState: LazyListState = rememberLazyListState(),
    isActive: Boolean = true,
) {
    if (state.historySection == HistorySection.PLAN) {
        val selectedPlanDetail = state.selectedCompletedPlan
        if (selectedPlanDetail != null) {
            CompletedPlanAppsScreen(
                detail = selectedPlanDetail,
                hasUsageAccess = state.hasUsageAccess,
                onBack = onCloseCompletedPlan,
                onOpenUsageAccess = onOpenUsageAccess,
                onAppClick = { app -> onCompletedPlanAppClick(app, selectedPlanDetail.network) },
                onSearchActiveChange = onCompletedPlanSearchActiveChange,
                onSearchQueryChange = onCompletedPlanSearchQueryChange,
                onReverseSortOrder = onReverseCompletedPlanSortOrder,
                modifier = modifier,
                contentPadding = contentPadding,
                isActive = isActive,
            )
        } else {
            CompletedPlansHistoryList(
                records = state.visibleCompletedPlans,
                onPlanClick = onCompletedPlanClick,
                modifier = modifier,
                contentPadding = contentPadding,
            )
        }
        return
    }

    val daily = state.daily
    val selectedDay = state.selectedDayIndex?.let { daily.getOrNull(it) }
    val searching = state.historySearchActive && selectedDay != null && state.hasUsageAccess
    val apps = state.visibleHistoryApps
    BackHandler(enabled = isActive && searching) { onSearchActiveChange(false) }

    Box(modifier = modifier.fillMaxWidth().imePadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            // Match the Apps tab: typing cannot move the normal history viewport. Closing
            // search restores the selected column, summary and pre-search scroll position.
            state = if (searching) searchListState else listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.hasUsageAccess) {
                item(key = "history_permission") {
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
                if (!searching) {
                    item(key = "history_chart") {
                        ChartCard(
                            state = state,
                            onSelectDay = onSelectDay,
                            onResetHistoryDate = onResetHistoryDate,
                            onChartTypeChange = onChartTypeChange,
                        )
                    }

                    if (selectedDay != null) {
                        item(key = "history_selected_day") {
                            SelectedDayCard(day = selectedDay, onClose = { onSelectDay(null) })
                        }
                    }

                    item(key = "history_summary") {
                        if (daily.isNotEmpty()) {
                            HistorySummaryCard(daily = daily, monthPeak = state.monthPeak)
                        } else {
                            ExpressiveCard {
                                EmptyState(
                                    icon = Icons.Rounded.ShowChart,
                                    title = stringResource(R.string.history_empty_title),
                                    message = stringResource(R.string.history_empty_body),
                                )
                            }
                        }
                    }
                }

                if (selectedDay != null) {
                    item(key = "history_apps_title") {
                        SectionHeader(
                            title = stringResource(
                                when {
                                    searching -> R.string.history_day_apps
                                    state.historyAppsSortOrder == AppsSortOrder.LOWEST_USAGE -> R.string.history_least_apps
                                    else -> R.string.history_top_apps
                                },
                            ),
                            supportingText = Format.fullDate(selectedDay.startMillis),
                        )
                    }
                    stickyHeader(key = "history_apps_search") {
                        AppsSearchControls(
                            isActive = isActive,
                            expanded = searching,
                            query = state.historySearchQuery,
                            sortOrder = state.historyAppsSortOrder,
                            onExpandedChange = { active ->
                                if (active) searchListState.requestScrollToItem(0)
                                onSearchActiveChange(active)
                            },
                            onQueryChange = { query ->
                                searchListState.requestScrollToItem(0)
                                onSearchQueryChange(query)
                            },
                            onReverseSortOrder = {
                                if (searching) searchListState.requestScrollToItem(0)
                                onReverseSortOrder()
                            },
                        )
                    }

                    when {
                        state.isSelectedDayAppsLoading -> item(key = "history_apps_loading") {
                            Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                LoadingIndicator(modifier = Modifier.size(48.dp))
                            }
                        }
                        apps.isEmpty() -> item(key = "history_apps_empty") {
                            EmptyState(
                                icon = Icons.Rounded.Info,
                                title = stringResource(
                                    if (searching && state.historySearchQuery.isNotBlank()) R.string.apps_search_empty_title
                                    else R.string.apps_empty_title,
                                ),
                                message = stringResource(
                                    if (searching && state.historySearchQuery.isNotBlank()) R.string.history_apps_search_empty
                                    else R.string.history_apps_empty,
                                ),
                            )
                        }
                        else -> itemsIndexed(items = apps, key = { _, app -> app.uid }) { index, app ->
                            val dayTotal = selectedDay.totals.totalBytes.coerceAtLeast(1L)
                            AppUsageRow(
                                app = app,
                                share = app.totals.totalBytes.toFloat() / dayTotal.toFloat(),
                                rank = index + 1,
                                onClick = { onAppClick(app) },
                            )
                        }
                    }
                }
            }
        }
        if (state.hasUsageAccess && selectedDay != null && !searching &&
            !state.isSelectedDayAppsLoading && apps.isNotEmpty()) {
            // Reuse the same edge gesture, but keep its state and targets local to this day.
            // Use the rendered row count (normally top ten), not all selectedDayApps.
            key(
                selectedDay.startMillis, selectedDay.endMillis, state.historyNetwork,
                state.historyYear, state.historyMonth,
                state.historyAppsSortOrder, apps.map { it.uid },
            ) {
                AppsFastScroller(listState, apps.size, headerKey = "history_apps_search")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChartCard(
    state: UiState,
    onSelectDay: (Int?) -> Unit,
    onResetHistoryDate: () -> Unit,
    onChartTypeChange: (Boolean) -> Unit,
) {
    // فهرس اليوم الذي يشير إليه المؤشر: يوم السحب الحالي، وإلا اليوم المحدد،
    // وإلا أحدث يوم في الفترة (نهاية القائمة في هذا الترتيب).
    var scrubIndex by remember(state.historyNetwork, state.historyYear, state.historyMonth) {
        mutableStateOf<Int?>(null)
    }
    val lastIndex = state.daily.lastIndex
    val selectedIndex = state.selectedDayIndex?.takeIf { it in state.daily.indices }
    val thumbIndex = (scrubIndex ?: selectedIndex ?: lastIndex).coerceAtLeast(0)

    val entries = state.daily.mapIndexed { index, day ->
        BarEntry(
            label = Format.dayOfMonth(day.startMillis),
            value = day.totals.totalBytes,
            // يوم المؤشر هو المميّز بلون مختلف، تمامًا كما يشير إليه الشريط تحته.
            highlight = index == thumbIndex,
        )
    }

    // حشوة سفلية أصغر لهذه البطاقة لأن شريط التمرير يجلس قريبًا من حاشيتها.
    ExpressiveCard(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = Format.monthYearHeader(state.historyYear, state.historyMonth),
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (state.isCustomHistoryDate) {
                            IconButton(
                                onClick = onResetHistoryDate,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.history_reset_date),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Text(
                            text = stringResource(
                                if (state.historyNetwork == UsageNetwork.WIFI) R.string.apps_network_wifi
                                else R.string.apps_network_mobile,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (entries.isEmpty()) {
                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    EmptyState(
                        icon = Icons.Rounded.ShowChart,
                        title = stringResource(R.string.history_empty_title),
                        message = stringResource(R.string.history_empty_body),
                    )
                }
            } else {
                DailyBarChart(
                    entries = entries,
                    lineChartEnabled = state.settings.historyLineChartEnabled,
                    onChartTypeChange = onChartTypeChange,
                    activeIndex = scrubIndex,
                    onEntryClick = { index ->
                        onSelectDay(if (state.selectedDayIndex == index) null else index)
                    },
                )
                if (entries.size > 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                    // شريط تمرير لاختيار اليوم: يتحرك بالنقطة، ويتّسع عمود اليوم
                    // المشار إليه ويُبرز رقمه في صف التسميات أسفل المخطط أثناء السحب.
                    DayScrubber(
                        count = entries.size,
                        thumbIndex = thumbIndex.coerceIn(0, entries.lastIndex),
                        active = scrubIndex != null,
                        spacing = chartSpacing(entries.size),
                        activeIndex = scrubIndex,
                        onScrubStart = {},
                        onScrub = { index -> scrubIndex = index },
                        onScrubEnd = { index ->
                            scrubIndex = null
                            onSelectDay(index)
                        },
                        onJump = { index ->
                            scrubIndex = null
                            onSelectDay(index)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySummaryCard(daily: List<DayUsage>, monthPeak: DayUsage?) {
    val total = daily.sumOf { it.totals.totalBytes }
    val average = if (daily.isEmpty()) 0L else total / daily.size
    // «أعلى يوم» مرجع من آخر 30 يومًا لا من الفترة المعروضة، فيبقى مفيدًا
    // للمقارنة حتى عندما تكون الفترة المختارة يومًا واحدًا.
    val peak = monthPeak

    ExpressiveCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = stringResource(R.string.history_screen_title),
                supportingText = stringResource(R.string.label_average),
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(
                icon = Icons.Rounded.ShowChart,
                label = stringResource(R.string.label_total),
                value = Format.bytes(total),
            )
            InfoRow(
                icon = Icons.Rounded.TrendingUp,
                label = stringResource(R.string.label_average),
                value = Format.bytes(average),
            )
            if (peak != null) {
                InfoRow(
                    icon = Icons.Rounded.TrendingUp,
                    label = stringResource(R.string.label_peak_month),
                    value = "${Format.bytes(peak.totals.totalBytes)} • ${Format.shortDate(peak.startMillis)}",
                )
            }
        }
    }
}

@Composable
private fun SelectedDayCard(day: DayUsage, onClose: () -> Unit) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var cardWidth by remember { mutableIntStateOf(0) }
    val screenWidthPx = with(density) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    // مسافة السحب التي يكفي تجاوزها لإغلاق البطاقة (أو الإفلات بسرعة عالية).
    val dismissThreshold = with(density) { 96.dp.toPx() }
    val travelDistance = (if (cardWidth > 0) cardWidth.toFloat() else screenWidthPx)

    /**
     * نهاية السحب: إن تجاوزت المسافة الحد أو كانت سرعة الإفلات عالية تُخرج
     * البطاقة من الشاشة ثم تُغلق، وإلا ترتد إلى مكانها.
     */
    fun settleDrag(velocity: Float) {
        val traveled = offsetX.value
        val dismissed = abs(traveled) > dismissThreshold || abs(velocity) > FLING_VELOCITY
        scope.launch {
            if (dismissed) {
                val direction = when {
                    traveled > 0f -> 1f
                    traveled < 0f -> -1f
                    velocity >= 0f -> 1f
                    else -> -1f
                }
                offsetX.animateTo(direction * travelDistance, tween(durationMillis = 200))
                onClose()
            } else {
                offsetX.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 400f))
            }
        }
    }

    ExpressiveCard(
        modifier = Modifier
            .onSizeChanged { size -> cardWidth = size.width }
            .graphicsLayer {
                translationX = offsetX.value
                // شفافية خفيفة أثناء السحب لإيضاح أن البطاقة قابلة للإغلاق.
                alpha = 1f - 0.55f * (abs(offsetX.value) / travelDistance).coerceIn(0f, 1f)
            }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    scope.launch {
                        offsetX.snapTo(
                            (offsetX.value + delta).coerceIn(-travelDistance, travelDistance),
                        )
                    }
                },
                onDragStopped = { velocity -> settleDrag(velocity) },
            ),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                // التاريخ في البداية (يمين) واليوم بجانبه من اليسار.
                title = Format.fullDate(day.startMillis),
                supportingText = Format.weekday(day.startMillis),
                trailing = {
                    // offset هنا واعٍ لاتجاه الواجهة: في RTL يعكس الموجب/السالب
                    // (rtlAware)، فالقيمة +16dp تعني 16dp نحو اليسار هنا،
                    // لمحاذاة الأيقونة مع حاشية المحتوى (20dp) بعد خصم نصف
                    // صندوق اللمس 48dp وحشوة صف العنوان 4dp.
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.offset(x = 16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.height(4.dp))
            // التنزيل والرفع والإجمالي: الأسماء في سطر واحد وتحت كل اسم رقمه
            // لتقصير ارتفاع البطاقة بدل الصفوف العمودية الثلاثة.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.label_download),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = Format.bytes(day.totals.rxBytes),
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        color = LocalDownloadColor.current,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.label_upload),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = Format.bytes(day.totals.txBytes),
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        color = LocalUploadColor.current,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.label_total),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = Format.bytes(day.totals.totalBytes),
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** سرعة الإفلات (بكسل/ثانية) التي تكفي وحدها لإغلاق بطاقة اليوم المحدد. */
private const val FLING_VELOCITY = 900f

@Composable
private fun CompletedPlansHistoryList(
    records: List<CompletedPlanRecord>,
    onPlanClick: (CompletedPlanRecord) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (records.isEmpty()) {
            item(key = "completed_plans_empty") {
                ExpressiveCard {
                    EmptyState(
                        icon = Icons.Rounded.ShowChart,
                        title = stringResource(R.string.history_plan_empty_title),
                        message = stringResource(R.string.history_plan_empty_body),
                    )
                }
            }
        } else {
            items(items = records, key = { it.id }) { record ->
                CompletedPlanCard(
                    record = record,
                    onClick = { onPlanClick(record) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CompletedPlanCard(
    record: CompletedPlanRecord,
    onClick: () -> Unit,
) {
    val logoBitmap = remember(record.logoPath) { loadPlanLogoBitmap(record.logoPath) }
    val capacityText = Format.bytes(record.quotaBytes)
    val durationText = "${record.durationValue} ${stringResource(record.durationUnit.labelResource())}"
    val usedText = Format.bytes(record.usedBytes)
    val hasPlanName = record.name.isNotBlank()
    val progress = if (record.quotaBytes > 0L) {
        (record.usedBytes.toFloat() / record.quotaBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    ExpressiveCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
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
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (hasPlanName) {
                            Text(
                                text = record.name,
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
                                MaterialTheme.typography.titleMediumEmphasized
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

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = "$usedText / $capacityText",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "${stringResource(R.string.history_plan_start_date)}: ${Format.shortDate(record.startMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "${stringResource(R.string.history_plan_end_date)}: ${Format.shortDate(record.endMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun CompletedPlanAppsScreen(
    detail: CompletedPlanDetailState,
    hasUsageAccess: Boolean,
    onBack: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onAppClick: (AppUsage) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onReverseSortOrder: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    isActive: Boolean = true,
) {
    val record = detail.record
    val searching = detail.searchActive && hasUsageAccess
    val apps = detail.visibleApps
    val listState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val logoBitmap = remember(record.logoPath) { loadPlanLogoBitmap(record.logoPath) }
    val capacityText = Format.bytes(record.quotaBytes)
    val durationText = "${record.durationValue} ${stringResource(record.durationUnit.labelResource())}"
    val hasPlanName = record.name.isNotBlank()
    val effectiveTotalBytes = maxOf(detail.totals.totalBytes, record.usedBytes)

    BackHandler(enabled = isActive) {
        if (searching) {
            onSearchActiveChange(false)
        } else {
            onBack()
        }
    }

    Box(modifier = modifier.fillMaxWidth().imePadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = if (searching) searchListState else listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!hasUsageAccess) {
                item(key = "completed_plan_permission") {
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
                if (!searching) {
                    item(key = "completed_plan_summary") {
                        ExpressiveCard {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        IconButton(onClick = onBack) {
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowBack,
                                                contentDescription = stringResource(R.string.cd_back),
                                            )
                                        }
                                        if (logoBitmap != null) {
                                            Image(
                                                bitmap = logoBitmap,
                                                contentDescription = stringResource(R.string.plan_logo_label),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(RoundedCornerShape(6.dp)),
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            if (hasPlanName) {
                                                Text(
                                                    text = record.name,
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
                                                    MaterialTheme.typography.titleMediumEmphasized
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

                                    Text(
                                        text = stringResource(
                                            if (detail.network == UsageNetwork.WIFI) R.string.apps_network_wifi
                                            else R.string.apps_network_mobile,
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.history_plan_start_date)}: ${Format.shortDate(record.startMillis)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "${stringResource(R.string.history_plan_end_date)}: ${Format.shortDate(record.endMillis)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDownward,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Text(
                                                text = stringResource(R.string.label_download),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Text(
                                            text = Format.bytes(detail.totals.rxBytes),
                                            style = MaterialTheme.typography.bodyMediumEmphasized,
                                            color = LocalDownloadColor.current,
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowUpward,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Text(
                                                text = stringResource(R.string.label_upload),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Text(
                                            text = Format.bytes(detail.totals.txBytes),
                                            style = MaterialTheme.typography.bodyMediumEmphasized,
                                            color = LocalUploadColor.current,
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ShowChart,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Text(
                                                text = stringResource(R.string.label_total),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Text(
                                            text = Format.bytes(effectiveTotalBytes),
                                            style = MaterialTheme.typography.bodyMediumEmphasized,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "completed_plan_apps_title") {
                    SectionHeader(
                        title = stringResource(
                            when {
                                searching -> R.string.history_plan_apps_title
                                detail.sortOrder == AppsSortOrder.LOWEST_USAGE -> R.string.history_least_apps
                                else -> R.string.history_top_apps
                            },
                        ),
                        supportingText = "${Format.shortDate(record.startMillis)} – ${Format.shortDate(record.endMillis)}",
                    )
                }

                stickyHeader(key = "completed_plan_apps_search") {
                    AppsSearchControls(
                        isActive = isActive,
                        expanded = searching,
                        query = detail.searchQuery,
                        sortOrder = detail.sortOrder,
                        onExpandedChange = { active ->
                            if (active) searchListState.requestScrollToItem(0)
                            onSearchActiveChange(active)
                        },
                        onQueryChange = { query ->
                            searchListState.requestScrollToItem(0)
                            onSearchQueryChange(query)
                        },
                        onReverseSortOrder = {
                            if (searching) searchListState.requestScrollToItem(0)
                            onReverseSortOrder()
                        },
                    )
                }

                when {
                    detail.isLoading -> item(key = "completed_plan_apps_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingIndicator(modifier = Modifier.size(48.dp))
                        }
                    }
                    apps.isEmpty() -> item(key = "completed_plan_apps_empty") {
                        EmptyState(
                            icon = Icons.Rounded.Info,
                            title = stringResource(
                                if (searching && detail.searchQuery.isNotBlank()) R.string.apps_search_empty_title
                                else R.string.apps_empty_title,
                            ),
                            message = stringResource(
                                if (searching && detail.searchQuery.isNotBlank()) R.string.history_plan_apps_search_empty
                                else R.string.history_plan_apps_empty,
                            ),
                        )
                    }
                    else -> itemsIndexed(items = apps, key = { _, app -> app.uid }) { index, app ->
                        val denom = effectiveTotalBytes.coerceAtLeast(1L)
                        AppUsageRow(
                            app = app,
                            share = app.totals.totalBytes.toFloat() / denom.toFloat(),
                            rank = index + 1,
                            onClick = { onAppClick(app) },
                        )
                    }
                }
            }
        }
        if (hasUsageAccess && !searching && !detail.isLoading && apps.isNotEmpty()) {
            key(record.id, detail.network, detail.sortOrder, apps.map { it.uid }) {
                AppsFastScroller(listState, apps.size, headerKey = "completed_plan_apps_search")
            }
        }
    }
}
