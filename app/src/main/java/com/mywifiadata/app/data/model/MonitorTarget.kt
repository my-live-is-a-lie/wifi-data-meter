package com.mywifiadata.app.data.model

/** الحافة الجانبية التي يلتصق بها لسان النافذة العائمة. */
enum class OverlayEdge { LEFT, RIGHT }

/**
 * هدف النافذة العائمة للاستهلاك المباشر: تطبيق واحد أو «جميع التطبيقات».
 *
 * تُحفظ بيانات الهدف (الاسم والأيقونة) مع الإعدادات كي تظهر البطاقة
 * والنافذة بالشكل الصحيح حتى قبل إعادة قراءة استهلاك التطبيقات.
 */
data class MonitorTarget(
    val uid: Int = NO_APP,
    val packageName: String = "",
    val label: String = "",
    val iconPath: String = "",
) {
    val isAllApps: Boolean get() = uid < 0

    /** مفتاح مطابق لـ [AppSearchSelection.appKey] ليُرتَّب الاختيار السابق. */
    val key: String get() = if (isAllApps) ALL_APPS_KEY else packageName.ifBlank { "uid:$uid" }

    /** تحويل الهدف إلى [AppUsage] لعرض أيقونته عبر مكوّنات التطبيقات. */
    fun asUsage(): AppUsage = AppUsage(
        uid = uid,
        packageName = packageName.ifBlank { null },
        label = label,
        isSystem = false,
        totals = TrafficTotals.Empty,
        isInstalled = true,
        cachedIconPath = iconPath.ifBlank { null },
    )

    companion object {
        const val NO_APP = -1
        const val ALL_APPS_KEY = ""

        val AllApps = MonitorTarget()

        fun fromApp(app: AppUsage): MonitorTarget = MonitorTarget(
            uid = app.uid,
            packageName = app.packageName.orEmpty(),
            label = app.label,
            iconPath = app.cachedIconPath.orEmpty(),
        )
    }
}
