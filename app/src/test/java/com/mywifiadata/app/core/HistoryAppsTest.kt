package com.mywifiadata.app.core

import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.TrafficTotals
import org.junit.Assert.*
import org.junit.Test

class HistoryAppsTest {
    private fun app(uid: Int, label: String = "App $uid", bytes: Long = uid.toLong()) =
        AppUsage(uid, "example.app$uid", label, false, TrafficTotals(bytes, 0))

    @Test fun `collapsed list keeps ten highest usage rows by default`() {
        val rows = historyApps((1..20).map { app(it) }, false, "", AppsSortOrder.HIGHEST_USAGE)
        assertEquals((20 downTo 11).toList(), rows.map { it.uid })
    }

    @Test fun `reverse sorts the whole day before limiting rather than reversing the top ten`() {
        val rows = historyApps((1..20).map { app(it) }, false, "", AppsSortOrder.LOWEST_USAGE)
        assertEquals((1..10).toList(), rows.map { it.uid })
    }

    @Test fun `search finds an app outside the collapsed ten rows`() {
        val rows = historyApps((1..20).map { app(it) }, true, "example.app1 ", AppsSortOrder.HIGHEST_USAGE)
        assertTrue(rows.any { it.uid == 1 })
        assertEquals(11, rows.size)
    }

    @Test fun `blank search shows all selected day apps in the chosen usage order`() {
        val rows = historyApps((1..20).map { app(it) }, true, "  ", AppsSortOrder.LOWEST_USAGE)
        assertEquals((1..20).toList(), rows.map { it.uid })
    }

    @Test fun `Arabic names ignore diacritics and package search ignores case`() {
        val apps = listOf(app(1, "مُتَصَفِّح"), app(2, "Kiwi Browser"))
        assertEquals(listOf(1), historyApps(apps, true, "متصفح", AppsSortOrder.HIGHEST_USAGE).map { it.uid })
        assertEquals(listOf(2), historyApps(apps, true, "EXAMPLE.APP2", AppsSortOrder.HIGHEST_USAGE).map { it.uid })
    }

    @Test fun `sort is respected while search is active`() {
        val apps = listOf(app(3, "Browser"), app(1, "Browser"), app(2, "Browser"))
        assertEquals(listOf(3, 2, 1), historyApps(apps, true, "Browser", AppsSortOrder.HIGHEST_USAGE).map { it.uid })
        assertEquals(listOf(1, 2, 3), historyApps(apps, true, "Browser", AppsSortOrder.LOWEST_USAGE).map { it.uid })
    }

    @Test fun `closed search ignores query and preserves archived identities and original totals`() {
        val removed = app(1).copy(isInstalled = false, isArchivedUsage = true, cachedIconPath = "/private/icon.png")
        val apps = listOf(removed, app(2).copy(isSystem = true))
        val result = historyApps(apps, false, "no match", AppsSortOrder.HIGHEST_USAGE)
        assertEquals(listOf(apps[1], removed), result)
        assertEquals(removed, apps[0])
    }

    @Test fun `no matches and a day with no rows return empty results`() {
        assertTrue(historyApps(listOf(app(1)), true, "absent", AppsSortOrder.HIGHEST_USAGE).isEmpty())
        assertTrue(historyApps(emptyList(), true, "", AppsSortOrder.LOWEST_USAGE).isEmpty())
    }
}
