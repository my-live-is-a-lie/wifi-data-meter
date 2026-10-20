package com.mywifiadata.app.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
import com.mywifiadata.app.core.TimeRange
import com.mywifiadata.app.core.UsageNetwork
import com.mywifiadata.app.data.AppSearchHistory
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
import kotlin.math.roundToInt

/**
 * خدمة أمامية تعرض نافذة عائمة (Overlay) لاستهلاك الواي فاي المباشر:
 * بطاقة كاملة، أو كبسولة صغيرة قابلة للسحب، أو لسان على حافة الشاشة.
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

    private var rootView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val ui = mutableStateOf(OverlayUiState())
    private var tickerJob: Job? = null
    private var settingsJob: Job? = null

    private var currentTargetKey = MonitorTarget.AllApps.key
    private var segmentStartMillis = 0L
    private var lastSampleMillis = 0L
    private var frozenTotals = TrafficTotals.Empty
    private var frozenElapsedMillis = 0L
    private var edgeInwardPx = 0f

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
        segmentStartMillis = System.currentTimeMillis()
        lastSampleMillis = segmentStartMillis
        ui.value = ui.value.copy(
            target = settings.monitorTarget,
            history = settings.monitorTargetHistory,
            edge = settings.overlayEdge,
        )

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
        rootView?.post { positionWindow() }
    }

    override fun onDestroy() {
        _running.value = false
        tickerJob?.cancel()
        settingsJob?.cancel()
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
                    UsageOverlayContent(state = ui.value, actions = overlayActions)
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
        view.post { positionWindow() }
    }

    private fun positionWindow() {
        val view = rootView ?: return
        val params = layoutParams ?: return
        if (view.width <= 0 || view.height <= 0) return
        val (screenWidth, screenHeight) = screenSize()
        if (screenWidth <= 0 || screenHeight <= 0) return
        val settings = repository.settingsStore.current
        val (targetX, targetY) = if (ui.value.mode == OverlayMode.EDGE) {
            val edgeX = if (ui.value.edge == OverlayEdge.LEFT) 0 else screenWidth - view.width
            val centeredY = (settings.overlayAnchorY * screenHeight - view.height / 2f).roundToInt()
            edgeX to OverlayGeometry.clampY(centeredY, view.height, screenHeight)
        } else {
            OverlayGeometry.topLeftFor(
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

    private fun screenSize(): Pair<Int, Int> {
        val metrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val point = android.graphics.Point()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(point)
            point.x to point.y
        }
        return if (metrics.first > 0 && metrics.second > 0) {
            metrics
        } else {
            resources.displayMetrics.widthPixels to resources.displayMetrics.heightPixels
        }
    }

    // ————————————————————————— السحب والإفلات —————————————————————————

    private fun handleDragStart() {
        ui.value = ui.value.copy(dismissArmed = false)
        edgeInwardPx = 0f
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
                )
                val armed = ui.value.mode == OverlayMode.PILL && drop == OverlayDrop.DISMISS
                if (ui.value.dismissArmed != armed) {
                    ui.value = ui.value.copy(dismissArmed = armed)
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
        } else {
            val drop = OverlayGeometry.dropTarget(
                x = params.x,
                y = params.y,
                width = view.width,
                height = view.height,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
            )
            if (mode == OverlayMode.PILL) {
                when (drop) {
                    OverlayDrop.DISMISS -> {
                        closeOverlay()
                        return
                    }

                    OverlayDrop.EDGE_LEFT -> switchToEdge(OverlayEdge.LEFT)
                    OverlayDrop.EDGE_RIGHT -> switchToEdge(OverlayEdge.RIGHT)
                    OverlayDrop.NONE -> {
                        persistCurrentCenter()
                        positionWindow()
                    }
                }
            } else {
                persistCurrentCenter()
                positionWindow()
            }
        }
        ui.value = ui.value.copy(dismissArmed = false)
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
        val leavingEdge = ui.value.mode == OverlayMode.EDGE && mode != OverlayMode.EDGE
        if (ui.value.mode == mode && !ui.value.pickerVisible) {
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
        ui.value = ui.value.copy(mode = mode, pickerVisible = false)
        setWindowFocusable(false)
        rootView?.post { positionWindow() }
    }

    private fun switchToEdge(side: OverlayEdge) {
        val view = rootView ?: return
        val params = layoutParams ?: return
        val (_, screenHeight) = screenSize()
        val centerY = ((params.y + view.height / 2f) / screenHeight).coerceIn(0.08f, 0.92f)
        repository.settingsStore.update { it.copy(overlayEdge = side, overlayAnchorY = centerY) }
        ui.value = ui.value.copy(mode = OverlayMode.EDGE, edge = side, pickerVisible = false)
        setWindowFocusable(false)
        rootView?.post { positionWindow() }
    }

    private fun closeOverlay() {
        ui.value = ui.value.copy(pickerVisible = false)
        stopSelf()
    }

    private fun togglePause() {
        if (ui.value.paused) {
            segmentStartMillis = System.currentTimeMillis()
            lastSampleMillis = segmentStartMillis
            ui.value = ui.value.copy(paused = false)
        } else {
            frozenTotals = ui.value.totals
            frozenElapsedMillis = ui.value.elapsedMillis
            ui.value = ui.value.copy(paused = true)
        }
        refreshNotification()
    }

    private fun resetSession() {
        frozenTotals = TrafficTotals.Empty
        frozenElapsedMillis = 0L
        segmentStartMillis = System.currentTimeMillis()
        lastSampleMillis = segmentStartMillis
        ui.value = ui.value.copy(totals = TrafficTotals.Empty, elapsedMillis = 0L, paused = false)
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
                delay(TICK_MILLIS)
            }
        }
    }

    private suspend fun refreshUsage() {
        if (ui.value.paused) return
        val target = ui.value.target
        val start = segmentStartMillis
        val now = System.currentTimeMillis()
        if (start <= 0L || now <= start) return
        // الوقت يتقدّم كل ثانية، أما قراءة إحصاءات الشبكة فأثقل وتُؤخذ كل ثانيتين.
        if (now - lastSampleMillis >= SAMPLE_MILLIS) {
            lastSampleMillis = now
            val range = TimeRange(start, now)
            val delta = if (target.isAllApps) {
                repository.deviceTotals(range, UsageNetwork.WIFI)
            } else {
                repository.uidTotals(target.uid, range, UsageNetwork.WIFI)
            }
            ui.value = ui.value.copy(totals = frozenTotals + delta)
        }
        ui.value = ui.value.copy(elapsedMillis = frozenElapsedMillis + (now - start))
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
        private const val SAMPLE_MILLIS = 2_000L

        const val ACTION_SHOW = "com.mywifiadata.app.overlay.SHOW"
        const val ACTION_CLOSE = "com.mywifiadata.app.overlay.CLOSE"

        private val BASE_WINDOW_FLAGS = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
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
    val language = LocalConfiguration.current.locales[0].language
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
