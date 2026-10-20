package com.mywifiadata.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageCardStylesTest {

    @Test
    fun `old clock index moves to the new clock index`() {
        assertEquals(UsageCardStyles.CLOCK, UsageCardStyles.migrate(3, storedVersion = 0))
    }

    @Test
    fun `removed side card falls back to the full ring`() {
        assertEquals(UsageCardStyles.RING, UsageCardStyles.migrate(2, storedVersion = 0))
    }

    @Test
    fun `ring and dotted ring indexes stay unchanged`() {
        assertEquals(UsageCardStyles.RING, UsageCardStyles.migrate(0, storedVersion = 0))
        assertEquals(UsageCardStyles.DOTTED_RING, UsageCardStyles.migrate(1, storedVersion = 0))
    }

    @Test
    fun `already migrated indexes are only clamped`() {
        assertEquals(UsageCardStyles.CLOCK, UsageCardStyles.migrate(2, storedVersion = 1))
        assertEquals(UsageCardStyles.CLOCK, UsageCardStyles.migrate(9, storedVersion = 1))
        assertEquals(UsageCardStyles.RING, UsageCardStyles.migrate(-4, storedVersion = 1))
    }

    @Test
    fun `migrated values stay inside the selectable range`() {
        assertTrue(
            UsageCardStyles.migrate(8, storedVersion = 0) in
                UsageCardStyles.RING..UsageCardStyles.CLOCK,
        )
        assertEquals(UsageCardStyles.COUNT, UsageCardStyles.CLOCK + 1)
    }
}
