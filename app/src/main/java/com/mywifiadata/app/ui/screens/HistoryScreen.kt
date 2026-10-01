package com.mywifiadata.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mywifiadata.app.ui.components.EmptyState
import com.mywifiadata.app.ui.components.ExpressiveCard
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

            if (daily.isNotEmpty()) {
                item { HistorySummaryCard(daily = daily) }
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
                    SelectedDayCard(
                        day = selectedDay,
                        onClose = { onSelectDay(null) },
                    )
                }

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
    val entries = state.daily.mapIndexed { index, day ->
        BarEntry(
            label = Format.dayOfMonth(day.startMillis),
            value = day.totals.totalBytes,
            highlight = index == state.daily.lastIndex,
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
                    onEntryClick = { index ->
                        onSelectDay(if (state.selectedDayIndex == index) null else index)
                    },
                )
            }
        }
    }
}

@Composable
private fun HistorySummaryCard(daily: List<DayUsage>) {
    val total = daily.sumOf { it.totals.totalBytes }
    val average = if (daily.isEmpty()) 0L else total / daily.size
    val peak = daily.maxByOrNull { it.totals.totalBytes }

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
                    label = stringResource(R.string.label_peak),
                    value = "${Format.bytes(peak.totals.totalBytes)} • ${Format.shortDate(peak.startMillis)}",
                )
            }
        }
    }
}

@Composable
private fun SelectedDayCard(day: DayUsage, onClose: () -> Unit) {
    ExpressiveCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                // اليوم والتاريخ في سطر واحد: الجمعة بجانب التاريخ.
                title = "${Format.weekday(day.startMillis)} • ${Format.fullDate(day.startMillis)}",
                trailing = {
                    // إزاحة 16dp لليسار: محاذاة الأيقونة مع حاشية المحتوى (20dp)
                    // بعد خصم نصف صندوق اللمس 48dp وحشوة صف العنوان 4dp.
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.offset(x = (-16).dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow(
                icon = Icons.Rounded.ArrowDownward,
                label = stringResource(R.string.label_download),
                value = Format.bytes(day.totals.rxBytes),
                valueColor = LocalDownloadColor.current,
            )
            InfoRow(
                icon = Icons.Rounded.ArrowUpward,
                label = stringResource(R.string.label_upload),
                value = Format.bytes(day.totals.txBytes),
                valueColor = LocalUploadColor.current,
            )
            InfoRow(
                icon = Icons.Rounded.ShowChart,
                label = stringResource(R.string.label_total),
                value = Format.bytes(day.totals.totalBytes),
            )
        }
    }
}
