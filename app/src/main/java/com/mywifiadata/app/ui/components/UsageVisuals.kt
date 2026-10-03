package com.mywifiadata.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.pointer.pointerInput

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
 * مخطط أعمدة لاستهلاك الأيام، مع إبراز اليوم الحالي، ودعم «المؤشر» (المتزلّج):
 * عند تمرير المؤشر يتّسع عمود اليوم المشار إليه أفقيًا وتنكمش الأعمدة المجاورة
 * وتنزاح، ويظهر يومه في صندوق فوقه.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DailyBarChart(
    entries: List<BarEntry>,
    modifier: Modifier = Modifier,
    barHeight: Dp = 140.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightColor: Color = MaterialTheme.colorScheme.tertiary,
    /** فهرس اليوم الذي يشير إليه المؤشر أثناء السحب (null عند عدم الاستخدام). */
    activeIndex: Int? = null,
    onEntryClick: ((Int) -> Unit)? = null,
) {
    val maxValue = (entries.maxOfOrNull { it.value } ?: 0L).coerceAtLeast(1L)
    // نعرض عددًا محدودًا من التسميات (٦ كحد أقصى) لأن عمود اليوم الواحد ضيّق جدًا
    // عند عرض ٣٠ يومًا، فتتكسّر الكلمة العربية حرفًا في كل سطر.
    val labelStride = ((entries.size + MAX_LABELS - 1) / MAX_LABELS).coerceAtLeast(1)
    val spacing = chartSpacing(entries.size)

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
            // عرض العمود يتبع المؤشر: المشار إليه يتّسع والمحيط به ينكمش وينزاح.
            val weight by animateFloatAsState(
                targetValue = barWeight(index = index, activeIndex = activeIndex),
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                label = "bar-weight-$index",
            )
            val isActive = index == activeIndex

            val clickModifier = if (onEntryClick != null) {
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onEntryClick(index) }
            } else {
                Modifier
            }

            Column(
                modifier = Modifier
                    .weight(weight)
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
                            // سقف لعرض العمود حتى لا يبدو ضخمًا عند عرض يوم واحد فقط،
                            // ويُرفع للعمود النشط ليتّسع أفقيًا كما في التصميم.
                            .widthIn(max = if (isActive) ACTIVE_BAR_MAX_WIDTH else 56.dp)
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
                            .background(
                                if (entry.highlight || isActive) highlightColor else barColor,
                            ),
                    )
                }
                // تسمية اليوم: صندوق ملوّن لليوم الذي يشير إليه المؤشر، ونص عادي لغيره.
                val labelText = if (index % labelStride == 0 || index == entries.lastIndex || isActive) {
                    entry.label
                } else {
                    ""
                }
                // ارتفاع صف التسميات ثابت حتى لا يقفز المخطط عند ظهور الصندوق.
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .height(LABEL_ROW_HEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = if (isActive) {
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        } else {
                            Modifier
                        },
                    ) {
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                        )
                    }
                }
            }
        }
    }
}

/**
 * شريط تمرير أسفل المخطط لاختيار اليوم: خط مسار أفقي، ونقطة عليه يعلوها مثلث
 * صغير يشير إلى عمود اليوم الحالي. عند الضغط أو السحب تكبر النقطة قليلًا
 * ويظهر يومها في صندوق فوق العمود، ويتحرك المؤشر مع الإصبع.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DayScrubber(
    count: Int,
    /** فهرس اليوم الذي يقف عليه المؤشر. */
    thumbIndex: Int,
    /** هل المستخدم يضغط/يسحب المؤشر الآن؟ */
    active: Boolean,
    spacing: Dp,
    onScrubStart: () -> Unit,
    onScrub: (Int) -> Unit,
    onScrubEnd: (Int) -> Unit,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** الفهرس الذي يتّسع عموده (نفس فهرس المؤشر أثناء السحب). */
    activeIndex: Int? = null,
) {
    if (count < 2) return
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val thumbColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SCRUBBER_HEIGHT)
            .pointerInput(count, rtl) {
                // آخر فهرس وصل إليه السحب، لتمريره عند الإفلات.
                var current = thumbIndex.coerceIn(0, count - 1)
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        onScrubStart()
                        current = scrubIndexAt(
                            xPx = offset.x,
                            widthPx = size.width.toFloat(),
                            count = count,
                            rtl = rtl,
                        )
                        onScrub(current)
                    },
                    onDragEnd = { onScrubEnd(current) },
                    onDragCancel = { onScrubEnd(current) },
                ) { change, _ ->
                    change.consume()
                    current = scrubIndexAt(
                        xPx = change.position.x,
                        widthPx = size.width.toFloat(),
                        count = count,
                        rtl = rtl,
                    )
                    onScrub(current)
                }
            }
            .pointerInput(count, rtl) {
                detectTapGestures { offset ->
                    onJump(
                        scrubIndexAt(
                            xPx = offset.x,
                            widthPx = size.width.toFloat(),
                            count = count,
                            rtl = rtl,
                        ),
                    )
                }
            },
    ) {
        // خط المسار
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        )
        // خلايا الأيام: نفس أوزان أعمدة المخطط حتى يقع المؤشر تحت عموده تمامًا.
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            repeat(count) { index ->
                val weight by animateFloatAsState(
                    targetValue = barWeight(index = index, activeIndex = activeIndex),
                    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                    label = "slider-weight-$index",
                )
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (index == thumbIndex) {
                        Thumb(active = active, color = thumbColor)
                    }
                }
            }
        }
    }
}

/**
 * نقطة المؤشر: دائرة تجلس على خط المسار بالضبط، ويعلوها مثلث صغير يشير
 * إلى عمود اليوم. تكبر قليلًا أثناء الضغط أو السحب.
 */
@Composable
private fun Thumb(active: Boolean, color: Color) {
    val scale by animateFloatAsState(
        targetValue = if (active) THUMB_ACTIVE_SCALE else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "thumb-scale",
    )
    Canvas(modifier = Modifier.size(width = 22.dp, height = SCRUBBER_HEIGHT)) {
        val centerX = size.width / 2f
        // مركز الدائرة = مركز الشريط = خط المسار نفسه.
        val centerY = size.height / 2f
        val circleRadius = 4.dp.toPx() * scale
        val triangleHeight = 6.dp.toPx() * scale
        val triangleHalfWidth = 4.dp.toPx() * scale
        val baseY = centerY - circleRadius - 1.dp.toPx()

        val triangle = Path().apply {
            moveTo(centerX, baseY - triangleHeight)
            lineTo(centerX - triangleHalfWidth, baseY)
            lineTo(centerX + triangleHalfWidth, baseY)
            close()
        }
        drawPath(path = triangle, color = color)
        drawCircle(color = color, radius = circleRadius, center = Offset(centerX, centerY))
    }
}

/**
 * وزن عمود واحد في المخطط وشريط التمرير:
 * 1 للعادي، أكبر للعمود المشار إليه (يتّسع أفقيًا)، وأقل لمجاوريه (ينكمشان).
 */
internal fun barWeight(index: Int, activeIndex: Int?): Float = when {
    activeIndex == null -> 1f
    index == activeIndex -> 1f + BAR_EXPANSION
    index == activeIndex - 1 || index == activeIndex + 1 -> 1f - BAR_NEIGHBOR_SHRINK
    else -> 1f
}

/** مسافة الأعمدة كما في المخطط (نفسها في شريط التمرير للتوافق التام). */
internal fun chartSpacing(count: Int): Dp = if (count > 14) 3.dp else 6.dp

/**
 * يحوّل موضع اللمس الأفقي إلى فهرس اليوم:
 * في تخطيط من اليمين لليسار يبدأ اليوم الأقدم من اليمين، لذا يُعكس الفهرس.
 */
internal fun scrubIndexAt(xPx: Float, widthPx: Float, count: Int, rtl: Boolean): Int {
    if (count <= 1) return 0
    val fraction = (xPx / widthPx.coerceAtLeast(1f)).coerceIn(0f, 0.999_9f)
    val logical = (fraction * count).toInt().coerceIn(0, count - 1)
    return if (rtl) count - 1 - logical else logical
}

/** ارتفاع صف تسميات الأيام (ثابت حتى لا يقفز المخطط عند ظهور صندوق اليوم). */
private val LABEL_ROW_HEIGHT = 18.dp

/** ارتفاع شريط التمرير (يستوعب النقطة والمثلث). */
private val SCRUBBER_HEIGHT = 34.dp

/** مقدار تكبير نقطة المؤشر أثناء الضغط أو السحب. */
private const val THUMB_ACTIVE_SCALE = 1.3f

/** أقصى عرض لعمود اليوم المشار إليه عند اتساعه. */
private val ACTIVE_BAR_MAX_WIDTH = 84.dp

/** زيادة وزن العمود المشار إليه (اتساع أفقي). */
internal const val BAR_EXPANSION = 0.9f

/** نقصان وزن العمودين المجاورين للمشار إليه (انكماش وإزاحة). */
internal const val BAR_NEIGHBOR_SHRINK = 0.35f

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
