package com.mywifiadata.app.ui.screens

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mywifiadata.app.R
import com.mywifiadata.app.core.SystemActions
import com.mywifiadata.app.ui.AppViewModel
import com.mywifiadata.app.ui.theme.AppButtonShape
import com.mywifiadata.app.widget.UsageWidgetProvider
import kotlinx.coroutines.launch

private enum class AppTab(val labelRes: Int, val icon: ImageVector) {
    DASHBOARD(R.string.tab_dashboard, Icons.Rounded.Home),
    APPS(R.string.tab_apps, Icons.Rounded.Apps),
    HISTORY(R.string.tab_history, Icons.Rounded.History),
    SETTINGS(R.string.tab_settings, Icons.Rounded.Settings),
}

/**
 * الهيكل العام للتطبيق: شريط علوي، تبويبات سفلية، وقائمة زر عائم (FAB Menu) للإجراءات السريعة.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    viewModel: AppViewModel,
    initialUsageCardStyle: Int = UsageWidgetProvider.DEFAULT_USAGE_CARD_STYLE,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val permissionMessage = stringResource(R.string.permission_needed_title)
    val openSettingsLabel = stringResource(R.string.action_open_usage_access)
    val notificationDeniedMessage = stringResource(R.string.notification_permission_denied)
    val wifiDeniedMessage = stringResource(R.string.wifi_permission_denied)
    val widgetPinRequestedMessage = stringResource(R.string.widget_pin_requested)
    val widgetPinUnsupportedMessage = stringResource(R.string.widget_pin_unsupported)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                scope.launch { snackbarHostState.showSnackbar(notificationDeniedMessage) }
            }
        },
    )

    // إذن قراءة اسم شبكة الواي فاي: NEARBY_WIFI_DEVICES على 13+ وإلا الموقع.
    val wifiPermissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.NEARBY_WIFI_DEVICES
    } else {
        Manifest.permission.ACCESS_FINE_LOCATION
    }
    val wifiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                scope.launch { snackbarHostState.showSnackbar(wifiDeniedMessage) }
            }
        },
    )
    var wifiPermissionAsked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.wifi.isWifiConnected, state.wifi.ssid) {
        if (!wifiPermissionAsked && state.wifi.isWifiConnected && state.wifi.ssid == null) {
            wifiPermissionAsked = true
            val granted = ContextCompat.checkSelfPermission(context, wifiPermissionName) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) {
                wifiPermissionLauncher.launch(wifiPermissionName)
            }
        }
    }

    fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun requestUsageAccess() {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = permissionMessage,
                actionLabel = openSettingsLabel,
            )
            if (result == SnackbarResult.ActionPerformed) {
                SystemActions.openUsageAccessSettings(context)
            }
        }
    }

    fun toggleTracking(enabled: Boolean) {
        if (enabled) {
            if (!state.hasUsageAccess) {
                requestUsageAccess()
                return
            }
            askForNotificationPermission()
            viewModel.startTracking()
        } else {
            viewModel.stopTracking()
        }
    }

    fun requestUsageWidgetPin(selectedStyleIndex: Int) {
        val widgetManager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, UsageWidgetProvider::class.java)
        val safeStyleIndex = selectedStyleIndex.coerceIn(
            UsageWidgetProvider.DEFAULT_USAGE_CARD_STYLE,
            UsageWidgetProvider.MAX_USAGE_CARD_STYLE_INDEX,
        )
        val pinResultIntent = Intent(context, UsageWidgetProvider::class.java).apply {
            action = UsageWidgetProvider.ACTION_WIDGET_PINNED
            putExtra(UsageWidgetProvider.EXTRA_USAGE_CARD_STYLE, safeStyleIndex)
        }
        val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pinResult = PendingIntent.getBroadcast(
            context,
            safeStyleIndex,
            pinResultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag,
        )
        val initialOptions = Bundle().apply {
            putInt(UsageWidgetProvider.EXTRA_USAGE_CARD_STYLE, safeStyleIndex)
        }
        val requested = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            widgetManager.isRequestPinAppWidgetSupported &&
            widgetManager.requestPinAppWidget(provider, initialOptions, pinResult)
        scope.launch {
            snackbarHostState.showSnackbar(
                if (requested) widgetPinRequestedMessage else widgetPinUnsupportedMessage,
            )
        }
    }

    // زر الرجوع يغلق تفاصيل التطبيق قبل الخروج من الشاشة
    BackHandler(enabled = state.appDetail != null) { viewModel.closeAppDetail() }

    // تأكيد تصفير الجلسة قبل تنفيذه (يغطي زر الأزرار السريعة وزر بطاقة الجلسة معًا)
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = stringResource(R.string.action_reset_session)) },
            text = { Text(text = stringResource(R.string.reset_session_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetSession()
                    },
                    shape = AppButtonShape,
                ) {
                    Text(text = stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }, shape = AppButtonShape) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }

    val tabs = AppTab.entries
    val currentTab = tabs[selectedTabIndex.coerceIn(0, tabs.lastIndex)]

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(currentTab.labelRes)) },
                actions = {
                    IconButton(onClick = { viewModel.refreshNow() }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh),
                        )
                    }
                },
            )
        },
        bottomBar = {
            ShortNavigationBar {
                tabs.forEachIndexed { index, tab ->
                    ShortNavigationBarItem(
                        selected = index == selectedTabIndex,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = { Text(text = stringResource(tab.labelRes)) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refreshNow() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    state = state,
                    onOpenUsageAccess = { SystemActions.openUsageAccessSettings(context) },
                    onOpenQuotaSettings = { selectedTabIndex = AppTab.SETTINGS.ordinal },
                    onRefresh = { viewModel.refreshNow() },
                    onResetSession = { showResetDialog = true },
                    onToggleTracking = { toggleTracking(!state.settings.trackingEnabled) },
                    onStartSpeedTest = { viewModel.startSpeedTest() },
                    onStopSpeedTest = { viewModel.stopSpeedTest() },
                    onAddUsageWidget = { styleIndex -> requestUsageWidgetPin(styleIndex) },
                    initialSelectedCardStyle = initialUsageCardStyle.coerceIn(
                        UsageWidgetProvider.DEFAULT_USAGE_CARD_STYLE,
                        UsageWidgetProvider.MAX_USAGE_CARD_STYLE_INDEX,
                    ),
                )

                AppTab.APPS -> {
                    val appDetail = state.appDetail
                    if (appDetail != null) {
                        AppDetailScreen(
                            detail = appDetail,
                            onBack = { viewModel.closeAppDetail() },
                        )
                    } else {
                        AppsScreen(
                            state = state,
                            onPeriodChange = { viewModel.setAppsPeriod(it) },
                            onAppsFilterChange = { viewModel.setAppsFilter(it) },
                            onAppsWeekChange = { viewModel.selectAppsWeek(it) },
                            onAppsSliceChange = { viewModel.selectAppsSlice(it) },
                            onOpenUsageAccess = { SystemActions.openUsageAccessSettings(context) },
                            onAppClick = { viewModel.openAppDetail(it) },
                        )
                    }
                }

                AppTab.HISTORY -> HistoryScreen(
                    state = state,
                    onPeriodChange = { viewModel.setHistoryPeriod(it) },
                    onSelectDay = { viewModel.selectDay(it) },
                    onOpenUsageAccess = { SystemActions.openUsageAccessSettings(context) },
                    onAppClick = { viewModel.openAppDetail(it) },
                )

                AppTab.SETTINGS -> SettingsScreen(
                    state = state,
                    onQuotaChange = { viewModel.setQuotaBytes(it) },
                    onCycleDayChange = { viewModel.setCycleStartDay(it) },
                    onNotificationsChange = { viewModel.setNotificationsEnabled(it) },
                    onThresholdChange = { viewModel.setAlertThreshold(it) },
                    onIntervalChange = { viewModel.setSampleInterval(it) },
                    onDynamicColorChange = { viewModel.setDynamicColorEnabled(it) },
                    onTrackingChange = { toggleTracking(it) },
                    onThemeModeChange = { viewModel.setThemeMode(it) },
                    onOpenUsageAccess = { SystemActions.openUsageAccessSettings(context) },
                    onOpenWifiSettings = { SystemActions.openWifiSettings(context) },
                    onClearData = { viewModel.clearData() },
                )
            }
        }
    }
}
