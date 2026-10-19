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

/**
 * أرشيف استهلاك التطبيقات اليومي، ويُستخدم لاستعادة تطبيق أُزيل قبل قراءة
 * استهلاكه. Only observations read before uninstall are archived, never
 * inferred shares of UID_REMOVED.
 *
 * تُقرأ ملفات الأرشيف عند الحاجة فقط بدل تحميلها كلها، فلا يكبر استهلاك
 * الذاكرة عند اختيار مدة احتفاظ طويلة (حتى ثلاث سنوات أو لا محدود). وتمسح
 * [prune] ما تجاوز المدة التي يختارها المستخدم في الإعدادات.
 */
class AppUsageArchive(
    private val directory: File,
    /** مدة الاحتفاظ بالمللي ثانية، وصفر يعني «لا محدود». */
    private val retentionMillis: () -> Long = { DEFAULT_RETENTION_MILLIS },
) {
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

    /**
     * ذاكرة مؤقتة للفترات التي قُرئت مؤخرًا فقط. تُفرَّغ عند تجاوز السقف
     * فتُقرأ الملفات من جديد عند الحاجة بدل أن تكبر الذاكرة بلا حد.
     */
    private val slots = mutableMapOf<String, List<Observation>>()

    @Synchronized
    fun record(range: TimeRange, network: UsageNetwork, apps: List<AppUsage>, capturedAt: Long) {
        if (range.startMillis < 0 || range.endMillis <= range.startMillis || range.endMillis > capturedAt) return
        val key = slotKey(range, network)
        val current = readSlot(key)
        val records = current.associateBy { it.uid to it.packageName }.toMutableMap()
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
        if (updated.isEmpty() || updated == current) return
        slots[key] = updated
        runCatching { write(key, updated) }
        prune(capturedAt)
    }

    @Synchronized
    fun read(range: TimeRange, network: UsageNetwork): List<SavedUsage> {
        if (range.endMillis <= range.startMillis) return emptyList()
        return candidateKeys(range, network)
            .flatMap { readSlot(it) }
            .filter {
                it.network == network &&
                    it.start >= range.startMillis &&
                    it.end <= range.endMillis
            }
            .groupBy { it.uid to it.packageName }
            .mapNotNull { (identity, observations) ->
                val best = nonOverlappingCoverage(observations)
                if (best.totals.totalBytes == 0L) {
                    null
                } else {
                    SavedUsage(identity.first, identity.second, best.totals, best.captured)
                }
            }
    }

    /** Restore only an absent UID whose cached owner is now uninstalled and still matches. */
    @Synchronized
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
    fun hasCompleteDay(range: TimeRange, network: UsageNetwork): Boolean =
        readSlot(slotKey(range, network)).any {
            it.network == network && it.start == range.startMillis && it.end == range.endMillis
        }

    /** يطبّق مدة الاحتفاظ الحالية فورًا، مثلًا بعد تقليلها من الإعدادات. */
    @Synchronized
    fun pruneNow(now: Long = System.currentTimeMillis()) {
        prune(now)
    }

    @Synchronized
    fun clear() {
        directory.deleteRecursively()
        slots.clear()
    }

    /** يقرأ فترة واحدة من الملف عند أول طلب فقط ثم يحتفظ بها في الذاكرة. */
    private fun readSlot(key: String): List<Observation> {
        slots[key]?.let { return it }
        val file = File(directory, "$key.usage")
        if (!file.isFile || file.length() > MAX_FILE_BYTES) return emptyList()
        val records = runCatching { file.readLines().mapNotNull(::decode) }.getOrDefault(emptyList())
        if (records.isEmpty()) return emptyList()
        if (slots.size >= MAX_CACHED_SLOTS) slots.clear()
        slots[key] = records
        return records
    }

    /**
     * مفاتيح الفترات التي قد تحتوي قراءات داخل [range]: من أسماء الملفات
     * (بدل تحميلها) ومن الذاكرة المؤقتة للقراءات التي لم تُكتب بعد.
     */
    private fun candidateKeys(range: TimeRange, network: UsageNetwork): Set<String> {
        val prefix = network.name + "-"
        val keys = linkedSetOf<String>()
        directory.listFiles()?.forEach { file ->
            if (!file.isFile || file.extension != "usage") return@forEach
            val key = file.nameWithoutExtension
            if (key.startsWith(prefix) && keyStart(key) in range.startMillis until range.endMillis) {
                keys += key
            }
        }
        slots.keys.forEach { key ->
            if (key.startsWith(prefix) && keyStart(key) in range.startMillis until range.endMillis) {
                keys += key
            }
        }
        return keys
    }

    private fun keyStart(key: String): Long {
        val start = key.split('-', limit = 3).getOrNull(1)?.toLongOrNull() ?: return -1L
        return if (start >= 0L) start else -1L
    }

    /**
     * أحدث وقت ممكن لقراءة داخل الفترة المسمّاة بالمفتاح؛ يُستخدم للحذف
     * بالعمر من اسم الملف دون تحميل محتواه.
     */
    private fun slotEnd(key: String): Long {
        val parts = key.split('-')
        val start = parts.getOrNull(1)?.toLongOrNull() ?: return Long.MIN_VALUE
        val suffix = parts.drop(2).joinToString("-")
        return when {
            suffix == "hour" -> start + HOUR
            suffix == "day" -> start + 26 * HOUR
            suffix.startsWith("range-") -> {
                val dayIndex = suffix.removePrefix("range-").toLongOrNull() ?: return start + 26 * HOUR
                (dayIndex + 1) * DAY
            }
            else -> start + 26 * HOUR
        }
    }

    /** سقف الملفات يتّسع للمدة المختارة، مع حد أمان أعلى عند «لا محدود». */
    private fun maxFilesFor(retention: Long): Int {
        if (retention <= 0L) return MAX_FILES_UNLIMITED
        val days = retention / DAY
        val needed = (days * 2L + 16L).coerceAtMost(MAX_FILES_UNLIMITED.toLong())
        return maxOf(DEFAULT_MAX_FILES, needed.toInt())
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

    /**
     * يحذف الفترات الأقدم من مدة الاحتفاظ ويبقي عدد الملفات داخل السقف.
     * يعمل بالاسم وتاريخ آخر كتابة، فلا يحمّل كل الأرشيف إلى الذاكرة.
     */
    private fun prune(now: Long) {
        val retention = retentionMillis().coerceAtLeast(0L)
        val cutoff = if (retention > 0L) now - retention else Long.MIN_VALUE
        val files = directory.listFiles()?.filter { it.isFile && it.extension == "usage" }.orEmpty()

        // الحذف بالعمر: من اسم الملف وحده.
        val expired = mutableSetOf<String>()
        files.forEach { file ->
            val key = file.nameWithoutExtension
            if (slotEnd(key) < cutoff) {
                expired += key
                runCatching { file.delete() }
            }
        }
        slots.keys.filter { slotEnd(it) < cutoff }.forEach { expired += it }
        expired.forEach { slots.remove(it) }

        // حد أمان للعدد حتى مع «لا محدود»، مع إبقاء الأحدث.
        val limit = maxFilesFor(retention)
        val remaining = directory.listFiles()?.filter { it.isFile && it.extension == "usage" }.orEmpty()
        if (remaining.size <= limit) return
        val kept = remaining
            .sortedWith(
                compareByDescending<File> { it.lastModified() }
                    .thenByDescending { keyStart(it.nameWithoutExtension) },
            )
            .take(limit)
            .mapTo(mutableSetOf()) { it.nameWithoutExtension }
        remaining.filter { it.nameWithoutExtension !in kept }.forEach { file ->
            slots.remove(file.nameWithoutExtension)
            runCatching { file.delete() }
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
        private const val DEFAULT_RETENTION_MILLIS = 30 * DAY
        private const val DEFAULT_MAX_FILES = 256
        private const val MAX_FILES_UNLIMITED = 8192
        private const val MAX_CACHED_SLOTS = 128
        private const val MAX_FILE_BYTES = 4 * 1024 * 1024L
    }
}
