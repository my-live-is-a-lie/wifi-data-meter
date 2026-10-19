package com.mywifiadata.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.widget.RemoteViews
import com.mywifiadata.app.MainActivity
import com.mywifiadata.app.R
import com.mywifiadata.app.core.AppLanguage
import com.mywifiadata.app.core.ClockHandGeometry
import com.mywifiadata.app.core.ClockText
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.HandObstacle
import com.mywifiadata.app.core.UsageCardStyles
import com.mywifiadata.app.core.WidgetFaceMetrics
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * أداة الشاشة الرئيسية لعرض استهلاك الواي فاي. تُحفظ آخر قراءة من التطبيق
 * محليًا كي تظهر فور إضافة الأداة حتى إن لم تكن الشاشة الرئيسية مفتوحة.
 *
 * الأداة أداة واحدة فقط («بطاقة الاستهلاك»)، وشكلها يتبع شكل البطاقة المختار
 * داخل التطبيق: تغيير النمط من البطاقة أو الإعدادات يعيد رسم كل الأدوات
 * المضافة فورًا، ولا يوجد للأداة إعداد شكل مستقل عنها.
 */
class UsageWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_WIDGET_PINNED) return

        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        updateWidgets(
            context,
            AppWidgetManager.getInstance(context),
            intArrayOf(widgetId),
        )
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    @Suppress("UNUSED_PARAMETER")
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    companion object {
        const val ACTION_WIDGET_PINNED = "com.mywifiadata.app.widget.ACTION_WIDGET_PINNED"

        const val DEFAULT_USAGE_CARD_STYLE = UsageCardStyles.RING
        const val MAX_USAGE_CARD_STYLE_INDEX = UsageCardStyles.CLOCK

        /** ترتيب أنماط البطاقة داخل التطبيق: حلقة، حلقة منقّطة، ساعة. */
        internal const val STYLE_RING = UsageCardStyles.RING
        internal const val STYLE_DOTTED = UsageCardStyles.DOTTED_RING
        internal const val STYLE_CLOCK = UsageCardStyles.CLOCK

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, UsageWidgetProvider::class.java)
            updateWidgets(context, manager, manager.getAppWidgetIds(component))
        }

        internal fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
        ) {
            val preferences = context.getSharedPreferences(
                UsageWidgetUpdater.PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )
            val todayDownload = preferences.getLong(UsageWidgetUpdater.KEY_TODAY_DOWNLOAD, 0L)
            val todayUpload = preferences.getLong(UsageWidgetUpdater.KEY_TODAY_UPLOAD, 0L)
            val cycleUsage = preferences.getLong(UsageWidgetUpdater.KEY_CYCLE_USAGE, 0L)
            val quota = preferences.getLong(UsageWidgetUpdater.KEY_QUOTA, 0L)
            val daysLeft = preferences.getInt(UsageWidgetUpdater.KEY_DAYS_LEFT, -1)
            // الفهرس المحفوظ في ذاكرة الأداة قد يكون من نسخة سبقت إزالة أحد
            // الأنماط، فيُرحَّل قبل الرسم ثم يُكتب مرة واحدة.
            val cachedStyle = preferences.getInt(
                UsageWidgetUpdater.KEY_STYLE,
                DEFAULT_USAGE_CARD_STYLE,
            )
            val cachedStyleVersion = preferences.getInt(UsageWidgetUpdater.KEY_STYLE_VERSION, 0)
            val styleIndex = UsageCardStyles.migrate(cachedStyle, cachedStyleVersion)
                .coerceIn(0, MAX_USAGE_CARD_STYLE_INDEX)
            if (cachedStyleVersion < UsageCardStyles.LATEST_VERSION) {
                preferences.edit()
                    .putInt(UsageWidgetUpdater.KEY_STYLE, styleIndex)
                    .putInt(UsageWidgetUpdater.KEY_STYLE_VERSION, UsageCardStyles.LATEST_VERSION)
                    .apply()
            }

            // نصوص الأداة تتبع لغة التطبيق نفسها، لا لغة النظام.
            val localized = AppLanguage.localizedContext(context.applicationContext)
            val locale = localized.resources.configuration.locales[0]

            val hasQuota = quota > 0L
            val used = if (hasQuota) cycleUsage else todayDownload + todayUpload
            val gaugeProgress = when {
                hasQuota -> cycleUsage.toDouble() / quota.toDouble()
                cycleUsage > 0L -> used.toDouble() / cycleUsage.toDouble()
                else -> 0.0
            }.coerceIn(0.0, 1.0).toFloat()
            val progressPercent = (gaugeProgress * 100f).roundToInt()
            val exceeded = hasQuota && cycleUsage > quota
            val headline = if (hasQuota) {
                localized.getString(R.string.percent_format, progressPercent)
            } else {
                Format.bytes(used)
            }
            val usageLabel = localized.getString(
                if (hasQuota) R.string.label_used else R.string.label_today,
            )
            val usedValue = Format.bytes(used)
            val remainingText = if (hasQuota) {
                localized.getString(
                    R.string.quota_remaining_of,
                    Format.bytes((quota - cycleUsage).coerceAtLeast(0L)),
                    Format.bytes(quota),
                )
            } else {
                localized.getString(R.string.quota_not_set)
            }
            val downloadText = localized.getString(
                R.string.widget_traffic_line,
                localized.getString(R.string.label_download),
                Format.bytes(todayDownload),
            )
            val uploadText = localized.getString(
                R.string.widget_traffic_line,
                localized.getString(R.string.label_upload),
                Format.bytes(todayUpload),
            )

            appWidgetIds.forEach { widgetId ->
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                // الأداة تُقاس في الوضع الحالي: العرض هو الحد الأدنى في الوضع
                // الطولي والحد الأقصى في العرضي، والارتفاع بالعكس. الاعتماد على
                // الحد الأدنى للارتفاع وحده يجعل الرسم صغيرًا في الوضع الطولي.
                val portrait = context.resources.configuration.orientation !=
                    Configuration.ORIENTATION_LANDSCAPE
                val widgetWidthDp = (if (portrait) {
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220)
                } else {
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 220)
                }).takeIf { it > 0 } ?: 220
                val widgetHeightDp = (if (portrait) {
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 250)
                } else {
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 250)
                }).takeIf { it > 0 } ?: 250
                // عرض الإطار الداخلي بعد حشوة 14dp على كل جهة.
                val frameWidthDp = (widgetWidthDp - 2 * WidgetFaceMetrics.WIDGET_PADDING_DP)
                    .coerceAtLeast(140)
                // ارتفاع مساحة القرص من ارتفاع الأداة: التكبير يكبر القرص
                // بدل أن يزيد الفراغ أسفل البطاقة.
                val frameHeightDp = WidgetFaceMetrics.frameHeightDp(widgetHeightDp, frameWidthDp)

                val views = RemoteViews(context.packageName, R.layout.widget_usage)
                val drawsTextInside = styleIndex == STYLE_CLOCK
                val face = createCardBitmap(
                    context = localized,
                    locale = locale,
                    styleIndex = styleIndex,
                    frameWidthDp = frameWidthDp,
                    frameHeightDp = frameHeightDp,
                    progress = gaugeProgress,
                    exceeded = exceeded,
                    headline = headline,
                    label = usageLabel,
                    usedValue = usedValue,
                    daysLeft = if (hasQuota) daysLeft else -1,
                )
                views.setImageViewBitmap(R.id.widget_gauge, face)
                // النمطان اللذان تُرسم نصوصهما داخل الصورة يخفيان عمود النصوص
                // كي لا تتكرر القيم فوق البطاقة.
                views.setViewVisibility(
                    R.id.widget_center_content,
                    if (drawsTextInside) View.GONE else View.VISIBLE,
                )
                views.setTextViewText(R.id.widget_usage_value, headline)
                views.setTextViewText(R.id.widget_usage_label, usageLabel)
                views.setTextViewText(R.id.widget_used_value, usedValue)
                views.setTextViewText(R.id.widget_remaining, remainingText)
                views.setTextViewText(R.id.widget_download, downloadText)
                views.setTextViewText(R.id.widget_upload, uploadText)

                val openApp = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    widgetId,
                    openApp,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                appWidgetManager.updateAppWidget(widgetId, views)
            }

            // تُحفظ لغة الرسم لمقارنتها لاحقًا: تغيير لغة التطبيق يعيد رسم الأداة
            // فورًا بدل أن تبقى نصوصها باللغة القديمة.
            if (appWidgetIds.isNotEmpty()) {
                preferences.edit()
                    .putString(UsageWidgetUpdater.KEY_RENDERED_LANGUAGE, locale.toLanguageTag())
                    .apply()
            }
        }
    }
}

/** يرسم وجه البطاقة داخل مساحة القياس (بعرض الأداة وارتفاع مساحة القرص). */
private fun createCardBitmap(
    context: Context,
    locale: Locale,
    styleIndex: Int,
    frameWidthDp: Int,
    frameHeightDp: Int,
    progress: Float,
    exceeded: Boolean,
    headline: String,
    label: String,
    usedValue: String,
    daysLeft: Int,
): Bitmap {
    val density = context.resources.displayMetrics.density
    // حد آمن للذاكرة مع الحفاظ على النسبة نفسها، فتظل الصورة مطابقة للمساحة.
    val (safeWidthDp, safeHeightDp) = WidgetFaceMetrics.boundedBitmapSize(
        widthDp = frameWidthDp,
        heightDp = frameHeightDp,
    )
    val bitmapWidth = (safeWidthDp * density).roundToInt().coerceAtLeast(1)
    val bitmapHeight = (safeHeightDp * density).roundToInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centerX = bitmapWidth / 2f
    val centerY = bitmapHeight / 2f
    val dialDiameter = minOf(bitmapWidth, bitmapHeight).toFloat()
    val safeProgress = progress.coerceIn(0f, 1f)
    val progressColor = context.getColor(
        if (exceeded) R.color.widget_error else R.color.widget_progress,
    )
    val trackColor = context.getColor(R.color.widget_progress_track)

    return when (styleIndex) {
        UsageWidgetProvider.STYLE_CLOCK -> {
            drawClockFace(
                context = context,
                locale = locale,
                canvas = canvas,
                centerX = centerX,
                centerY = centerY,
                dialRadius = dialDiameter / 2f,
                progress = safeProgress,
                exceeded = exceeded,
                headline = headline,
                label = label,
                usedValue = usedValue,
                daysLeft = daysLeft,
                progressColor = progressColor,
            )
            bitmap
        }

        UsageWidgetProvider.STYLE_DOTTED -> {
            drawDottedRing(
                context = context,
                canvas = canvas,
                centerX = centerX,
                centerY = centerY,
                diameter = dialDiameter * 0.92f,
                progress = safeProgress,
                progressColor = progressColor,
                trackColor = trackColor,
            )
            bitmap
        }

        else -> {
            drawFullRing(
                context = context,
                canvas = canvas,
                centerX = centerX,
                centerY = centerY,
                diameter = dialDiameter * 0.92f,
                progress = safeProgress,
                exceeded = exceeded,
                progressColor = progressColor,
                trackColor = trackColor,
            )
            bitmap
        }
    }
}

/** الحلقة الكاملة: قوس متدرج يبدأ من أعلى الدائرة، والنصوص في المنتصف. */
private fun drawFullRing(
    context: Context,
    canvas: Canvas,
    centerX: Float,
    centerY: Float,
    diameter: Float,
    progress: Float,
    exceeded: Boolean,
    progressColor: Int,
    trackColor: Int,
) {
    val stroke = diameter * 0.10f
    val radius = (diameter - stroke) / 2f
    val bounds = RectF(
        centerX - radius,
        centerY - radius,
        centerX + radius,
        centerY + radius,
    )
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = trackColor
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawCircle(centerX, centerY, radius, trackPaint)
    if (progress <= 0.001f) return

    val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = progressColor
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
        // تدرّج لوني دائري مطابق للحلقة داخل التطبيق (أزرق ثم بنفسجي).
        shader = SweepGradient(
            centerX,
            centerY,
            intArrayOf(
                progressColor,
                context.getColor(R.color.widget_upload),
                progressColor,
            ),
            floatArrayOf(0f, 0.45f, 1f),
        ).apply {
            // يبدأ التدرّج من الساعة 12 بدل الساعة 3 كي تتطابق الألوان مع القوس.
            val matrix = Matrix().apply { setRotate(-90f, centerX, centerY) }
            setLocalMatrix(matrix)
        }
        if (exceeded) shader = null
    }
    canvas.drawArc(bounds, -90f, 360f * progress, false, progressPaint)
}

/** الحلقة المنقّطة: قوس مفتوح من الأسفل مع نقاط صغيرة ومؤشر شعاعي. */
private fun drawDottedRing(
    context: Context,
    canvas: Canvas,
    centerX: Float,
    centerY: Float,
    diameter: Float,
    progress: Float,
    progressColor: Int,
    trackColor: Int,
) {
    val stroke = diameter * 0.05f
    val radius = (diameter - stroke) / 2f
    val arcBounds = RectF(
        centerX - radius,
        centerY - radius,
        centerX + radius,
        centerY + radius,
    )
    val startAngle = 145f
    val sweepAngle = 250f
    val steps = 39
    val progressSweep = sweepAngle * progress
    val indicatorGap = Math.toDegrees((stroke * 0.5f / radius).toDouble()).toFloat()
    val arcSweep = (progressSweep - indicatorGap).coerceAtLeast(0f)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = trackColor
        alpha = 140
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = progressColor
        alpha = 184
        style = Paint.Style.FILL
    }
    val activeDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.widget_surface)
        style = Paint.Style.FILL
    }
    val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = progressColor
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.BUTT
    }
    val progressCapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = progressColor
        style = Paint.Style.FILL
    }

    canvas.drawArc(arcBounds, startAngle, sweepAngle, false, trackPaint)

    if (arcSweep > 0.001f) {
        // طرف النهاية مسطّح وبداية القوس مستديرة، كما في البطاقة داخل التطبيق.
        canvas.drawArc(arcBounds, startAngle, arcSweep, false, progressPaint)
        val startRadians = Math.toRadians(startAngle.toDouble())
        canvas.drawCircle(
            centerX + cos(startRadians).toFloat() * radius,
            centerY + sin(startRadians).toFloat() * radius,
            stroke / 2f,
            progressCapPaint,
        )
    }

    val dotRadius = diameter * 0.0114f
    val dotStep = sweepAngle / (steps - 1)
    for (index in 0 until steps) {
        val dotSweep = dotStep * index
        val angle = Math.toRadians((startAngle + dotSweep).toDouble())
        val isActiveDot = index > 0 && dotSweep < arcSweep
        val isPointerNeighbor = progress > 0.001f &&
            abs(dotSweep - progressSweep) <= dotStep * 0.55f
        if (index == 0 && arcSweep > 0.001f) continue
        if (isPointerNeighbor && !isActiveDot) continue

        canvas.drawCircle(
            centerX + cos(angle).toFloat() * radius,
            centerY + sin(angle).toFloat() * radius,
            if (isActiveDot) dotRadius * 0.72f else dotRadius,
            if (isActiveDot) activeDotPaint else dotPaint,
        )
    }

    if (progress > 0.001f) {
        // المؤشر الشعاعي: قاعدة مستوية ورأس خارجي دائري.
        val pointerAngle = Math.toRadians((startAngle + progressSweep).toDouble())
        val pointerLength = radius * 0.18f
        val pointerX = cos(pointerAngle).toFloat()
        val pointerY = sin(pointerAngle).toFloat()
        val pointerStroke = stroke * 0.40f
        val pointerEndX = centerX + pointerX * (radius + pointerLength / 2f)
        val pointerEndY = centerY + pointerY * (radius + pointerLength / 2f)
        val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = progressColor
            style = Paint.Style.STROKE
            strokeWidth = pointerStroke
            strokeCap = Paint.Cap.BUTT
        }
        canvas.drawLine(
            centerX + pointerX * (radius - pointerLength / 2f),
            centerY + pointerY * (radius - pointerLength / 2f),
            pointerEndX,
            pointerEndY,
            pointerPaint,
        )
        canvas.drawCircle(pointerEndX, pointerEndY, pointerStroke / 2f, progressCapPaint)
    }
}

/**
 * وجه «ساعة الاستهلاك» داخل الأداة: ميناء منقّط، قوس استهلاك خارجي يبدأ من
 * أسفل الدائرة، عقارب ساعة حقيقية تتقلّص عند النصوص، ثم النصوص نفسها.
 *
 * لا يُرسم عقرب الثواني هنا: الأداة تتحدّث على فترات متباعدة، وعقرب ثوانٍ
 * ساكنًا بين تحديثين قد يعرض وقتًا غير صحيح.
 */
private fun drawClockFace(
    context: Context,
    locale: Locale,
    canvas: Canvas,
    centerX: Float,
    centerY: Float,
    dialRadius: Float,
    progress: Float,
    exceeded: Boolean,
    headline: String,
    label: String,
    usedValue: String,
    daysLeft: Int,
    progressColor: Int,
) {
    val density = context.resources.displayMetrics.density
    val designRadius = WidgetFaceMetrics.DESIGN_DIAL_RADIUS_DP * density
    val scale = WidgetFaceMetrics.clockScale(dialRadius, density)
    if (scale <= 0f) return
    // نرسم الوجه كله في فضاء تصميم ثابت (قطر 252dp كما في بطاقة التطبيق) ثم
    // نكبّر الرسم بنسبة واحدة. هكذا تكبر النقاط والنصوص والعقارب معًا وتبقى
    // نِسبها صحيحة عند أي مقاس للأداة، بدل أن تكبر النقاط وحدها فتظهر ضخمة
    // وتزدحم النصوص فوق الحلقة.
    canvas.save()
    canvas.scale(scale, scale, centerX, centerY)
    drawClockFaceInDesignSpace(
        context = context,
        locale = locale,
        canvas = canvas,
        centerX = centerX,
        centerY = centerY,
        dialRadius = designRadius,
        progress = progress,
        exceeded = exceeded,
        headline = headline,
        label = label,
        usedValue = usedValue,
        daysLeft = daysLeft,
        progressColor = progressColor,
    )
    canvas.restore()
}

/** يرسم وجه الساعة في فضاء التصميم المرجعي (نصف قطر 126dp) قبل التكبير. */
private fun drawClockFaceInDesignSpace(
    context: Context,
    locale: Locale,
    canvas: Canvas,
    centerX: Float,
    centerY: Float,
    dialRadius: Float,
    progress: Float,
    exceeded: Boolean,
    headline: String,
    label: String,
    usedValue: String,
    daysLeft: Int,
    progressColor: Int,
) {
    val density = context.resources.displayMetrics.density
    val onSurface = context.getColor(R.color.widget_on_surface)
    val muted = context.getColor(R.color.widget_on_surface_variant)
    val accent = if (exceeded) context.getColor(R.color.widget_error) else progressColor
    val arcColor = if (exceeded) {
        context.getColor(R.color.widget_error)
    } else {
        context.getColor(R.color.widget_upload)
    }
    val dotRingRadius = dialRadius * 0.85f
    val arcRadius = dialRadius * 0.965f

    // مسار باهت خلف نقاط الميناء.
    canvas.drawCircle(
        centerX,
        centerY,
        dotRingRadius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.widget_progress_track)
            alpha = 60
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * density
        },
    )

    // قوس الاستهلاك: يبدأ من أسفل الدائرة ويدور باتجاه عقارب الساعة.
    if (progress > 0.001f) {
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = arcColor
            style = Paint.Style.STROKE
            strokeWidth = 7f * density
            strokeCap = Paint.Cap.ROUND
        }
        val arcBounds = RectF(
            centerX - arcRadius,
            centerY - arcRadius,
            centerX + arcRadius,
            centerY + arcRadius,
        )
        canvas.drawArc(arcBounds, 90f, 360f * progress, false, arcPaint)
    }

    // نقاط الساعة: نقطة كبيرة لكل ساعة ونقاط دقيقة بينها.
    val hourDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = progressColor
        alpha = 235
        style = Paint.Style.FILL
    }
    val minuteDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = progressColor
        alpha = 110
        style = Paint.Style.FILL
    }
    for (index in 0 until 60) {
        val angle = Math.toRadians(index * 6.0 - 90.0)
        val isHourDot = index % 5 == 0
        canvas.drawCircle(
            centerX + cos(angle).toFloat() * dotRingRadius,
            centerY + sin(angle).toFloat() * dotRingRadius,
            (if (isHourDot) 3f else 1.5f) * density,
            if (isHourDot) hourDotPaint else minuteDotPaint,
        )
    }

    // النصوص: الساعة، النسبة، التسمية، القيمة، ثم الأيام أعلى الميناء.
    val calendar = Calendar.getInstance()
    val minuteOfHour = calendar.get(Calendar.MINUTE) + calendar.get(Calendar.SECOND) / 60f
    val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY) + minuteOfHour / 60f
    val timeText = ClockText.format(
        hourOfDay = calendar.get(Calendar.HOUR_OF_DAY),
        minutes = calendar.get(Calendar.MINUTE),
        locale = locale,
        amLabel = context.getString(R.string.clock_usage_time_am),
        pmLabel = context.getString(R.string.clock_usage_time_pm),
    )

    val contentWidth = (dialRadius * 1.30f).roundToInt().coerceAtLeast(1)
    val minTextSizePx = 7f * density
    val timePaint = createTextPaint(context, locale, 10f, muted, bold = false)
    val headlinePaint = createTextPaint(
        context,
        locale,
        if (headline.length > 6) 21f else 30f,
        if (exceeded) accent else onSurface,
        bold = true,
    )
    // نفس مقاسات نصوص بطاقة التطبيق (تُكبَّر وتُصغَّر مع الرسم كله).
    val labelPaint = createTextPaint(context, locale, 11f, muted, bold = false)
    val usedPaint = createTextPaint(context, locale, 12f, accent, bold = false)
    val daysPaint = createTextPaint(context, locale, 14f, onSurface, bold = true)
    val unitPaint = createTextPaint(context, locale, 11f, muted, bold = false)

    fitTextSize(timePaint, timeText, contentWidth.toFloat(), minTextSizePx)
    fitTextSize(headlinePaint, headline, contentWidth.toFloat(), minTextSizePx)
    fitTextSize(labelPaint, label, contentWidth.toFloat(), minTextSizePx)
    fitTextSize(usedPaint, usedValue, contentWidth.toFloat(), minTextSizePx)

    val timeLayout = buildTextLayout(timeText, timePaint, contentWidth)
    val headlineLayout = buildTextLayout(headline, headlinePaint, contentWidth)
    val labelLayout = buildTextLayout(label, labelPaint, contentWidth)
    val usedLayout = buildTextLayout(usedValue, usedPaint, contentWidth)
    val daysText = if (daysLeft >= 0) daysLeft.toString() else ""
    val unitText = context.getString(R.string.clock_usage_days_unit)
    val daysLayout = if (daysLeft >= 0) buildTextLayout(daysText, daysPaint, contentWidth) else null
    val unitLayout = if (daysLeft >= 0) buildTextLayout(unitText, unitPaint, contentWidth) else null

    val blockGap = 1f * density
    val blockHeight = timeLayout.height + headlineLayout.height + labelLayout.height +
        usedLayout.height + blockGap * 3f
    val blockTop = centerY - blockHeight / 2f
    // عرض النص الفعلي لا عرض صندوق التخطيط، كي لا تُقصّ العقارب بعيدًا عنه.
    val blockHalfWidth = maxOf(
        timePaint.measureText(timeText),
        headlinePaint.measureText(headline),
        labelPaint.measureText(label),
        usedPaint.measureText(usedValue),
    ) / 2f

    val daysTop = centerY - dialRadius * 0.79f
    val daysHalfWidth = if (daysLayout != null) {
        maxOf(daysPaint.measureText(daysText), unitPaint.measureText(unitText)) / 2f
    } else {
        0f
    }
    val daysHeight = if (daysLayout != null) {
        daysLayout.height + (unitLayout?.height ?: 0) - 2f * density
    } else {
        0f
    }

    // نصوص تُحوّل إلى معوّقات نسبية إلى المركز فيتقلّص العقرب قبلها.
    val obstacles = buildList {
        if (blockHalfWidth > 0f) {
            add(
                HandObstacle(
                    left = -blockHalfWidth,
                    top = blockTop - centerY,
                    right = blockHalfWidth,
                    bottom = blockTop - centerY + blockHeight,
                ).inflated(4f * density),
            )
        }
        if (daysLayout != null) {
            add(
                HandObstacle(
                    left = -daysHalfWidth,
                    top = daysTop - centerY,
                    right = daysHalfWidth,
                    bottom = daysTop - centerY + daysHeight,
                ).inflated(2.5f * density),
            )
        }
    }

    val handsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    handsPaint.color = muted
    handsPaint.alpha = 140
    handsPaint.strokeWidth = 5f * density
    drawClippedHand(
        canvas = canvas,
        centerX = centerX,
        centerY = centerY,
        angleDeg = minuteOfHour * 6f,
        endRadius = dialRadius * 0.70f,
        maxReach = dialRadius * 0.84f,
        minVisibleLength = dialRadius * 0.14f,
        paint = handsPaint,
        obstacles = obstacles,
    )
    handsPaint.color = progressColor
    handsPaint.alpha = 255
    handsPaint.strokeWidth = 6f * density
    drawClippedHand(
        canvas = canvas,
        centerX = centerX,
        centerY = centerY,
        angleDeg = (hourOfDay % 12f) * 30f,
        endRadius = dialRadius * 0.60f,
        maxReach = dialRadius * 0.84f,
        minVisibleLength = dialRadius * 0.14f,
        paint = handsPaint,
        obstacles = obstacles,
    )

    if (daysLayout != null && unitLayout != null) {
        canvas.save()
        canvas.translate(centerX - daysLayout.width / 2f, daysTop)
        daysLayout.draw(canvas)
        canvas.restore()
        canvas.save()
        canvas.translate(centerX - unitLayout.width / 2f, daysTop + daysLayout.height - 2f * density)
        unitLayout.draw(canvas)
        canvas.restore()
    }

    var top = blockTop
    listOf(timeLayout, headlineLayout, labelLayout, usedLayout).forEach { layout ->
        canvas.save()
        canvas.translate(centerX - layout.width / 2f, top)
        layout.draw(canvas)
        canvas.restore()
        top += layout.height + blockGap
    }
}

/** يرسم عقربًا مقصوصًا عند نصوص الوجه بنفس هندسة البطاقة داخل التطبيق. */
private fun drawClippedHand(
    canvas: Canvas,
    centerX: Float,
    centerY: Float,
    angleDeg: Float,
    endRadius: Float,
    maxReach: Float,
    minVisibleLength: Float,
    paint: Paint,
    obstacles: List<HandObstacle>,
) {
    val segment = ClockHandGeometry.visibleSegment(
        angleDeg = angleDeg,
        endRadius = endRadius,
        obstacles = obstacles,
        minLength = 0f,
        maxReach = maxReach,
        minVisibleLength = minVisibleLength,
    ) ?: return
    val radians = Math.toRadians(angleDeg.toDouble() - 90.0)
    val dx = cos(radians).toFloat()
    val dy = sin(radians).toFloat()
    canvas.drawLine(
        centerX + dx * segment.first,
        centerY + dy * segment.first,
        centerX + dx * segment.second,
        centerY + dy * segment.second,
        paint,
    )
}

private fun createTextPaint(
    context: Context,
    locale: Locale,
    sizeSp: Float,
    color: Int,
    bold: Boolean,
): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = sizeSp * context.resources.displayMetrics.scaledDensity
    this.color = color
    typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    textLocale = locale
}

/** يصغّر الخط تدريجيًا حتى تتسع الكلمة للعرض المتاح. */
private fun fitTextSize(paint: TextPaint, text: String, maxWidth: Float, minTextSizePx: Float) {
    var size = paint.textSize
    while (size > minTextSizePx && paint.measureText(text) > maxWidth) {
        size -= 0.5f
        paint.textSize = size
    }
}

private fun buildTextLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
    StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setIncludePad(false)
        .setMaxLines(1)
        .build()

/** يخزن آخر قراءة ويحدّث واجهة الأداة على فترات معقولة بدل تحديثها كل ثانية. */
object UsageWidgetUpdater {
    internal const val PREFERENCES_NAME = "usage_widget_data"
    internal const val KEY_TODAY_DOWNLOAD = "today_download"
    internal const val KEY_TODAY_UPLOAD = "today_upload"
    internal const val KEY_CYCLE_USAGE = "cycle_usage"
    internal const val KEY_QUOTA = "quota"
    internal const val KEY_DAYS_LEFT = "days_left"
    internal const val KEY_STYLE = "usage_card_style"
    internal const val KEY_STYLE_VERSION = "usage_card_style_version"
    internal const val KEY_RENDERED_LANGUAGE = "rendered_language"

    private const val KEY_LAST_SAVED_AT = "last_saved_at"
    private const val KEY_LAST_RENDERED_AT = "last_rendered_at"
    private const val CACHE_INTERVAL_MILLIS = 30_000L
    private const val WIDGET_REFRESH_INTERVAL_MILLIS = 60_000L

    fun publish(
        context: Context,
        todayDownload: Long,
        todayUpload: Long,
        cycleUsage: Long,
        quota: Long,
        daysLeft: Int,
        styleIndex: Int,
    ) {
        val appContext = context.applicationContext
        val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val safeStyle = styleIndex.coerceIn(0, UsageWidgetProvider.MAX_USAGE_CARD_STYLE_INDEX)
        val cachedQuota = preferences.getLong(KEY_QUOTA, Long.MIN_VALUE)
        val quotaChanged = cachedQuota != quota
        val styleChanged = preferences.getInt(KEY_STYLE, Int.MIN_VALUE) != safeStyle
        val hasWidgets = AppWidgetManager.getInstance(appContext)
            .getAppWidgetIds(ComponentName(appContext, UsageWidgetProvider::class.java))
            .isNotEmpty()
        val languageTag = AppLanguage.localizedContext(appContext)
            .resources.configuration.locales[0].toLanguageTag()
        // تُقارن اللغة فقط عند وجود أدوات مضافة، وإلا ظلّت «متغيّرة» بلا نهاية.
        val languageChanged = hasWidgets &&
            preferences.getString(KEY_RENDERED_LANGUAGE, null) != languageTag
        val hasChanged = quotaChanged ||
            styleChanged ||
            languageChanged ||
            preferences.getInt(KEY_DAYS_LEFT, Int.MIN_VALUE) != daysLeft ||
            preferences.getLong(KEY_TODAY_DOWNLOAD, Long.MIN_VALUE) != todayDownload ||
            preferences.getLong(KEY_TODAY_UPLOAD, Long.MIN_VALUE) != todayUpload ||
            preferences.getLong(KEY_CYCLE_USAGE, Long.MIN_VALUE) != cycleUsage
        val cacheIsOld = now - preferences.getLong(KEY_LAST_SAVED_AT, 0L) >= CACHE_INTERVAL_MILLIS

        if (hasChanged && (cacheIsOld || quotaChanged || styleChanged)) {
            preferences.edit()
                .putLong(KEY_TODAY_DOWNLOAD, todayDownload)
                .putLong(KEY_TODAY_UPLOAD, todayUpload)
                .putLong(KEY_CYCLE_USAGE, cycleUsage)
                .putLong(KEY_QUOTA, quota)
                .putInt(KEY_DAYS_LEFT, daysLeft)
                .putInt(KEY_STYLE, safeStyle)
                .putInt(KEY_STYLE_VERSION, UsageCardStyles.LATEST_VERSION)
                .putLong(KEY_LAST_SAVED_AT, now)
                .apply()
        }

        val renderedAt = preferences.getLong(KEY_LAST_RENDERED_AT, 0L)
        val refreshDue = now - renderedAt >= WIDGET_REFRESH_INTERVAL_MILLIS
        // نمط الساعة يعرض عقارب الوقت، فيُعاد رسمه دوريًا حتى لو لم تتغيّر القيم
        // كي لا تتجمّد العقارب بين تحديثات البيانات.
        val isClockStyle = safeStyle == UsageWidgetProvider.STYLE_CLOCK
        val shouldRender =
            (hasChanged && (quotaChanged || styleChanged || languageChanged || refreshDue)) ||
                (isClockStyle && refreshDue)
        if (shouldRender) {
            UsageWidgetProvider.updateAll(appContext)
            preferences.edit().putLong(KEY_LAST_RENDERED_AT, now).apply()
        }
    }

    /**
     * يزامن شكل الأداة مع شكل البطاقة المختار في التطبيق ويحدّثها فورًا،
     * حتى تتغيّر كل الأدوات المضافة بمجرد تغيير النمط من داخل التطبيق.
     */
    fun syncStyle(context: Context, styleIndex: Int) {
        val appContext = context.applicationContext
        val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val safeStyle = styleIndex.coerceIn(0, UsageWidgetProvider.MAX_USAGE_CARD_STYLE_INDEX)
        if (preferences.getInt(KEY_STYLE, Int.MIN_VALUE) == safeStyle) return
        preferences.edit()
            .putInt(KEY_STYLE, safeStyle)
            .putInt(KEY_STYLE_VERSION, UsageCardStyles.LATEST_VERSION)
            .putLong(KEY_LAST_RENDERED_AT, System.currentTimeMillis())
            .apply()
        UsageWidgetProvider.updateAll(appContext)
    }
}
