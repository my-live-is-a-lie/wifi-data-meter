package com.mywifiadata.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.ClockHandGeometry
import com.mywifiadata.app.core.HandObstacle
import com.mywifiadata.app.ui.theme.LocalDownloadColor
import com.mywifiadata.app.ui.theme.LocalUploadColor
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/**
 * بطاقة «ساعة الاستهلاك»: ميناء ساعة منقّط تحيط به حلقة، ويحيط بالحلقة قوس
 * يعرض نسبة الاستهلاك من الباقة (يبدأ من أعلى الدائرة ويدور عكس عقارب الساعة
 * كما في التصميم المطلوب). داخل الميناء نص الساعة الحالية، ونسبة الاستهلاك،
 * وتسمية «المستهلك» مع قيمته، وعدد الأيام المتبقية أعلى الدائرة.
 *
 * عقارب الساعة (الثواني، الساعات، الدقائق) تتحرك مع وقت الجهاز الحقيقي،
 * وتُرسم بأطوال مختلفة: الثواني الأقصر ثم الساعات ثم الدقائق الأطول،
 * مع رسم الأقصر فوق الأطول. وعندما يتجه عقرب نحو نص (نسبة الاستهلاك أو عدد
 * الأيام المتبقية) يتقلّص من جهة النص فلا يلامسه ولا يغطيه.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ClockUsageGauge(
    progress: Float,
    daysLeft: Int?,
    timeMillis: Long,
    headline: String,
    label: String,
    value: String,
    exceeded: Boolean,
    modifier: Modifier = Modifier,
    maxDialSize: Dp = 252.dp,
    contentDescription: String? = null,
) {
    val density = LocalDensity.current
    val numberGapPx = with(density) { 5.dp.toPx() }
    val minHandPx = with(density) { 3.dp.toPx() }

    val contentColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val errorColor = MaterialTheme.colorScheme.error
    val ringTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    // ألوان التنزيل (أزرق) والرفع (بنفسجي) هي نفسها في التصميم المرجعي:
    // نقاط الميناء والعقرب الأزرقان، وقوس الاستهلاك البنفسجي الفاتح.
    val accentColor = LocalDownloadColor.current
    val arcAccentColor = LocalUploadColor.current
    val hourDotColor = accentColor.copy(alpha = 0.92f)
    val minuteDotColor = accentColor.copy(alpha = 0.42f)
    val arcColor = if (exceeded) errorColor else arcAccentColor
    val hourHandColor = accentColor
    val minuteHandColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val secondHandColor = errorColor

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "clock-usage-gauge-progress",
    )

    // زوايا العقارب من وقت الجهاز؛ الثواني تُقاد بالثانية الكاملة، والدقائق
    // والساعات تتقدم تدريجيًا معها فلا تقفز بين المواضع.
    val clock = remember(timeMillis) { clockHandsFor(timeMillis) }
    val clockText = remember(clock) {
        String.format(java.util.Locale.US, "%02d:%02d", clock.hourOfDay, clock.minute)
    }

    var centerBlock by remember { mutableStateOf(Rect.Zero) }
    var daysBlock by remember { mutableStateOf(Rect.Zero) }
    val daysUnit = stringResource(R.string.clock_usage_days_unit)

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val dialSize = minOf(maxWidth, maxDialSize)

        Box(
            modifier = Modifier.size(dialSize),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .semantics {
                        if (contentDescription != null) this.contentDescription = contentDescription
                    },
            ) {
                val clockRadius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val dotRingRadius = clockRadius * 0.85f
                val arcRadius = clockRadius * 0.965f
                val arcStroke = 7.dp.toPx()
                val hourDotRadius = 3.dp.toPx()
                val minuteDotRadius = 1.5.dp.toPx()

                // مسار باهت خلف النقاط يعطي إحساس قرص الساعة.
                drawCircle(
                    color = ringTrackColor,
                    radius = dotRingRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )

                // قوس الاستهلاك خارج الحلقة، من أعلى الدائرة عكس عقارب الساعة.
                if (animatedProgress > 0.0005f) {
                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = -360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                        size = Size(arcRadius * 2f, arcRadius * 2f),
                        style = Stroke(width = arcStroke, cap = StrokeCap.Round),
                    )
                }

                // نقاط الساعة: نقطة كبيرة لكل ساعة ونقاط صغيرة للدقائق بينها.
                for (index in 0 until 60) {
                    val angle = Math.toRadians(index * 6.0 - 90.0)
                    val isHourDot = index % 5 == 0
                    drawCircle(
                        color = if (isHourDot) hourDotColor else minuteDotColor,
                        radius = if (isHourDot) hourDotRadius else minuteDotRadius,
                        center = Offset(
                            x = center.x + cos(angle).toFloat() * dotRingRadius,
                            y = center.y + sin(angle).toFloat() * dotRingRadius,
                        ),
                    )
                }

                // مستطيلات النصوص محوّلة إلى إحداثيات نسبية إلى مركز الميناء،
                // وموسّعة قليلًا ليبقى فراغ بين العقرب والنص.
                val obstacles = buildList {
                    if (centerBlock.width > 0f && centerBlock.height > 0f) {
                        add(centerBlock.toObstacle(center).inflated(numberGapPx))
                    }
                    if (daysLeft != null && daysBlock.width > 0f && daysBlock.height > 0f) {
                        add(daysBlock.toObstacle(center).inflated(numberGapPx))
                    }
                }

                // الأطول أولًا حتى يظهر الأقصر فوقه: الدقائق ثم الساعات ثم الثواني.
                drawHand(
                    angleDeg = clock.minuteAngle,
                    endRadius = clockRadius * 0.70f,
                    width = 8.dp.toPx(),
                    color = minuteHandColor,
                    center = center,
                    obstacles = obstacles,
                    minLength = minHandPx,
                )
                drawHand(
                    angleDeg = clock.hourAngle,
                    endRadius = clockRadius * 0.60f,
                    width = 8.dp.toPx(),
                    color = hourHandColor,
                    center = center,
                    obstacles = obstacles,
                    minLength = minHandPx,
                )
                drawHand(
                    angleDeg = clock.secondAngle,
                    endRadius = clockRadius * 0.50f,
                    width = 5.dp.toPx(),
                    color = secondHandColor,
                    center = center,
                    obstacles = obstacles,
                    minLength = minHandPx,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .onGloballyPositioned { coordinates -> centerBlock = coordinates.boundsInParent() },
            ) {
                Text(
                    text = clockText,
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedColor,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(1.dp))
                // القيم الطويلة (مثل «79.67 GB» عند غياب الباقة) تُصغَّر لتبقى داخل الميناء.
                val headlineSize = if (headline.length > 6) 21.sp else 30.sp
                Text(
                    text = headline,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = headlineSize,
                        lineHeight = headlineSize * 1.16f,
                    ),
                    color = if (exceeded) errorColor else contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedColor,
                    maxLines = 1,
                )
                if (value.isNotBlank()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (exceeded) errorColor else accentColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }

            if (daysLeft != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 26.dp)
                        .onGloballyPositioned { coordinates -> daysBlock = coordinates.boundsInParent() },
                ) {
                    Text(
                        text = daysLeft.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = contentColor,
                        maxLines = 1,
                    )
                    Text(
                        text = daysUnit,
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * مواضع عقارب الساعة: الزوايا بالدرجات (صفر عند الساعة 12 واتجاه عقارب الساعة)،
 * والساعة والدقيقة بالأرقام لعرض النص أعلى النسبة.
 */
private data class ClockHands(
    val hourAngle: Float,
    val minuteAngle: Float,
    val secondAngle: Float,
    val hourOfDay: Int,
    val minute: Int,
)

private fun clockHandsFor(timeMillis: Long): ClockHands {
    val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
    val second = calendar.get(Calendar.SECOND).toFloat()
    val minute = calendar.get(Calendar.MINUTE) + second / 60f
    val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY) + minute / 60f
    return ClockHands(
        hourAngle = (hourOfDay % 12f) * 30f,
        minuteAngle = minute * 6f,
        secondAngle = second * 6f,
        hourOfDay = calendar.get(Calendar.HOUR_OF_DAY),
        minute = calendar.get(Calendar.MINUTE),
    )
}

/** يحوّل مستطيلًا في إحداثيات الأب إلى مستطيل نسبي إلى مركز الميناء. */
private fun Rect.toObstacle(center: Offset): HandObstacle = HandObstacle(
    left = left - center.x,
    top = top - center.y,
    right = right - center.x,
    bottom = bottom - center.y,
)

/** يرسم عقربًا واحدًا من المركز إلى الخارج بعد تقليصه عند النصوص التي تعترضه. */
private fun DrawScope.drawHand(
    angleDeg: Float,
    endRadius: Float,
    width: Float,
    color: Color,
    center: Offset,
    obstacles: List<HandObstacle>,
    minLength: Float,
) {
    val segment = ClockHandGeometry.visibleSegment(
        angleDeg = angleDeg,
        endRadius = endRadius,
        obstacles = obstacles,
        minLength = minLength,
    ) ?: return

    val radians = Math.toRadians(angleDeg.toDouble() - 90.0)
    val dx = cos(radians).toFloat()
    val dy = sin(radians).toFloat()
    drawLine(
        color = color,
        start = Offset(center.x + dx * segment.first, center.y + dy * segment.first),
        end = Offset(center.x + dx * segment.second, center.y + dy * segment.second),
        strokeWidth = width,
        cap = StrokeCap.Round,
    )
}
