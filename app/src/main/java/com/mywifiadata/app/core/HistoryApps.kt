package com.mywifiadata.app.core

import com.mywifiadata.app.data.AppSearchHistory
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.AppsFilter

/** History controls are independent of the Apps tab and only receive the selected day's rows. */
internal fun historyApps(
    apps: List<AppUsage>,
    searchActive: Boolean,
    query: String,
    sortOrder: AppsSortOrder,
): List<AppUsage> {
    val sorted = filterApps(apps, AppsFilter.ALL, sortOrder = sortOrder)
    // Search the whole day, not just the ten rows of the collapsed top-apps list.
    // No selection-history ranking here: the explicit usage order always wins.
    return if (searchActive) AppSearchHistory.search(sorted, query, emptyList()) else sorted.take(10)
}
