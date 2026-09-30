package com.mywifiadata.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.ui.theme.LocalDownloadColor
import com.mywifiadata.app.ui.theme.LocalUploadColor

/** أيقونة تطبيق مقروءة من نظام الحزمة (null إذا تعذّر قراءتها). */
@Composable
fun appIconBitmap(packageName: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(packageName) {
        val name = packageName ?: return@remember null
        runCatching {
            val packageManager = context.packageManager
            val info = packageManager.getApplicationInfo(name, 0)
            packageManager.getApplicationIcon(info).toBitmap(width = 96, height = 96).asImageBitmap()
        }.getOrNull()
    }
}

/** أيقونة تطبيق مستديرة جاهزة للعرض. */
@Composable
fun AppIcon(
    packageName: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val bitmap = appIconBitmap(packageName) ?: return
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 2.5f)),
    )
}

/**
 * سطر استهلاك تطبيق واحد: الأيقونة، الاسم، التنزيل/الرفع، الإجمالي ونسبة الاستهلاك.
 *
 * عند تمرير [onClick] يصبح السطر قابلًا للضغط (لفتح تفاصيل التطبيق).
 */
@Composable
fun AppUsageRow(
    app: AppUsage,
    share: Float,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val downloadColor = LocalDownloadColor.current
    val uploadColor = LocalUploadColor.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (rank != null) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = rank.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            if (app.packageName != null) {
                AppIcon(packageName = app.packageName, size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(color = downloadColor, size = 8.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Format.bytes(app.totals.rxBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(color = uploadColor, size = 8.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Format.bytes(app.totals.txBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Format.bytes(app.totals.totalBytes),
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = context.getString(R.string.percent_format, (share * 100).toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.size(8.dp))

        QuotaBar(
            progress = share,
            height = 6.dp,
            startColor = downloadColor,
            endColor = uploadColor,
        )
    }
}
