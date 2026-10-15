package com.mywifiadata.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationPageMotionTest {
    @Test fun `moving to higher RTL tab enters from physical left`() {
        assertEquals(-1, navigationPageEntrySide(0, 1, true))
        assertEquals(-1, navigationPageEntrySide(1, 2, true))
    }

    @Test fun `moving to lower RTL tab enters from physical right`() {
        assertEquals(1, navigationPageEntrySide(2, 1, true))
        assertEquals(1, navigationPageEntrySide(1, 0, true))
    }

    @Test fun `LTR uses the opposite physical sides`() {
        assertEquals(1, navigationPageEntrySide(0, 1, false))
        assertEquals(-1, navigationPageEntrySide(2, 1, false))
    }

    @Test fun `selecting the same tab never starts movement`() {
        for (index in 0..3) for (rtl in listOf(false, true)) {
            assertEquals(0, navigationPageEntrySide(index, index, rtl))
        }
    }

    @Test fun `skipping tabs keeps one viewport slide rather than multiplying distance`() {
        assertEquals(-1, navigationPageEntrySide(0, 3, true))
        assertEquals(1, navigationPageEntrySide(3, 0, true))
        assertEquals(1, navigationPageEntrySide(0, 3, false))
    }

    @Test fun `rapid scrubbing reverses relative to last requested tab without waiting to settle`() {
        val requested = listOf(0, 1, 2, 1, 3, 0)
        val sides = requested.zipWithNext { from, to -> navigationPageEntrySide(from, to, true) }
        assertEquals(listOf(-1, -1, 1, -1, 1), sides)
    }

    @Test fun `short swipes and continuous selection use the same direction rule`() {
        val leftNeighbor = navigationShortSwipeTarget(1, -20f, 4, true, 16f, true, false, true)!!
        val rightNeighbor = navigationShortSwipeTarget(1, 20f, 4, true, 16f, true, false, true)!!
        assertEquals(-1, navigationPageEntrySide(1, leftNeighbor, true))
        assertEquals(1, navigationPageEntrySide(1, rightNeighbor, true))
    }

    @Test fun `physical sides mirror exactly when layout direction changes`() {
        for (from in 0..3) for (to in 0..3) {
            assertEquals(-navigationPageEntrySide(from, to, false), navigationPageEntrySide(from, to, true))
        }
    }

    @Test fun `a tap changes pages instantly whether idle or interrupting a slide`() {
        for (from in 0..3) for (to in 0..3) if (from != to) {
            for (running in listOf(false, true)) {
                assertEquals(NavigationPageChange.SNAP, navigationPageChange(from, to, false, running))
            }
        }
    }

    @Test fun `continuous drag and short swipe selections keep their slide motion`() {
        for (from in 0..3) for (to in 0..3) if (from != to) {
            for (running in listOf(false, true)) {
                assertEquals(NavigationPageChange.SLIDE, navigationPageChange(from, to, true, running))
            }
        }
    }

    @Test fun `an ordinary tap on the already settled page is a no-op`() {
        for (index in 0..3) {
            assertEquals(NavigationPageChange.NONE, navigationPageChange(index, index, false, false))
        }
    }

    @Test fun `tapping the selected destination of an unfinished slide settles it immediately`() {
        assertEquals(NavigationPageChange.SNAP, navigationPageChange(1, 1, false, true))
    }

    @Test fun `remaining in the same drag slot does not interrupt its running slide`() {
        assertEquals(NavigationPageChange.NONE, navigationPageChange(1, 1, true, true))
    }

    @Test fun `tap and drag motion choices do not leak into the next interaction`() {
        assertEquals(NavigationPageChange.SLIDE, navigationPageChange(0, 1, true, false))
        assertEquals(NavigationPageChange.SNAP, navigationPageChange(1, 3, false, true))
        assertEquals(NavigationPageChange.SLIDE, navigationPageChange(3, 2, true, false))
        assertEquals(NavigationPageChange.SNAP, navigationPageChange(2, 2, false, true))
        assertEquals(NavigationPageChange.NONE, navigationPageChange(2, 2, false, false))
    }
}
