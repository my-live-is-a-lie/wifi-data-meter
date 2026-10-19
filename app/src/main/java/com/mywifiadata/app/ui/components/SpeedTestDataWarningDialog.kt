package com.mywifiadata.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.SpeedTestDataUsage
import com.mywifiadata.app.ui.theme.AppButtonShape

/**
 * تحذير قبل بدء اختبار السرعة على بيانات الهاتف: يشرح أن الاختبار يحمّل
 * ويرفع بيانات حقيقية، ويعرض كمية الاستهلاك التقديرية المتكيّفة مع نتائج
 * الاختبارات السابقة.
 *
 * الخيارات: متابعة الاختبار، أو الإلغاء بلا أي استهلاك، أو تجاهل التحذير
 * ليوم كامل يعود بعده تلقائيًا.
 */
@Composable
fun SpeedTestDataWarningDialog(
    estimate: SpeedTestDataUsage.Estimate,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
    onIgnoreForDay: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                imageVector = Icons.Rounded.SignalCellularAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.speed_test_mobile_warning_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.speed_test_mobile_warning_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.speed_test_mobile_warning_estimate_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "≈ ${Format.bytes(estimate.bytes)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                if (estimate.isBasedOnHistory) {
                                    R.string.speed_test_mobile_warning_estimate_from_history
                                } else {
                                    R.string.speed_test_mobile_warning_estimate_default
                                },
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(
                    onClick = onIgnoreForDay,
                    shape = AppButtonShape,
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.speed_test_mobile_warning_ignore))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onContinue, shape = AppButtonShape) {
                Text(text = stringResource(R.string.speed_test_mobile_warning_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, shape = AppButtonShape) {
                Text(text = stringResource(R.string.speed_test_mobile_warning_cancel))
            }
        },
    )
}
