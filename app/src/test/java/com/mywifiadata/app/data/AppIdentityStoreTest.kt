package com.mywifiadata.app.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AppIdentityStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun record(uid: Int = 10001, name: String = "com.example.app") =
        CachedAppIdentity(uid, name, "تطبيق | Example", false, true, 123L)

    @Test
    fun `metadata and icon survive store recreation and uninstall`() {
        val directory = temporary.newFolder()
        val first = AppIdentityStore(directory)
        val app = record()
        val icon = byteArrayOf(1, 2, 3, 4)
        first.put(app)
        first.writeIcon(app, icon)
        first.markRemoved(app.packageName)
        val reopened = AppIdentityStore(directory)
        assertEquals(app.copy(isInstalled = false), reopened.get(app.uid))
        assertArrayEquals(icon, reopened.iconFile(app).readBytes())
        assertEquals(1, reopened.all().size)
    }

    @Test
    fun `same uid reinstallation restores installed status without losing icon`() {
        val store = AppIdentityStore(temporary.newFolder())
        val app = record()
        store.put(app)
        store.writeIcon(app, byteArrayOf(1, 2))
        store.markRemoved(app.packageName)
        val reinstalled = app.copy(lastUpdateTime = 456L)
        store.put(reinstalled)
        assertEquals(reinstalled, store.get(app.uid))
        assertTrue(store.iconFile(reinstalled).isFile)
    }

    @Test
    fun `different package reusing a uid never gets old identity or icon`() {
        val directory = temporary.newFolder()
        val store = AppIdentityStore(directory)
        val old = record()
        store.put(old)
        store.writeIcon(old, byteArrayOf(9, 9))
        store.markRemoved(old.packageName)
        val new = record(name = "com.example.other").copy(label = "Other")
        store.put(new)
        assertEquals(new, AppIdentityStore(directory).get(old.uid))
        assertNotEquals(store.iconFile(old), store.iconFile(new))
        assertFalse(store.iconFile(old).exists())
        assertFalse(store.iconFile(new).exists())
    }

    @Test
    fun `reinstallation under a new uid retains the old uninstalled record`() {
        val store = AppIdentityStore(temporary.newFolder())
        val old = record()
        store.put(old)
        store.markRemoved(old.packageName)
        val new = old.copy(uid = 10002, lastUpdateTime = 456L)
        store.put(new)
        assertEquals(old.copy(isInstalled = false), store.get(old.uid))
        assertEquals(new, store.get(new.uid))
    }

    @Test
    fun `removal is scoped to the matching package`() {
        val store = AppIdentityStore(temporary.newFolder())
        store.put(record())
        val other = record(10002, "com.example.other")
        store.put(other)
        store.markRemoved("com.example.app")
        assertEquals(other, store.get(other.uid))
        store.markRemoved("absent.package")
        assertEquals(2, store.all().size)
    }

    @Test
    fun `codec preserves unicode and delimiters and rejects corrupt records`() {
        val app = record()
        assertEquals(app, AppIdentityStore.decode(AppIdentityStore.encode(app)))
        assertNull(AppIdentityStore.decode("broken"))
        assertNull(AppIdentityStore.decode(AppIdentityStore.encode(app).replace("10001", "-1")))
        assertNull(AppIdentityStore.decode(AppIdentityStore.encode(app).replace("false", "maybe")))
    }

    @Test
    fun `corrupt files filename mismatches and temp files are ignored`() {
        val directory = temporary.newFolder()
        File(directory, "10001.identity").writeText("broken")
        File(directory, "10002.identity").writeText(AppIdentityStore.encode(record()))
        File(directory, "capture.tmp").writeText(AppIdentityStore.encode(record()))
        val store = AppIdentityStore(directory)
        assertTrue(store.all().isEmpty())
        store.put(record())
        assertEquals(record(), AppIdentityStore(directory).get(10001))
    }

    @Test
    fun `icons stay inside private directory and no temporary writes remain`() {
        val directory = temporary.newFolder()
        val store = AppIdentityStore(directory)
        val unusual = record(name = "../../outside")
        store.put(unusual)
        store.writeIcon(unusual, byteArrayOf(1))
        assertEquals(directory.canonicalFile, store.iconFile(unusual).canonicalFile.parentFile)
        assertFalse(directory.listFiles().orEmpty().any { it.extension == "tmp" })
    }

    @Test
    fun `clear removes metadata and icons including after reopening`() {
        val directory = temporary.newFolder()
        val store = AppIdentityStore(directory)
        store.put(record())
        store.writeIcon(record(), byteArrayOf(1))
        store.clear()
        assertTrue(store.all().isEmpty())
        assertTrue(AppIdentityStore(directory).all().isEmpty())
        assertFalse(store.iconFile(record()).exists())
        store.put(record())
        assertEquals(record(), AppIdentityStore(directory).get(10001))
    }
}
