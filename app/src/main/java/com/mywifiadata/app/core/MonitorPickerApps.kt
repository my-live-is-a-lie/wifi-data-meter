package com.mywifiadata.app.core

import com.mywifiadata.app.data.AppSearchHistory
import com.mywifiadata.app.data.model.AppSearchSelection
import com.mywifiadata.app.data.model.AppUsage

/**
 * ترتيب قائمة اختيار هدف النافذة العائمة:
 * «جميع التطبيقات» خيار ثابت أولًا (تُضيفه الواجهة قبل القائمة)،
 * ثم التطبيقات بحسب عدد مرات اختيارها، والأكثر استهلاكًا عند التساوي.
 */
internal object MonitorPickerApps {

    fun apps(
        apps: List<AppUsage>,
        query: String,
        history: List<AppSearchSelection>,
    ): List<AppUsage> = AppSearchHistory.search(apps, query, history)
}
