package com.mywifiadata.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.UsagePeriod
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.DayUsage
import com.mywifiadata.app.ui.UiState
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
import com.mywifiadata.app.ui.theme.AppButtonShape
import com.mywifiadata.app.ui.theme.LocalDownloadColor
import com.mywifiadata.app.ui.theme.LocalUploadColor

/**
 * شاشة السجلّ: مخطط استهلاك الأيام وتفاصيل اليوم المحدد.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(
    state: UiState,
    onPeriodChange: (UsagePeriod) -> Unit,
    onSelectDay: (Int?) -> Unit,
    onOpenUsageAccess: () -> Unit,
    onAppClick: (AppUsage) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val daily = state.daily
    val selectedIndex = state.selectedDayIndex
    val selectedDay = selectedIndex?.let { daily.getOrNull(it) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PeriodSelector(
                period = state.historyPeriod,
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
            item { ChartCard(state = state, onSelectDay = onSelectDay) }

            // بطاقة اليوم المحدد تظهر فورًا فوق سجل الاستهلاك.
            if (selectedDay != null) {
                item {
                    SelectedDayCard(
                        day = selectedDay,
                        onClose = { onSelectDay(null) },
                    )
                }
            }

            if (daily.isNotEmpty()) {
                item { HistorySummaryCard(daily = daily, monthPeak = state.monthPeak) }
            } else {
                item {
                    ExpressiveCard {
                        EmptyState(
                            icon = Icons.Rounded.ShowChart,
                            title = stringResource(R.string.history_empty_title),
                            message = stringResource(R.string.history_empty_body),
                        )
                    }
                }
            }

            if (selectedDay != null) {

                item {
                    SectionHeader(
                        title = stringResource(R.string.history_top_apps),
                        supportingText = Format.fullDate(selectedDay.startMillis),
                    )
                }

                if (state.selectedDayApps.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.Info,
                            title = stringResource(R.string.apps_empty_title),
                            message = stringResource(R.string.apps_no_permission),
                        )
                    }
                } else {
                    itemsIndexed(
                        items = state.selectedDayApps.take(10),
                        key = { _, app -> app.uid },
                    ) { index, app ->
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
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChartCard(state: UiState, onSelectDay: (Int?) -> Unit) {
    // فهرس اليوم الذي يشير إليه المؤشر: يوم السحب الحالي، وإلا اليوم المحدد،
    // وإلا أحدث يوم في الفترة (نهاية القائمة في هذا الترتيب).
    var scrubIndex by remember { mutableStateOf<Int?>(null) }
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

    ExpressiveCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = if (state.historyDays <= 1) {
                    stringResource(R.string.history_today_title)
                } else {
                    stringResource(R.string.history_days_shown, state.historyDays)
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
                    activeIndex = scrubIndex,
                    onEntryClick = { index ->
                        onSelectDay(if (state.selectedDayIndex == index) null else index)
                    },
                )
                if (entries.size > 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    // شريط تمرير لاختيار اليوم: يتحرك بالنقطة، ويتّسع عمود اليوم
                    // المشار إليه ويظهر رقمه في صندوق فوقه أثناء السحب.
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
