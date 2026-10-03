package com.mywifiadata.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.core.Format

/**
 * قيمة بيانات مقسومة إلى رقم ووحدة كنصين متجاورين.
 *
 * الفصل مقصود: في تخطيط من اليمين إلى اليسار يظهر الرقم في البداية وبجانبه
 * الوحدة إلى اليسار («MB 532.9») كما في التصميم المرجعي، دون أن تعيد خوارزمية
 * الاتجاهين ترتيب النص داخل سطر واحد.
 */
@Composable
fun ByteValue(
    bytes: Long,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val (amount, unit) = Format.bytesParts(bytes)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = amount, style = style, color = color, maxLines = 1, softWrap = false)
        Text(text = unit, style = style, color = color, maxLines = 1, softWrap = false)
    }
}

/**
 * بلاطة استهلاك كبيرة (تنزيل/رفع): القيمة بلون دلالي في الأعلى، ثم التسمية
 * والسهم في الأسفل — كما في التصميم المرجعي.
 */
@Composable
fun TrafficTile(
    label: String,
    bytes: Long,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
        ) {
            ByteValue(
                bytes = bytes,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = accent,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = accent,
                    maxLines = 1,
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * صف الإجمالي: القيمة الكبيرة مع أيقونة دورية في البداية (يمينًا)، ثم تسمية
 * الإجمالي مع قيمته الصغيرة في الطرف الآخر (يسارًا) — كما في التصميم المرجعي.
 */
@Composable
fun TodayTotalRow(
    bytes: Long,
    label: String,
    secondaryBytes: Long,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Refresh,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLargeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ByteValue(
                bytes = bytes,
                style = MaterialTheme.typography.headlineMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                ByteValue(
                    bytes = secondaryBytes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
