package com.mywifiadata.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardLayoutTest {

    @Test
    fun `encoding round trips order and hidden elements`() {
        val layout = DashboardLayout(
            cardOrder = listOf(
                DashboardLayoutIds.TODAY_CARD,
                DashboardLayoutIds.SPEED_TEST_CARD,
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
                DashboardLayoutIds.SPEED_TEST_CARD,
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
    fun `legacy visible speed test migrates to an independent card`() {
        val layout = DashboardLayout.decode(legacyEncodedLayout())

        assertEquals(
            listOf(
                DashboardLayoutIds.USAGE_CARD,
                DashboardLayoutIds.QUICK_ACTIONS_CARD,
                DashboardLayoutIds.WIFI_CARD,
                DashboardLayoutIds.TODAY_CARD,
                DashboardLayoutIds.SPEED_TEST_CARD,
                DashboardLayoutIds.SESSION_CARD,
            ),
            layout.cardOrder,
        )
        assertEquals(emptySet<String>(), layout.hiddenCards)
        assertEquals(DashboardLayoutIds.TODAY_SECTION_ORDER, layout.todaySectionOrder)
        assertEquals(emptySet<String>(), layout.hiddenTodaySections)
    }

    @Test
    fun `legacy hidden speed test remains hidden after migration`() {
        val layout = DashboardLayout.decode(legacyEncodedLayout(hiddenTodaySections = "today_speed_test"))

        assertEquals(setOf(DashboardLayoutIds.SPEED_TEST_CARD), layout.hiddenCards)
        assertEquals(emptySet<String>(), layout.hiddenTodaySections)
    }

    @Test
    fun `unknown or corrupt versions decode to defaults`() {
        assertEquals(DashboardLayout.Default, DashboardLayout.decode(null))
        assertEquals(DashboardLayout.Default, DashboardLayout.decode("v3;broken"))
    }

    private fun legacyEncodedLayout(hiddenTodaySections: String = ""): String = listOf(
        "v1",
        "usage_card,quick_actions_card,wifi_card,today_card,session_card",
        "",
        "tracking_action,refresh_action,reset_session_action",
        "",
        "today_traffic_row,today_total_row,today_speed_test",
        hiddenTodaySections,
        "download_tile,upload_tile",
        "",
    ).joinToString(";")
}
