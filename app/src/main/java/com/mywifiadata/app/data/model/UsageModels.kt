package com.mywifiadata.app.data.model

import com.mywifiadata.app.core.AppLanguage
import com.mywifiadata.app.core.SamplingIntervals
import com.mywifiadata.app.core.UsageNetwork

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
    val isInstalled: Boolean = true,
    /** Private, device-local PNG; may be absent if Android did not expose an icon. */
    val cachedIconPath: String? = null,
    val iconVersion: Long = 0L,
    /** True only for observed usage restored from the local archive after uninstall. */
    val isArchivedUsage: Boolean = false,
    val usageRecordedAt: Long = 0L,
)

/** A locally remembered selection from the app search UI. */
data class AppSearchSelection(
    val appKey: String,
    val selectionCount: Long,
    val lastSelectedMillis: Long,
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

/** اتجاه عرض الواجهة: تلقائي حسب لغة التطبيق أو اختيار يدوي (من اليمين لليسار / من اليسار لليمين). */
enum class AppLayoutDirection {
    AUTO,
    RTL,
    LTR,
}

/** وحدة قياس سعة الباقة (ميجابايت / جيجابايت / تيرابايت). */
enum class PlanCapacityUnit(val bytesFactor: Long) {
    MB(1_000_000L),
    GB(1_000_000_000L),
    TB(1_000_000_000_000L),
}

/** وحدة مدة الباقة (يوم / أسبوع / شهر / سنة). */
enum class PlanDurationUnit(val daysPerUnit: Int) {
    DAY(1),
    WEEK(7),
    MONTH(30),
    YEAR(365),
}

/** نوع شبكة الباقة (واي فاي / بيانات الهاتف / كلاهما). */
enum class PlanNetworkType {
    WIFI,
    MOBILE,
    BOTH,
}

/** بيانات باقة واحدة (للواي فاي أو بيانات الهاتف). */
data class DataPlan(
    val name: String = "",
    val logoPath: String = "",
    val quotaBytes: Long = 0L,
    val capacityUnit: PlanCapacityUnit = PlanCapacityUnit.GB,
    val durationValue: Int = 28,
    val durationUnit: PlanDurationUnit = PlanDurationUnit.DAY,
    val preventEdit: Boolean = false,
    val pendingActivation: Boolean = false,
    val activatedAtMillis: Long = 0L,
) {
    val hasQuota: Boolean get() = quotaBytes > 0L

    val totalDays: Int
        get() = (durationValue.coerceAtLeast(1) * durationUnit.daysPerUnit).coerceAtLeast(1)

    fun daysLeft(now: Long): Int = when {
        !hasQuota -> 0
        pendingActivation -> totalDays
        activatedAtMillis > 0L -> {
            val endMillis = activatedAtMillis + totalDays.toLong() * 86_400_000L
            val remainingMillis = (endMillis - now).coerceAtLeast(0L)
            kotlin.math.ceil(remainingMillis.toDouble() / 86_400_000.0).toInt().coerceIn(0, totalDays)
        }
        else -> totalDays
    }
}

/** بيانات الباقة المُدخلة من نافذة إضافة/تحديث الباقة. */
data class PlanDraft(
    val name: String = "",
    val logoPath: String = "",
    val capacityValue: Double = 0.0,
    val capacityUnit: PlanCapacityUnit = PlanCapacityUnit.GB,
    val durationValue: Int = 28,
    val durationUnit: PlanDurationUnit = PlanDurationUnit.DAY,
    val networkType: PlanNetworkType = PlanNetworkType.WIFI,
    val preventEdit: Boolean = false,
) {
    val quotaBytes: Long
        get() = (capacityValue.coerceAtLeast(0.0) * capacityUnit.bytesFactor.toDouble()).toLong()
}

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
    val planName: String = "",
    val planLogoPath: String = "",
    val planCapacityUnit: PlanCapacityUnit = PlanCapacityUnit.GB,
    val planDurationValue: Int = 28,
    val planDurationUnit: PlanDurationUnit = PlanDurationUnit.DAY,
    val planNetworkType: PlanNetworkType = PlanNetworkType.WIFI,
    val planPreventEdit: Boolean = false,
    val planPendingActivation: Boolean = false,
    val planActivatedAtMillis: Long = 0L,
    val mobilePlan: DataPlan = DataPlan(),
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
    /** إخفاء أيقونة إشعار المراقبة من شريط الحالة مع إبقاء إشعار الخدمة. */
    val hideStatusBarIcon: Boolean = false,
    /** فهرس تصميم بطاقة استهلاك الباقة المختار على لوحة المعلومات. */
    val usageCardStyleIndex: Int = 0,
    /** ترتيب العناصر المخفية وترتيبها في الصفحة الرئيسية. */
    val dashboardLayout: DashboardLayout = DashboardLayout.Default,
    /** نمط مخطط السجل محفوظ بشكل مستقل عن نمط مخططات التطبيقات. */
    val historyLineChartEnabled: Boolean = false,
    /** نمط مخططات التطبيقات محفوظ ليستمر بعد إعادة تشغيل التطبيق. */
    val appsLineChartEnabled: Boolean = false,
    val appSearchHistory: List<AppSearchSelection> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val layoutDirection: AppLayoutDirection = AppLayoutDirection.AUTO,
    val sessionStartMillis: Long = System.currentTimeMillis(),
    val sessionBaseline: TrafficTotals = TrafficTotals.Empty,
    val alertCycleStartMillis: Long = 0L,
    val alertLevelReached: Int = 0,
    /** أحدث نتائج اختبارات السرعة محفوظة محليًا (الأحدث أولًا). */
    val speedTestHistory: List<SpeedTestRecord> = emptyList(),
    /** آخر نتيجة اختبار سرعة محفوظة؛ تبقى منفصلة لتوافق التخزين القديم وملخص البطاقة. */
    val lastSpeedTest: SpeedTestRecord? = null,
) {
    val planTotalDays: Int
        get() = (planDurationValue.coerceAtLeast(1) * planDurationUnit.daysPerUnit).coerceAtLeast(1)

    val wifiPlan: DataPlan
        get() = DataPlan(
            name = planName,
            logoPath = planLogoPath,
            quotaBytes = quotaBytes,
            capacityUnit = planCapacityUnit,
            durationValue = planDurationValue,
            durationUnit = planDurationUnit,
            preventEdit = planPreventEdit,
            pendingActivation = planPendingActivation,
            activatedAtMillis = planActivatedAtMillis,
        )

    fun planFor(network: UsageNetwork): DataPlan = when (network) {
        UsageNetwork.WIFI -> wifiPlan
        UsageNetwork.MOBILE -> mobilePlan
    }

    fun withPlanFor(network: UsageNetwork, plan: DataPlan): AppSettings = when (network) {
        UsageNetwork.WIFI -> copy(
            planName = plan.name,
            planLogoPath = plan.logoPath,
            quotaBytes = plan.quotaBytes,
            planCapacityUnit = plan.capacityUnit,
            planDurationValue = plan.durationValue,
            planDurationUnit = plan.durationUnit,
            planPreventEdit = plan.preventEdit,
            planPendingActivation = plan.pendingActivation,
            planActivatedAtMillis = plan.activatedAtMillis,
        )
        UsageNetwork.MOBILE -> copy(mobilePlan = plan)
    }
}
