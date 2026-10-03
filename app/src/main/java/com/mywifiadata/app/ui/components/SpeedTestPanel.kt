package com.mywifiadata.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.SpeedTestEngine
import com.mywifiadata.app.ui.SpeedTestState
import com.mywifiadata.app.ui.theme.LocalDownloadColor
import com.mywifiadata.app.ui.theme.LocalSpeedButtonContainerColor
import com.mywifiadata.app.ui.theme.LocalSpeedButtonContentColor
import com.mywifiadata.app.ui.theme.LocalUploadColor

/** عرض الشاشة الذي يكفي لوضع النتائج في سطر العنوان نفسه (كما في التصميم). */
private val INLINE_RESULTS_MIN_WIDTH = 300.dp

/**
 * لوحة اختبار السرعة داخل بطاقة اليوم: زر تشغيل أزرق في البداية (يمينًا)،
 * ثم العنوان «اختبار السرعة» وسطر «يستخدم موقع fast.com»، ثم نتائج التنزيل
 * والرفع في الطرف الآخر — كما في التصميم المرجعي.
 *
 * تُضاف تحت ذلك رسالة الحالة: مرحلة القياس أثناء التشغيل، سبب التوقف عند
 * انقطاع الاتصال أو مرور وقت طويل دون نتيجة، أو وقت آخر اختبار محفوظ.
 */
@Composable
fun SpeedTestPanel(
    state: SpeedTestState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val running = state is SpeedTestState.Running
    val downloadColor = LocalDownloadColor.current
    val uploadColor = LocalUploadColor.current
    val buttonContainer = LocalSpeedButtonContainerColor.current
    val buttonContent = LocalSpeedButtonContentColor.current

    val statusText = speedTestStatusText(state)
    val statusColor = if (state is SpeedTestState.Failed) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLargeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // على الشاشات الضيقة تُنقل النتائج إلى سطر ثانٍ حتى لا يُقتطع العنوان.
            val stackResults = maxWidth < INLINE_RESULTS_MIN_WIDTH

            val controlButton: @Composable () -> Unit = {
                Surface(
                    onClick = { if (running) onStop() else onStart() },
                    shape = RoundedCornerShape(14.dp),
                    color = buttonContainer,
                    contentColor = buttonContent,
                    modifier = Modifier.size(width = 56.dp, height = 46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (running) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = buttonContent,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.speed_test_start),
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            }

            val titleBlock: @Composable (Modifier) -> Unit = { blockModifier ->
                Column(modifier = blockModifier) {
                    Text(
                        text = stringResource(R.string.speed_test_title),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.speed_test_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            val resultsRow: @Composable (Modifier) -> Unit = { rowModifier ->
                Row(
                    modifier = rowModifier,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SpeedResult(
                        label = stringResource(R.string.label_download),
                        value = speedTestValue(state, download = true),
                        icon = Icons.Rounded.ArrowDownward,
                        tint = downloadColor,
                    )
                    SpeedResult(
                        label = stringResource(R.string.label_upload),
                        value = speedTestValue(state, download = false),
                        icon = Icons.Rounded.ArrowUpward,
                        tint = uploadColor,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    controlButton()
                    Spacer(modifier = Modifier.width(10.dp))
                    titleBlock(Modifier.weight(1f))
                    if (!stackResults) {
                        Spacer(modifier = Modifier.width(10.dp))
                        resultsRow(Modifier)
                    }
                }

                if (stackResults) {
                    Spacer(modifier = Modifier.height(12.dp))
                    resultsRow(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.End),
                    )
                }

                if (statusText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** قيمة إحدى نتيجتي الاختبار (أو شرطة أثناء عدم توفّرها). */
private fun speedTestValue(state: SpeedTestState, download: Boolean): String = when (state) {
    is SpeedTestState.Done -> Format.bitsPlain(
        if (download) state.downloadBitsPerSecond else state.uploadBitsPerSecond,
    )

    is SpeedTestState.Running -> {
        val measuredPhase =
            if (download) SpeedTestEngine.Phase.DOWNLOAD else SpeedTestEngine.Phase.UPLOAD
        if (state.phase == measuredPhase && state.bitsPerSecond > 0.0) {
            Format.bitsPlain(state.bitsPerSecond)
        } else {
            NO_VALUE
        }
    }

    else -> NO_VALUE
}

/** رسالة الحالة أسفل الصف: المرحلة الجارية، سبب الفشل، أو وقت آخر نتيجة. */
@Composable
private fun speedTestStatusText(state: SpeedTestState): String? = when (state) {
    is SpeedTestState.Idle -> stringResource(R.string.speed_test_never)

    is SpeedTestState.Running -> stringResource(
        when (state.phase) {
            SpeedTestEngine.Phase.PREPARING -> R.string.speed_test_running_preparing
            SpeedTestEngine.Phase.DOWNLOAD -> R.string.speed_test_running_download
            SpeedTestEngine.Phase.UPLOAD -> R.string.speed_test_running_upload
            SpeedTestEngine.Phase.FINISHED -> R.string.speed_test_finished
        },
    )

    is SpeedTestState.Done -> {
        val whenText = "${Format.shortDate(state.timestampMillis)} ${Format.clock(state.timestampMillis)}"
        stringResource(
            if (state.usedFastCom) R.string.speed_test_last_at else R.string.speed_test_last_at_fallback,
            whenText,
        )
    }

    is SpeedTestState.Failed -> stringResource(
        when (state.failure) {
            SpeedTestEngine.Failure.NO_CONNECTION -> R.string.speed_test_no_connection
            SpeedTestEngine.Failure.CONNECTION_LOST -> R.string.speed_test_connection_lost
            SpeedTestEngine.Failure.STALLED -> R.string.speed_test_stalled
            SpeedTestEngine.Failure.TIMEOUT -> R.string.speed_test_timeout
            SpeedTestEngine.Failure.ERROR -> R.string.speed_test_failed
        },
    )
}

private const val NO_VALUE = "—"

/** عمود نتيجة واحدة: القيمة فوق، وتحتها التسمية ثم السهم بلونها الدلالي. */
@Composable
private fun SpeedResult(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
