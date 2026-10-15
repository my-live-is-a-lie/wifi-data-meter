package com.mywifiadata.app.core

/** One shared button opens search, clears text, or closes an already-empty field. */
enum class AppsSearchAction { OPEN, CLEAR_QUERY, CLOSE }

fun appsSearchAction(expanded: Boolean, query: String): AppsSearchAction = when {
    !expanded -> AppsSearchAction.OPEN
    query.isNotEmpty() -> AppsSearchAction.CLEAR_QUERY
    else -> AppsSearchAction.CLOSE
}
