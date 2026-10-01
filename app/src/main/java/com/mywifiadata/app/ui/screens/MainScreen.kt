package com.mywifiadata.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
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
import androidx.compose.material3.ToggleFloatingActionButton
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
import com.mywifiadata.app.core.Format
import com.mywifiadata.app.core.SystemActions
import com.mywifiadata.app.ui.AppViewModel
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
fun MainScreen(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val permissionMessage = stringResource(R.string.permission_needed_title)
    val openSettingsLabel = stringResource(R.string.action_open_usage_access)
    val notificationDeniedMessage = stringResource(R.string.notification_permission_denied)
    val wifiDeniedMessage = stringResource(R.string.wifi_permission_denied)

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

    BackHandler(enabled = fabMenuExpanded) { fabMenuExpanded = false }

    // زر الرجوع يغلق تفاصيل التطبيق قبل الخروج من الشاشة
    BackHandler(enabled = state.appDetail != null) { viewModel.closeAppDetail() }

    val tabs = AppTab.entries
    val currentTab = tabs[selectedTabIndex.coerceIn(0, tabs.lastIndex)]

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(currentTab.labelRes)) },
                subtitle = {
                    val speedText = Format.speed(state.currentSpeed?.totalBytesPerSecond ?: 0.0)
                    Text(
                        text = when {
                            // اسم الشبكة كما هو، وإن لم يتوفر نعرض نصًا جاهزًا بدل قالب %1$s
                            state.wifi.isWifiConnected ->
                                "${state.wifi.ssid ?: stringResource(R.string.status_connected_unknown)} • $speedText"

                            state.wifi.isOnline -> "${stringResource(R.string.status_not_wifi)} • $speedText"

                            else -> stringResource(R.string.wifi_not_connected_title)
                        },
                    )
                },
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
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = fabMenuExpanded,
                button = {
                    ToggleFloatingActionButton(
                        checked = fabMenuExpanded,
                        onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                    ) {
                        Icon(
                            imageVector = if (checkedProgress > 0.5f) {
                                Icons.Rounded.Close
                            } else {
                                Icons.Rounded.Add
                            },
                            contentDescription = stringResource(R.string.cd_more_options),
                        )
                    }
                },
            ) {
                FloatingActionButtonMenuItem(
                    onClick = {
                        fabMenuExpanded = false
                        toggleTracking(!state.settings.trackingEnabled)
                    },
                    text = {
                        Text(
                            text = stringResource(
                                if (state.settings.trackingEnabled) {
                                    R.string.action_stop_tracking
                                } else {
                                    R.string.action_start_tracking
                                },
                            ),
                        )
                    },
                    icon = { Icon(imageVector = Icons.Rounded.Tune, contentDescription = null) },
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        fabMenuExpanded = false
                        viewModel.refreshNow()
                    },
                    text = { Text(text = stringResource(R.string.action_refresh)) },
                    icon = { Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null) },
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        fabMenuExpanded = false
                        viewModel.resetSession()
                    },
                    text = { Text(text = stringResource(R.string.action_reset_session)) },
                    icon = { Icon(imageVector = Icons.Rounded.RestartAlt, contentDescription = null) },
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        fabMenuExpanded = false
                        selectedTabIndex = AppTab.SETTINGS.ordinal
                    },
                    text = { Text(text = stringResource(R.string.tab_settings)) },
                    icon = { Icon(imageVector = Icons.Rounded.Settings, contentDescription = null) },
                )
            }
        },
    ) { innerPadding ->
        // مسافة سفلية إضافية لئلا يغطي زر FAB القائم محتوى الشاشات أسفله.
        val screenContentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 96.dp,
        )
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
                    onResetSession = { viewModel.resetSession() },
                    contentPadding = screenContentPadding,
                )

                AppTab.APPS -> {
                    val appDetail = state.appDetail
                    if (appDetail != null) {
                        AppDetailScreen(
                            detail = appDetail,
                            onBack = { viewModel.closeAppDetail() },
                            contentPadding = screenContentPadding,
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
                            contentPadding = screenContentPadding,
                        )
                    }
                }

                AppTab.HISTORY -> HistoryScreen(
                    state = state,
                    onPeriodChange = { viewModel.setHistoryPeriod(it) },
                    onSelectDay = { viewModel.selectDay(it) },
                    onOpenUsageAccess = { SystemActions.openUsageAccessSettings(context) },
                    onAppClick = { viewModel.openAppDetail(it) },
                    contentPadding = screenContentPadding,
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
                    contentPadding = screenContentPadding,
                )
            }
        }
    }
}
