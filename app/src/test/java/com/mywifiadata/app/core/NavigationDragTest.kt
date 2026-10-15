package com.mywifiadata.app.core

import org.junit.Assert.*
import org.junit.Test

class NavigationDragTest {
    @Test fun `one continuous right to left sweep visits every RTL page`() {
        val samples = listOf(399f, 350f, 301f, 299f, 250f, 201f, 199f, 150f, 101f, 99f, 50f, 0f)
        val pages = samples.map { navigationIndexAt(it, 400f, 4, true) }.distinct()
        assertEquals(listOf(0, 1, 2, 3), pages)
    }

    @Test fun `left to right sweep visits RTL pages in reverse`() {
        assertEquals(listOf(3, 2, 1, 0), listOf(50f, 150f, 250f, 350f).map {
            navigationIndexAt(it, 400f, 4, true)
        })
    }

    @Test fun `LTR destinations follow their physical order`() {
        assertEquals(listOf(0, 1, 2, 3), listOf(50f, 150f, 250f, 350f).map {
            navigationIndexAt(it, 400f, 4, false)
        })
    }

    @Test fun `crossing a slot boundary switches immediately not on finger release`() {
        assertEquals(0, navigationIndexAt(300f, 400f, 4, true))
        assertEquals(1, navigationIndexAt(299.9f, 400f, 4, true))
        assertEquals(0, navigationIndexAt(300.1f, 400f, 4, true))
    }

    @Test fun `edge and out of bounds x coordinates stay on the nearest end tab`() {
        assertEquals(3, navigationIndexAt(-100f, 400f, 4, true))
        assertEquals(0, navigationIndexAt(500f, 400f, 4, true))
        assertEquals(0, navigationIndexAt(0f, 400f, 4, false))
        assertEquals(3, navigationIndexAt(400f, 400f, 4, false))
    }

    @Test fun `unmeasured or invalid bars never select a page`() {
        assertNull(navigationIndexAt(20f, 0f, 4, true))
        assertNull(navigationIndexAt(20f, 400f, 0, true))
        assertNull(navigationIndexAt(Float.NaN, 400f, 4, true))
        assertNull(navigationIndexAt(20f, Float.POSITIVE_INFINITY, 4, true))
    }

    @Test fun `tab centers mirror with RTL and work after resizing`() {
        assertEquals(350f, navigationTabCenter(0, 400f, 4, true), 0.001f)
        assertEquals(50f, navigationTabCenter(0, 400f, 4, false), 0.001f)
        assertEquals(700f, navigationTabCenter(0, 800f, 4, true), 0.001f)
        assertEquals(100f, navigationTabCenter(3, 800f, 4, true), 0.001f)
    }

    @Test fun `fractional widths map each center to its own item`() {
        for (rtl in listOf(false, true)) {
            for (index in 0..3) {
                assertEquals(index, navigationIndexAt(navigationTabCenter(index, 393f, 4, rtl), 393f, 4, rtl))
            }
        }
    }

    @Test fun `drag head follows finger inside the bar but stays inside the end icons`() {
        assertEquals(187f, navigationDragCenter(187f, 400f, 4), 0.001f)
        assertEquals(50f, navigationDragCenter(-20f, 400f, 4), 0.001f)
        assertEquals(350f, navigationDragCenter(420f, 400f, 4), 0.001f)
        assertEquals(200f, navigationDragCenter(300f, 400f, 1), 0.001f)
    }

    @Test fun `rightward movement extends only the left edge of the capsule`() {
        val stretch = navigationTrailImpulse(5f, 0f, 18f)
        assertTrue(stretch > 0)
        val bounds = navigationIndicatorBounds(200f, 56f, stretch, 400f)
        assertEquals(228f, bounds.right, 0.001f)
        assertTrue(bounds.left < 172f)
    }

    @Test fun `leftward movement extends only the right edge of the capsule`() {
        val stretch = navigationTrailImpulse(-5f, 0f, 18f)
        assertTrue(stretch < 0)
        val bounds = navigationIndicatorBounds(200f, 56f, stretch, 400f)
        assertEquals(172f, bounds.left, 0.001f)
        assertTrue(bounds.right > 228f)
    }

    @Test fun `direction reversal swaps the trailing edge without inherited stretch`() {
        assertEquals(-9f, navigationTrailImpulse(-5f, 18f, 18f), 0.001f)
        assertEquals(9f, navigationTrailImpulse(5f, -18f, 18f), 0.001f)
    }

    @Test fun `fast swipes have a bounded tail`() {
        assertEquals(18f, navigationTrailImpulse(500f, 18f, 18f), 0.001f)
        assertEquals(-18f, navigationTrailImpulse(-500f, -18f, 18f), 0.001f)
    }

    @Test fun `zero stretch restores the normal centered capsule`() {
        val bounds = navigationIndicatorBounds(200f, 56f, 0f, 400f)
        assertEquals(172f, bounds.left, 0.001f)
        assertEquals(228f, bounds.right, 0.001f)
        assertEquals(56f, bounds.right - bounds.left, 0.001f)
    }

    @Test fun `a tail cannot draw outside a narrow bar`() {
        val left = navigationIndicatorBounds(10f, 56f, 18f, 60f)
        val right = navigationIndicatorBounds(50f, 56f, -18f, 60f)
        assertEquals(0f, left.left, 0.001f)
        assertEquals(60f, right.right, 0.001f)
    }

    @Test fun `stationary samples do not add stretch and invalid input is safe`() {
        assertEquals(8f, navigationTrailImpulse(0f, 8f, 18f), 0.001f)
        assertEquals(0f, navigationTrailImpulse(Float.NaN, 8f, 18f), 0.001f)
        assertEquals(0f, navigationTrailImpulse(8f, 0f, 0f), 0.001f)
    }

    private fun shortSwipe(
        index: Int = 1,
        delta: Float = -20f,
        rtl: Boolean = true,
        startedOnIndicator: Boolean = true,
        crossed: Boolean = false,
        completed: Boolean = true,
        count: Int = 4,
    ) = navigationShortSwipeTarget(index, delta, count, rtl, 16f, startedOnIndicator, crossed, completed)

    @Test fun `short left swipe from the capsule selects the icon to the left in RTL`() {
        assertEquals(2, shortSwipe(delta = -20f))
    }

    @Test fun `short right swipe from the capsule selects the icon to the right in RTL`() {
        assertEquals(0, shortSwipe(delta = 20f))
    }

    @Test fun `short swipes respect physical directions in LTR too`() {
        assertEquals(2, shortSwipe(delta = 20f, rtl = false))
        assertEquals(0, shortSwipe(delta = -20f, rtl = false))
    }

    @Test fun `short swipes never wrap around at either end`() {
        assertNull(shortSwipe(index = 0, delta = 20f))
        assertNull(shortSwipe(index = 3, delta = -20f))
        assertNull(shortSwipe(index = 0, delta = -20f, rtl = false))
        assertNull(shortSwipe(index = 3, delta = 20f, rtl = false))
    }

    @Test fun `taps and tiny finger jitter do not select a neighbor`() {
        assertNull(shortSwipe(delta = 0f))
        assertNull(shortSwipe(delta = 15.9f))
        assertNull(shortSwipe(delta = -15.9f))
        assertEquals(2, shortSwipe(delta = -16f))
    }

    @Test fun `scrubbing to other tabs including out and back never adds a release step`() {
        assertNull(shortSwipe(delta = -100f, crossed = true))
        assertNull(shortSwipe(delta = -20f, crossed = true))
    }

    @Test fun `a swipe from a label or unselected icon is not a capsule shortcut`() {
        assertNull(shortSwipe(startedOnIndicator = false))
    }

    @Test fun `cancellation never navigates and returning to the press point does not navigate`() {
        assertNull(shortSwipe(completed = false))
        assertNull(shortSwipe(delta = 0f))
    }

    @Test fun `invalid index or one item cannot produce a neighbor`() {
        assertNull(shortSwipe(index = -1))
        assertNull(shortSwipe(index = 4))
        assertNull(shortSwipe(index = 0, count = 1))
        assertNull(shortSwipe(delta = Float.NaN))
    }

    @Test fun `capsule hit test includes center and rounded edge but excludes corner and label`() {
        val pill = NavigationIndicatorBounds(172f, 228f)
        assertTrue(navigationIndicatorContains(200f, 22f, pill, 22f, 32f))
        assertTrue(navigationIndicatorContains(172f, 22f, pill, 22f, 32f))
        assertTrue(navigationIndicatorContains(200f, 6f, pill, 22f, 32f))
        assertFalse(navigationIndicatorContains(172f, 6f, pill, 22f, 32f))
        assertFalse(navigationIndicatorContains(200f, 50f, pill, 22f, 32f))
    }

    @Test fun `hit test includes the visible stretched tail but not another icon`() {
        val pill = navigationIndicatorBounds(200f, 56f, 18f, 400f)
        assertTrue(navigationIndicatorContains(160f, 22f, pill, 22f, 32f))
        assertFalse(navigationIndicatorContains(100f, 22f, pill, 22f, 32f))
    }

    @Test fun `invalid capsule measurements are rejected`() {
        assertFalse(navigationIndicatorContains(1f, 1f, NavigationIndicatorBounds(0f, 0f), 0f, 32f))
        assertFalse(navigationIndicatorContains(Float.NaN, 1f, NavigationIndicatorBounds(0f, 56f), 0f, 32f))
    }

    @Test fun `icon grows progressively as the capsule approaches and shrinks when it leaves`() {
        val approach = listOf(0f, 25f, 50f, 75f, 100f).map {
            navigationIconScale(it, 100f, 100f, true)
        }
        assertEquals(1f, approach.first(), 0.0001f)
        assertEquals(1.2f, approach.last(), 0.0001f)
        assertTrue(approach.zipWithNext().all { (a, b) -> b > a })
        val departure = listOf(100f, 125f, 150f, 175f, 200f).map {
            navigationIconScale(it, 100f, 100f, true)
        }
        assertEquals(approach.reversed(), departure)
    }

    @Test fun `stationary pressed capsule keeps the icon at maximum scale`() {
        repeat(60) {
            assertEquals(1.2f, navigationIconScale(150f, 150f, 100f, true), 0.0001f)
        }
    }

    @Test fun `release or cancelled press returns every icon target to normal even under capsule`() {
        listOf(50f, 100f, 150f, 200f).forEach { x ->
            assertEquals(1f, navigationIconScale(x, 150f, 100f, false), 0.0001f)
        }
    }

    @Test fun `arriving at next icon restores previous icon target without waiting for release`() {
        assertEquals(1f, navigationIconScale(250f, 150f, 100f, true), 0.0001f)
        assertEquals(1.2f, navigationIconScale(250f, 250f, 100f, true), 0.0001f)
    }

    @Test fun `proximity is symmetric and blends neighboring icons at midpoint`() {
        assertEquals(
            navigationIconScale(125f, 150f, 100f, true),
            navigationIconScale(175f, 150f, 100f, true),
            0.0001f,
        )
        assertEquals(1.1f, navigationIconScale(200f, 150f, 100f, true), 0.0001f)
        assertEquals(1.1f, navigationIconScale(200f, 250f, 100f, true), 0.0001f)
    }

    @Test fun `proximity scale follows physical icon positions in both RTL and LTR`() {
        for (rtl in listOf(false, true)) {
            val head = navigationTabCenter(1, 400f, 4, rtl)
            for (index in 0..3) {
                val actual = navigationIconScale(head, navigationTabCenter(index, 400f, 4, rtl), 100f, true)
                assertEquals(if (index == 1) 1.2f else 1f, actual, 0.0001f)
            }
        }
    }

    @Test fun `scale is bounded for all sampled positions and adapts to bar resizing`() {
        for (x in -100..500) {
            assertTrue(navigationIconScale(x.toFloat(), 150f, 100f, true) in 1f..1.2f)
        }
        assertEquals(
            navigationIconScale(125f, 150f, 100f, true),
            navigationIconScale(250f, 300f, 200f, true),
            0.0001f,
        )
    }

    @Test fun `invalid or unmeasured scale geometry safely keeps the natural size`() {
        assertEquals(1f, navigationIconScale(150f, 150f, 0f, true), 0.0001f)
        assertEquals(1f, navigationIconScale(Float.NaN, 150f, 100f, true), 0.0001f)
        assertEquals(1f, navigationIconScale(150f, Float.POSITIVE_INFINITY, 100f, true), 0.0001f)
        assertEquals(1f, navigationIconScale(150f, 150f, Float.NaN, true), 0.0001f)
    }
}
