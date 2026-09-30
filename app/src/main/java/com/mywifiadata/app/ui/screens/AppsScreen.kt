package com.mywifiadata.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.UsagePeriod
import com.mywifiadata.app.data.model.AppUsage
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
 * شاشة التطبيقات: ترتيب التطبيقات حسب استهلاك الواي فاي في الفترة المختارة.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppsScreen(
    state: UiState,
    onPeriodChange: (UsagePeriod) -> Unit,
    onShowSystemAppsChange: (Boolean) -> Unit,
    onOpenUsageAccess: () -> Unit,
    onAppClick: (AppUsage) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val apps = state.visibleApps
    val totalBytes = apps.sumOf { it.totals.totalBytes }.coerceAtLeast(1L)

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

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = state.showSystemApps,
                        onClick = { onShowSystemAppsChange(!state.showSystemApps) },
                        label = { Text(text = stringResource(R.string.system_apps_label)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Android,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
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
                    ExpressiveCard {
                        EmptyState(
                            icon = Icons.Rounded.Apps,
                            title = stringResource(R.string.apps_empty_title),
                            message = stringResource(R.string.apps_empty_body),
                        )
                    }
                }
            } else {
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
