package com.mywifiadata.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mywifiadata.app.core.UsagePeriod
import com.mywifiadata.app.data.model.AppSettings
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.DayUsage
import com.mywifiadata.app.data.model.SpeedSample
import com.mywifiadata.app.data.model.TrafficTotals
import com.mywifiadata.app.data.model.WifiBand
import com.mywifiadata.app.data.model.WifiStatus
import com.mywifiadata.app.ui.screens.AppDetailScreen
import com.mywifiadata.app.ui.screens.AppsScreen
import com.mywifiadata.app.ui.screens.DashboardScreen
import com.mywifiadata.app.ui.screens.HistoryScreen
import com.mywifiadata.app.ui.screens.SettingsScreen
import com.mywifiadata.app.ui.theme.AppTheme
import java.time.LocalDate
import java.time.ZoneId

/**
 * بيانات تجريبية تُستخدم في معاينات Android Studio فقط.
 */
private fun sampleState(): UiState {
    val now = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()
    val todayStart = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
    val dayMillis = 86_400_000L

    val daily = (29 downTo 0).map { back ->
        val start = todayStart - back * dayMillis
        val seed = (back * 7 % 11)
        DayUsage(
            startMillis = start,
            endMillis = start + dayMillis,
            totals = TrafficTotals(
                rxBytes = (1_200_000_000L + seed * 240_000_000L),
                txBytes = (180_000_000L + seed * 40_000_000L),
            ),
        )
    }

    val apps = listOf(
        AppUsage(10123, "com.example.video", "تطبيق الفيديو", false, TrafficTotals(18_400_000_000, 1_200_000_000)),
        AppUsage(10140, "com.example.social", "شبكة التواصل", false, TrafficTotals(12_600_000_000, 3_400_000_000)),
        AppUsage(10155, "com.example.music", "الموسيقى", false, TrafficTotals(7_200_000_000, 240_000_000)),
        AppUsage(1000, "android", "خدمات النظام", true, TrafficTotals(2_100_000_000, 900_000_000)),
    )

    return UiState(
        now = now,
        isLoading = false,
        hasUsageAccess = true,
        settings = AppSettings(
            quotaBytes = 100_000_000_000,
            cycleStartDay = 5,
            trackingEnabled = true,
            sessionStartMillis = now - 5_400_000L,
        ),
        wifi = WifiStatus(
            isWifiConnected = true,
            isOnline = true,
            ssid = "Home_5G",
            rssiDbm = -52,
            signalLevel = 4,
            linkSpeedMbps = 866,
            rxLinkSpeedMbps = 1200,
            txLinkSpeedMbps = 866,
            frequencyMhz = 5500,
            band = WifiBand.GHZ_5,
            standard = "Wi‑Fi 6/6E (11ax)",
            ipv4 = "192.168.1.42",
            gateway = "192.168.1.1",
            dnsServers = listOf("1.1.1.1", "8.8.8.8"),
        ),
        today = daily.last().totals,
        cycle = TrafficTotals(48_000_000_000, 9_000_000_000),
        session = TrafficTotals(1_800_000_000, 300_000_000),
        samples = List(16) { index ->
            SpeedSample(
                timestampMillis = now - (16 - index) * 4_000L,
                rxBytesPerSecond = 240_000.0 + index * 12_000.0,
                txBytesPerSecond = 40_000.0,
            )
        },
        speedTest = SpeedTestState.Done(
            downloadBitsPerSecond = 125_400_000.0,
            uploadBitsPerSecond = 40_200_000.0,
            timestampMillis = now - 600_000L,
            usedFastCom = true,
        ),
        daily = daily,
        monthPeak = daily.maxByOrNull { day -> day.totals.totalBytes },
        apps = apps,
        appsPeriod = UsagePeriod.MONTH,
        historyPeriod = UsagePeriod.WEEK,
        selectedDayIndex = null,
    )
}

@Composable
private fun PreviewFrame(content: @Composable () -> Unit) {
    AppTheme(darkTheme = false, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            content()
        }
    }
}

@Preview(name = "لوحة المعلومات", showBackground = true, heightDp = 1400)
@Composable
private fun DashboardPreview() {
    PreviewFrame {
        DashboardScreen(
            state = sampleState(),
            onOpenUsageAccess = {},
            onOpenQuotaSettings = {},
            onRefresh = {},
            onResetSession = {},
            onToggleTracking = {},
            onStartSpeedTest = {},
            onStopSpeedTest = {},
            onAddUsageWidget = {},
            onUsageCardStyleChange = {},
        )
    }
}

@Preview(name = "التطبيقات", showBackground = true, heightDp = 1200)
@Composable
private fun AppsPreview() {
    PreviewFrame {
        AppsScreen(
            state = sampleState(),
            onPeriodChange = {},
            onAppsFilterChange = {},
            onAppsWeekChange = {},
            onAppsSliceChange = {},
            onChartTypeChange = {},
            onOpenUsageAccess = {},
            onAppClick = {},
        )
    }
}

@Preview(name = "السجل", showBackground = true, heightDp = 1200)
@Composable
private fun HistoryPreview() {
    PreviewFrame {
        HistoryScreen(
            state = sampleState(),
            onPeriodChange = {},
            onSelectDay = {},
            onChartTypeChange = {},
            onOpenUsageAccess = {},
            onAppClick = {},
        )
    }
}

@Preview(name = "تفاصيل تطبيق", showBackground = true, heightDp = 900)
@Composable
private fun AppDetailPreview() {
    val apps = sampleState().apps
    PreviewFrame {
        AppDetailScreen(
            detail = AppDetailState(
                app = apps.first(),
                isLoading = false,
                today = TrafficTotals(2_600_000_000, 240_000_000),
                week = TrafficTotals(9_400_000_000, 900_000_000),
                month = TrafficTotals(18_400_000_000, 1_200_000_000),
            ),
            onBack = {},
        )
    }
}

@Preview(name = "الإعدادات", showBackground = true, heightDp = 1400)
@Composable
private fun SettingsPreview() {
    PreviewFrame {
        SettingsScreen(
            state = sampleState(),
            onQuotaChange = {},
            onCycleDayChange = {},
            onNotificationsChange = {},
            onThresholdChange = {},
            onIntervalChange = {},
            onDynamicColorChange = {},
            onAccentColorChange = {},
            onQuickSettingsTileEnabledChange = {},
            onHideStatusBarIconChange = {},
            onAppIconHiddenChange = { false },
            onTrackingChange = {},
            onThemeModeChange = {},
            onOpenUsageAccess = {},
            onOpenWifiSettings = {},
            onClearData = {},
        )
    }
}

@Preview(name = "الإعدادات (داكن)", showBackground = true, heightDp = 1400)
@Composable
private fun SettingsDarkPreview() {
    AppTheme(darkTheme = true, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            SettingsScreen(
                state = sampleState(),
                onQuotaChange = {},
                onCycleDayChange = {},
                onNotificationsChange = {},
                onThresholdChange = {},
                onIntervalChange = {},
                onDynamicColorChange = {},
                onAccentColorChange = {},
                onQuickSettingsTileEnabledChange = {},
                onHideStatusBarIconChange = {},
                onAppIconHiddenChange = { false },
                onTrackingChange = {},
                onThemeModeChange = {},
                onOpenUsageAccess = {},
                onOpenWifiSettings = {},
                onClearData = {},
            )
        }
    }
}
