package com.mywifiadata.app.ui.screens

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon as AndroidIcon
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mywifiadata.app.R
import com.mywifiadata.app.core.HistorySection
import com.mywifiadata.app.core.NavigationPageChange
import com.mywifiadata.app.core.navigationPageChange
import com.mywifiadata.app.core.navigationPageEntrySide
import com.mywifiadata.app.core.AppLanguage
import com.mywifiadata.app.core.SystemActions
import com.mywifiadata.app.data.model.DashboardLayout
import com.mywifiadata.app.monitor.MonitoringTileService
import com.mywifiadata.app.ui.components.DragNavigationBar
import com.mywifiadata.app.ui.components.DragNavigationItem
import com.mywifiadata.app.ui.components.AppsUsageFilterBar
import com.mywifiadata.app.ui.components.HistoryUsageFilterBar
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
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pageStateHolder = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val dashboardListState = rememberLazyListState()
    val appsListState = rememberLazyListState()
    val appsSearchListState = rememberLazyListState()
    val historyListState = rememberLazyListState()
    val historySearchListState = rememberLazyListState()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var pageEntrySide by remember { mutableIntStateOf(0) }
    var languageMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var appsFiltersExpanded by rememberSaveable { mutableStateOf(false) }
    var appsFiltersBeforeSearch by rememberSaveable { mutableStateOf(false) }
    var historyFiltersExpanded by rememberSaveable { mutableStateOf(false) }
    var historyFiltersBeforeSearch by rememberSaveable { mutableStateOf(false) }
    var isDashboardEditing by remember { mutableStateOf(false) }
    var dashboardLayoutDraft by remember { mutableStateOf(state.settings.dashboardLayout.normalized()) }

    val permissionMessage = stringResource(R.string.permission_needed_title)
    val openSettingsLabel = stringResource(R.string.action_open_usage_access)
    val notificationDeniedMessage = stringResource(R.string.notification_permission_denied)
    val wifiDeniedMessage = stringResource(R.string.wifi_permission_denied)
    val widgetPinRequestedMessage = stringResource(R.string.widget_pin_requested)
    val widgetPinUnsupportedMessage = stringResource(R.string.widget_pin_unsupported)
    val quickSettingsTileAddedMessage = stringResource(R.string.qs_tile_added)
    val quickSettingsTileAlreadyAddedMessage = stringResource(R.string.qs_tile_already_added)
    val quickSettingsTileAddCancelledMessage = stringResource(R.string.qs_tile_add_cancelled)
    val quickSettingsTileManualAddMessage = stringResource(R.string.qs_tile_manual_add)
    val quickSettingsTileDisabledMessage = stringResource(R.string.qs_tile_disabled)

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

    fun beginDashboardEditing() {
        dashboardLayoutDraft = state.settings.dashboardLayout.normalized()
        isDashboardEditing = true
    }

    fun cancelDashboardEditing() {
        dashboardLayoutDraft = state.settings.dashboardLayout.normalized()
        isDashboardEditing = false
    }

    fun saveDashboardEditing() {
        viewModel.setDashboardLayout(dashboardLayoutDraft.normalized())
        isDashboardEditing = false
    }

    fun configureQuickSettingsTile(enabled: Boolean) {
        viewModel.setQuickSettingsTileEnabled(enabled)
        if (!enabled) {
            scope.launch { snackbarHostState.showSnackbar(quickSettingsTileDisabledMessage) }
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val message = if (viewModel.uiState.value.settings.quickSettingsTileAdded) {
                quickSettingsTileAlreadyAddedMessage
            } else {
                quickSettingsTileManualAddMessage
            }
            scope.launch { snackbarHostState.showSnackbar(message) }
            return
        }

        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
        if (statusBarManager == null) {
            scope.launch { snackbarHostState.showSnackbar(quickSettingsTileManualAddMessage) }
            return
        }

        runCatching {
            statusBarManager.requestAddTileService(
                ComponentName(context, MonitoringTileService::class.java),
                context.getString(R.string.qs_tile_label),
                AndroidIcon.createWithResource(context, R.drawable.ic_network_monitor),
                ContextCompat.getMainExecutor(context),
            ) { result ->
                when (result) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> {
                        viewModel.setQuickSettingsTileAdded(true)
                        scope.launch { snackbarHostState.showSnackbar(quickSettingsTileAddedMessage) }
                    }

                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> {
                        viewModel.setQuickSettingsTileAdded(true)
                        scope.launch { snackbarHostState.showSnackbar(quickSettingsTileAlreadyAddedMessage) }
                    }

                    else -> {
                        viewModel.setQuickSettingsTileEnabled(false)
                        scope.launch { snackbarHostState.showSnackbar(quickSettingsTileAddCancelledMessage) }
                    }
                }
            }
        }.onFailure {
            viewModel.setQuickSettingsTileEnabled(false)
            scope.launch { snackbarHostState.showSnackbar(quickSettingsTileAddCancelledMessage) }
        }
    }

    fun selectAppLanguage(languageTag: String) {
        languageMenuExpanded = false
        val language = AppLanguage.normalize(languageTag)
        if (state.settings.appLanguage == language) return
        viewModel.setAppLanguage(language)
        MonitoringTileService.requestRefresh(context)
        context.findActivity()?.recreate()
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

    // زر الرجوع يلغي تخصيص الصفحة أولًا، ثم يغلق تفاصيل التطبيق إن وُجدت.
    BackHandler(enabled = isDashboardEditing || state.appDetail != null) {
        if (isDashboardEditing) cancelDashboardEditing() else viewModel.closeAppDetail()
    }

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
    var pageTransitionState by remember { mutableStateOf(MutableTransitionState(currentTab)) }
    val pageTransition = updateTransition(pageTransitionState, label = "Navigation pages")
    val isPageChanging = pageTransition.currentState != pageTransition.targetState

    fun selectTab(index: Int, fromDrag: Boolean = false) {
        val next = index.coerceIn(0, tabs.lastIndex)
        val change = navigationPageChange(selectedTabIndex, next, fromDrag, !pageTransitionState.isIdle)
        if (change == NavigationPageChange.NONE) return
        if (next != selectedTabIndex) {
            if (isDashboardEditing && next != AppTab.DASHBOARD.ordinal) cancelDashboardEditing()
            // Release the outgoing editor before the destination requests focus.
            focusManager.clearFocus()
            keyboard?.hide()
        }
        when (change) {
            NavigationPageChange.SLIDE -> {
                // A quick reversal uses the last requested tab, not the last settled page.
                pageEntrySide = navigationPageEntrySide(selectedTabIndex, next, isRtl)
                pageTransitionState.targetState = tabs[next]
            }
            NavigationPageChange.SNAP -> {
                pageEntrySide = 0
                // Start already settled: no enter/exit movement, even when a tap interrupts
                // a running drag slide or taps its already-selected destination.
                pageTransitionState = MutableTransitionState(tabs[next])
            }
            NavigationPageChange.NONE -> Unit
        }
        selectedTabIndex = next
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentTab != AppTab.DASHBOARD || !isDashboardEditing) {
                TopAppBar(
                    title = {
                        if (currentTab == AppTab.HISTORY && state.appDetail == null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = stringResource(currentTab.labelRes),
                                    maxLines = 1,
                                )
                                HistorySectionSelector(
                                    section = state.historySection,
                                    onSectionChange = viewModel::setHistorySection,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            Text(text = stringResource(currentTab.labelRes))
                        }
                    },
                    actions = {
                        if (currentTab == AppTab.SETTINGS) {
                            Box {
                                IconButton(onClick = { languageMenuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Language,
                                        contentDescription = stringResource(R.string.cd_change_language),
                                    )
                                }
                                DropdownMenu(
                                    expanded = languageMenuExpanded,
                                    onDismissRequest = { languageMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(R.string.language_system)) },
                                        onClick = { selectAppLanguage(AppLanguage.SYSTEM) },
                                        trailingIcon = if (state.settings.appLanguage == AppLanguage.SYSTEM) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(R.string.language_arabic)) },
                                        onClick = { selectAppLanguage(AppLanguage.ARABIC) },
                                        trailingIcon = if (state.settings.appLanguage == AppLanguage.ARABIC) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(R.string.language_english)) },
                                        onClick = { selectAppLanguage(AppLanguage.ENGLISH) },
                                        trailingIcon = if (state.settings.appLanguage == AppLanguage.ENGLISH) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                    )
                                }
                            }
                        } else if (currentTab == AppTab.DASHBOARD) {
                            if (!isDashboardEditing) {
                                IconButton(onClick = { beginDashboardEditing() }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = stringResource(R.string.cd_edit_dashboard),
                                    )
                                }
                            }
                        } else if (currentTab == AppTab.APPS && state.appDetail == null) {
                            IconButton(onClick = { appsFiltersExpanded = !appsFiltersExpanded }) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = stringResource(
                                        if (appsFiltersExpanded) R.string.apps_hide_filters else R.string.apps_show_filters,
                                    ),
                                    tint = if (appsFiltersExpanded) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else if (currentTab == AppTab.HISTORY && state.appDetail == null) {
                            if (state.historySection == HistorySection.NETWORK) {
                                IconButton(onClick = { historyFiltersExpanded = !historyFiltersExpanded }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Menu,
                                        contentDescription = stringResource(
                                            if (historyFiltersExpanded) R.string.history_hide_filters else R.string.history_show_filters,
                                        ),
                                        tint = if (historyFiltersExpanded) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(48.dp))
                            }
                        } else if (state.appDetail == null) {
                            IconButton(onClick = { viewModel.refreshNow() }) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.cd_refresh),
                                )
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            DragNavigationBar(
                items = tabs.map { DragNavigationItem(stringResource(it.labelRes), it.icon) },
                selectedIndex = selectedTabIndex,
                onSelect = { selectTab(it) },
                onDragSelect = { selectTab(it, fromDrag = true) },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        val screenContent: @Composable (AppTab) -> Unit = { pageTab ->
            // Render the animation's page, not currentTab: outgoing content must remain
            // its own screen rather than suddenly being replaced with the destination.
            when (pageTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    state = state,
                    onOpenUsageAccess = { SystemActions.openUsageAccessSettings(context) },
                    onOpenQuotaSettings = { selectTab(AppTab.SETTINGS.ordinal) },
                    onRefresh = { viewModel.refreshNow() },
                    onResetSession = { showResetDialog = true },
                    onToggleTracking = { toggleTracking(!state.settings.trackingEnabled) },
                    onStartSpeedTest = { viewModel.startSpeedTest() },
                    onStopSpeedTest = { viewModel.stopSpeedTest() },
                    onAddUsageWidget = { styleIndex -> requestUsageWidgetPin(styleIndex) },
                    onUsageCardStyleChange = { styleIndex -> viewModel.setUsageCardStyle(styleIndex) },
                    onLoadTodayApps = { viewModel.loadTodayApps() },
                    dashboardListState = dashboardListState,
                    isEditing = isDashboardEditing,
                    dashboardLayout = dashboardLayoutDraft,
                    onDashboardLayoutChange = { dashboardLayoutDraft = it.normalized() },
                    onSaveDashboardLayout = { saveDashboardEditing() },
                    onCancelDashboardLayout = { cancelDashboardEditing() },
                )

                AppTab.APPS -> {
                    val appDetail = state.appDetail
                    if (appDetail != null) {
                        AppDetailScreen(
                            detail = appDetail,
                            onBack = { viewModel.closeAppDetail() },
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            AnimatedVisibility(visible = appsFiltersExpanded) {
                                AppsUsageFilterBar(
                                    network = state.appsNetwork,
                                    scope = state.appsScope,
                                    countLimit = state.appsCountLimit,
                                    sortOrder = state.appsSortOrder,
                                    onNetworkChange = viewModel::setAppsNetwork,
                                    onScopeChange = viewModel::setAppsScope,
                                    onCountLimitChange = viewModel::setAppsCountLimit,
                                    onSortOrderChange = viewModel::setAppsSortOrder,
                                )
                            }
                            AppsScreen(
                                state = state,
                                isActive = pageTab == currentTab,
                                modifier = Modifier.weight(1f),
                                onPeriodChange = { viewModel.setAppsPeriod(it) },
                                onAppsFilterChange = { viewModel.setAppsFilter(it) },
                                onAppsWeekChange = { viewModel.selectAppsWeek(it) },
                                onAppsSliceChange = { viewModel.selectAppsSlice(it) },
                                onChartTypeChange = { viewModel.setAppsLineChartEnabled(it) },
                                onOpenUsageAccess = { SystemActions.openUsageAccessSettings(context) },
                                onAppClick = viewModel::openAppsListDetail,
                                onSearchActiveChange = { active ->
                                    if (active && !state.appsSearchActive) {
                                        appsFiltersBeforeSearch = appsFiltersExpanded
                                        appsFiltersExpanded = false
                                    } else if (!active && state.appsSearchActive) {
                                        appsFiltersExpanded = appsFiltersBeforeSearch
                                    }
                                    viewModel.setAppsSearchActive(active)
                                },
                                onSearchQueryChange = viewModel::setAppsSearchQuery,
                                listState = appsListState,
                                searchListState = appsSearchListState,
                                onReverseSortOrder = viewModel::reverseAppsSortOrder,
                            )
                        }
                    }
                }

                AppTab.HISTORY -> {
                    val appDetail = state.appDetail
                    if (appDetail != null) {
                        AppDetailScreen(
                            detail = appDetail,
                            onBack = { viewModel.closeAppDetail() },
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            AnimatedVisibility(
                                visible = historyFiltersExpanded &&
                                    state.historySection == HistorySection.NETWORK &&
                                    !state.historySearchActive,
                            ) {
                                HistoryUsageFilterBar(
                                    network = state.historyNetwork,
                                    selectedMonth = state.historyMonth,
                                    selectedYear = state.historyYear,
                                    availableMonths = state.availableHistoryMonths,
                                    availableYears = state.availableHistoryYears,
                                    onNetworkChange = viewModel::setHistoryNetwork,
                                    onMonthChange = viewModel::setHistoryMonth,
                                    onYearChange = viewModel::setHistoryYear,
                                )
                            }
                            HistoryScreen(
                                state = state,
                                isActive = pageTab == currentTab,
                                modifier = Modifier.weight(1f),
                                listState = historyListState,
                                searchListState = historySearchListState,
                                onSearchActiveChange = { active ->
                                    if (active && !state.historySearchActive) {
                                        historyFiltersBeforeSearch = historyFiltersExpanded
                                        historyFiltersExpanded = false
                                    } else if (!active && state.historySearchActive) {
                                        historyFiltersExpanded = historyFiltersBeforeSearch
                                    }
                                    viewModel.setHistorySearchActive(active)
                                },
                                onSearchQueryChange = viewModel::setHistorySearchQuery,
                                onReverseSortOrder = viewModel::reverseHistoryAppsSortOrder,
                                onPeriodChange = { viewModel.setHistoryPeriod(it) },
                                onSelectDay = { viewModel.selectDay(it) },
                                onResetHistoryDate = viewModel::resetHistoryDate,
                                onChartTypeChange = { viewModel.setHistoryLineChartEnabled(it) },
                                onOpenUsageAccess = { SystemActions.openUsageAccessSettings(context) },
                                onAppClick = { viewModel.openAppDetail(it, state.historyNetwork) },
                            )
                        }
                    }
                }

                AppTab.SETTINGS -> SettingsScreen(
                    state = state,
                    onQuotaChange = { viewModel.setQuotaBytes(it) },
                    onCycleDayChange = { viewModel.setCycleStartDay(it) },
                    onNotificationsChange = { viewModel.setNotificationsEnabled(it) },
                    onThresholdChange = { viewModel.setAlertThreshold(it) },
                    onIntervalChange = { viewModel.setSampleInterval(it) },
                    onDynamicColorChange = { viewModel.setDynamicColorEnabled(it) },
                    onAccentColorChange = { viewModel.setAccentColor(it) },
                    onQuickSettingsTileEnabledChange = { configureQuickSettingsTile(it) },
                    onHideStatusBarIconChange = { viewModel.setHideStatusBarIcon(it) },
                    onAppIconHiddenChange = { viewModel.setAppIconHidden(it) },
                    onTrackingChange = { toggleTracking(it) },
                    onThemeModeChange = { viewModel.setThemeMode(it) },
                    onOpenUsageAccess = { SystemActions.openUsageAccessSettings(context) },
                    onOpenWifiSettings = { SystemActions.openWifiSettings(context) },
                    onClearData = { viewModel.clearData() },
                    onLayoutDirectionChange = { viewModel.setLayoutDirection(it) },
                    onSavePlan = { viewModel.savePlan(it) },
                    onActivatePlan = { viewModel.activatePlan(it) },
                )
            }
        }

        val isAppsSearchVisible = currentTab == AppTab.APPS && state.appsSearchActive && state.appDetail == null
        val isHistorySearchVisible = currentTab == AppTab.HISTORY && state.historySearchActive && state.hasUsageAccess &&
            state.selectedDayIndex?.let { state.daily.getOrNull(it) } != null && state.appDetail == null
        val isHistoryPlanVisible = currentTab == AppTab.HISTORY && state.historySection == HistorySection.PLAN
        val pullRefreshEnabled = !isPageChanging && !isAppsSearchVisible && !isHistorySearchVisible &&
            !isHistoryPlanVisible && !(isDashboardEditing && currentTab == AppTab.DASHBOARD)
        // Discard any partially pulled distance on entering/leaving search. Key only this
        // state, not the content: the editor, its animation and scroll states must survive.
        val pullRefreshState = key(pullRefreshEnabled) { rememberPullToRefreshState() }
        val refreshModifier = if (pullRefreshEnabled) {
            Modifier.pullToRefresh(
                isRefreshing = state.isRefreshing,
                state = pullRefreshState,
                onRefresh = { viewModel.refreshNow() },
            )
        } else {
            // Removing the nested-scroll handler also blocks keyboard/cursor bring-into-view
            // scrolling from being interpreted as a pull gesture. Hiding the icon alone is not enough.
            Modifier
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Consume Scaffold padding before the apps list applies its IME inset.
                .consumeWindowInsets(innerPadding)
                .then(refreshModifier),
        ) {
            pageTransition.AnimatedContent(
                modifier = Modifier.fillMaxSize().clipToBounds().pointerInput(isPageChanging) {
                    // Don't let a tap hit an outgoing row while it slides away. This gate
                    // covers page content only, never the live drag-navigation bar.
                    if (isPageChanging) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    }
                },
                contentKey = { it },
                transitionSpec = {
                    val side = pageEntrySide
                    (slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { width -> side * width } togetherWith
                        slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { width -> -side * width })
                        .using(null) // Fixed viewport: no height/size animation or page bounce.
                },
            ) { pageTab ->
                pageStateHolder.SaveableStateProvider(pageTab.name) {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                            .then(if (pageTab != currentTab) Modifier.clearAndSetSemantics { } else Modifier),
                    ) {
                        screenContent(pageTab)
                    }
                }
            }
            if (pullRefreshEnabled) {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = state.isRefreshing,
                    state = pullRefreshState,
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current != null) {
        if (current is Activity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}
