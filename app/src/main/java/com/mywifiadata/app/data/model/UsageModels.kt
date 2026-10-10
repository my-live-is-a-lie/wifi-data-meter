package com.mywifiadata.app.data.model

import com.mywifiadata.app.core.AppLanguage
import com.mywifiadata.app.core.SamplingIntervals

/** إجماليات المرور (تنزيل/رفع). */
data class TrafficTotals(
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L,
) {
    val totalBytes: Long get() = rxBytes + txBytes

    operator fun plus(other: TrafficTotals) = TrafficTotals(
        rxBytes = rxBytes + other.rxBytes,
        txBytes = txBytes + other.txBytes,
    )

    companion object {
        val Empty = TrafficTotals()
    }
}

/** نطاق ترددي لشبكة الواي فاي. */
enum class WifiBand { GHZ_2_4, GHZ_5, GHZ_6, UNKNOWN }

/** حالة شبكة الواي فاي الحالية وتفاصيلها. */
data class WifiStatus(
    val isWifiConnected: Boolean = false,
    val isOnline: Boolean = false,
    val ssid: String? = null,
    val bssid: String? = null,
    val rssiDbm: Int? = null,
    /** 0..4 */
    val signalLevel: Int = 0,
    val linkSpeedMbps: Int? = null,
    val rxLinkSpeedMbps: Int? = null,
    val txLinkSpeedMbps: Int? = null,
    val frequencyMhz: Int? = null,
    val band: WifiBand = WifiBand.UNKNOWN,
    /** مثال: "Wi‑Fi 6 (11ax)" */
    val standard: String? = null,
    val ipv4: String? = null,
    val gateway: String? = null,
    val dnsServers: List<String> = emptyList(),
    val downstreamKbps: Int? = null,
    val upstreamKbps: Int? = null,
)

/** قياس سرعة لحظي. */
data class SpeedSample(
    val timestampMillis: Long,
    val rxBytesPerSecond: Double,
    val txBytesPerSecond: Double,
) {
    val totalBytesPerSecond: Double get() = rxBytesPerSecond + txBytesPerSecond
}

/** حالة جلسة المراقبة الحالية. */
data class SessionStats(
    val startMillis: Long,
    val baseline: TrafficTotals,
    val totals: TrafficTotals,
    val rxSpeed: Double = 0.0,
    val txSpeed: Double = 0.0,
    val samples: List<SpeedSample> = emptyList(),
) {
    val totalSpeed: Double get() = rxSpeed + txSpeed
}

/** استهلاك تطبيق واحد. */
data class AppUsage(
    val uid: Int,
    val packageName: String?,
    val label: String,
    val isSystem: Boolean,
    val totals: TrafficTotals,
)

/** استهلاك يوم كامل. */
data class DayUsage(
    val startMillis: Long,
    val endMillis: Long,
    val totals: TrafficTotals,
)

/**
 * شريحة زمنية في قسم التطبيقات (ساعة، يوم، أو أسبوع) مع إجمالياتها،
 * تُستخدم لعرض تفصيل الفترة ثم الضغط عليها لعرض تطبيقاتها.
 */
data class UsageSlice(
    val startMillis: Long,
    val endMillis: Long,
    val totals: TrafficTotals,
)

/** فلتر عرض التطبيقات: الكل (عند عدم اختيار أي فلتر) أو المثبتة فقط أو النظام فقط. */
enum class AppsFilter { ALL, INSTALLED, SYSTEM }

/**
 * آخر نتيجة اختبار سرعة محفوظة على الجهاز لتظهر في لوحة اليوم حتى بعد
 * إغلاق التطبيق وتشغيله من جديد.
 */
data class SpeedTestRecord(
    val downloadBitsPerSecond: Double,
    val uploadBitsPerSecond: Double,
    val timestampMillis: Long,
    val usedFastCom: Boolean,
    /** false إن تعذّر قياس الرفع في ذلك الاختبار. */
    val uploadMeasured: Boolean = true,
)

/**
 * سمة التطبيق: اتباع النظام أو اختيار يدوي.
 *
 * [BLACK] هو وضع «مظلم» بخلفية AMOLED سوداء بالكامل لتوفير استهلاك الطاقة
 * على الشاشات OLED.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK, BLACK }

/** لون التمييز المختار من لوحة الألوان الجاهزة. */
enum class AccentColor {
    /** ألوان السمة الافتراضية (ألوان النظام إن كانت مفعّلة). */
    DEFAULT,
    INDIGO,
    BLUE,
    CYAN,
    TEAL,
    GREEN,
    AMBER,
    ORANGE,
    RED,
    PINK,
    PURPLE,
}

/** إعدادات التطبيق المخزّنة محليًا. */
data class AppSettings(
    val quotaBytes: Long = 0L,
    val cycleStartDay: Int = 1,
    val notificationsEnabled: Boolean = true,
    val alertThresholdPercent: Int = 80,
    /** فهرس خيار المدة في [SamplingIntervals]؛ يحفظ الوحدة حتى مع تساوي المدد. */
    val sampleIntervalIndex: Int = SamplingIntervals.DEFAULT_INDEX,
    val dynamicColorEnabled: Boolean = true,
    val accentColor: AccentColor = AccentColor.DEFAULT,
    val quickSettingsTileEnabled: Boolean = false,
    /** True only after the user has added the tile through system UI/manual edit. */
    val quickSettingsTileAdded: Boolean = false,
    /** Hide the launcher alias; the QS tile remains the way back into the app. */
    val appIconHidden: Boolean = false,
    /** `system`, `ar`, or `en`. */
    val appLanguage: String = AppLanguage.SYSTEM,
    val trackingEnabled: Boolean = false,
    /** فهرس تصميم بطاقة استهلاك الباقة المختار على لوحة المعلومات. */
    val usageCardStyleIndex: Int = 0,
    /** نمط مخطط السجل محفوظ بشكل مستقل عن نمط مخططات التطبيقات. */
    val historyLineChartEnabled: Boolean = false,
    /** نمط مخططات التطبيقات محفوظ ليستمر بعد إعادة تشغيل التطبيق. */
    val appsLineChartEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val sessionStartMillis: Long = System.currentTimeMillis(),
    val sessionBaseline: TrafficTotals = TrafficTotals.Empty,
    val alertCycleStartMillis: Long = 0L,
    val alertLevelReached: Int = 0,
    /** آخر نتيجة اختبار سرعة (fast.com) محفوظة محليًا. */
    val lastSpeedTest: SpeedTestRecord? = null,
)
