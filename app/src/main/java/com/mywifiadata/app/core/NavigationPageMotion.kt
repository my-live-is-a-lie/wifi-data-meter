package com.mywifiadata.app.core

/** Physical side from which the destination enters: -1 = left, +1 = right, 0 = unchanged. */
internal fun navigationPageEntrySide(fromIndex: Int, toIndex: Int, isRtl: Boolean): Int {
    val logicalDirection = toIndex.compareTo(fromIndex)
    return if (isRtl) -logicalDirection else logicalDirection
}

internal enum class NavigationPageChange { NONE, SNAP, SLIDE }

/** A tap can also finish an interrupted slide to the already-selected destination. */
internal fun navigationPageChange(
    fromIndex: Int,
    toIndex: Int,
    fromDrag: Boolean,
    isTransitionRunning: Boolean,
): NavigationPageChange = when {
    !fromDrag && (fromIndex != toIndex || isTransitionRunning) -> NavigationPageChange.SNAP
    fromDrag && fromIndex != toIndex -> NavigationPageChange.SLIDE
    else -> NavigationPageChange.NONE
}
