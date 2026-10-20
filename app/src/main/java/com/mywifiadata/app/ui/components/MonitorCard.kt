package com.mywifiadata.app.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.data.model.MonitorTarget

/**
 * بطاقة النافذة العائمة على لوحة المعلومات: يُفتح منتقي التطبيقات من أيقونة
 * الهدف في الطرف، ويبدأ عرض النافذة من زر التشغيل.
 *
 * الأيقونة تعرض شعار التطبيق المراقب نفسه بعد اختياره، أو شبكة التطبيقات
 * عندما يكون الهدف «جميع التطبيقات».
 */
@Composable
fun MonitorCard(
    target: MonitorTarget,
    isRunning: Boolean,
    isEditing: Boolean,
    onOpenPicker: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetLabel = if (target.isAllApps) {
        stringResource(R.string.monitor_all_apps)
    } else {
        target.label.ifBlank { stringResource(R.string.value_unknown) }
    }
    ExpressiveCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(enabled = !isEditing, onClick = onStart),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.monitor_start),
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = stringResource(R.string.monitor_card_title),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isRunning) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }
                    Text(
                        text = targetLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(enabled = !isEditing, onClick = onOpenPicker),
                contentAlignment = Alignment.Center,
            ) {
                if (target.isAllApps) {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = stringResource(R.string.monitor_pick_target),
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    AppIcon(app = target.asUsage(), size = 40.dp)
                }
            }
        }
    }
}
