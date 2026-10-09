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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
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
 * مؤشر استهلاك بقوس مفتوح ذي مسار منقّط. تظهر نقاط داكنة داخل قوس التقدّم الأزرق،
 * مع طرف مستوٍ للقوس ورأس دائري للمؤشر الشعاعي. يبدأ المسار من أسفل اليسار ويمر
 * أعلى البطاقة حتى أسفل اليمين مثل التصميم المرجعي.
 */
@Composable
fun DottedUsageGauge(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 9.dp,
    dotRadius: Dp = 2.dp,
    dotCount: Int = 31,
    dotColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
    /** لون الثقوب الداكنة فوق جزء التقدّم المكتمل. */
    activeDotColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
    progressColor: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "dotted-usage-gauge-progress",
    )

    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = strokeWidth.toPx()
            val dot = dotRadius.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension - stroke) / 2f
            val arcSize = Size(radius * 2f, radius * 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            // أطراف القوس مرفوعة قليلًا عن الوضع الافتراضي، فتكون الفتحة السفلية أوسع وأقصر.
            val startAngle = 145f
            val sweepAngle = 250f
            val steps = dotCount.coerceAtLeast(2)

            val progressSweep = sweepAngle * animatedProgress
            // فجوة صغيرة تعادل نصف سماكة الشريط، كي يقترب المؤشر من القوس دون أن يلتصق به.
            val indicatorGap = Math.toDegrees((stroke * 0.5f / radius).toDouble()).toFloat()
            val arcSweep = (progressSweep - indicatorGap).coerceAtLeast(0f)

            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (arcSweep > 0.001f) {
                // نهاية التقدّم مسطّحة كما في المرجع، مع تدوير بداية الشريط فقط.
                drawArc(
                    color = progressColor,
                    startAngle = startAngle,
                    sweepAngle = arcSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                val startRadians = Math.toRadians(startAngle.toDouble())
                drawCircle(
                    color = progressColor,
                    radius = stroke / 2f,
                    center = Offset(
                        x = center.x + cos(startRadians).toFloat() * radius,
                        y = center.y + sin(startRadians).toFloat() * radius,
                    ),
                )
            }

            // النقاط غير المكتملة تبقى زرقاء، بينما تظهر نقاط داكنة داخل الشريط الأزرق.
            // نخفي أقرب نقطة زرقاء للمؤشر حتى لا تظهر تحته أو خلف الشرطة.
            val dotStep = sweepAngle / (steps - 1)
            for (index in 0 until steps) {
                val dotSweep = dotStep * index
                val angle = Math.toRadians((startAngle + dotSweep).toDouble())
                val isActiveDot = index > 0 && dotSweep < arcSweep
                val isPointerNeighbor = animatedProgress > 0.001f &&
                    abs(dotSweep - progressSweep) <= dotStep * 0.55f
                if (index == 0 && arcSweep > 0.001f) continue
                if (isPointerNeighbor && !isActiveDot) continue
                drawCircle(
                    color = if (isActiveDot) activeDotColor else dotColor,
                    radius = if (isActiveDot) dot * 0.72f else dot,
                    center = Offset(
                        x = center.x + cos(angle).toFloat() * radius,
                        y = center.y + sin(angle).toFloat() * radius,
                    ),
                )
            }

            if (animatedProgress > 0.001f) {
                // المؤشر الشعاعي له قاعدة مستوية ورأس خارجي دائري، بدل الكبسولة الدائرية من الطرفين.
                val pointerAngle = Math.toRadians((startAngle + progressSweep).toDouble())
                val pointerLength = radius * 0.18f
                val pointerRadiusStart = radius - pointerLength / 2f
                val pointerRadiusEnd = radius + pointerLength / 2f
                val pointerX = cos(pointerAngle).toFloat()
                val pointerY = sin(pointerAngle).toFloat()
                val pointerStroke = stroke * 0.40f
                val pointerEnd = Offset(
                    x = center.x + pointerX * pointerRadiusEnd,
                    y = center.y + pointerY * pointerRadiusEnd,
                )
                drawLine(
                    color = progressColor,
                    start = Offset(
                        x = center.x + pointerX * pointerRadiusStart,
                        y = center.y + pointerY * pointerRadiusStart,
                    ),
                    end = pointerEnd,
                    strokeWidth = pointerStroke,
                    cap = StrokeCap.Butt,
                )
                drawCircle(
                    color = progressColor,
                    radius = pointerStroke / 2f,
                    center = pointerEnd,
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

    Column(modifier = modifier.fillMaxWidth()) {
        // صف الأعمدة: عرض كل عمود يتبع المؤشر (المشار إليه يتّسع والبقية تنزاح).
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                val weight by animateFloatAsState(
                    targetValue = barWeight(
                        index = index,
                        activeIndex = activeIndex,
                        count = entries.size,
                    ),
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
                                // سقف لعرض العمود حتى لا يبدو ضخمًا عند عرض يوم واحد
                                // فقط، ويُرفع للعمود النشط ليتّسع أفقيًا.
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
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // تسميات الأيام في طبقة مستقلة أسفل الأعمدة: تُقاس بحرّية ولا تخضع لعرض
        // خانة العمود الضيقة، فلا يُقص رقم اليوم ولا صندوقه.
        DayLabelsRow(
            entries = entries,
            activeIndex = activeIndex,
            spacing = spacing,
            stride = labelStride,
        )
    }
}

/**
 * صف تسميات الأيام: كل تسمية تُقاس بحرّية (بلا قيد عرض خانتها) وتُوسَّط على
 * خانتها بالحساب، فيظهر رقم اليوم وصندوقه كاملين مهما ضاقت الأعمدة.
 *
 * يوازن هذا الصف صفَّ الأعمدة تمامًا: نفس التباعد ونفس الأوزان، لأن موضع كل
 * تسمية يُحسب من نفس معادلة توزيع العرض.
 */
@Composable
private fun DayLabelsRow(
    entries: List<BarEntry>,
    activeIndex: Int?,
    spacing: Dp,
    stride: Int,
    modifier: Modifier = Modifier,
) {
    val pillColor = MaterialTheme.colorScheme.primaryContainer
    val onPillColor = MaterialTheme.colorScheme.onPrimaryContainer
    val normalColor = MaterialTheme.colorScheme.onSurfaceVariant

    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            entries.forEachIndexed { index, entry ->
                val isActive = index == activeIndex
                if (showDayLabel(index, entries.size, stride, activeIndex)) {
                    if (isActive) {
                        // صندوق اليوم: Box بقياس محسوس ومحتوى مُوسَّط تمامًا، فيظهر
                        // الرقم في منتصفه مهما كان عدد خاناته وفي كل الاتجاهات.
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = DAY_PILL_MIN_WIDTH)
                                .clip(RoundedCornerShape(9.dp))
                                .background(pillColor)
                                .padding(
                                    horizontal = DAY_PILL_HORIZONTAL_PADDING,
                                    vertical = DAY_PILL_VERTICAL_PADDING,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = entry.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = onPillColor,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    } else {
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = normalColor,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier)
                }
            }
        },
    ) { measurables, constraints ->
        val spacingPx = spacing.roundToPx()
        val weights = entries.indices.map { index ->
            barWeight(index = index, activeIndex = activeIndex, count = entries.size)
        }
        val totalWeight = weights.sum().coerceAtLeast(0.0001f)
        val available = (constraints.maxWidth - spacingPx * (entries.size - 1))
            .coerceAtLeast(1)
        // قياس حر (0..∞) لكل تسمية حتى يتّسع صندوق اليوم بلا قصّ.
        val placeables = measurables.map { measurable -> measurable.measure(Constraints()) }
        // ارتفاع ثابت لا يقل عن ارتفاع صندوق اليوم، فلا يقفز المخطط عند ظهوره.
        val rowHeight = maxOf(
            placeables.maxOfOrNull { it.height } ?: 0,
            DAY_LABEL_ROW_MIN_HEIGHT.roundToPx(),
        )

        layout(width = constraints.maxWidth, height = rowHeight) {
            var cursor = 0f
            weights.forEachIndexed { index, weight ->
                val slot = available * weight / totalWeight
                val placeable = placeables[index]
                if (placeable.width > 0) {
                    val center = cursor + slot / 2f
                    // تثبيت التسمية داخل حدود المخطط: صندوق اليوم أعرض من خانته،
                    // فيُزاح قليلًا عند أول/آخر يوم بدل أن يُقص عند حافة البطاقة.
                    val maxX = (constraints.maxWidth - placeable.width).coerceAtLeast(0)
                    val x = (center - placeable.width / 2f).roundToInt().coerceIn(0, maxX)
                    placeable.placeRelative(x = x, y = (rowHeight - placeable.height) / 2)
                }
                cursor += slot + spacingPx
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
        // خط المسار: يجلس قريبًا من أسفل الشريط فيقلّ الفراغ أسفل البطاقة.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = SCRUBBER_TRACK_BOTTOM_GAP - TRACK_THICKNESS / 2)
                .fillMaxWidth()
                .height(TRACK_THICKNESS)
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
                    targetValue = barWeight(
                        index = index,
                        activeIndex = activeIndex,
                        count = count,
                    ),
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
    Canvas(modifier = Modifier.size(width = 30.dp, height = SCRUBBER_HEIGHT)) {
        val centerX = size.width / 2f
        // مركز الدائرة على خط المسار نفسه (نفس الإزاحة المستخدمة في رسم الخط).
        val centerY = size.height - SCRUBBER_TRACK_BOTTOM_GAP.toPx()
        val circleRadius = 5.5.dp.toPx() * scale
        val triangleHeight = 7.5.dp.toPx() * scale
        val triangleHalfWidth = 5.dp.toPx() * scale
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
 * وزن عمود واحد في المخطط وشريط التمرير.
 *
 * عندما يقف المؤشر على يوم، يتّسع عموده إلى [ACTIVE_BAR_WEIGHT] ضعفًا، بينما
 * تتقاسم **بقية الأعمدة كلها** العرض المتبقّي بالتساوي (فتنزاح جميعها ولا
 * يبقى الانزياح محصورًا في العمود الملاصق). مجموع الأوزان يبقى مساويًا لعدد
 * الأعمدة، فلا يتغيّر عرض المخطط ولا يقفز.
 */
internal fun barWeight(index: Int, activeIndex: Int?, count: Int): Float {
    if (activeIndex == null || count < 2) return 1f
    if (index == activeIndex) return ACTIVE_BAR_WEIGHT
    val othersWeight = (count - ACTIVE_BAR_WEIGHT) / (count - 1)
    return othersWeight.coerceAtLeast(MIN_OTHERS_WEIGHT)
}

/**
 * هل تُعرض تسمية هذا اليوم؟
 *
 * التسميات المنتظمة (كل [stride]) تظهر دائمًا، لكن أثناء سحب المؤشر تُخفى
 * تسمية اليوم المشار إليه وجيرانه المباشرين، لأن صندوق اليوم يتّسع أفقيًا
 * فتتراكب الأرقام كما لو كانت رقمًا واحدًا مقطوعًا.
 */
internal fun showDayLabel(index: Int, count: Int, stride: Int, activeIndex: Int?): Boolean {
    val isRegular = index % stride == 0 || index == count - 1
    if (activeIndex == null) return isRegular
    if (index == activeIndex) return true
    if (abs(index - activeIndex) <= 1) return false
    return isRegular
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

/** أقل ارتفاع لصف تسميات الأيام = ارتفاع صندوق اليوم، فلا يقفز المخطط عند ظهوره. */
private val DAY_LABEL_ROW_MIN_HEIGHT = 22.dp

/**
 * ارتفاع شريط التمرير: يكفي المثلث والنقطة فوق خط المسار دون فراغ زائد أسفله.
 */
private val SCRUBBER_HEIGHT = 28.dp

/** المسافة من أسفل الشريط إلى مركز خط المسار. */
private val SCRUBBER_TRACK_BOTTOM_GAP = 8.dp

/** سمك خط المسار. */
private val TRACK_THICKNESS = 4.dp

/** مقدار تكبير نقطة المؤشر أثناء الضغط أو السحب. */
private const val THUMB_ACTIVE_SCALE = 1.3f

/** أقصى عرض لعمود اليوم المشار إليه عند اتساعه. */
private val ACTIVE_BAR_MAX_WIDTH = 84.dp

/** عرض العمود المشار إليه كنسبة من عمود عادي (يتّسع أفقيًا). */
internal const val ACTIVE_BAR_WEIGHT = 2.6f

/** حدّ أدنى لوزن العمود العادي حتى لا يختفي عند قلة الأعمدة. */
internal const val MIN_OTHERS_WEIGHT = 0.25f

/** عرض الصندوق الأزرق الذي يعرض رقم اليوم المشار إليه. */
private val DAY_PILL_MIN_WIDTH = 38.dp

/** الحشوة الأفقية داخل صندوق اليوم: توسّعه من الجانبين حول الرقم. */
private val DAY_PILL_HORIZONTAL_PADDING = 9.dp

/** الحشوة الرأسية داخل صندوق اليوم. */
private val DAY_PILL_VERTICAL_PADDING = 2.dp

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
