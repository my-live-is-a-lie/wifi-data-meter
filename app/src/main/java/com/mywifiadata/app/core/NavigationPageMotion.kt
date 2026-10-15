package com.mywifiadata.app.core

/** Physical side from which the destination enters: -1 = left, +1 = right, 0 = unchanged. */
internal fun navigationPageEntrySide(fromIndex: Int, toIndex: Int, isRtl: Boolean): Int {
    val logicalDirection = toIndex.compareTo(fromIndex)
    return if (isRtl) -logicalDirection else logicalDirection
}
