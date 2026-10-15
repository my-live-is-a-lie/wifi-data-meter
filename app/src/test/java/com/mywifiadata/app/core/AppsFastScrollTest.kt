package com.mywifiadata.app.core

import org.junit.Assert.*
import org.junit.Test

class AppsFastScrollTest {
    // Five 88px app rows + four 12px gaps fit below a pinned 160px control header.
    private fun metrics(
        count: Int = 10,
        headerOffset: Int = -16,
        forward: Boolean = true,
        rows: List<AppScrollRow> = (0..4).map { AppScrollRow(4 + it, 156 + it * 100, 88) },
    ) = appsFastScrollMetrics(3, headerOffset, 160, -16, 660, 16, 12, count, rows, forward)

    @Test fun `thumb is hidden until the type header is pinned`() {
        assertNull(metrics(headerOffset = 30))
        assertNull(metrics(headerOffset = 0))
        assertNotNull(metrics(headerOffset = -16))
    }

    @Test fun `empty single and entirely fitting app lists do not need a thumb`() {
        assertNull(metrics(count = 0))
        assertNull(metrics(count = 1))
        assertNull(metrics(count = 5))
    }

    @Test fun `no rows or only header measurements cannot create a thumb`() {
        assertNull(metrics(rows = emptyList()))
        assertNull(metrics(rows = listOf(AppScrollRow(3, -16, 160))))
        assertNull(metrics(rows = listOf(AppScrollRow(4, 156, 0))))
    }

    @Test fun `top position counts only apps not summary and filter items`() {
        val result = metrics()!!
        assertEquals(0f, result.progress, 0.001f)
        assertEquals(4, result.firstAppIndex)
        assertEquals(5f, result.maxFirstRow, 0.001f)
    }

    @Test fun `middle includes the partially clipped first app`() {
        val result = metrics(rows = (0..5).map { AppScrollRow(6 + it, 106 + it * 100, 88) })!!
        assertEquals(0.5f, result.progress, 0.001f)
    }

    @Test fun `real bottom is always exactly one even with height estimation error`() {
        val result = metrics(forward = false, rows = listOf(AppScrollRow(12, 280, 100), AppScrollRow(13, 392, 190)))!!
        assertEquals(1f, result.progress, 0.001f)
    }

    @Test fun `overlay track excludes pinned header padding and bottom inset`() {
        val result = metrics()!!
        assertEquals(172, result.trackTop)
        assertEquals(660, result.trackBottom)
        assertEquals(156, result.bodyTop)
    }

    @Test fun `top target keeps the first app below the pinned controls`() {
        assertEquals(AppScrollTarget(4, -156), metrics()!!.target(0f))
    }

    @Test fun `middle target includes app index and partial row scroll offset`() {
        assertEquals(AppScrollTarget(6, -106), metrics()!!.target(0.5f))
    }

    @Test fun `bottom target uses final app allowing the actual list to clamp`() {
        assertEquals(AppScrollTarget(13), metrics()!!.target(1f))
    }

    @Test fun `out of bounds or invalid progress never targets a chart or invalid app`() {
        val result = metrics()!!
        assertEquals(result.target(0f), result.target(-1f))
        assertEquals(result.target(1f), result.target(2f))
        assertEquals(result.target(0f), result.target(Float.NaN))
    }

    @Test fun `changing filters recomputes the target range`() {
        assertEquals(AppScrollTarget(23), metrics(count = 20)!!.target(1f))
        assertEquals(AppScrollTarget(9), metrics(count = 6)!!.target(1f))
    }

    @Test fun `variable row heights keep progress and targets within bounds`() {
        val result = metrics(count = 90, rows = listOf(
            AppScrollRow(40, 90, 140), AppScrollRow(41, 242, 88), AppScrollRow(42, 342, 180),
        ))!!
        assertTrue(result.progress in 0f..1f)
        for (i in 0..100) assertTrue(result.target(i / 100f).index in 4..93)
    }

    @Test fun `edge position maps directly to start middle and end`() {
        assertEquals(0f, fastScrollEdgeProgress(16f, 432f, 32f), 0.001f)
        assertEquals(0.5f, fastScrollEdgeProgress(216f, 432f, 32f), 0.001f)
        assertEquals(1f, fastScrollEdgeProgress(416f, 432f, 32f), 0.001f)
        assertEquals(0f, fastScrollEdgeProgress(-50f, 432f, 32f), 0.001f)
        assertEquals(1f, fastScrollEdgeProgress(500f, 432f, 32f), 0.001f)
    }

    @Test fun `a viewport covered by the header has no usable track`() {
        assertNull(appsFastScrollMetrics(3, -16, 700, -16, 660, 16, 12, 10,
            listOf(AppScrollRow(4, 700, 88)), true))
    }

    @Test fun `invalid drag geometry is safe`() {
        assertEquals(0f, fastScrollEdgeProgress(50f, 0f, 32f), 0.001f)
        assertEquals(0f, fastScrollEdgeProgress(Float.NaN, 400f, 32f), 0.001f)
        assertEquals(0f, fastScrollEdgeProgress(50f, 400f, Float.NaN), 0.001f)
        assertEquals(0f, fastScrollEdgeProgress(50f, 32f, 32f), 0.001f)
    }

    @Test fun `a grab anywhere on the edge is accepted without hitting the old thumb`() {
        for (y in listOf(1f, 100f, 300f, 599f)) {
            assertTrue(fastScrollEdgeContains(12f, y, 24f, 600f))
        }
    }

    @Test fun `moving away horizontally leaves the edge even without vertical movement`() {
        assertTrue(fastScrollEdgeContains(2f, 300f, 24f, 600f))
        assertFalse(fastScrollEdgeContains(-1f, 300f, 24f, 600f))
        assertFalse(fastScrollEdgeContains(25f, 300f, 24f, 600f))
    }

    @Test fun `header and bottom navigation are outside the active rail`() {
        assertFalse(fastScrollEdgeContains(12f, -1f, 24f, 600f))
        assertFalse(fastScrollEdgeContains(12f, 601f, 24f, 600f))
    }

    @Test fun `finger near bottom selects near end even if previous thumb was at top`() {
        val progress = fastScrollEdgeProgress(316f, 432f, 32f)
        assertEquals(0.75f, progress, 0.001f)
        assertEquals(metrics()!!.target(0.75f), metrics()!!.target(progress))
    }

    @Test fun `edge mapping has no accumulated drift when reversing direction`() {
        val there = fastScrollEdgeProgress(316f, 432f, 32f)
        fastScrollEdgeProgress(116f, 432f, 32f)
        assertEquals(there, fastScrollEdgeProgress(316f, 432f, 32f), 0.001f)
        assertEquals(0.5f, fastScrollEdgeProgress(432f, 864f, 64f), 0.001f)
    }

    @Test fun `invalid edge coordinates cannot activate the grab`() {
        assertFalse(fastScrollEdgeContains(Float.NaN, 20f, 24f, 600f))
        assertFalse(fastScrollEdgeContains(2f, Float.POSITIVE_INFINITY, 24f, 600f))
        assertFalse(fastScrollEdgeContains(2f, 20f, 0f, 600f))
    }

    private fun historyMetrics(headerOffset: Int = -16, count: Int = 10) = appsFastScrollMetrics(
        headerIndex = 5, headerOffset = headerOffset, headerSize = 56,
        viewportStart = -16, viewportEnd = 660, afterPadding = 16, spacing = 12,
        appCount = count,
        rows = (0..5).map { AppScrollRow(6 + it, 52 + it * 100, 88) },
        canScrollForward = true,
    )

    @Test fun `history track begins below its shorter search header and skips chart cards`() {
        val history = historyMetrics()!!
        assertEquals(68, history.trackTop)
        assertEquals(AppScrollTarget(6, -52), history.target(0f))
        for (i in 0..100) assertTrue(history.target(i / 100f).index in 6..15)
    }

    @Test fun `history scroll end uses the ten rendered rows rather than all apps in the day`() {
        assertEquals(AppScrollTarget(15), historyMetrics(count = 10)!!.target(1f))
        assertNull(historyMetrics(count = 3)) // A newly selected short day needs no handle.
    }

    @Test fun `history thumb stays hidden while its controls are still below the pinned position`() {
        assertNull(historyMetrics(headerOffset = 200))
        assertNull(historyMetrics(headerOffset = 0))
        assertNotNull(historyMetrics(headerOffset = -16))
    }

    @Test fun `Apps and History targets are independent despite sharing the scroller`() {
        val apps = metrics()!!
        val history = historyMetrics()!!
        assertEquals(AppScrollTarget(4, -156), apps.target(0f))
        assertEquals(AppScrollTarget(6, -52), history.target(0f))
        assertEquals(172, apps.trackTop)
        assertEquals(68, history.trackTop)
    }
}
