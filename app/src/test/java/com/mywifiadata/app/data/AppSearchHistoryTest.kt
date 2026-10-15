package com.mywifiadata.app.data

import com.mywifiadata.app.data.model.AppSearchSelection
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.TrafficTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchHistoryTest {
    private fun app(id: Int, label: String = "App $id", packageName: String? = "app.$id") =
        AppUsage(id, packageName, label, false, TrafficTotals(id.toLong(), 0))

    @Test
    fun `selection increments count and refreshes recency without duplicates`() {
        val first = AppSearchHistory.record(emptyList(), app(1), 100)
        val second = AppSearchHistory.record(first, app(2), 200)
        val third = AppSearchHistory.record(second, app(1), 300)
        assertEquals(listOf(AppSearchSelection("app.1", 2, 300), AppSearchSelection("app.2", 1, 200)), third)
    }

    @Test
    fun `frequency comes before recency and unselected apps retain usage order`() {
        val apps = (5 downTo 1).map { app(it) }
        val history = listOf(
            AppSearchSelection("app.1", 3, 100),
            AppSearchSelection("app.2", 1, 300),
            AppSearchSelection("app.3", 1, 200),
        )
        assertEquals(listOf(1, 2, 3, 5, 4), AppSearchHistory.search(apps, "App", history).map { it.uid })
    }

    @Test
    fun `search trims input matches names and packages and ignores case and Arabic marks`() {
        val apps = listOf(app(1, "Kiwi Browser", "com.kiwibrowser.browser"), app(2, "أخبار اليوم"))
        assertEquals(listOf(1), AppSearchHistory.search(apps, "  KIWI  ", emptyList()).map { it.uid })
        assertEquals(listOf(1), AppSearchHistory.search(apps, "COM.KIWIBROWSER", emptyList()).map { it.uid })
        assertEquals(listOf(2), AppSearchHistory.search(apps, "اخبار", emptyList()).map { it.uid })
        assertTrue(AppSearchHistory.search(apps, "absent", emptyList()).isEmpty())
    }

    @Test
    fun `history never introduces apps outside the supplied period and filters`() {
        val history = listOf(AppSearchSelection("app.99", 100, 900))
        assertEquals(listOf(1), AppSearchHistory.search(listOf(app(1)), "", history).map { it.uid })
        assertEquals(listOf(1), AppSearchHistory.search(listOf(app(1)), "App", history).map { it.uid })
    }

    @Test
    fun `history survives storage round trip and damaged entries are ignored`() {
        val history = listOf(AppSearchSelection("app.1", 5, 100), AppSearchSelection("uid:24", 1, 50))
        assertEquals(history, AppSearchHistory.decode(AppSearchHistory.encode(history)))
        assertEquals(history, AppSearchHistory.decode("garbage;!!!,3,4;YXBwLjI=,-1,3;" + AppSearchHistory.encode(history)))
        assertTrue(AppSearchHistory.decode(null).isEmpty())
    }

    @Test
    fun `unknown apps use uid while known apps keep identity across uid changes`() {
        assertEquals("uid:1", AppSearchHistory.key(app(1, packageName = null)))
        val history = AppSearchHistory.record(emptyList(), app(1), 100)
        val changedUid = app(99, packageName = "app.1")
        assertEquals(2L, AppSearchHistory.record(history, changedUid, 200).single().selectionCount)
    }

    @Test
    fun `history is bounded to the most recent hundred unique selections`() {
        var history = emptyList<AppSearchSelection>()
        (1..110).forEach { history = AppSearchHistory.record(history, app(it), it.toLong()) }
        assertEquals(AppSearchHistory.LIMIT, history.size)
        assertEquals("app.110", history.first().appKey)
        assertEquals("app.11", history.last().appKey)
        assertEquals(history, AppSearchHistory.decode(AppSearchHistory.encode(history)))
    }
    @Test
    fun `empty and whitespace queries show prior selections followed by remaining apps`() {
        val apps = (5 downTo 1).map { app(it) }
        val history = listOf(
            AppSearchSelection("app.1", 3, 100),
            AppSearchSelection("app.2", 1, 300),
            AppSearchSelection("app.3", 1, 200),
        )
        for (query in listOf("", "   ")) {
            val result = AppSearchHistory.search(apps, query, history)
            assertEquals(listOf(1, 2, 3, 5, 4), result.map { it.uid })
            assertEquals(apps.size, result.map { it.uid }.distinct().size)
        }
    }

    @Test
    fun `empty history preserves all apps in their input order`() {
        val apps = listOf(app(1), app(2))
        assertEquals(apps, AppSearchHistory.search(apps, "", emptyList()))
        assertEquals(listOf(1, 2), AppSearchHistory.search(apps, "App", emptyList()).map { it.uid })
    }

}
