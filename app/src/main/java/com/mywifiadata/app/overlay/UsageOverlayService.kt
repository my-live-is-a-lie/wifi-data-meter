package com.mywifiadata.app.overlay

import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.animation.doOnEnd
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mywifiadata.app.MyWifiApplication
import com.mywifiadata.app.R
import com.mywifiadata.app.core.AppLanguage
import com.mywifiadata.app.core.OverlayDrop
import com.mywifiadata.app.core.OverlayGeometry
import com.mywifiadata.app.core.OverlayMotion
import com.mywifiadata.app.core.OverlaySampling
import com.mywifiadata.app.core.TimeRange
import com.mywifiadata.app.core.TimeRanges
import com.mywifiadata.app.core.UsageNetwork
import com.mywifiadata.app.core.UsageSample
import com.mywifiadata.app.data.AppSearchHistory
import com.mywifiadata.app.data.LiveReading
import com.mywifiadata.app.data.LiveTrafficReader
import com.mywifiadata.app.data.NetworkStatsGate
import com.mywifiadata.app.data.UsageRepository
import com.mywifiadata.app.data.model.AppLayoutDirection
import com.mywifiadata.app.data.model.AppSettings
import com.mywifiadata.app.data.model.MonitorTarget
import com.mywifiadata.app.data.model.OverlayEdge
import com.mywifiadata.app.data.model.ThemeMode
import com.mywifiadata.app.data.model.TrafficTotals
import com.mywifiadata.app.monitor.Notifier
import com.mywifiadata.app.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * خدمة أمامية تعرض نافذة عائمة (Overlay) لاستهلاك الواي فاي المباشر:
 * بطاقة كاملة، أو كبسولة صغيرة قابلة للسحب، أو لسان على حافة الشاشة،
 * أو شريط رفيع تحت شريط الحالة تُصغَّر إليه النافذة عند إفلاتها أعلى الشاشة.
 *
 * العدّادات تبدأ عند فتح النافذة وتُصفَّر عند تغيير هدف المراقبة،
 * وتُجمَّد عند الإيقاف المؤقت. النافذة نافذة Compose تُضاف إلى [WindowManager]،
 * لذا تُهيّأ هنا مالكو دورة الحياة والحالة المحفوظة التي تحتاجها Compose.
 */
class UsageOverlayService :
    Service(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.localizedContext(newBase))
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()

    private lateinit var repository: UsageRepository
    private lateinit var notifier: Notifier
    private lateinit var windowManager: WindowManager

    /**
     * مدير نوافذ لقياس الشاشة من **سياق التطبيق** (بلا تهيئة مُجاوَزة)، لأن قياس
     * الخدمة نفسه يتجمّد على لحظة إنشائها (الشرح في [screenSize]).
     */
    private val metricsWindowManager: WindowManager by lazy(LazyThreadSafetyMode.NONE) {
        applicationContext.getSystemService(WindowManager::class.java) ?: windowManager
    }

    private var rootView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val ui = mutableStateOf(OverlayUiState())
    private var tickerJob: Job? = null
    private var settingsJob: Job? = null

    private var currentTargetKey = MonitorTarget.AllApps.key
    private var segmentStartMillis = 0L
    private var lastSampleMillis = 0L

    /**
     * بداية النافذة التراكمية التي تُقرأ منها إحصاءات النظام (منتصف ليل بدء المقطع).
     * القراءة التراكمية الواسعة أثبت من نافذة ثانيتين: النظام يجمّع الاستهلاك في
     * حِزم زمنية، فالنوافذ القصيرة تعود غالبًا صفرًا أو ناقصة.
     */
    private var cumulativeStartMillis = 0L
    private var baselineTotals: TrafficTotals? = null
    /** آخر القراءات الفعلية لحساب المعدّل اللحظي فوق نافذة متحركة. */
    private val sampleHistory = mutableListOf<UsageSample>()
    private var rxSpeed = 0.0
    private var txSpeed = 0.0

    /** آخر قراءة مباشرة من TrafficStats (null = لم تُؤخذ بعد في هذا المقطع). */
    private var liveLast: LiveReading? = null

    /** استهلاك المقطع الحالي المتراكم من فروق القراءات المباشرة. */
    private var liveAccum = TrafficTotals.Empty

    private var frozenTotals = TrafficTotals.Empty
    private var frozenElapsedMillis = 0L
    private var edgeInwardPx = 0f
    /** المسافة الرأسية المتراكمة لسحب الشريط العلوي لأسفل (لإعادته نافذة). */
    private var topBarDragDy = 0f

    /**
     * ارتفاع شريط الحالة بالبكسل، مخزّن لأنه يُقرأ في كل إطار من انميشن الموضع.
     * يُبطَل عند تغيّر الشاشة (الدوران يبدّله في الوضع الأفقي).
     */
    private var statusBarHeightPx = -1

    /** انميشن الموضع الجاري أثناء تحوّل الشكل؛ وجوده يمنع القفز إلى الموضع النهائي. */
    private var positionAnimator: ValueAnimator? = null

    /**
     * آخر مقاس شاشة قيس عند آخر تموضع للنافذة.
     *
     * `null` تعني «لا مقاس مرجعي»، فيُعاد التموضع في أول قياس يليها. هكذا نلتقط
     * الدوران حتى إن وصل ردّ [onConfigurationChanged] قبل أن تكتمل مقاييس العرض،
     * أو لم يصل أصلًا (الردّ غير مضمون لخدمة بلا واجهة).
     */
    private var lastScreenSize: Pair<Int, Int>? = null

    /**
     * أصحّ إشارة لتغيّر الشاشة في خدمة بلا واجهة: [onConfigurationChanged] غير مضمون
     * للخدمات، أمّا هذا المستمع فيصل مع كل تغيّر في العرض (الدوران، الكثافة).
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            // بلا force: نتحرّك فقط إن تغيّر المقاس فعلًا، فلا نقطع انميشن جاريًا
            // لمجرّد تغيّر حالة العرض (إضاءة/إطفاء) بلا تغيّر في أبعاده.
            if (displayId == Display.DEFAULT_DISPLAY) handleScreenChange(force = false)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as MyWifiApplication
        repository = app.repository
        notifier = Notifier(this)
        notifier.ensureChannels()
        windowManager = getSystemService(WindowManager::class.java)
            ?: error("WindowManager is unavailable")

        // Compose يحتاج مالكي شجرة العرض قبل تركيب المحتوى.
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val settings = repository.settingsStore.current
        currentTargetKey = settings.monitorTarget.key
        startNewSegment()
        ui.value = ui.value.copy(
            target = settings.monitorTarget,
            history = settings.monitorTargetHistory,
            edge = settings.overlayEdge,
            usageAccessMissing = !repository.hasUsageAccess(),
        )

        runCatching {
            getSystemService(DisplayManager::class.java)
                // handler فارغ = الاستدعاء على لوبور الخيط الرئيسي الحالي.
                ?.registerDisplayListener(displayListener, null)
        }
        _running.value = true
        observeSettings()
        startTicker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CLOSE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val foregroundStarted = runCatching { startAsForeground() }
            .onFailure { Log.e(TAG, "Could not start the floating window service", it) }
            .isSuccess
        if (!foregroundStarted) {
            stopSelf()
            return START_NOT_STICKY
        }

        showWindow()
        if (ui.value.mode != OverlayMode.EXPANDED || ui.value.pickerVisible) {
            switchMode(OverlayMode.EXPANDED)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // التهيئة تغيّرت يقينًا: نعيد الحساب ولو بدا المقاس كما هو (دوران 180°).
        handleScreenChange(force = true)
    }

    /**
     * يعيد تموضع النافذة بعد تغيّر الشاشة (تدوير، أو تغيّر مقاسها).
     *
     * اللسان الملتصق بالحافة أحوج الأشكال إلى هذا: موضعه محسوب من عرض الشاشة لا من
     * نسبة محفوظة، فبقاء إحداثي الوضع الرأسي بعد الدوران يضعه في منتصف الشاشة
     * الأفقية. نتحقّق في الإطار الحالي، ثم مرّة أخرى في الذي يليه لأن مقاسات العرض
     * قد تتأخّر إطارًا بعد الإشارة؛ والاستماع إلى القياس في [showWindow] شبكة أمان
     * لما بعد اكتمال الدوران.
     */
    private fun handleScreenChange(force: Boolean) {
        val view = rootView ?: return
        // ارتفاع شريط الحالة يتبدّل مع الدوران؛ يُقرأ من جديد عند أول تموضع تالٍ.
        statusBarHeightPx = -1
        view.post {
            positionWindowIfScreenChanged(force = force)
            view.post { positionWindowIfScreenChanged(force = force) }
        }
    }

    /**
     * يعيد تموضع النافذة إن كان مقاس الشاشة قد تغيّر منذ آخر تموضع.
     *
     * المقارنة بالمقاس لا بالدوران تجعلها آمنة أثناء السحب: السحب يغيّر موضع
     * النافذة لا مقاس الشاشة، فلا يُعاد التموضع فوق إصبع المستخدم.
     *
     * @param force إعادة التموضع ولو لم يتغيّر المقاس (عند إشارة تغيّر صريحة).
     * @return `true` إن أُعيد التموضع.
     */
    private fun positionWindowIfScreenChanged(force: Boolean): Boolean {
        val view = rootView ?: return false
        if (view.width <= 0 || view.height <= 0) return false
        val size = screenSize()
        if (size.first <= 0 || size.second <= 0) return false
        val previous = lastScreenSize
        lastScreenSize = size
        val changed = force || previous == null || previous != size
        if (!changed) return false
        // أثناء التحوّل يقود الانميشن النافذة إلى هدف محسوب بمقاس قديم؛ إيقافه أولًا.
        cancelPositionAnimation()
        positionWindow()
        return true
    }

    override fun onDestroy() {
        _running.value = false
        tickerJob?.cancel()
        settingsJob?.cancel()
        cancelPositionAnimation()
        runCatching {
            getSystemService(DisplayManager::class.java)?.unregisterDisplayListener(displayListener)
        }
        rootView?.let { view -> runCatching { windowManager.removeViewImmediate(view) } }
        rootView = null
        layoutParams = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        scope.cancel()
        super.onDestroy()
    }

    // ————————————————————————— إنشاء النافذة —————————————————————————

    private fun showWindow() {
        if (rootView != null) return
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@UsageOverlayService)
            setViewTreeSavedStateRegistryOwner(this@UsageOverlayService)
            setViewTreeViewModelStoreOwner(this@UsageOverlayService)
            setContent {
                val settings by repository.settingsStore.settings.collectAsState()
                OverlayTheme(settings) {
                    // ارتساء مطلق بأعلى يسار النافذة (لا TopStart فيتقلب مع الاتجاه):
                    // نافذة الشريط بعرض الشاشة كله، والمحتوى المتحوّل يلتصق بحرف
                    // النافذة فينمو من موضعها بدل أن يقفز إلى وسطها.
                    // بلا fillMaxSize قصدًا: حجم الغلاف = حجم المحتوى، فيبقى إطار
                    // النافذة بحجم الشكل الظاهر فقط؛ لو ملأ الشاشة لابتلع كل لمسة
                    // خارج الشريط ومنعها عن التطبيقات خلفه.
                    Box(
                        contentAlignment = AbsoluteAlignment.TopLeft,
                    ) {
                        UsageOverlayContent(state = ui.value, actions = overlayActions)
                    }
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            BASE_WINDOW_FLAGS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            // إحداثيات مطلقة من الحافة اليسرى للشاشة، لا تتبع اتجاه الواجهة (RTL).
            gravity = Gravity.TOP or Gravity.LEFT
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            x = 0
            y = 0
        }
        view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            // فحص مقاس الشاشة أولًا: الدوران يمرّ من هنا وإن لم يصل ردّ التهيئة،
            // وحجم المحتوى قد لا يتغيّر أصلًا (اللسان عرضه ثابت) فلا يكفي وحده.
            if (positionWindowIfScreenChanged(force = false)) return@addOnLayoutChangeListener
            val sizeChanged = (right - left) != (oldRight - oldLeft) ||
                (bottom - top) != (oldBottom - oldTop)
            if (sizeChanged) positionWindow()
        }
        val added = runCatching { windowManager.addView(view, params) }.isSuccess
        if (!added) {
            Log.e(TAG, "The floating window could not be added (missing overlay permission?)")
            stopSelf()
            return
        }
        rootView = view
        layoutParams = params
        // مرجع المقاس من الآن: أول قياس يليه لا يُعدّ «تغيّر شاشة» فيقطع انميشن جارٍ.
        lastScreenSize = screenSize()
        view.post { positionWindow() }
    }

    private fun positionWindow() {
        // أثناء التحوّل يقود الانميشن الموضع؛ القفز إلى الهدف النهائي يقطعه.
        if (positionAnimator != null) return
        val view = rootView ?: return
        val params = layoutParams ?: return
        if (view.width <= 0 || view.height <= 0) return
        val (screenWidth, screenHeight) = screenSize()
        if (screenWidth <= 0 || screenHeight <= 0) return
        val settings = repository.settingsStore.current
        val (targetX, targetY) = when (ui.value.mode) {
            OverlayMode.TOP_BAR -> topBarTarget()
            OverlayMode.EDGE -> {
                val edgeX = OverlayGeometry.edgeX(
                    atLeftEdge = ui.value.edge == OverlayEdge.LEFT,
                    width = view.width,
                    screenWidth = screenWidth,
                )
                val centeredY = (settings.overlayAnchorY * screenHeight - view.height / 2f).roundToInt()
                edgeX to OverlayGeometry.clampY(centeredY, view.height, screenHeight)
            }
            else -> OverlayGeometry.topLeftFor(
                fractionX = settings.overlayAnchorX,
                fractionY = settings.overlayAnchorY,
                width = view.width,
                height = view.height,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
            )
        }
        if (params.x == targetX && params.y == targetY) return
        params.x = targetX
        params.y = targetY
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    /**
     * موضع الشريط العلوي: ملتصقًا تحت شريط الحالة مباشرة، ومتوسطًا أفقيًا كي ينمو
     * متماثلًا من الجهتين أثناء تحوّل القياس (عرضه النهائي كامل عرض الشاشة).
     */
    private fun topBarTarget(): Pair<Int, Int> = 0 to statusBarHeight()

    /**
     * الشريط يمتد من حرف إلى حرف: عرض النافذة يُجعل مطابقًا لعرض الشاشة في وضع
     * الشريط (لا `WRAP_CONTENT` كي لا تُقصّ النافذة بعرض المحتوى فتظهر بهوامش)،
     * ويُعاد لفاف المحتوى عند الخروج منه ليتتبّع حجم الشكل المتحوّل إطارًا بإطار.
     */
    private fun applyWindowWidthForMode(mode: OverlayMode) {
        val view = rootView ?: return
        val params = layoutParams ?: return
        val wanted = if (mode == OverlayMode.TOP_BAR) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            WindowManager.LayoutParams.WRAP_CONTENT
        }
        if (params.width == wanted) return
        params.width = wanted
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    /**
     * يحرّك النافذة بسلاسة إلى موضعها الجديد بينما يتغيّر حجمها في الوقت نفسه.
     *
     * الهدف يُحسب في كل إطار من الحجم المقيس حينها، فيبقى مركز النافذة ملتصقًا
     * بنقطة الارتساء وهو يتقلّص أو يتوسّع، بدل أن يقفز ثم يصحّح نفسه بعد القياس.
     */
    private fun animateTo(
        expanding: Boolean,
        target: (width: Int, height: Int, screenWidth: Int, screenHeight: Int) -> Pair<Int, Int>,
    ) {
        val view = rootView ?: return
        val params = layoutParams ?: return
        cancelPositionAnimation()
        val startX = params.x
        val startY = params.y
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = OverlayMotion.durationMillis(expanding)
            // التدرّج يُحسب في المستمع (OverlayMotion.easeOut)، فلا نضاعفه بمُنعِّم آخر.
            interpolator = null
        }
        animator.addUpdateListener { animation ->
            if (animation !== positionAnimator) return@addUpdateListener
            if (view.width <= 0 || view.height <= 0) return@addUpdateListener
            val eased = OverlayMotion.easeOut(animation.animatedValue as Float)
            val (screenWidth, screenHeight) = screenSize()
            val (endX, endY) = target(view.width, view.height, screenWidth, screenHeight)
            params.x = OverlayMotion.lerp(startX, endX, eased)
            params.y = OverlayMotion.lerp(startY, endY, eased)
            runCatching { windowManager.updateViewLayout(view, params) }
        }
        animator.doOnEnd { finished ->
            // الإلغاء يُفرغ الحقل أولًا، فلا يُثبَّت موضع انميشن لم يكتمل.
            if (finished !== positionAnimator) return@doOnEnd
            positionAnimator = null
            // تثبيت نهائي بعد اكتمال القياس (قد يتغيّر الحجم في آخر إطار).
            positionWindow()
        }
        positionAnimator = animator
        animator.start()
    }

    /** يوقف حركة الموضع الجارية ويسلّم القيادة للمتّصل (سحب أو دوران الشاشة). */
    private fun cancelPositionAnimation() {
        val animator = positionAnimator ?: return
        positionAnimator = null
        animator.cancel()
    }

    /**
     * موضع الشكل الجديد: البطاقة والكبسولة حول نقطة الارتساء المحفوظة،
     * واللسان ملتصقًا بالحافة عند ارتفاع الكبسولة نفسه، والشريط العلوي تحت
     * شريط الحالة مباشرة.
     */
    private fun animateToMode(mode: OverlayMode, expanding: Boolean) {
        val settings = repository.settingsStore.current
        val edge = ui.value.edge
        animateTo(expanding) { width, height, screenWidth, screenHeight ->
            when (mode) {
                OverlayMode.TOP_BAR -> topBarTarget()
                OverlayMode.EDGE -> {
                    val x = OverlayGeometry.edgeX(
                        atLeftEdge = edge == OverlayEdge.LEFT,
                        width = width,
                        screenWidth = screenWidth,
                    )
                    val y = OverlayGeometry.clampY(
                        (settings.overlayAnchorY * screenHeight - height / 2f).roundToInt(),
                        height,
                        screenHeight,
                    )
                    x to y
                }
                else -> OverlayGeometry.topLeftFor(
                    fractionX = settings.overlayAnchorX,
                    fractionY = settings.overlayAnchorY,
                    width = width,
                    height = height,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                )
            }
        }
    }

    /**
     * مقاس مساحة النافذة بالبكسل في دورانها الحالي.
     *
     * يُقرأ من مدير نوافذ مأخوذ من **سياق التطبيق** لا من سياق الخدمة: هذا السياق
     * (وسياق النشاط) مبني على `createConfigurationContext` في [AppLanguage]، وأندرويد
     * يطبّق التهيئة المُجاوَزة **فوق** التهيئة الحيّة عند كل تغيّر
     * (`tmpConfig.updateFrom(mOverrideConfiguration)`)، فتُثبَّت معها
     * `windowConfiguration` — الحدود والدوران — على قيمتها لحظة إنشاء المكوّن.
     * عندها لا ترى خدمة النافذة العائمة تدوير الشاشة أبدًا، فتبقى النافذة عند
     * إحداثيات الوضع القديم: يظهر اللسان في **منتصف** الشاشة الأفقية بدل حافتها.
     * سياق التطبيق بلا تهيئة مُجاوَزة، فمقاسه يتبدّل مع الدوران فورًا.
     */
    private fun screenSize(): Pair<Int, Int> {
        val manager = metricsWindowManager
        val metrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = manager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val point = android.graphics.Point()
            @Suppress("DEPRECATION")
            manager.defaultDisplay.getRealSize(point)
            point.x to point.y
        }
        return if (metrics.first > 0 && metrics.second > 0) {
            metrics
        } else {
            resources.displayMetrics.widthPixels to resources.displayMetrics.heightPixels
        }
    }

    /**
     * ارتفاع شريط الحالة: الشريط العلوي للنافذة يلتصق تحته مباشرة لا فوقه.
     *
     * يُقرأ من حدود النافذة الحالية (API 30+) لأن نافذة الخدمة مُضافة براية
     * `FLAG_LAYOUT_IN_SCREEN` فإحداثيها صفر عند حرف الشاشة الأعلى خلف شريط الحالة؛
     * وعند تعذّر القراءة نعود إلى مورد النظام المعروف، ثم إلى تقدير بالكثافة.
     */
    private fun statusBarHeight(): Int {
        val cached = statusBarHeightPx
        if (cached >= 0) return cached
        val height = readStatusBarHeight()
        statusBarHeightPx = height
        return height
    }

    private fun readStatusBarHeight(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = runCatching { metricsWindowManager.currentWindowMetrics.windowInsets }.getOrNull()
            val top = insets?.getInsets(WindowInsets.Type.statusBars())?.top ?: 0
            if (top > 0) return top
        }
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            (24 * resources.displayMetrics.density).roundToInt()
        }
    }

    // ————————————————————————— السحب والإفلات —————————————————————————

    private fun handleDragStart() {
        // وسط التحوّل: السحب يوقف الانميشن ويأخذ الموضع كما هو بدل انتظاره.
        cancelPositionAnimation()
        ui.value = ui.value.copy(dismissArmed = false, edgeArmed = null, topBarArmed = false)
        edgeInwardPx = 0f
        topBarDragDy = 0f
    }

    private fun handleDrag(dx: Float, dy: Float) {
        val view = rootView ?: return
        val params = layoutParams ?: return
        val (screenWidth, screenHeight) = screenSize()
        when (ui.value.mode) {
            OverlayMode.EDGE -> {
                params.y = OverlayGeometry.clampY(
                    (params.y + dy).roundToInt(),
                    view.height,
                    screenHeight,
                )
                edgeInwardPx += if (ui.value.edge == OverlayEdge.LEFT) dx else -dx
                runCatching { windowManager.updateViewLayout(view, params) }
            }

            OverlayMode.TOP_BAR -> {
                // الشريط بعرض الشاشة كاملًا: يتبع الإصبع رأسيًا فقط، والمسافة المتراكمة
                // هي التي تقرّر عند الإفلات إن عاد نافذة منبثقة أم ارتدّ إلى موضعه.
                params.x = 0
                params.y = OverlayGeometry.clampY(
                    (params.y + dy).roundToInt(),
                    view.height,
                    screenHeight,
                )
                topBarDragDy += dy
                runCatching { windowManager.updateViewLayout(view, params) }
            }

            else -> {
                params.x = OverlayGeometry.clampX(
                    (params.x + dx).roundToInt(),
                    view.width,
                    screenWidth,
                )
                params.y = OverlayGeometry.clampY(
                    (params.y + dy).roundToInt(),
                    view.height,
                    screenHeight,
                )
                runCatching { windowManager.updateViewLayout(view, params) }
                val drop = OverlayGeometry.dropTarget(
                    x = params.x,
                    y = params.y,
                    width = view.width,
                    height = view.height,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    statusBarHeight = statusBarHeight(),
                )
                val armed = ui.value.mode == OverlayMode.PILL && drop == OverlayDrop.DISMISS
                val armedEdge = if (ui.value.mode == OverlayMode.PILL) {
                    when (drop) {
                        OverlayDrop.EDGE_LEFT -> OverlayEdge.LEFT
                        OverlayDrop.EDGE_RIGHT -> OverlayEdge.RIGHT
                        else -> null
                    }
                } else {
                    null
                }
                // منطقة الشريط العلوي متاحة للبطاقة والكبسولة معًا.
                val armedTop = drop == OverlayDrop.TOP_BAR
                if (ui.value.dismissArmed != armed ||
                    ui.value.edgeArmed != armedEdge ||
                    ui.value.topBarArmed != armedTop
                ) {
                    ui.value = ui.value.copy(
                        dismissArmed = armed,
                        edgeArmed = armedEdge,
                        topBarArmed = armedTop,
                    )
                }
            }
        }
    }

    private fun handleDragEnd() {
        val view = rootView ?: return
        val params = layoutParams ?: return
        val mode = ui.value.mode
        val (screenWidth, screenHeight) = screenSize()
        if (mode == OverlayMode.EDGE) {
            val inward = edgeInwardPx
            edgeInwardPx = 0f
            val (_, centerY) = OverlayGeometry.centerFractions(
                x = params.x,
                y = params.y,
                width = view.width,
                height = view.height,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
            )
            if (inward >= OverlayGeometry.EDGE_RETURN_DP * resources.displayMetrics.density) {
                // سحب اللسان إلى الداخل يعيد الكبسولة كما يفعل النقر عليه.
                val centerX = if (ui.value.edge == OverlayEdge.LEFT) 0.3f else 0.7f
                persistAnchor(centerX, centerY)
                switchMode(OverlayMode.PILL)
            } else {
                // البقاء على الحافة: يُحفظ الارتفاع فقط، ويُعاد الوضع الأفقي عند الخروج من الحافة.
                persistAnchorY(centerY)
                positionWindow()
            }
        } else if (mode == OverlayMode.TOP_BAR) {
            val pulled = topBarDragDy
            topBarDragDy = 0f
            if (pulled >= OverlayGeometry.TOP_BAR_RETURN_DP * resources.displayMetrics.density) {
                // السحب لأسفل يعيد النافذة المنبثقة في موضعها المحفوظ قبل التصغير.
                switchMode(OverlayMode.EXPANDED)
            } else {
                // سحبة قصيرة: يرتدّ الشريط إلى مكانه تحت شريط الحالة بانميشن لا بقفزة.
                animateToMode(OverlayMode.TOP_BAR, expanding = false)
            }
        } else {
            val drop = OverlayGeometry.dropTarget(
                x = params.x,
                y = params.y,
                width = view.width,
                height = view.height,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                statusBarHeight = statusBarHeight(),
            )
            when {
                // الإفلات أعلى الشاشة يصغّر النافذة شريطًا؛ يبقى الارتساء المحفوظ
                // كما هو لتعود النافذة إليه عند التكبير لا إلى موضع الإفلات.
                drop == OverlayDrop.TOP_BAR -> switchMode(OverlayMode.TOP_BAR)
                mode == OverlayMode.PILL -> when (drop) {
                    OverlayDrop.DISMISS -> {
                        closeOverlay()
                        return
                    }

                    OverlayDrop.EDGE_LEFT -> switchToEdge(OverlayEdge.LEFT)
                    OverlayDrop.EDGE_RIGHT -> switchToEdge(OverlayEdge.RIGHT)
                    else -> {
                        persistCurrentCenter()
                        positionWindow()
                    }
                }
                else -> {
                    persistCurrentCenter()
                    positionWindow()
                }
            }
        }
        ui.value = ui.value.copy(dismissArmed = false, edgeArmed = null, topBarArmed = false)
    }

    private fun persistCurrentCenter() {
        val view = rootView ?: return
        val params = layoutParams ?: return
        val (screenWidth, screenHeight) = screenSize()
        val (centerX, centerY) = OverlayGeometry.centerFractions(
            x = params.x,
            y = params.y,
            width = view.width,
            height = view.height,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
        )
        persistAnchor(centerX, centerY)
    }

    private fun persistAnchorY(centerY: Float) {
        repository.settingsStore.update { it.copy(overlayAnchorY = centerY.coerceIn(0f, 1f)) }
    }

    private fun persistAnchor(centerX: Float, centerY: Float) {
        repository.settingsStore.update {
            it.copy(
                overlayAnchorX = centerX.coerceIn(0f, 1f),
                overlayAnchorY = centerY.coerceIn(0f, 1f),
            )
        }
    }

    // ————————————————————————— الأشكال والأهداف —————————————————————————

    private fun switchMode(mode: OverlayMode) {
        val previousMode = ui.value.mode
        val leavingEdge = previousMode == OverlayMode.EDGE && mode != OverlayMode.EDGE
        if (previousMode == mode && !ui.value.pickerVisible) {
            rootView?.post { positionWindow() }
            return
        }
        if (leavingEdge) {
            // الخروج من الحافة يعيد التمركز داخل الشاشة بدل الالتصاق بها.
            val centerX = when {
                mode == OverlayMode.EXPANDED -> 0.5f
                ui.value.edge == OverlayEdge.LEFT -> 0.34f
                else -> 0.66f
            }
            repository.settingsStore.update { it.copy(overlayAnchorX = centerX) }
        }
        ui.value = ui.value.copy(
            mode = mode,
            pickerVisible = false,
            edgeArmed = null,
            topBarArmed = false,
        )
        setWindowFocusable(false)
        // عرض النافذة أولًا: الشريط يحتاج عرض الشاشة كله، والخروج منه يعيد التتبع
        // لحجم المحتوى، وقبل الانميشن كي لا يقفز المحتوى وسط نافذة أعرض منه.
        applyWindowWidthForMode(mode)
        // المحتوى يتدرّج في الحجم، والنافذة تنزلق إلى موضعها الجديد في الوقت نفسه.
        animateToMode(mode, expanding = previousMode.expandsTo(mode))
    }

    private fun switchToEdge(side: OverlayEdge) {
        val view = rootView ?: return
        val params = layoutParams ?: return
        val (_, screenHeight) = screenSize()
        val centerY = ((params.y + view.height / 2f) / screenHeight).coerceIn(0.08f, 0.92f)
        repository.settingsStore.update { it.copy(overlayEdge = side, overlayAnchorY = centerY) }
        ui.value = ui.value.copy(mode = OverlayMode.EDGE, edge = side, pickerVisible = false, edgeArmed = null)
        setWindowFocusable(false)
        // الطيّ إلى الحافة تصغير دائمًا: ينزلق اللسان حتى يلتصق بالحرف.
        animateToMode(OverlayMode.EDGE, expanding = false)
    }

    private fun closeOverlay() {
        ui.value = ui.value.copy(pickerVisible = false)
        stopSelf()
    }

    private fun togglePause() {
        if (ui.value.paused) {
            // المتابعة تبدأ مقطعًا جديدًا: تُعاد قراءة خط الأساس بدل جمع المقطع مرتين.
            startNewSegment()
            frozenElapsedMillis = ui.value.elapsedMillis
            ui.value = ui.value.copy(paused = false, rxSpeed = 0.0, txSpeed = 0.0)
        } else {
            frozenTotals = ui.value.totals
            frozenElapsedMillis = ui.value.elapsedMillis
            ui.value = ui.value.copy(paused = true, rxSpeed = 0.0, txSpeed = 0.0)
        }
        refreshNotification()
    }

    /** يصفّر العدّادات والوقت ويبدأ مقطع قياس جديدًا من الآن. */
    private fun resetSession() {
        frozenTotals = TrafficTotals.Empty
        frozenElapsedMillis = 0L
        startNewSegment()
        ui.value = ui.value.copy(
            totals = TrafficTotals.Empty,
            elapsedMillis = 0L,
            paused = false,
            rxSpeed = 0.0,
            txSpeed = 0.0,
            usageAccessMissing = !repository.hasUsageAccess(),
            collecting = false,
            readFailed = false,
        )
    }

    /**
     * يبدأ مقطعًا زمنيًا جديدًا: خط أساس جديد من إحصاءات النظام.
     * نافذة القراءة تبقى منذ منتصف الليل كي تعود القراءة كاملة غير منقوصة.
     */
    private fun startNewSegment() {
        val now = System.currentTimeMillis()
        segmentStartMillis = now
        // صفر كي تُلتقط قراءة خط الأساس في أول دورة (خلال ثانية) لا بعد 16 ثانية.
        lastSampleMillis = 0L
        if (cumulativeStartMillis <= 0L) {
            cumulativeStartMillis = TimeRanges.startOfToday(now)
        }
        baselineTotals = null
        liveLast = null
        liveAccum = TrafficTotals.Empty
        sampleHistory.clear()
        rxSpeed = 0.0
        txSpeed = 0.0
    }

    private fun openPicker() {
        ui.value = ui.value.copy(pickerVisible = true, pickerLoading = true, pickerApps = emptyList())
        setWindowFocusable(true)
        rootView?.post { positionWindow() }
        scope.launch {
            val apps = runCatching { repository.installedApps() }.getOrDefault(emptyList())
            if (!ui.value.pickerVisible) return@launch
            ui.value = ui.value.copy(pickerApps = apps, pickerLoading = false)
        }
    }

    private fun closePicker() {
        if (!ui.value.pickerVisible) return
        hideKeyboard()
        ui.value = ui.value.copy(pickerVisible = false)
        setWindowFocusable(false)
        rootView?.post { positionWindow() }
    }

    private fun selectTarget(target: MonitorTarget) {
        val now = System.currentTimeMillis()
        repository.settingsStore.update { settings ->
            settings.copy(
                monitorTarget = target,
                monitorTargetHistory = if (target.isAllApps) {
                    settings.monitorTargetHistory
                } else {
                    AppSearchHistory.record(settings.monitorTargetHistory, target.asUsage(), now)
                },
            )
        }
        // جامع الإعدادات يرى الهدف الجديد فيصفّر العدّادات والوقت.
        closePicker()
    }

    /**
     * حقل البحث يحتاج نافذة قابلة للتركيز كي تظهر لوحة المفاتيح؛
     * تبقى اللمسات خارج النافذة تمرّ إلى التطبيقات بسبب FLAG_NOT_TOUCH_MODAL.
     */
    private fun setWindowFocusable(focusable: Boolean) {
        val view = rootView ?: return
        val params = layoutParams ?: return
        params.flags = if (focusable) {
            (BASE_WINDOW_FLAGS or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL) and
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            BASE_WINDOW_FLAGS
        }
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun hideKeyboard() {
        val view = rootView ?: return
        runCatching {
            getSystemService(InputMethodManager::class.java)
                ?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    // ————————————————————————— التحديث الدوري —————————————————————————

    private fun observeSettings() {
        settingsJob = scope.launch {
            repository.settingsStore.settings.collect { settings ->
                val target = settings.monitorTarget
                val targetChanged = target.key != currentTargetKey
                if (targetChanged) {
                    currentTargetKey = target.key
                    resetSession()
                }
                ui.value = ui.value.copy(
                    target = target,
                    history = settings.monitorTargetHistory,
                    edge = settings.overlayEdge,
                )
                if (targetChanged) refreshNotification()
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                runCatching { refreshUsage() }
                    .onFailure { Log.e(TAG, "Usage refresh failed", it) }
                delay(TICK_MILLIS)
            }
        }
    }

    private suspend fun refreshUsage() {
        if (ui.value.paused) return
        val start = segmentStartMillis
        val now = System.currentTimeMillis()
        if (start <= 0L || now <= start) return

        // الوقت يتقدّم كل ثانية (حساب محلي)، أما القراءة من النظام فتُؤخذ كل 16 ثانية
        // لأن أندرويد لا يسمح بإجبار تحديث الإحصاءات أكثر من مرة كل 15 ثانية.
        ui.value = ui.value.copy(elapsedMillis = frozenElapsedMillis + (now - start))

        // المسار الرئيسي: قراءة مباشرة كل ثانية من TrafficStats.
        if (refreshLive()) return

        // المسار البديل (جهاز لا يعطي عدّادات مباشرة لهذا الهدف): NetworkStatsManager كل 16 ثانية.
        liveLast = null
        if (now - lastSampleMillis < SAMPLE_MILLIS) return
        // قراءة نظام واحدة كل 16 ثانية على مستوى التطبيق؛ إن كانت مأخوذة الآن
        // نعيد المحاولة في الدورة التالية (كل ثانية) بدل إحداث نداء يمنع التحديث.
        if (!NetworkStatsGate.tryAcquire(now)) return
        lastSampleMillis = now

        // فحص الصلاحية وحالة الواي فاي خارج الخيط الرئيسي (نداءات نظام).
        val (hasUsageAccess, wifiConnected) = withContext(Dispatchers.IO) {
            repository.hasUsageAccess() to repository.isWifiConnected()
        }

        // بلا صلاحية «الوصول إلى الاستخدام» لا يمكن قراءة أي إحصاء؛ نُعلن ذلك بدل صفر صامت.
        if (!hasUsageAccess) {
            // نُبقي آخر إجمالي قيس فعلًا بدل إعادته إلى الصفر، لكن نُعلن سبب توقّفه.
            ui.value = ui.value.copy(
                usageAccessMissing = true,
                wifiConnected = wifiConnected,
                rxSpeed = 0.0,
                txSpeed = 0.0,
                collecting = false,
                readFailed = false,
            )
            return
        }
        // مُنحت الصلاحية (أو كانت ممنوحة): أزل اللافتة حتى لو تعذّرت قراءة هذه الدورة.
        ui.value = ui.value.copy(usageAccessMissing = false, wifiConnected = wifiConnected)

        val target = ui.value.target
        // قراءة فاشلة ليست صفرًا: نتجاهل هذه الدورة ونبقي خط الأساس كما هو.
        val current = readCumulativeTotals(target, cumulativeStartMillis, now)
        if (current == null) {
            if (!ui.value.readFailed) {
                ui.value = ui.value.copy(readFailed = true, usageAccessMissing = false)
            }
            return
        }
        val baseline = baselineTotals
        if (baseline == null) {
            // أول قراءة في المقطع هي خط الأساس؛ لا استهلاك معروفًا بعد فنُعلن «جارٍ القياس».
            baselineTotals = current
            sampleHistory.clear()
            sampleHistory.add(UsageSample(now, current))
            ui.value = ui.value.copy(
                usageAccessMissing = false,
                readFailed = false,
                collecting = true,
                totals = frozenTotals,
                rxSpeed = 0.0,
                txSpeed = 0.0,
            )
            return
        }

        if (current.totalBytes < baseline.totalBytes) {
            // أُعيد ضبط إحصاءات النظام؛ نبدأ من القراءة الحالية بدل أرقام سالبة.
            baselineTotals = current
            sampleHistory.clear()
            sampleHistory.add(UsageSample(now, current))
            ui.value = ui.value.copy(
                usageAccessMissing = false,
                readFailed = false,
                collecting = true,
                totals = frozenTotals,
                rxSpeed = 0.0,
                txSpeed = 0.0,
            )
            return
        }

        // المعدّل من نافذة متحركة على القراءات الحقيقية؛ يتراجع إلى الصفر عند توقف الحركة.
        sampleHistory.add(UsageSample(now, current))
        val windowStart = OverlaySampling.rateWindowStart(sampleHistory, now)
        if (windowStart != null && sampleHistory.size > 1) {
            val rate = OverlaySampling.rate(windowStart, sampleHistory.last())
            rxSpeed = rate.rxBytesPerSecond
            txSpeed = rate.txBytesPerSecond
        }
        while (sampleHistory.size > MAX_RATE_SAMPLES) {
            sampleHistory.removeAt(0)
        }

        val session = OverlaySampling.sessionTotals(baseline, current)
        ui.value = ui.value.copy(
            usageAccessMissing = false,
            readFailed = false,
            collecting = false,
            totals = frozenTotals + session,
            rxSpeed = rxSpeed,
            txSpeed = txSpeed,
        )
    }

    /**
     * قراءة مباشرة كل ثانية من `TrafficStats`.
     *
     * - المعدّل = فرق الاستهلاك المتراكم ÷ الزمن الفعلي (متوسط آخر 3 ثوانٍ).
     * - «جميع التطبيقات»: واي فاي = فرق إجمالي الجهاز − فرق بيانات الجوال.
     * - تطبيق واحد: لا يفصل العدّاد الشبكتين، فنحتسب الفرق فقط وقت الاتصال بالواي فاي.
     * - الزمن يُقاس بـ `elapsedRealtime` فلا يتأثر بتغيير ساعة الجهاز.
     *
     * @return `true` إن عُولجت هذه الدورة هنا، و`false` ليستعمل المتّصل المسار البديل.
     */
    private suspend fun refreshLive(): Boolean {
        val target = ui.value.target
        val (reading, wifiConnected) = withContext(Dispatchers.IO) {
            val live = if (target.isAllApps) {
                LiveTrafficReader.readDevice()
            } else {
                LiveTrafficReader.readUid(target.uid)
            }
            live to repository.isWifiConnected()
        }
        if (reading == null) return false

        val nowReal = SystemClock.elapsedRealtime()
        val previous = liveLast
        liveLast = reading

        if (previous == null) {
            // أول قراءة في المقطع = خط الأساس فقط.
            sampleHistory.clear()
            sampleHistory.add(UsageSample(nowReal, liveAccum))
            ui.value = ui.value.copy(
                usageAccessMissing = false,
                readFailed = false,
                collecting = false,
                wifiConnected = wifiConnected,
                totals = frozenTotals + liveAccum,
                rxSpeed = 0.0,
                txSpeed = 0.0,
            )
            return true
        }

        val previousMobile = previous.mobile
        val currentMobile = reading.mobile
        val delta = when {
            previousMobile != null && currentMobile != null ->
                OverlaySampling.wifiDelta(previous.total, reading.total, previousMobile, currentMobile)

            wifiConnected -> OverlaySampling.delta(previous.total, reading.total)
            else -> TrafficTotals.Empty
        }
        liveAccum += delta

        sampleHistory.add(UsageSample(nowReal, liveAccum))
        val windowStart = OverlaySampling.rateWindowStart(
            sampleHistory,
            nowReal,
            OverlaySampling.LIVE_RATE_WINDOW_MILLIS,
        )
        if (windowStart != null && sampleHistory.size > 1) {
            val rate = OverlaySampling.rate(windowStart, sampleHistory.last())
            rxSpeed = rate.rxBytesPerSecond
            txSpeed = rate.txBytesPerSecond
        }
        while (sampleHistory.size > MAX_LIVE_SAMPLES) {
            sampleHistory.removeAt(0)
        }

        ui.value = ui.value.copy(
            usageAccessMissing = false,
            readFailed = false,
            collecting = false,
            wifiConnected = wifiConnected,
            totals = frozenTotals + liveAccum,
            rxSpeed = rxSpeed,
            txSpeed = txSpeed,
        )
        return true
    }

    /**
     * قراءة تراكمية واحدة من إحصاءات النظام للهدف المختار على شبكة الواي فاي.
     *
     * نستخدم ملخّص الاستهلاك لكل uid (`querySummary`) لأنه يوزّع الحزمة الزمنية الجارية
     * نسبيًا فيرى الحركة الحديثة؛ أما `queryDetailsForUid` فتتجاهل الحزم الجزئية ولا
     * تصلح لقياس لحظي. «جميع التطبيقات» = مجموع التطبيقات الظاهرة للنظام.
     * `null` تعني أن القراءة فشلت الآن، لا أن الاستهلاك صفر.
     */
    private suspend fun readCumulativeTotals(
        target: MonitorTarget,
        fromMillis: Long,
        toMillis: Long,
    ): TrafficTotals? {
        val totals = repository.perUidTotalsOrNull(
            TimeRange(fromMillis, toMillis),
            UsageNetwork.WIFI,
        ) ?: return null
        return if (target.isAllApps) {
            totals.values.fold(TrafficTotals.Empty) { sum, part -> sum + part }
        } else {
            totals[target.uid] ?: TrafficTotals.Empty
        }
    }

    /** يفتح إعدادات النظام لمَنح صلاحية قراءة الاستهلاك الفعلي. */
    private fun openUsageAccess() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
            .onFailure { Log.w(TAG, "Could not open usage access settings", it) }
    }

    // ————————————————————————— الخدمة الأمامية —————————————————————————

    private fun startAsForeground() {
        ServiceCompat.startForeground(
            this,
            Notifier.OVERLAY_ID,
            notifier.buildOverlayNotification(targetLabel()),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
    }

    private fun refreshNotification() {
        runCatching { notifier.updateOverlay(notifier.buildOverlayNotification(targetLabel())) }
    }

    private fun targetLabel(): String = if (ui.value.target.isAllApps) {
        getString(R.string.monitor_all_apps)
    } else {
        ui.value.target.label.ifBlank { getString(R.string.value_unknown) }
    }

    private val overlayActions = OverlayActions(
        expand = { switchMode(OverlayMode.EXPANDED) },
        fromEdge = { switchMode(OverlayMode.PILL) },
        minimize = { switchMode(OverlayMode.PILL) },
        close = { closeOverlay() },
        togglePause = { togglePause() },
        openUsageAccess = { openUsageAccess() },
        openPicker = { openPicker() },
        closePicker = { closePicker() },
        selectTarget = { target -> selectTarget(target) },
        dragStart = { handleDragStart() },
        drag = { dx, dy -> handleDrag(dx, dy) },
        dragEnd = { handleDragEnd() },
    )

    companion object {
        private const val TAG = "UsageOverlayService"
        private const val TICK_MILLIS = 1_000L

        /**
         * فترة القراءة من إحصاءات النظام.
         *
         * أندرويد يسمح بنداء نظام واحد يفرض تحديث الإحصاءات على الأكثر كل **15 ثانية**
         * (`POLL_RATE_LIMIT_MS` في NetworkStatsService)، وأي نداء جديد يعيد ضبط المهلة.
         * لذلك كل قراءة أسرع من ذلك تمنع التحديث كليًا وتُرجع نفس الأرقام القديمة — وهو
         * سبب بقاء العدّاد على صفر. نقرأ كل 16 ثانية كي تفرض كل قراءة تحديثًا فعليًا.
         */
        private const val SAMPLE_MILLIS = 16_000L

        /** يكفي لأربع قراءات (دقيقة) فتغطي نافذة المعدّل (16 ثانية) كاملة. */
        private const val MAX_RATE_SAMPLES = 4

        /** قراءات المسار المباشر (كل ثانية): تكفي لنافذة 3 ثوانٍ مع هامش. */
        private const val MAX_LIVE_SAMPLES = 8

        const val ACTION_SHOW = "com.mywifiadata.app.overlay.SHOW"
        const val ACTION_CLOSE = "com.mywifiadata.app.overlay.CLOSE"

        // NOT_TOUCH_MODAL صريحةً: كل لمسة خارج إطار النافذة تمرّ إلى التطبيقات
        // خلفها؛ بلا هذه الراية يصبح إطار النافذة (وبخاصة الشريط بعرض الشاشة)
        // حاجزًا يبتلع لمسات المستخدم كلها.
        private val BASE_WINDOW_FLAGS = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()
    }
}

/** يطبّق سمة التطبيق نفسها على محتوى النافذة العائمة. */
@Composable
private fun OverlayTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.BLACK -> true
    }
    val locales = LocalConfiguration.current.locales
    // LocaleList ليس Iterable؛ get(0) آمنة ما دامت هناك لغة واحدة على الأقل.
    val language = runCatching { locales.get(0).language }.getOrDefault("ar")
    val layoutDirection = when (settings.layoutDirection) {
        AppLayoutDirection.RTL -> LayoutDirection.Rtl
        AppLayoutDirection.LTR -> LayoutDirection.Ltr
        AppLayoutDirection.AUTO -> if (language == "en") LayoutDirection.Ltr else LayoutDirection.Rtl
    }
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        AppTheme(
            darkTheme = darkTheme,
            dynamicColor = settings.dynamicColorEnabled,
            amoled = settings.themeMode == ThemeMode.BLACK,
            accentColor = settings.accentColor,
            content = content,
        )
    }
}
