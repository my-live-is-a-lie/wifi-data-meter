package com.mywifiadata.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockArcTest {

    @Test
    fun `arc starts at the top middle of the dial`() {
        assertEquals(-90f, ClockArc.START_ANGLE_DEG, 0f)
    }

    @Test
    fun `empty usage draws no arc`() {
        assertEquals(0f, ClockArc.sweepAngleDeg(0f), 0f)
    }

    @Test
    fun `half usage covers a quarter turn down the right side`() {
        assertEquals(90f, ClockArc.sweepAngleDeg(0.5f), 0.001f)
    }

    @Test
    fun `full usage ends at the bottom middle`() {
        assertEquals(180f, ClockArc.sweepAngleDeg(1f), 0f)
    }

    @Test
    fun `values outside the range never pass the bottom`() {
        assertEquals(0f, ClockArc.sweepAngleDeg(-0.4f), 0f)
        assertEquals(180f, ClockArc.sweepAngleDeg(2.5f), 0f)
    }
}
