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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.UsageNetwork
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.data.model.TrafficTotals
import com.mywifiadata.app.ui.AppDetailState
import com.mywifiadata.app.ui.components.AppName
import com.mywifiadata.app.ui.components.AppIcon
import com.mywifiadata.app.ui.components.ExpressiveCard
import com.mywifiadata.app.ui.components.InfoRow
import com.mywifiadata.app.ui.theme.LocalDownloadColor
import com.mywifiadata.app.ui.theme.LocalUploadColor

/**
 * تفاصيل تطبيق واحد: استهلاكه على الشبكة المختارة في اليوم وآخر ٧ أيام وآخر ٣٠ يومًا.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppDetailScreen(
    detail: AppDetailState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ExpressiveCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.app_detail_back),
                        )
                    }
                    AppIcon(app = detail.app, size = 44.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        AppName(
                            app = detail.app,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                        )
                        Text(
                            text = detail.app.packageName
                                ?: stringResource(R.string.unknown_app_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (detail.app.isArchivedUsage) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.app_archived_usage,
                            "${Format.shortDate(detail.app.usageRecordedAt)} ${Format.clock(detail.app.usageRecordedAt)}",
                        ) + " · " + stringResource(R.string.apps_archived_usage_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        if (detail.network == UsageNetwork.WIFI) R.string.apps_network_wifi else R.string.apps_network_mobile,
                    ) + " · " + stringResource(R.string.app_detail_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (detail.isLoading) {
            item {
                ExpressiveCard {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LoadingIndicator(
                                modifier = Modifier.size(44.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.app_detail_loading),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            item {
                PeriodUsageCard(
                    title = stringResource(R.string.label_today),
                    totals = detail.today,
                    icon = Icons.Rounded.Today,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            }
            item {
                PeriodUsageCard(
                    title = stringResource(R.string.label_week),
                    totals = detail.week,
                    icon = Icons.Rounded.DateRange,
                )
            }
            item {
                PeriodUsageCard(
                    title = stringResource(R.string.label_month),
                    totals = detail.month,
                    icon = Icons.Rounded.CalendarMonth,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PeriodUsageCard(
    title: String,
    totals: TrafficTotals,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
) {
    ExpressiveCard(containerColor = containerColor) {
        // سطر واحد: الإجمالي في البداية واسم الفترة مع أيقونته في النهاية
        // (يسار في العربية) لتوفير سطر كامل وملء عرض البطاقة.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = Format.bytes(totals.totalBytes),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        InfoRow(
            icon = Icons.Rounded.ArrowDownward,
            label = stringResource(R.string.label_download),
            value = Format.bytes(totals.rxBytes),
            valueColor = LocalDownloadColor.current,
        )
        InfoRow(
            icon = Icons.Rounded.ArrowUpward,
            label = stringResource(R.string.label_upload),
            value = Format.bytes(totals.txBytes),
            valueColor = LocalUploadColor.current,
        )
    }
}
