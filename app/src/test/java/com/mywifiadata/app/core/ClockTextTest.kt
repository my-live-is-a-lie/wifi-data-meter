package com.mywifiadata.app.core

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockTextTest {

    private val arabic = Locale.forLanguageTag("ar")
    private val english = Locale.forLanguageTag("en")

    @Test
    fun `arabic time uses arabic-indic digits and the pm label`() {
        val text = ClockText.format(
            hourOfDay = 17,
            minutes = 4,
            locale = arabic,
            amLabel = "ص",
            pmLabel = "م",
        )

        assertEquals("٥:٠٤ م", text)
    }

    @Test
    fun `english time uses latin digits and the pm label`() {
        val text = ClockText.format(
            hourOfDay = 17,
            minutes = 4,
            locale = english,
            amLabel = "AM",
            pmLabel = "PM",
        )

        assertEquals("5:04 PM", text)
    }

    @Test
    fun `midnight and noon are both shown as twelve`() {
        assertEquals(
            "١٢:٠٠ ص",
            ClockText.format(0, 0, arabic, amLabel = "ص", pmLabel = "م"),
        )
        assertEquals(
            "12:00 PM",
            ClockText.format(12, 0, english, amLabel = "AM", pmLabel = "PM"),
        )
    }

    @Test
    fun `suffix switches from am to pm at noon`() {
        assertEquals(
            "١١:٥٩ ص",
            ClockText.format(11, 59, arabic, amLabel = "ص", pmLabel = "م"),
        )
        assertEquals(
            "١٢:٠١ م",
            ClockText.format(12, 1, arabic, amLabel = "ص", pmLabel = "م"),
        )
    }

    @Test
    fun `digits outside arabic are left untouched`() {
        assertEquals("12:30", ClockText.localizedDigits("12:30", english))
        assertEquals("١٢:٣٠", ClockText.localizedDigits("12:30", arabic))
    }
}
