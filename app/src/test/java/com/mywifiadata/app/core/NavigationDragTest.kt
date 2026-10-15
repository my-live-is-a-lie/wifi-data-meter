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
}
