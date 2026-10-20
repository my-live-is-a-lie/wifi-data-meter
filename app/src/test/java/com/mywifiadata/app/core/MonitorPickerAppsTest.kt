package com.mywifiadata.app.core

import com.mywifiadata.app.data.model.AppSearchSelection
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.TrafficTotals
import org.junit.Assert.assertEquals
import org.junit.Test

/** ترتيب قائمة اختيار هدف النافذة العائمة. */
class MonitorPickerAppsTest {

    private fun app(id: Int, label: String = "App $id") =
        AppUsage(id, "app.$id", label, false, TrafficTotals(id.toLong() * 1_000, 0))

    @Test
    fun `frequently chosen apps rise to the top without changing the rest`() {
        val apps = listOf(app(1), app(2), app(3))
        val history = listOf(
            AppSearchSelection("app.3", 4, 900),
            AppSearchSelection("app.1", 1, 800),
        )

        val ordered = MonitorPickerApps.apps(apps, "", history)

        assertEquals(listOf(3, 1, 2), ordered.map { it.uid })
    }

    @Test
    fun `search still filters while keeping the selection order`() {
        val apps = listOf(app(1, "WhatsApp"), app(2, "YouTube"), app(3, "Telegram"))
        val history = listOf(AppSearchSelection("app.3", 2, 100))

        val ordered = MonitorPickerApps.apps(apps, "tel", history)

        assertEquals(listOf(3), ordered.map { it.uid })
    }

    @Test
    fun `all apps stay the caller's fixed first option`() {
        // القائمة لا تُدخل «جميع التطبيقات» بنفسها؛ الواجهة تضيفه قبل النتائج دائمًا.
        val apps = listOf(app(1), app(2))

        assertEquals(listOf(1, 2), MonitorPickerApps.apps(apps, "", emptyList()).map { it.uid })
    }
}
