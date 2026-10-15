package com.mywifiadata.app.data

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Base64

/** One identity per Android UID. A live new owner replaces an old owner on UID reuse. */
data class CachedAppIdentity(
    val uid: Int,
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val isInstalled: Boolean,
    val lastUpdateTime: Long,
) {
    val iconFileName: String
        get() {
            val hash = MessageDigest.getInstance("SHA-256").digest(packageName.toByteArray(Charsets.UTF_8))
                .take(16).joinToString("") { "%02x".format(it.toInt() and 0xff) }
            return "$uid-$hash.png"
        }
}

/** Private persistent files, not Android's evictable cache. All callers use an IO thread. */
class AppIdentityStore(private val directory: File) {
    private val records = mutableMapOf<Int, CachedAppIdentity>()
    private var loaded = false

    @Synchronized
    fun get(uid: Int): CachedAppIdentity? {
        load()
        return records[uid]
    }

    @Synchronized
    fun all(): List<CachedAppIdentity> {
        load()
        return records.values.toList()
    }

    @Synchronized
    fun put(record: CachedAppIdentity) {
        require(record.uid >= 0 && record.packageName.isNotBlank() && record.label.isNotBlank())
        load()
        val old = records[record.uid]
        if (old == record) return
        // Do not let storage failures prevent usage from being shown for the current session.
        runCatching { writeAtomically(File(directory, "${record.uid}.identity"), encode(record).toByteArray(Charsets.UTF_8)) }
        records[record.uid] = record
        if (old != null && old.iconFileName != record.iconFileName) {
            File(directory, old.iconFileName).delete()
        }
    }

    @Synchronized
    fun markRemoved(packageName: String) {
        all().filter { it.packageName == packageName }.forEach { put(it.copy(isInstalled = false)) }
    }

    @Synchronized
    fun iconFile(record: CachedAppIdentity): File = File(directory, record.iconFileName)

    @Synchronized
    fun writeIcon(record: CachedAppIdentity, png: ByteArray) {
        writeAtomically(iconFile(record), png)
    }

    @Synchronized
    fun clear() {
        directory.deleteRecursively()
        records.clear()
        loaded = true
    }

    private fun load() {
        if (loaded) return
        directory.listFiles()?.filter { it.extension == "identity" }?.forEach { file ->
            val record = runCatching { decode(file.readText()) }.getOrNull()
            if (record != null && file.nameWithoutExtension == record.uid.toString()) records[record.uid] = record
        }
        loaded = true
    }

    private fun writeAtomically(file: File, bytes: ByteArray) {
        check(directory.isDirectory || directory.mkdirs())
        val temp = File.createTempFile("identity-", ".tmp", directory)
        try {
            temp.writeBytes(bytes)
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            temp.delete()
        }
    }

    internal companion object {
        private fun encodeText(text: String) = Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
        private fun decodeText(text: String) = String(Base64.getDecoder().decode(text), Charsets.UTF_8)

        fun encode(record: CachedAppIdentity): String = listOf(
            "1", record.uid.toString(), encodeText(record.packageName), encodeText(record.label),
            record.isSystem.toString(), record.isInstalled.toString(), record.lastUpdateTime.toString(),
        ).joinToString("|")

        fun decode(text: String): CachedAppIdentity? = runCatching {
            val fields = text.split('|')
            require(fields.size == 7 && fields[0] == "1")
            CachedAppIdentity(
                uid = fields[1].toInt().also { require(it >= 0) },
                packageName = decodeText(fields[2]).also { require(it.isNotBlank()) },
                label = decodeText(fields[3]).also { require(it.isNotBlank()) },
                isSystem = fields[4].toBooleanStrict(),
                isInstalled = fields[5].toBooleanStrict(),
                lastUpdateTime = fields[6].toLong().also { require(it >= 0) },
            )
        }.getOrNull()
    }
}
