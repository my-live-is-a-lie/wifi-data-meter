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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.data.model.WifiBand
import com.mywifiadata.app.data.model.WifiStatus
import com.mywifiadata.app.ui.UiState
import com.mywifiadata.app.ui.components.Dot
import com.mywifiadata.app.ui.components.EmptyState
import com.mywifiadata.app.ui.components.ExpressiveCard
import com.mywifiadata.app.ui.components.InfoRow
import com.mywifiadata.app.ui.components.PermissionCard
import com.mywifiadata.app.ui.components.QuotaBar
import com.mywifiadata.app.ui.components.SectionHeader
import com.mywifiadata.app.ui.components.SignalBarsIcon
import com.mywifiadata.app.ui.components.StatTile
import com.mywifiadata.app.ui.components.UsageRing
import com.mywifiadata.app.ui.theme.LocalDownloadColor
import com.mywifiadata.app.ui.theme.LocalUploadColor
import kotlin.math.roundToInt

/**
 * لوحة المعلومات: نسبة الباقة، حالة الشبكة، إجماليات اليوم والجلسة، والسرعة اللحظية.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DashboardScreen(
    state: UiState,
    onOpenUsageAccess: () -> Unit,
    onOpenQuotaSettings: () -> Unit,
    onRefresh: () -> Unit,
    onResetSession: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { HeroCard(state = state, onOpenQuotaSettings = onOpenQuotaSettings) }

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
        }

        item {
            QuickActions(
                onRefresh = onRefresh,
                onResetSession = onResetSession,
                onOpenQuotaSettings = onOpenQuotaSettings,
            )
        }

        item {
            if (state.wifi.isWifiConnected) {
                WifiDetailsCard(wifi = state.wifi)
            } else {
                NotConnectedCard(isOnline = state.wifi.isOnline)
            }
        }

        item { TodayCard(state = state) }

        item { SessionCard(state = state, onResetSession = onResetSession) }
    }
}

@Composable
private fun HeroCard(state: UiState, onOpenQuotaSettings: () -> Unit) {
    val hasQuota = state.hasQuota
    val progress = when {
        hasQuota -> state.quotaProgress
        state.cycle.totalBytes > 0L -> state.today.totalBytes.toFloat() / state.cycle.totalBytes.toFloat()
        else -> 0f
    }
    val downloadColor = LocalDownloadColor.current
    val uploadColor = LocalUploadColor.current
    val speed = state.currentSpeed?.totalBytesPerSecond ?: 0.0

    ExpressiveCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            UsageRing(
                progress = progress,
                modifier = Modifier.size(220.dp),
                strokeWidth = 22.dp,
                startColor = if (state.quotaExceeded) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                endColor = if (state.quotaExceeded) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                contentDescription = stringResource(R.string.quota_ring_title),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (hasQuota) {
                            stringResource(
                                R.string.percent_format,
                                (state.quotaProgress * 100).roundToInt(),
                            )
                        } else {
                            Format.bytes(state.today.totalBytes)
                        },
                        style = MaterialTheme.typography.displaySmallEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = if (hasQuota) {
                            stringResource(R.string.label_used)
                        } else {
                            stringResource(R.string.label_today)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Format.speed(speed),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (hasQuota) {
                Text(
                    text = stringResource(
                        R.string.quota_remaining_of,
                        Format.bytes(state.remainingBytes),
                        Format.bytes(state.quotaBytes),
                    ),
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.quota_used_of,
                        Format.bytes(state.cycle.totalBytes),
                        Format.bytes(state.quotaBytes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (state.quotaExceeded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.quota_exceeded),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.quota_not_set),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                QuotaBar(
                    progress = if (state.cycle.totalBytes > 0L) {
                        state.today.totalBytes.toFloat() / state.cycle.totalBytes.toFloat()
                    } else {
                        0f
                    },
                    startColor = downloadColor,
                    endColor = uploadColor,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.session_since,
                        Format.fullDate(state.settings.sessionStartMillis),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenQuotaSettings) {
                    Text(text = stringResource(R.string.quota_set_action))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                LegendItem(
                    color = downloadColor,
                    label = stringResource(R.string.label_download),
                    value = Format.bytes(state.today.rxBytes),
                )
                LegendItem(
                    color = uploadColor,
                    label = stringResource(R.string.label_upload),
                    value = Format.bytes(state.today.txBytes),
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color = color, size = 10.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QuickActions(
    onRefresh: () -> Unit,
    onResetSession: () -> Unit,
    onOpenQuotaSettings: () -> Unit,
) {
    ButtonGroup(
        overflowIndicator = { menuState ->
            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        clickableItem(
            onClick = onRefresh,
            label = stringResource(R.string.action_refresh),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            weight = 1f,
        )
        clickableItem(
            onClick = onResetSession,
            label = stringResource(R.string.action_reset_session),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            weight = 1f,
        )
        clickableItem(
            onClick = onOpenQuotaSettings,
            label = stringResource(R.string.label_quota),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.MonetizationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            weight = 1f,
        )
    }
}

@Composable
private fun WifiDetailsCard(wifi: WifiStatus) {
    val unknown = stringResource(R.string.value_unknown)
    ExpressiveCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SignalBarsIcon(
                    level = wifi.signalLevel,
                    modifier = Modifier.size(width = 34.dp, height = 26.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = wifi.ssid ?: stringResource(R.string.status_connected_unknown),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            if (wifi.isOnline) R.string.status_connected else R.string.status_disconnected,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (wifi.isOnline) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = stringResource(R.string.cd_wifi),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader(title = stringResource(R.string.wifi_card_title))

            InfoRow(
                icon = Icons.Rounded.Speed,
                label = stringResource(R.string.label_link_speed),
                value = wifi.linkSpeedMbps?.let { "$it ${stringResource(R.string.unit_mbps)}" } ?: unknown,
            )
            InfoRow(
                icon = Icons.Rounded.Router,
                label = stringResource(R.string.label_band),
                value = when (wifi.band) {
                    WifiBand.GHZ_2_4 -> stringResource(R.string.band_2_4)
                    WifiBand.GHZ_5 -> stringResource(R.string.band_5)
                    WifiBand.GHZ_6 -> stringResource(R.string.band_6)
                    WifiBand.UNKNOWN -> unknown
                },
            )
            InfoRow(
                icon = Icons.Rounded.Info,
                label = stringResource(R.string.label_standard),
                value = wifi.standard ?: unknown,
            )
            InfoRow(
                icon = Icons.Rounded.Info,
                label = stringResource(R.string.label_signal),
                value = wifi.rssiDbm?.let { "$it ${stringResource(R.string.unit_dbm)}" } ?: unknown,
            )
            InfoRow(
                icon = Icons.Rounded.Info,
                label = stringResource(R.string.label_frequency),
                value = wifi.frequencyMhz?.let { "$it MHz" } ?: unknown,
            )
            InfoRow(
                icon = Icons.Rounded.Info,
                label = stringResource(R.string.label_ip),
                value = wifi.ipv4 ?: unknown,
            )
            InfoRow(
                icon = Icons.Rounded.Info,
                label = stringResource(R.string.label_gateway),
                value = wifi.gateway ?: unknown,
            )
            if (wifi.dnsServers.isNotEmpty()) {
                InfoRow(
                    icon = Icons.Rounded.Info,
                    label = "DNS",
                    value = wifi.dnsServers.joinToString(", "),
                )
            }
        }
    }
}

@Composable
private fun NotConnectedCard(isOnline: Boolean) {
    ExpressiveCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        EmptyState(
            icon = Icons.Rounded.WifiOff,
            title = stringResource(R.string.wifi_not_connected_title),
            message = stringResource(R.string.wifi_not_connected_body),
        )
        if (isOnline) {
            Text(
                text = stringResource(R.string.status_not_wifi),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TodayCard(state: UiState) {
    val downloadColor = LocalDownloadColor.current
    val uploadColor = LocalUploadColor.current

    ExpressiveCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = stringResource(R.string.label_today),
                supportingText = Format.fullDate(state.now),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    label = stringResource(R.string.label_download),
                    value = Format.bytes(state.today.rxBytes),
                    icon = Icons.Rounded.ArrowDownward,
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = downloadColor,
                )
                StatTile(
                    label = stringResource(R.string.label_upload),
                    value = Format.bytes(state.today.txBytes),
                    icon = Icons.Rounded.ArrowUpward,
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = uploadColor,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    label = stringResource(R.string.label_total),
                    value = Format.bytes(state.today.totalBytes),
                    icon = Icons.Rounded.DataUsage,
                    modifier = Modifier.weight(1f),
                    caption = Format.bytes(state.averagePerDay),
                )
                StatTile(
                    label = stringResource(R.string.label_current_speed),
                    value = Format.speed(state.currentSpeed?.totalBytesPerSecond ?: 0.0),
                    icon = Icons.Rounded.Speed,
                    modifier = Modifier.weight(1f),
                    caption = stringResource(R.string.label_session) + ": " + Format.bytes(state.session.totalBytes),
                )
            }
        }
    }
}

@Composable
private fun SessionCard(state: UiState, onResetSession: () -> Unit) {
    ExpressiveCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = stringResource(R.string.label_session),
                supportingText = stringResource(
                    R.string.session_since,
                    Format.clock(state.settings.sessionStartMillis),
                ),
                trailing = {
                    TextButton(onClick = onResetSession) {
                        Text(text = stringResource(R.string.action_reset_session))
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(
                icon = Icons.Rounded.Timer,
                label = stringResource(R.string.label_total),
                value = Format.bytes(state.session.totalBytes),
            )
            InfoRow(
                icon = Icons.Rounded.Speed,
                label = stringResource(R.string.label_current_speed),
                value = Format.speed(state.currentSpeed?.totalBytesPerSecond ?: 0.0),
            )
            InfoRow(
                icon = Icons.Rounded.ShowChart,
                label = stringResource(R.string.label_average),
                value = Format.bytes(state.averagePerDay),
            )
        }
    }
}
