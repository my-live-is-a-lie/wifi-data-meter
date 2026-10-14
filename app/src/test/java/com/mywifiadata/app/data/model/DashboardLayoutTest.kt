package com.mywifiadata.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardLayoutTest {

    @Test
    fun `encoding round trips order and hidden elements`() {
        val layout = DashboardLayout(
            cardOrder = listOf(
                DashboardLayoutIds.TODAY_CARD,
                DashboardLayoutIds.USAGE_CARD,
                DashboardLayoutIds.QUICK_ACTIONS_CARD,
                DashboardLayoutIds.WIFI_CARD,
                DashboardLayoutIds.SESSION_CARD,
            ),
            hiddenCards = setOf(DashboardLayoutIds.WIFI_CARD),
            quickActionOrder = listOf(
                DashboardLayoutIds.RESET_SESSION_ACTION,
                DashboardLayoutIds.TRACKING_ACTION,
                DashboardLayoutIds.REFRESH_ACTION,
            ),
            hiddenQuickActions = setOf(DashboardLayoutIds.REFRESH_ACTION),
            todaySectionOrder = listOf(
                DashboardLayoutIds.TODAY_SPEED_TEST,
                DashboardLayoutIds.TODAY_TRAFFIC_ROW,
                DashboardLayoutIds.TODAY_TOTAL_ROW,
            ),
            hiddenTodaySections = setOf(DashboardLayoutIds.TODAY_TOTAL_ROW),
            trafficTileOrder = listOf(
                DashboardLayoutIds.UPLOAD_TILE,
                DashboardLayoutIds.DOWNLOAD_TILE,
            ),
            hiddenTrafficTiles = setOf(DashboardLayoutIds.DOWNLOAD_TILE),
        )

        assertEquals(layout, DashboardLayout.decode(layout.encode()))
    }

    @Test
    fun `normalization removes unknown and duplicate ids and restores missing items`() {
        val layout = DashboardLayout(
            cardOrder = listOf("unknown", DashboardLayoutIds.WIFI_CARD, DashboardLayoutIds.WIFI_CARD),
            hiddenCards = setOf(
                "unknown",
                DashboardLayoutIds.TODAY_CARD,
                DashboardLayoutIds.QUICK_ACTIONS_CARD,
                DashboardLayoutIds.WIFI_CARD,
            ),
            hiddenTodaySections = setOf(
                DashboardLayoutIds.TODAY_TRAFFIC_ROW,
                DashboardLayoutIds.TODAY_TOTAL_ROW,
            ),
        ).normalized()

        assertEquals(
            listOf(
                DashboardLayoutIds.WIFI_CARD,
                DashboardLayoutIds.USAGE_CARD,
                DashboardLayoutIds.QUICK_ACTIONS_CARD,
                DashboardLayoutIds.TODAY_CARD,
                DashboardLayoutIds.SESSION_CARD,
            ),
            layout.cardOrder,
        )
        assertEquals(setOf(DashboardLayoutIds.WIFI_CARD), layout.hiddenCards)
        assertEquals(
            setOf(DashboardLayoutIds.TODAY_TOTAL_ROW),
            layout.hiddenTodaySections,
        )
    }

    @Test
    fun `unknown or corrupt versions decode to defaults`() {
        assertEquals(DashboardLayout.Default, DashboardLayout.decode(null))
        assertEquals(DashboardLayout.Default, DashboardLayout.decode("v3;broken"))
    }
}
