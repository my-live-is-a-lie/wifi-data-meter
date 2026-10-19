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
    val isMobileConnected: Boolean = false,
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
    /**
     * كمية البيانات التي استهلكها الاختبار فعليًا (تنزيل + رفع + تجهيز).
     *
     * صفر يعني أن الاختبار سُجّل قبل إضافة هذا القياس؛ فيُقدَّر استهلاكه من
     * سرعتيه عند حساب تقدير الاستهلاك للاختبار القادم.
     */
    val dataBytes: Long = 0L,
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

/** الحد الأقصى للباقات الإضافية التابعة للباقة النشطة. */
const val MAX_ADDITIONAL_PLANS = 2

/** باقة إضافية تابعة للباقة النشطة تأخذ مكانها عند انتهائها. */
data class AdditionalPlan(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val logoPath: String = "",
    val quotaBytes: Long = 0L,
    val capacityUnit: PlanCapacityUnit = PlanCapacityUnit.MB,
    val durationValue: Int = 7,
    val durationUnit: PlanDurationUnit = PlanDurationUnit.DAY,
    val preventEdit: Boolean = false,
) {
    val totalDays: Int
        get() = (durationValue.coerceAtLeast(1) * durationUnit.daysPerUnit).coerceAtLeast(1)
}

/** بيانات الباقة الإضافية المُدخلة من نافذة إضافة/تحديث باقة إضافية. */
data class AdditionalPlanDraft(
    val name: String = "",
    val logoPath: String = "",
    val capacityValue: Double = 0.0,
    val capacityUnit: PlanCapacityUnit = PlanCapacityUnit.MB,
    val durationValue: Int = 7,
    val durationUnit: PlanDurationUnit = PlanDurationUnit.DAY,
    val preventEdit: Boolean = false,
) {
    val quotaBytes: Long
        get() = (capacityValue.coerceAtLeast(0.0) * capacityUnit.bytesFactor.toDouble()).toLong()
}

/** بيانات باقة واحدة (للواي فاي أو بيانات الهاتف). */
data class DataPlan(
    val id: String = "default-plan",
    val name: String = "",
    val logoPath: String = "",
    val quotaBytes: Long = 0L,
    val capacityUnit: PlanCapacityUnit = PlanCapacityUnit.GB,
    val durationValue: Int = 28,
    val durationUnit: PlanDurationUnit = PlanDurationUnit.DAY,
    val preventEdit: Boolean = false,
    val pendingActivation: Boolean = false,
    val activatedAtMillis: Long = 0L,
    val additionalPlans: List<AdditionalPlan> = emptyList(),
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

    fun isExpiredOrDepleted(usedBytes: Long, now: Long): Boolean {
        if (!hasQuota || pendingActivation || activatedAtMillis <= 0L) return false
        val endMillis = activatedAtMillis + totalDays.toLong() * 86_400_000L
        return now >= endMillis || usedBytes >= quotaBytes
    }

    companion object {
        fun fromAdditionalPlan(
            additional: AdditionalPlan,
            remainingAdditional: List<AdditionalPlan> = emptyList(),
        ): DataPlan = DataPlan(
            id = additional.id,
            name = additional.name,
            logoPath = additional.logoPath,
            quotaBytes = additional.quotaBytes,
            capacityUnit = additional.capacityUnit,
            durationValue = additional.durationValue,
            durationUnit = additional.durationUnit,
            preventEdit = additional.preventEdit,
            pendingActivation = true,
            activatedAtMillis = 0L,
            additionalPlans = remainingAdditional.take(MAX_ADDITIONAL_PLANS),
        )
    }
}

/**
 * سجل باقة منتهية (سواء بانتهاء المدة الزمنية أو نفاد السعة 100%) لعرضه في قسم «الباقة» داخل شاشة السجل.
 */
data class CompletedPlanRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val planId: String = "",
    val name: String = "",
    val logoPath: String = "",
    val quotaBytes: Long = 0L,
    val capacityUnit: PlanCapacityUnit = PlanCapacityUnit.GB,
    val durationValue: Int = 28,
    val durationUnit: PlanDurationUnit = PlanDurationUnit.DAY,
    val startMillis: Long = 0L,
    val endMillis: Long = 0L,
    val usedBytes: Long = 0L,
) {
    val totalDays: Int
        get() = (durationValue.coerceAtLeast(1) * durationUnit.daysPerUnit).coerceAtLeast(1)
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
    val planId: String = "wifi-active",
    val planName: String = "",
    val planLogoPath: String = "",
    val planCapacityUnit: PlanCapacityUnit = PlanCapacityUnit.GB,
    val planDurationValue: Int = 28,
    val planDurationUnit: PlanDurationUnit = PlanDurationUnit.DAY,
    val planNetworkType: PlanNetworkType = PlanNetworkType.WIFI,
    val planPreventEdit: Boolean = false,
    val planPendingActivation: Boolean = false,
    val planActivatedAtMillis: Long = 0L,
    val planAdditionalPlans: List<AdditionalPlan> = emptyList(),
    val mobilePlan: DataPlan = DataPlan(id = "mobile-active"),
    val savedWifiPlans: List<DataPlan> = emptyList(),
    val savedMobilePlans: List<DataPlan> = emptyList(),
    val wifiActivePlanIndex: Int = 0,
    val mobileActivePlanIndex: Int = 0,
    val completedWifiPlans: List<CompletedPlanRecord> = emptyList(),
    val completedMobilePlans: List<CompletedPlanRecord> = emptyList(),
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
    /**
     * نهاية فترة تجاهل تحذير استهلاك البيانات في اختبار السرعة على بيانات
     * الهاتف. صفر أو قيمة ماضية تعني أن التحذير يظهر من جديد.
     */
    val speedTestWarningSnoozedUntilMillis: Long = 0L,
) {
    val planTotalDays: Int
        get() = (planDurationValue.coerceAtLeast(1) * planDurationUnit.daysPerUnit).coerceAtLeast(1)

    val wifiPlan: DataPlan
        get() = DataPlan(
            id = planId,
            name = planName,
            logoPath = planLogoPath,
            quotaBytes = quotaBytes,
            capacityUnit = planCapacityUnit,
            durationValue = planDurationValue,
            durationUnit = planDurationUnit,
            preventEdit = planPreventEdit,
            pendingActivation = planPendingActivation,
            activatedAtMillis = planActivatedAtMillis,
            additionalPlans = planAdditionalPlans,
        )

    fun planFor(network: UsageNetwork): DataPlan = when (network) {
        UsageNetwork.WIFI -> wifiPlan
        UsageNetwork.MOBILE -> mobilePlan
    }

    fun withPlanFor(network: UsageNetwork, plan: DataPlan): AppSettings = when (network) {
        UsageNetwork.WIFI -> copy(
            planId = plan.id,
            planName = plan.name,
            planLogoPath = plan.logoPath,
            quotaBytes = plan.quotaBytes,
            planCapacityUnit = plan.capacityUnit,
            planDurationValue = plan.durationValue,
            planDurationUnit = plan.durationUnit,
            planPreventEdit = plan.preventEdit,
            planPendingActivation = plan.pendingActivation,
            planActivatedAtMillis = plan.activatedAtMillis,
            planAdditionalPlans = plan.additionalPlans.take(MAX_ADDITIONAL_PLANS),
        )
        UsageNetwork.MOBILE -> copy(
            mobilePlan = plan.copy(additionalPlans = plan.additionalPlans.take(MAX_ADDITIONAL_PLANS)),
        )
    }

    fun savedPlansFor(network: UsageNetwork): List<DataPlan> = when (network) {
        UsageNetwork.WIFI -> savedWifiPlans
        UsageNetwork.MOBILE -> savedMobilePlans
    }

    fun withSavedPlansFor(network: UsageNetwork, plans: List<DataPlan>): AppSettings = when (network) {
        UsageNetwork.WIFI -> copy(
            savedWifiPlans = plans,
            wifiActivePlanIndex = wifiActivePlanIndex.coerceIn(0, plans.size),
        )
        UsageNetwork.MOBILE -> copy(
            savedMobilePlans = plans,
            mobileActivePlanIndex = mobileActivePlanIndex.coerceIn(0, plans.size),
        )
    }

    fun activePlanIndexFor(network: UsageNetwork): Int = when (network) {
        UsageNetwork.WIFI -> wifiActivePlanIndex
        UsageNetwork.MOBILE -> mobileActivePlanIndex
    }.coerceIn(0, savedPlansFor(network).size)

    fun withActivePlanIndexFor(network: UsageNetwork, index: Int): AppSettings {
        val coerced = index.coerceIn(0, savedPlansFor(network).size)
        return when (network) {
            UsageNetwork.WIFI -> copy(wifiActivePlanIndex = coerced)
            UsageNetwork.MOBILE -> copy(mobileActivePlanIndex = coerced)
        }
    }

    /**
     * قائمة جميع الباقات المعروضة في نافذة «الباقات» بالترتيب الذي اختاره المستخدم،
     * بما في ذلك الباقة المفعلة والباقات المحفوظة.
     */
    fun allPlansFor(network: UsageNetwork): List<DataPlan> {
        val active = planFor(network)
        val saved = savedPlansFor(network)
        if (!active.hasQuota) return saved
        val list = saved.toMutableList()
        val insertIndex = activePlanIndexFor(network).coerceIn(0, list.size)
        list.add(insertIndex, active)
        return list
    }

    fun completedPlansFor(network: UsageNetwork): List<CompletedPlanRecord> = when (network) {
        UsageNetwork.WIFI -> completedWifiPlans
        UsageNetwork.MOBILE -> completedMobilePlans
    }

    fun withCompletedPlansFor(network: UsageNetwork, records: List<CompletedPlanRecord>): AppSettings = when (network) {
        UsageNetwork.WIFI -> copy(completedWifiPlans = records)
        UsageNetwork.MOBILE -> copy(completedMobilePlans = records)
    }

    /**
     * عند انتهاء مدة الباقة النشطة أو نفاد سعتها، تُسجّل في سجل الباقات المنتهية،
     * وإذا وُجدت باقات إضافية تحل الباقة الإضافية الأولى مكان الباقة الرئيسية
     * (بانتظار التفعيل اليدوي) وتُحفظ الباقة السابقة ضمن قائمة الباقات المحفوظة.
     */
    fun promoteExpiredPlanForNetwork(
        network: UsageNetwork,
        usedBytes: Long,
        now: Long,
    ): AppSettings {
        val currentPlan = planFor(network)
        if (!currentPlan.isExpiredOrDepleted(usedBytes, now)) return this
        val startMillis = currentPlan.activatedAtMillis
        val scheduledEndMillis = startMillis + currentPlan.totalDays.toLong() * 86_400_000L
        val effectiveEndMillis = minOf(now, scheduledEndMillis).coerceAtLeast(startMillis)
        val recordId = "${currentPlan.id}_$startMillis"
        val existingCompleted = completedPlansFor(network)
        val updatedWithRecord = if (existingCompleted.any { it.id == recordId }) {
            this
        } else {
            val completedRecord = CompletedPlanRecord(
                id = recordId,
                planId = currentPlan.id,
                name = currentPlan.name,
                logoPath = currentPlan.logoPath,
                quotaBytes = currentPlan.quotaBytes,
                capacityUnit = currentPlan.capacityUnit,
                durationValue = currentPlan.durationValue,
                durationUnit = currentPlan.durationUnit,
                startMillis = startMillis,
                endMillis = effectiveEndMillis,
                usedBytes = usedBytes,
            )
            withCompletedPlansFor(network, existingCompleted + completedRecord)
        }
        val firstAdditional = currentPlan.additionalPlans.firstOrNull() ?: return updatedWithRecord
        val remainingAdditional = currentPlan.additionalPlans.drop(1)
        val archivedCurrent = currentPlan.copy(
            id = currentPlan.id.ifBlank { java.util.UUID.randomUUID().toString() },
            pendingActivation = true,
            activatedAtMillis = 0L,
            additionalPlans = emptyList(),
        )
        val promotedPlan = DataPlan.fromAdditionalPlan(
            additional = firstAdditional,
            remainingAdditional = remainingAdditional,
        )
        val updatedSaved = updatedWithRecord.savedPlansFor(network)
            .filterNot { it.id == archivedCurrent.id } + archivedCurrent
        return updatedWithRecord.withPlanFor(network, promotedPlan)
            .withSavedPlansFor(network, updatedSaved)
    }

    fun promoteExpiredPlansIfNeeded(
        wifiUsedBytes: Long,
        mobileUsedBytes: Long,
        now: Long,
    ): AppSettings = promoteExpiredPlanForNetwork(UsageNetwork.WIFI, wifiUsedBytes, now)
        .promoteExpiredPlanForNetwork(UsageNetwork.MOBILE, mobileUsedBytes, now)

    fun addOrSavePlanFromList(draft: PlanDraft): AppSettings {
        val targetNetworks = when (draft.networkType) {
            PlanNetworkType.WIFI -> listOf(UsageNetwork.WIFI)
            PlanNetworkType.MOBILE -> listOf(UsageNetwork.MOBILE)
            PlanNetworkType.BOTH -> listOf(UsageNetwork.WIFI, UsageNetwork.MOBILE)
        }
        var updated = copy(planNetworkType = draft.networkType)
        val newQuotaBytes = draft.quotaBytes
        if (newQuotaBytes <= 0L) return updated
        val safeDuration = draft.durationValue.coerceAtLeast(1)
        for (network in targetNetworks) {
            val newPlan = DataPlan(
                id = java.util.UUID.randomUUID().toString(),
                name = draft.name.trim(),
                logoPath = draft.logoPath,
                quotaBytes = newQuotaBytes,
                capacityUnit = draft.capacityUnit,
                durationValue = safeDuration,
                durationUnit = draft.durationUnit,
                preventEdit = draft.preventEdit,
                pendingActivation = true,
                activatedAtMillis = 0L,
            )
            val currentActive = updated.planFor(network)
            updated = if (!currentActive.hasQuota) {
                updated.withPlanFor(network, newPlan)
            } else {
                updated.withSavedPlansFor(network, updated.savedPlansFor(network) + newPlan)
            }
        }
        return updated
    }

    fun updateSavedPlan(
        network: UsageNetwork,
        planId: String,
        draft: PlanDraft,
    ): AppSettings {
        val updatedList = savedPlansFor(network).map { plan ->
            if (plan.id != planId) {
                plan
            } else if (plan.hasQuota && plan.preventEdit) {
                plan.copy(
                    name = draft.name.trim(),
                    logoPath = draft.logoPath,
                )
            } else {
                val newQuotaBytes = draft.quotaBytes
                val safeDuration = draft.durationValue.coerceAtLeast(1)
                plan.copy(
                    name = draft.name.trim(),
                    logoPath = draft.logoPath,
                    quotaBytes = newQuotaBytes,
                    capacityUnit = draft.capacityUnit,
                    durationValue = safeDuration,
                    durationUnit = draft.durationUnit,
                    preventEdit = if (newQuotaBytes > 0L) draft.preventEdit else false,
                )
            }
        }.filter { it.hasQuota }
        return withSavedPlansFor(network, updatedList)
    }

    fun switchToSavedPlan(
        network: UsageNetwork,
        planId: String,
        now: Long,
    ): AppSettings {
        val savedList = savedPlansFor(network)
        val targetPlan = savedList.firstOrNull { it.id == planId } ?: return this
        val currentActive = planFor(network)
        val activatedTarget = targetPlan.copy(
            pendingActivation = false,
            activatedAtMillis = now,
        )
        if (!currentActive.hasQuota) {
            val remainingSaved = savedList.filterNot { it.id == planId }
            val targetIdx = savedList.indexOfFirst { it.id == planId }.coerceIn(0, remainingSaved.size)
            return withPlanFor(network, activatedTarget)
                .withSavedPlansFor(network, remainingSaved)
                .withActivePlanIndexFor(network, targetIdx)
        }
        val resetPrevious = currentActive.copy(
            id = currentActive.id.ifBlank { java.util.UUID.randomUUID().toString() },
            pendingActivation = true,
            activatedAtMillis = 0L,
        )
        val combined = allPlansFor(network)
        val newActiveIdx = combined.indexOfFirst { it.id == planId }.coerceAtLeast(0)
        val updatedSaved = combined.mapNotNull { item ->
            when (item.id) {
                planId -> null
                currentActive.id -> resetPrevious
                else -> item
            }
        }
        return withPlanFor(network, activatedTarget)
            .withSavedPlansFor(network, updatedSaved)
            .withActivePlanIndexFor(network, newActiveIdx)
    }

    fun deletePlan(
        network: UsageNetwork,
        planId: String,
    ): AppSettings {
        val currentActive = planFor(network)
        if (currentActive.hasQuota && currentActive.id == planId) {
            val firstAdditional = currentActive.additionalPlans.firstOrNull()
            return if (firstAdditional != null) {
                val promoted = DataPlan.fromAdditionalPlan(
                    additional = firstAdditional,
                    remainingAdditional = currentActive.additionalPlans.drop(1),
                )
                withPlanFor(network, promoted)
            } else {
                withPlanFor(
                    network = network,
                    plan = DataPlan(
                        id = if (network == UsageNetwork.WIFI) "wifi-active" else "mobile-active",
                    ),
                )
            }
        }
        return withSavedPlansFor(
            network = network,
            plans = savedPlansFor(network).filterNot { it.id == planId },
        )
    }

    fun addAdditionalPlan(
        network: UsageNetwork,
        draft: AdditionalPlanDraft,
    ): AppSettings {
        val currentActive = planFor(network)
        if (!currentActive.hasQuota || currentActive.additionalPlans.size >= MAX_ADDITIONAL_PLANS) {
            return this
        }
        val newQuotaBytes = draft.quotaBytes
        if (newQuotaBytes <= 0L) return this
        val newAdditional = AdditionalPlan(
            id = java.util.UUID.randomUUID().toString(),
            name = draft.name.trim(),
            logoPath = draft.logoPath,
            quotaBytes = newQuotaBytes,
            capacityUnit = draft.capacityUnit,
            durationValue = draft.durationValue.coerceAtLeast(1),
            durationUnit = draft.durationUnit,
            preventEdit = draft.preventEdit,
        )
        return withPlanFor(
            network = network,
            plan = currentActive.copy(
                additionalPlans = (currentActive.additionalPlans + newAdditional).take(MAX_ADDITIONAL_PLANS),
            ),
        )
    }

    fun updateAdditionalPlan(
        network: UsageNetwork,
        additionalPlanId: String,
        draft: AdditionalPlanDraft,
    ): AppSettings {
        val currentActive = planFor(network)
        val updatedAdditional = currentActive.additionalPlans.map { item ->
            if (item.id != additionalPlanId) {
                item
            } else if (item.preventEdit) {
                item.copy(
                    name = draft.name.trim(),
                    logoPath = draft.logoPath,
                )
            } else {
                val newQuotaBytes = draft.quotaBytes
                if (newQuotaBytes <= 0L) {
                    item
                } else {
                    item.copy(
                        name = draft.name.trim(),
                        logoPath = draft.logoPath,
                        quotaBytes = newQuotaBytes,
                        capacityUnit = draft.capacityUnit,
                        durationValue = draft.durationValue.coerceAtLeast(1),
                        durationUnit = draft.durationUnit,
                        preventEdit = draft.preventEdit,
                    )
                }
            }
        }
        return withPlanFor(
            network = network,
            plan = currentActive.copy(additionalPlans = updatedAdditional),
        )
    }

    fun deleteAdditionalPlan(
        network: UsageNetwork,
        additionalPlanId: String,
    ): AppSettings {
        val currentActive = planFor(network)
        return withPlanFor(
            network = network,
            plan = currentActive.copy(
                additionalPlans = currentActive.additionalPlans.filterNot { it.id == additionalPlanId },
            ),
        )
    }

    fun reorderAdditionalPlans(
        network: UsageNetwork,
        fromIndex: Int,
        toIndex: Int,
    ): AppSettings {
        val currentActive = planFor(network)
        val list = currentActive.additionalPlans.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) {
            return this
        }
        val moved = list.removeAt(fromIndex)
        list.add(toIndex, moved)
        return withPlanFor(
            network = network,
            plan = currentActive.copy(additionalPlans = list),
        )
    }

    fun reorderSavedPlans(
        network: UsageNetwork,
        fromIndex: Int,
        toIndex: Int,
    ): AppSettings {
        val active = planFor(network)
        val saved = savedPlansFor(network)
        if (!active.hasQuota) {
            val list = saved.toMutableList()
            if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) {
                return this
            }
            val moved = list.removeAt(fromIndex)
            list.add(toIndex, moved)
            return withSavedPlansFor(network, list)
        }
        val activeIndex = activePlanIndexFor(network).coerceIn(0, saved.size)
        val tagged = saved.map { it to false }.toMutableList()
        tagged.add(activeIndex, active to true)
        if (fromIndex !in tagged.indices || toIndex !in tagged.indices || fromIndex == toIndex) {
            return this
        }
        val moved = tagged.removeAt(fromIndex)
        tagged.add(toIndex, moved)
        val newActiveIndex = tagged.indexOfFirst { it.second }.coerceAtLeast(0)
        val newSaved = tagged.filterNot { it.second }.map { it.first }
        return withSavedPlansFor(network, newSaved)
            .withActivePlanIndexFor(network, newActiveIndex)
    }

    /**
     * إعادة تعيين الباقة الرئيسية: تصفير الاستهلاك وعدد الأيام قبل بدء الاحتساب
     * وإلغاء خيار «امنع التعديل» ليتمكن المستخدم من تعديل السعة والمدة مجددًا.
     */
    fun resetActivePlan(network: UsageNetwork): AppSettings {
        val current = planFor(network)
        if (!current.hasQuota) return this
        return withPlanFor(
            network = network,
            plan = current.copy(
                preventEdit = false,
                pendingActivation = true,
                activatedAtMillis = 0L,
            ),
        )
    }

    /**
     * إعادة تعيين باقة محفوظة: تصفير حالتها وإلغاء خيار «امنع التعديل».
     */
    fun resetSavedPlan(network: UsageNetwork, planId: String): AppSettings {
        val updated = savedPlansFor(network).map { plan ->
            if (plan.id == planId) {
                plan.copy(
                    preventEdit = false,
                    pendingActivation = true,
                    activatedAtMillis = 0L,
                )
            } else {
                plan
            }
        }
        return withSavedPlansFor(network, updated)
    }

    /**
     * إعادة تعيين باقة إضافية لإلغاء خيار «امنع التعديل».
     */
    fun resetAdditionalPlan(network: UsageNetwork, additionalPlanId: String): AppSettings {
        val currentActive = planFor(network)
        val updatedAdditional = currentActive.additionalPlans.map { item ->
            if (item.id == additionalPlanId) item.copy(preventEdit = false) else item
        }
        return withPlanFor(
            network = network,
            plan = currentActive.copy(additionalPlans = updatedAdditional),
        )
    }
}
