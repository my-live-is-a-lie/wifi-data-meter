package com.mywifiadata.app.data

import com.mywifiadata.app.core.TimeRange
import com.mywifiadata.app.core.UsageNetwork
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.TrafficTotals
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64

/** Only observations read before uninstall are archived, never inferred shares of UID_REMOVED. */
class AppUsageArchive(private val directory: File) {
    data class SavedUsage(
        val uid: Int,
        val packageName: String,
        val totals: TrafficTotals,
        val recordedAt: Long,
    )

    private data class Observation(
        val uid: Int,
        val packageName: String,
        val network: UsageNetwork,
        val start: Long,
        val end: Long,
        val captured: Long,
        val totals: TrafficTotals,
    )

    private val slots = mutableMapOf<String, List<Observation>>()
    private var loaded = false

    @Synchronized
    fun record(range: TimeRange, network: UsageNetwork, apps: List<AppUsage>, capturedAt: Long) {
        if (range.startMillis < 0 || range.endMillis <= range.startMillis || range.endMillis > capturedAt) return
        load()
        prune(capturedAt)
        val key = slotKey(range, network)
        val records = slots[key].orEmpty().associateBy { it.uid to it.packageName }.toMutableMap()
        apps.filter { it.uid >= 0 && it.packageName != null && !it.isArchivedUsage && it.totals.totalBytes > 0 }.forEach { app ->
            val packageName = app.packageName ?: return@forEach
            val identity = app.uid to packageName
            val previous = records[identity]
            if (previous == null || (range.endMillis >= previous.end && capturedAt >= previous.captured)) {
                records[identity] = Observation(app.uid, packageName, network, range.startMillis, range.endMillis, capturedAt, app.totals)
            }
        }
        // Missing UIDs may have just been removed. Keep their original timestamps and totals.
        val updated = records.values.toList()
        if (updated.isEmpty() || updated == slots[key]) return
        slots[key] = updated
        runCatching { write(key, updated) }
        prune(capturedAt)
    }

    @Synchronized
    fun read(range: TimeRange, network: UsageNetwork): List<SavedUsage> {
        if (range.endMillis <= range.startMillis) return emptyList()
        load()
        return slots.values.flatten().filter {
            it.network == network && it.start >= range.startMillis && it.end <= range.endMillis
        }.groupBy { it.uid to it.packageName }.mapNotNull { (identity, observations) ->
            val best = nonOverlappingCoverage(observations)
            if (best.totals.totalBytes == 0L) null else SavedUsage(identity.first, identity.second, best.totals, best.captured)
        }
    }

    /** Restore only an absent UID whose cached owner is now uninstalled and still matches. */
    fun restoreMissing(
        systemUids: Set<Int>,
        range: TimeRange,
        network: UsageNetwork,
        resolve: (Int, TrafficTotals) -> AppUsage,
    ): List<AppUsage> = read(range, network).mapNotNull { saved ->
        if (saved.uid in systemUids) return@mapNotNull null
        val identity = resolve(saved.uid, saved.totals)
        if (identity.isInstalled || identity.packageName != saved.packageName) return@mapNotNull null
        identity.copy(isArchivedUsage = true, usageRecordedAt = saved.recordedAt)
    }

    @Synchronized
    fun hasCompleteDay(range: TimeRange, network: UsageNetwork): Boolean {
        load()
        return slots[slotKey(range, network)].orEmpty().any {
            it.network == network && it.start == range.startMillis && it.end == range.endMillis
        }
    }

    @Synchronized
    fun clear() {
        directory.deleteRecursively()
        slots.clear()
        loaded = true
    }

    private data class Coverage(val millis: Long = 0, val captured: Long = 0, val totals: TrafficTotals = TrafficTotals.Empty)

    /** Choose the greatest observed coverage without adding overlapping day/week/month queries twice. */
    private fun nonOverlappingCoverage(observations: List<Observation>): Coverage {
        val ordered = observations.sortedWith(compareBy<Observation> { it.end }.thenBy { it.start }.thenBy { it.captured })
        val best = MutableList(ordered.size + 1) { Coverage() }
        ordered.forEachIndexed { index, observation ->
            var previous = index - 1
            while (previous >= 0 && ordered[previous].end > observation.start) previous--
            val base = best[previous + 1]
            val included = Coverage(
                millis = base.millis + observation.end - observation.start,
                captured = maxOf(base.captured, observation.captured),
                totals = base.totals + observation.totals,
            )
            val excluded = best[index]
            best[index + 1] = if (included.millis > excluded.millis ||
                (included.millis == excluded.millis && included.captured >= excluded.captured)) included else excluded
        }
        return best.last()
    }

    private fun slotKey(range: TimeRange, network: UsageNetwork): String {
        val duration = range.endMillis - range.startMillis
        val suffix = when {
            duration <= HOUR -> "hour"
            duration <= 26 * HOUR -> "day" // DST days may contain 23 or 25 hours.
            else -> "range-${range.endMillis / DAY}"
        }
        return "${network.name}-${range.startMillis}-$suffix"
    }

    private fun load() {
        if (loaded) return
        directory.listFiles()?.filter { it.extension == "usage" && it.length() <= MAX_FILE_BYTES }?.forEach { file ->
            val records = runCatching { file.readLines().mapNotNull(::decode) }.getOrDefault(emptyList())
            if (records.isNotEmpty()) slots[file.nameWithoutExtension] = records
        }
        loaded = true
    }

    private fun prune(now: Long) {
        val expired = slots.filterValues { values -> values.all { it.end < now - RETENTION } }.keys.toSet()
        val overflow = (slots.keys - expired).sortedByDescending { key -> slots.getValue(key).maxOf { it.captured } }.drop(MAX_FILES)
        (expired + overflow).forEach { key ->
            slots.remove(key)
            File(directory, "$key.usage").delete()
        }
    }

    private fun write(key: String, records: List<Observation>) {
        check(directory.isDirectory || directory.mkdirs())
        val target = File(directory, "$key.usage")
        val temp = File.createTempFile("usage-", ".tmp", directory)
        try {
            temp.writeText(records.joinToString("\n") {
                val pkg = Base64.getEncoder().encodeToString(it.packageName.toByteArray(Charsets.UTF_8))
                listOf("1", it.uid, pkg, it.network.name, it.start, it.end, it.captured, it.totals.rxBytes, it.totals.txBytes).joinToString("|")
            })
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            temp.delete()
        }
    }

    private fun decode(line: String): Observation? = runCatching {
        val fields = line.split('|')
        require(fields.size == 9 && fields[0] == "1")
        val value = Observation(
            uid = fields[1].toInt(),
            packageName = String(Base64.getDecoder().decode(fields[2]), Charsets.UTF_8),
            network = UsageNetwork.valueOf(fields[3]),
            start = fields[4].toLong(), end = fields[5].toLong(), captured = fields[6].toLong(),
            totals = TrafficTotals(fields[7].toLong(), fields[8].toLong()),
        )
        require(value.uid >= 0 && value.packageName.isNotBlank() && value.start >= 0 && value.end > value.start && value.captured >= value.end)
        require(value.totals.rxBytes >= 0 && value.totals.txBytes >= 0 && value.totals.totalBytes > 0)
        value
    }.getOrNull()

    companion object {
        private const val HOUR = 3_600_000L
        private const val DAY = 24 * HOUR
        private const val RETENTION = 45 * DAY
        private const val MAX_FILES = 256
        private const val MAX_FILE_BYTES = 4 * 1024 * 1024L
    }
}
