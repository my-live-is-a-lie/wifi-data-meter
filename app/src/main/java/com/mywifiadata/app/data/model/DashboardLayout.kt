package com.mywifiadata.app.data.model

/**
 * ترتيب الصفحة الرئيسية وحالات إخفاء عناصرها. تُحفظ المسودة في الواجهة أولًا،
 * ثم يُرمّز هذا النموذج في الإعدادات عند اعتماد المستخدم للتغييرات.
 */
data class DashboardLayout(
    val cardOrder: List<String> = DashboardLayoutIds.CARD_ORDER,
    val hiddenCards: Set<String> = emptySet(),
    val quickActionOrder: List<String> = DashboardLayoutIds.QUICK_ACTION_ORDER,
    val hiddenQuickActions: Set<String> = emptySet(),
    val todaySectionOrder: List<String> = DashboardLayoutIds.TODAY_SECTION_ORDER,
    val hiddenTodaySections: Set<String> = emptySet(),
    val trafficTileOrder: List<String> = DashboardLayoutIds.TRAFFIC_TILE_ORDER,
    val hiddenTrafficTiles: Set<String> = emptySet(),
) {
    /** Removes invalid/duplicate ids and appends any supported item missing from a saved layout. */
    fun normalized(): DashboardLayout = copy(
        cardOrder = normalizeOrder(cardOrder, DashboardLayoutIds.CARD_ORDER),
        hiddenCards = normalizeHidden(hiddenCards, DashboardLayoutIds.CARD_ORDER)
            .filterNot { it in DashboardLayoutIds.COMPOSITE_CARD_IDS }
            .toSet(),
        quickActionOrder = normalizeOrder(quickActionOrder, DashboardLayoutIds.QUICK_ACTION_ORDER),
        hiddenQuickActions = normalizeHidden(hiddenQuickActions, DashboardLayoutIds.QUICK_ACTION_ORDER),
        todaySectionOrder = normalizeOrder(todaySectionOrder, DashboardLayoutIds.TODAY_SECTION_ORDER),
        hiddenTodaySections = normalizeHidden(hiddenTodaySections, DashboardLayoutIds.TODAY_SECTION_ORDER)
            .filterNot { it in DashboardLayoutIds.COMPOSITE_TODAY_SECTION_IDS }
            .toSet(),
        trafficTileOrder = normalizeOrder(trafficTileOrder, DashboardLayoutIds.TRAFFIC_TILE_ORDER),
        hiddenTrafficTiles = normalizeHidden(hiddenTrafficTiles, DashboardLayoutIds.TRAFFIC_TILE_ORDER),
    )

    /** Compact, versioned representation stored in SharedPreferences. */
    fun encode(): String {
        val layout = normalized()
        return listOf(
            VERSION,
            layout.cardOrder.joinToString(ITEM_SEPARATOR),
            encodeHidden(layout.hiddenCards, DashboardLayoutIds.CARD_ORDER),
            layout.quickActionOrder.joinToString(ITEM_SEPARATOR),
            encodeHidden(layout.hiddenQuickActions, DashboardLayoutIds.QUICK_ACTION_ORDER),
            layout.todaySectionOrder.joinToString(ITEM_SEPARATOR),
            encodeHidden(layout.hiddenTodaySections, DashboardLayoutIds.TODAY_SECTION_ORDER),
            layout.trafficTileOrder.joinToString(ITEM_SEPARATOR),
            encodeHidden(layout.hiddenTrafficTiles, DashboardLayoutIds.TRAFFIC_TILE_ORDER),
        ).joinToString(FIELD_SEPARATOR)
    }

    companion object {
        private const val VERSION = "v1"
        private const val FIELD_SEPARATOR = ";"
        private const val ITEM_SEPARATOR = ","

        val Default = DashboardLayout()

        fun decode(encoded: String?): DashboardLayout {
            if (encoded.isNullOrBlank()) return Default
            val fields = encoded.split(FIELD_SEPARATOR)
            if (fields.size != 9 || fields.firstOrNull() != VERSION) return Default

            return runCatching {
                DashboardLayout(
                    cardOrder = decodeItems(fields[1]),
                    hiddenCards = decodeHidden(fields[2]),
                    quickActionOrder = decodeItems(fields[3]),
                    hiddenQuickActions = decodeHidden(fields[4]),
                    todaySectionOrder = decodeItems(fields[5]),
                    hiddenTodaySections = decodeHidden(fields[6]),
                    trafficTileOrder = decodeItems(fields[7]),
                    hiddenTrafficTiles = decodeHidden(fields[8]),
                ).normalized()
            }.getOrDefault(Default)
        }

        private fun decodeItems(encoded: String): List<String> =
            if (encoded.isEmpty()) emptyList() else encoded.split(ITEM_SEPARATOR)

        private fun decodeHidden(encoded: String): Set<String> =
            decodeItems(encoded).toCollection(linkedSetOf())

        private fun encodeHidden(hidden: Set<String>, known: List<String>): String =
            known.filter { it in hidden }.joinToString(ITEM_SEPARATOR)

        private fun normalizeOrder(order: List<String>, known: List<String>): List<String> {
            val valid = order.filter { it in known }.distinct()
            return valid + known.filterNot(valid::contains)
        }

        private fun normalizeHidden(hidden: Set<String>, known: List<String>): Set<String> =
            known.filter { it in hidden }.toCollection(linkedSetOf())
    }
}

/** Stable ids and default ordering for all dashboard-editable groups. */
object DashboardLayoutIds {
    const val USAGE_CARD = "usage_card"
    const val QUICK_ACTIONS_CARD = "quick_actions_card"
    const val WIFI_CARD = "wifi_card"
    const val TODAY_CARD = "today_card"
    const val SESSION_CARD = "session_card"

    const val TRACKING_ACTION = "tracking_action"
    const val REFRESH_ACTION = "refresh_action"
    const val RESET_SESSION_ACTION = "reset_session_action"

    const val TODAY_TRAFFIC_ROW = "today_traffic_row"
    const val TODAY_TOTAL_ROW = "today_total_row"
    const val TODAY_SPEED_TEST = "today_speed_test"

    const val DOWNLOAD_TILE = "download_tile"
    const val UPLOAD_TILE = "upload_tile"

    val CARD_ORDER = listOf(
        USAGE_CARD,
        QUICK_ACTIONS_CARD,
        WIFI_CARD,
        TODAY_CARD,
        SESSION_CARD,
    )
    val COMPOSITE_CARD_IDS = setOf(QUICK_ACTIONS_CARD, TODAY_CARD)
    val QUICK_ACTION_ORDER = listOf(
        TRACKING_ACTION,
        REFRESH_ACTION,
        RESET_SESSION_ACTION,
    )
    val TODAY_SECTION_ORDER = listOf(
        TODAY_TRAFFIC_ROW,
        TODAY_TOTAL_ROW,
        TODAY_SPEED_TEST,
    )
    val COMPOSITE_TODAY_SECTION_IDS = setOf(TODAY_TRAFFIC_ROW)
    val TRAFFIC_TILE_ORDER = listOf(DOWNLOAD_TILE, UPLOAD_TILE)
}
