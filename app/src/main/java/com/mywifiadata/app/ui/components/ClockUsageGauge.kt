package com.mywifiadata.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mywifiadata.app.R
import com.mywifiadata.app.core.ClockArc
import com.mywifiadata.app.core.ClockHandGeometry
import com.mywifiadata.app.core.ClockText
import com.mywifiadata.app.core.HandObstacle
import com.mywifiadata.app.core.HandSpring
import com.mywifiadata.app.ui.theme.LocalDownloadColor
import com.mywifiadata.app.ui.theme.LocalUploadColor
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

/**
 * بطاقة «ساعة الاستهلاك»: ميناء ساعة منقّط تحيط به حلقة، ويحيط بالحلقة قوس
 * يعرض نسبة الاستهلاك من الباقة (يبدأ من منتصف أعلى الدائرة وينزل مع عقارب
 * الساعة حتى منتصف أسفلها عند اكتمال النسبة). داخل الميناء نص الساعة الحالية، ونسبة الاستهلاك،
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
    val centerTextGapPx = with(density) { 10.dp.toPx() }
    val daysTextGapPx = with(density) { 5.dp.toPx() }

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

    // صيغة 12 ساعة بلغة التطبيق: «٥:٠٤ م» بالعربية و«5:04 PM» بالإنجليزية،
    // أما زوايا العقارب فتُحسب من ساعة متحركة بإطارات الشاشة كساعة حائط حقيقية.
    val amLabel = stringResource(R.string.clock_usage_time_am)
    val pmLabel = stringResource(R.string.clock_usage_time_pm)
    val appLocale = LocalConfiguration.current.locales[0]
    val clockText = remember(timeMillis, appLocale) {
        formatClockText(clockHandsFor(timeMillis), appLocale, amLabel, pmLabel)
    }
    val sweepingNow = remember { mutableLongStateOf(timeMillis) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { sweepingNow.longValue = System.currentTimeMillis() }
        }
    }
    // نابض لكل عقرب: يخفف تقلّصه وتمدده عند مروره بالنصوص.
    val handSprings = remember { List(3) { HandSpring() } }

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

                // قوس الاستهلاك خارج الحلقة: يبدأ من منتصف أعلى الدائرة وينزل مع
                // عقارب الساعة حتى منتصف أسفلها عند اكتمال النسبة.
                if (animatedProgress > 0.0005f) {
                    drawArc(
                        color = arcColor,
                        startAngle = ClockArc.START_ANGLE_DEG,
                        sweepAngle = ClockArc.sweepAngleDeg(animatedProgress),
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
                        add(centerBlock.toObstacle(center).inflated(centerTextGapPx))
                    }
                    if (daysLeft != null && daysBlock.width > 0f && daysBlock.height > 0f) {
                        add(daysBlock.toObstacle(center).inflated(daysTextGapPx))
                    }
                }

                val nowMillis = sweepingNow.longValue
                val clock = clockHandsFor(nowMillis)

                // الأطول أولًا حتى يظهر الأقصر فوقه: الدقائق ثم الساعات ثم الثواني.
                drawHand(
                    angleDeg = clock.minuteAngle,
                    endRadius = clockRadius * 0.70f,
                    width = 8.dp.toPx(),
                    color = minuteHandColor,
                    center = center,
                    obstacles = obstacles,
                    spring = handSprings[0],
                    nowMillis = nowMillis,
                    maxReach = clockRadius * 0.84f,
                    minVisibleLength = clockRadius * 0.14f,
                )
                drawHand(
                    angleDeg = clock.hourAngle,
                    endRadius = clockRadius * 0.60f,
                    width = 8.dp.toPx(),
                    color = hourHandColor,
                    center = center,
                    obstacles = obstacles,
                    spring = handSprings[1],
                    nowMillis = nowMillis,
                    maxReach = clockRadius * 0.84f,
                    minVisibleLength = clockRadius * 0.14f,
                )
                drawHand(
                    angleDeg = clock.secondAngle,
                    endRadius = clockRadius * 0.58f,
                    width = 5.dp.toPx(),
                    color = secondHandColor,
                    center = center,
                    obstacles = obstacles,
                    spring = handSprings[2],
                    nowMillis = nowMillis,
                    maxReach = clockRadius * 0.84f,
                    minVisibleLength = clockRadius * 0.11f,
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
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                    ),
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
                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 11.sp),
                        color = mutedColor,
                        maxLines = 1,
                        modifier = Modifier.offset(y = (-3).dp),
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
    // نحسب زمن اليوم (بتوقيت الجهاز) مباشرة من المللي ثانية بلا كائنات إضافية،
    // لأن هذه الدالة تُستدعى في كل إطار رسم. نضيف أجزاء الثانية إلى الزوايا
    // فتنساب العقارب بلا قفزات كما في ساعة حائط ميكانيكية.
    val timeZoneOffset = TimeZone.getDefault().getOffset(timeMillis).toLong()
    val dayMillis = Math.floorMod(timeMillis + timeZoneOffset, MILLIS_PER_DAY)
    val second = (dayMillis % MILLIS_PER_MINUTE) / 1000f
    val minuteOfDay = dayMillis % MILLIS_PER_HOUR / MILLIS_PER_MINUTE
    val minute = minuteOfDay + second / 60f
    val hourOfDay = dayMillis / MILLIS_PER_HOUR + minute / 60f
    return ClockHands(
        hourAngle = (hourOfDay % 12f) * 30f,
        minuteAngle = minute * 6f,
        secondAngle = second * 6f,
        hourOfDay = (dayMillis / MILLIS_PER_HOUR).toInt(),
        minute = minuteOfDay.toInt(),
    )
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_HOUR = 3_600_000L
private const val MILLIS_PER_DAY = 86_400_000L

/** يحوّل مستطيلًا في إحداثيات الأب إلى مستطيل نسبي إلى مركز الميناء. */
private fun Rect.toObstacle(center: Offset): HandObstacle = HandObstacle(
    left = left - center.x,
    top = top - center.y,
    right = right - center.x,
    bottom = bottom - center.y,
)

/**
 * يرسم عقربًا واحدًا من المركز إلى الخارج بعد تقليصه عند النصوص التي تعترضه،
 * مع نابض يخفف تغيّر طوله فيبدو التقلص والتمدد مطاطيًا ومريحًا للعين.
 */
private fun DrawScope.drawHand(
    angleDeg: Float,
    endRadius: Float,
    width: Float,
    color: Color,
    center: Offset,
    obstacles: List<HandObstacle>,
    spring: HandSpring,
    nowMillis: Long,
    maxReach: Float,
    minVisibleLength: Float,
) {
    val segment = ClockHandGeometry.visibleSegment(
        angleDeg = angleDeg,
        endRadius = endRadius,
        obstacles = obstacles,
        minLength = 0f,
        maxReach = maxReach,
        minVisibleLength = minVisibleLength,
    )
    val targetLength = segment?.let { it.second - it.first } ?: 0f
    val visibleLength = spring.advance(targetLength, nowMillis)
    if (segment == null || visibleLength <= 0.5f) return

    val radians = Math.toRadians(angleDeg.toDouble() - 90.0)
    val dx = cos(radians).toFloat()
    val dy = sin(radians).toFloat()
    drawLine(
        color = color,
        start = Offset(center.x + dx * segment.first, center.y + dy * segment.first),
        end = Offset(
            center.x + dx * (segment.first + visibleLength),
            center.y + dy * (segment.first + visibleLength),
        ),
        strokeWidth = width,
        cap = StrokeCap.Round,
    )
}

/**
 * نص الساعة بصيغة 12 ساعة بلغة التطبيق، مثل «٥:٠٤ م» بالعربية و«5:04 PM»
 * بالإنجليزية، مع أرقام عربية-هندية عندما تكون لغة التطبيق العربية.
 */
private fun formatClockText(
    clock: ClockHands,
    locale: Locale,
    amLabel: String,
    pmLabel: String,
): String = ClockText.format(
    hourOfDay = clock.hourOfDay,
    minutes = clock.minute,
    locale = locale,
    amLabel = amLabel,
    pmLabel = pmLabel,
)
