package com.mywifiadata.app.data

import com.mywifiadata.app.core.TimeRange
import com.mywifiadata.app.core.UsageNetwork
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.TrafficTotals
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppUsageArchiveTest {
    @get:Rule val temporary = TemporaryFolder()
    private val day = 86_400_000L
    private val wifi = UsageNetwork.WIFI
    private fun app(bytes: Long = 100, pkg: String = "example.app") = AppUsage(10001, pkg, "Example", false, TrafficTotals(bytes, 10))

    @Test
    fun `uninstall restores an absent UID after reopening with original identity and saved flag`() {
        val directory = temporary.newFolder()
        AppUsageArchive(directory).record(TimeRange(0, day), wifi, listOf(app()), day)
        val restored = AppUsageArchive(directory).restoreMissing(emptySet(), TimeRange(0, day), wifi) { _, totals ->
            app().copy(isInstalled = false, totals = totals, cachedIconPath = "/private/saved.png")
        }.single()
        assertEquals("Example", restored.label)
        assertEquals("/private/saved.png", restored.cachedIconPath)
        assertFalse(restored.isInstalled)
        assertTrue(restored.isArchivedUsage)
        assertEquals(day, restored.usageRecordedAt)
        assertEquals(TrafficTotals(100, 10), restored.totals)
    }

    @Test
    fun `live UIDs including zero readings are not duplicated and installed apps are not restored`() {
        val archive = AppUsageArchive(temporary.newFolder())
        archive.record(TimeRange(0, day), wifi, listOf(app()), day)
        assertTrue(archive.restoreMissing(setOf(10001), TimeRange(0, day), wifi) { _, _ -> error("live UID must not be resolved") }.isEmpty())
        assertTrue(archive.restoreMissing(emptySet(), TimeRange(0, day), wifi) { _, totals -> app().copy(totals = totals) }.isEmpty())
    }

    @Test
    fun `UID reuse or unknown identity cannot receive another apps archived usage`() {
        val archive = AppUsageArchive(temporary.newFolder())
        archive.record(TimeRange(0, day), wifi, listOf(app()), day)
        assertTrue(archive.restoreMissing(emptySet(), TimeRange(0, day), wifi) { _, totals ->
            app(pkg = "other.app").copy(isInstalled = false, totals = totals)
        }.isEmpty())
        assertTrue(archive.restoreMissing(emptySet(), TimeRange(0, day), wifi) { _, totals ->
            app().copy(packageName = null, isInstalled = false, totals = totals)
        }.isEmpty())
    }

    @Test
    fun `empty later query does not erase the removed app or pretend its reading is newer`() {
        val archive = AppUsageArchive(temporary.newFolder())
        archive.record(TimeRange(0, day / 2), wifi, listOf(app()), day / 2)
        archive.record(TimeRange(0, day), wifi, emptyList(), day)
        val saved = archive.read(TimeRange(0, day), wifi).single()
        assertEquals(day / 2, saved.recordedAt)
        assertEquals(TrafficTotals(100, 10), saved.totals)
    }

    @Test
    fun `daily and overlapping multi day readings are never counted twice`() {
        val archive = AppUsageArchive(temporary.newFolder())
        archive.record(TimeRange(0, day), wifi, listOf(app(100)), 3 * day)
        archive.record(TimeRange(day, 2 * day), wifi, listOf(app(200)), 3 * day)
        archive.record(TimeRange(0, 2 * day), wifi, listOf(app(300)), 4 * day)
        assertEquals(TrafficTotals(300, 10), archive.read(TimeRange(0, 2 * day), wifi).single().totals)
        assertEquals(TrafficTotals(200, 10), archive.read(TimeRange(day, 2 * day), wifi).single().totals)
    }

    @Test
    fun `network and exact period boundaries are respected without prorating daily totals`() {
        val archive = AppUsageArchive(temporary.newFolder())
        archive.record(TimeRange(0, day), wifi, listOf(app()), day)
        archive.record(TimeRange(0, day), UsageNetwork.MOBILE, listOf(app(500)), day)
        assertEquals(100L, archive.read(TimeRange(0, day), wifi).single().totals.rxBytes)
        assertEquals(500L, archive.read(TimeRange(0, day), UsageNetwork.MOBILE).single().totals.rxBytes)
        assertTrue(archive.read(TimeRange(0, day / 2), wifi).isEmpty())
        assertTrue(archive.read(TimeRange(day, 2 * day), wifi).isEmpty())
    }

    @Test
    fun `newer capture replaces the same slot rather than being added`() {
        val archive = AppUsageArchive(temporary.newFolder())
        archive.record(TimeRange(0, day / 2), wifi, listOf(app(100)), day / 2)
        archive.record(TimeRange(0, day), wifi, listOf(app(200)), day)
        assertEquals(200L, archive.read(TimeRange(0, day), wifi).single().totals.rxBytes)
        assertTrue(archive.hasCompleteDay(TimeRange(0, day), wifi))
    }

    @Test
    fun `restored rows are never rearchived and clear removes retained data`() {
        val directory = temporary.newFolder()
        val archive = AppUsageArchive(directory)
        archive.record(TimeRange(0, day), wifi, listOf(app().copy(isArchivedUsage = true)), day)
        assertTrue(archive.read(TimeRange(0, day), wifi).isEmpty())
        archive.record(TimeRange(0, day), wifi, listOf(app()), day)
        archive.clear()
        assertTrue(AppUsageArchive(directory).read(TimeRange(0, day), wifi).isEmpty())
    }

    @Test
    fun `adjacent days add download and upload without losing a missing later UID`() {
        val archive = AppUsageArchive(temporary.newFolder())
        archive.record(TimeRange(0, day), wifi, listOf(app(100)), day)
        archive.record(TimeRange(day, 2 * day), wifi, listOf(app(200)), 2 * day)
        archive.record(TimeRange(day, 2 * day), wifi, listOf(app(400).copy(uid = 10002)), 3 * day)
        val saved = archive.read(TimeRange(0, 2 * day), wifi).single { it.uid == 10001 }
        assertEquals(TrafficTotals(300, 20), saved.totals)
        assertEquals(2 * day, saved.recordedAt)
    }

    @Test
    fun `corrupt records are ignored without discarding valid observations`() {
        val directory = temporary.newFolder()
        AppUsageArchive(directory).record(TimeRange(0, day), wifi, listOf(app()), day)
        directory.listFiles()!!.single().appendText("\ninvalid record\n1|-1|bad|WIFI|0|1|1|9|0")
        assertEquals(TrafficTotals(100, 10), AppUsageArchive(directory).read(TimeRange(0, day), wifi).single().totals)
    }

    @Test
    fun `expired observations are removed from memory and disk`() {
        val directory = temporary.newFolder()
        val archive = AppUsageArchive(directory)
        archive.record(TimeRange(0, day), wifi, listOf(app()), day)
        archive.record(TimeRange(49 * day, 50 * day), wifi, listOf(app(200)), 50 * day)
        assertTrue(archive.read(TimeRange(0, day), wifi).isEmpty())
        assertTrue(AppUsageArchive(directory).read(TimeRange(0, day), wifi).isEmpty())
        assertEquals(1, directory.listFiles()!!.count { it.extension == "usage" })
    }

    @Test
    fun `archive stays bounded and keeps its newest observations after restart`() {
        val directory = temporary.newFolder()
        val archive = AppUsageArchive(directory)
        val hour = day / 24
        repeat(257) { index ->
            archive.record(TimeRange(index * hour, (index + 1) * hour), wifi, listOf(app()), (index + 1) * hour)
        }
        val reopened = AppUsageArchive(directory)
        assertEquals(256, directory.listFiles()!!.count { it.extension == "usage" })
        assertTrue(reopened.read(TimeRange(0, hour), wifi).isEmpty())
        assertEquals(100L, reopened.read(TimeRange(256 * hour, 257 * hour), wifi).single().totals.rxBytes)
    }
}
