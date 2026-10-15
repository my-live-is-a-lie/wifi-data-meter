package com.mywifiadata.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AppsSearchActionTest {
    @Test
    fun `collapsed button opens search`() {
        assertEquals(AppsSearchAction.OPEN, appsSearchAction(false, ""))
    }

    @Test
    fun `open field with text clears without closing`() {
        assertEquals(AppsSearchAction.CLEAR_QUERY, appsSearchAction(true, "Kiwi"))
        assertEquals(AppsSearchAction.CLEAR_QUERY, appsSearchAction(true, "تطبيق"))
        assertEquals(AppsSearchAction.CLEAR_QUERY, appsSearchAction(true, "   "))
    }

    @Test
    fun `empty field closes and next click reopens`() {
        assertEquals(AppsSearchAction.CLOSE, appsSearchAction(true, ""))
        assertEquals(AppsSearchAction.OPEN, appsSearchAction(false, ""))
    }
}
