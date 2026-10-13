package com.mywifiadata.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.SpeedTestEngine
import com.mywifiadata.app.data.SPEED_TEST_HISTORY_LIMIT
import com.mywifiadata.app.data.model.SpeedTestRecord
import com.mywifiadata.app.ui.SpeedTestState
import com.mywifiadata.app.ui.theme.LocalDownloadColor
import com.mywifiadata.app.ui.theme.LocalSpeedButtonContainerColor
import com.mywifiadata.app.ui.theme.LocalSpeedButtonContentColor
import com.mywifiadata.app.ui.theme.LocalUploadColor
import java.text.NumberFormat

/**
 * بطاقة اختبار السرعة: زر التشغيل والعنوان في الأعلى، ونتائج التنزيل والرفع
 * مقابل وقت آخر اختبار. يفتح السهم السفلي سجل النتائج السابقة داخل البطاقة.
 */
@Composable
fun SpeedTestPanel(
    state: SpeedTestState,
    history: List<SpeedTestRecord>,
    nowMillis: Long,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var historyExpanded by rememberSaveable { mutableStateOf(false) }
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

    // يعرض الملخص الاختبار الأحدث بالفعل، لذا تبقى هنا التسع نتائج السابقة.
    // عند الفشل أو أثناء اختبار جديد لا توجد نتيجة في الملخص، فنُظهر أحدث نتيجة
    // محفوظة ضمن السجل أيضًا.
    val previousTests = if (state is SpeedTestState.Done) {
        history.filterNot { it.timestampMillis == state.timestampMillis }
            .take(SPEED_TEST_HISTORY_LIMIT - 1)
    } else {
        history.take(SPEED_TEST_HISTORY_LIMIT)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLargeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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

                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
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

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (statusText != null) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier.weight(1.08f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
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

            val detail = (state as? SpeedTestState.Failed)?.detail
            if (!detail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.speed_test_details, detail),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            AnimatedVisibility(
                visible = historyExpanded,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    if (previousTests.isEmpty()) {
                        Text(
                            text = stringResource(R.string.speed_test_history_empty),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        previousTests.forEach { result ->
                            SpeedTestHistoryRow(result, nowMillis)
                        }
                    }
                }
            }

            IconButton(
                onClick = { historyExpanded = !historyExpanded },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(
                        if (historyExpanded) R.string.speed_test_history_collapse
                        else R.string.speed_test_history_expand,
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (historyExpanded) 180f else 0f),
                )
            }
        }
    }
}

/** قيمة إحدى نتيجتي الاختبار (أو شرطة أثناء عدم توفّرها). */
private fun speedTestValue(state: SpeedTestState, download: Boolean): String = when (state) {
    is SpeedTestState.Done -> when {
        download -> Format.bitsPlain(state.downloadBitsPerSecond)
        state.uploadMeasured -> Format.bitsPlain(state.uploadBitsPerSecond)
        else -> NO_VALUE
    }

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

/** رسالة الحالة القصيرة بجانب نتائج الاختبار. */
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
        "${stringResource(R.string.speed_test_last_at)}\n$whenText"
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

/** سطر سجل واحد: التاريخ، الوقت النسبي، التنزيل، ثم الرفع (من اليمين إلى اليسار). */
@Composable
private fun SpeedTestHistoryRow(record: SpeedTestRecord, nowMillis: Long) {
    val dateTime = "${Format.shortDate(record.timestampMillis)} ${Format.clock(record.timestampMillis)}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dateTime,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.45f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
        )
        Text(
            text = speedTestAge(record.timestampMillis, nowMillis),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f),
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
        Text(
            text = Format.bitsPlain(record.downloadBitsPerSecond),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (record.uploadMeasured) Format.bitsPlain(record.uploadBitsPerSecond) else NO_VALUE,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
        )
    }
}

/** صيغة قصيرة للوقت المنقضي، مثل «٣٤د» و«١س» في التصميم المرجعي. */
@Composable
private fun speedTestAge(timestampMillis: Long, nowMillis: Long): String {
    val elapsed = (nowMillis - timestampMillis).coerceAtLeast(0L)
    if (elapsed < MINUTE_MILLIS) return stringResource(R.string.speed_test_age_now)

    val (resource, amount) = when {
        elapsed < HOUR_MILLIS -> R.string.speed_test_age_minutes to elapsed / MINUTE_MILLIS
        elapsed < DAY_MILLIS -> R.string.speed_test_age_hours to elapsed / HOUR_MILLIS
        elapsed < MONTH_MILLIS -> R.string.speed_test_age_days to elapsed / DAY_MILLIS
        elapsed < YEAR_MILLIS -> R.string.speed_test_age_months to elapsed / MONTH_MILLIS
        else -> R.string.speed_test_age_years to elapsed / YEAR_MILLIS
    }
    val locale = LocalConfiguration.current.locales[0]
    val formattedAmount = NumberFormat.getIntegerInstance(locale).format(amount.coerceAtLeast(1L))
    return stringResource(resource, formattedAmount)
}

private const val NO_VALUE = "—"
private const val MINUTE_MILLIS = 60_000L
private const val HOUR_MILLIS = 60L * MINUTE_MILLIS
private const val DAY_MILLIS = 24L * HOUR_MILLIS
private const val MONTH_MILLIS = 30L * DAY_MILLIS
private const val YEAR_MILLIS = 365L * DAY_MILLIS

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
