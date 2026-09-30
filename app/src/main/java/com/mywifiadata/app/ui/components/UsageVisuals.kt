package com.mywifiadata.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** أقصى عدد تسميات على محور المخطط. */
private const val MAX_LABELS = 6

/**
 * حلقة استهلاك متحرّكة تُستخدم لعرض نسبة الاستهلاك من الباقة.
 */
@Composable
fun UsageRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 20.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    startColor: Color = MaterialTheme.colorScheme.primary,
    endColor: Color = MaterialTheme.colorScheme.tertiary,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "usage-ring-progress",
    )

    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (animatedProgress > 0.001f) {
                val brush = Brush.sweepGradient(
                    0f to startColor,
                    0.45f to endColor,
                    1f to startColor,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
                drawArc(
                    brush = brush,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/**
 * أيقونة قوة الإشارة (أربعة أعمدة) مرسومة يدويًا لمظهر أنعم من أيقونة النظام.
 */
@Composable
fun SignalBarsIcon(
    level: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val animatedLevel by animateFloatAsState(
        targetValue = level.coerceIn(0, 4).toFloat(),
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "signal-level",
    )

    Canvas(modifier = modifier) {
        val barCount = 4
        val gap = size.width * 0.1f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

        for (index in 0 until barCount) {
            val fraction = 0.4f + 0.6f * ((index + 1).toFloat() / barCount)
            val barHeight = size.height * fraction
            val filled = index < animatedLevel
            drawRoundRect(
                color = if (filled) activeColor else inactiveColor,
                topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = radius,
            )
        }
    }
}

/**
 * شريط تقدّم مستدير متحرّك.
 */
@Composable
fun QuotaBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    startColor: Color = MaterialTheme.colorScheme.primary,
    endColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "quota-bar-progress",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(startColor, endColor))),
        )
    }
}

/** عنصر واحد في مخطط أعمدة الأيام. */
data class BarEntry(
    val label: String,
    val value: Long,
    val highlight: Boolean = false,
)

/**
 * مخطط أعمدة بسيط لاستهلاك الأيام، مع إبراز اليوم الحالي.
 */
@Composable
fun DailyBarChart(
    entries: List<BarEntry>,
    modifier: Modifier = Modifier,
    barHeight: Dp = 140.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightColor: Color = MaterialTheme.colorScheme.tertiary,
    onEntryClick: ((Int) -> Unit)? = null,
) {
    val maxValue = (entries.maxOfOrNull { it.value } ?: 0L).coerceAtLeast(1L)
    // نعرض عددًا محدودًا من التسميات (٦ كحد أقصى) لأن عمود اليوم الواحد ضيّق جدًا
    // عند عرض ٣٠ يومًا، فتتكسّر الكلمة العربية حرفًا في كل سطر.
    val labelStride = ((entries.size + MAX_LABELS - 1) / MAX_LABELS).coerceAtLeast(1)
    val spacing = if (entries.size > 14) 3.dp else 6.dp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.Bottom,
    ) {
        entries.forEachIndexed { index, entry ->
            val fraction = (entry.value.toFloat() / maxValue.toFloat()).coerceIn(0.03f, 1f)
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                label = "bar-$index",
            )

            val clickModifier = if (onEntryClick != null) {
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onEntryClick(index) }
            } else {
                Modifier
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(clickModifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            // سقف لعرض العمود حتى لا يبدو ضخمًا عند عرض يوم واحد فقط
                            .widthIn(max = 56.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(animatedFraction)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 8.dp,
                                    topEnd = 8.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 4.dp,
                                ),
                            )
                            .background(if (entry.highlight) highlightColor else barColor),
                    )
                }
                Text(
                    text = if (index % labelStride == 0 || index == entries.lastIndex) entry.label else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                )
            }
        }
    }
}

/** نقطة ملوّنة صغيرة تُستخدم في وسائل الإيضاح. */
@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(color),
    )
}
