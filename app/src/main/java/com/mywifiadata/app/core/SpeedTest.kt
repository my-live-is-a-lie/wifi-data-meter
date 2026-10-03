package com.mywifiadata.app.core

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.coroutineContext

/**
 * قياس سرعة الإنترنت بالخوادم خلف موقع fast.com (Netflix Open Connect) كمصدر
 * أساسي، مع **معايرة متوازية** لعدد من المصادر البديلة (Google، jsDelivr، cdnjs،
 * Cloudflare، Hetzner، Tele2، OVH، ThinkBroadband) وأخذ أسرعها إن كان fast.com
 * محجوبًا أو متوقفًا على الشبكة الحالية.
 *
 * ضمانات السلامة المطلوبة:
 * - **رسالة عند انقطاع الاتصال**: مرصد يفحص الشبكة كل نصف ثانية، وعند فقدانها
 *   يتوقف كل شيء فورًا ويُفصل كل اتصال.
 * - **رسالة عند مرور وقت طويل دون نتيجة**: إن توقف وصول البيانات خلال القياس
 *   [Config.stallTimeoutMillis]، أو تجاوز الاختبار [Config.overallTimeoutMillis]
 *   يُغلق التحميل ويُعاد سبب الفشل للواجهة.
 * - **إغلاق التحميل دائمًا**: كل اتصال يُفصل في `finally`، وإلغاء الـCoroutine
 *   (زر الإيقاف) يوقف القراءة/الكتابة فورًا.
 * - **تفاصيل الفشل**: تُسجَّل نتيجة كل مصدر لتُعرض في الواجهة عند تعذّر الاختبار.
 */
object SpeedTestEngine {

    /** مراحل الاختبار كما تُعرض في الواجهة. */
    enum class Phase { PREPARING, DOWNLOAD, UPLOAD, FINISHED }

    /** أسباب فشل الاختبار (تُترجم إلى رسائل عربية في الواجهة). */
    enum class Failure { NO_CONNECTION, CONNECTION_LOST, STALLED, TIMEOUT, ERROR }

    /** فشل اختبار السرعة مصحوبًا بسببه وتفاصيل المحاولات. */
    class SpeedTestException(
        val failure: Failure,
        message: String,
        val detail: String? = null,
        cause: Throwable? = null,
    ) : IOException(message, cause)

    /** تقدّم لحظي أثناء الاختبار (لعرض السرعة الجارية). */
    data class Progress(
        val phase: Phase,
        val elapsedMillis: Long,
        val bytesTransferred: Long,
        val bitsPerSecond: Double,
    )

    /** نتيجة الاختبار: قيمتان بالبت/ثانية + المصدر المستخدم. */
    data class Result(
        val downloadBitsPerSecond: Double,
        val uploadBitsPerSecond: Double,
        val usedFastCom: Boolean,
        val durationMillis: Long,
        /** false إن تعذّر قياس الرفع (تُعرض شرطة بدل صفر). */
        val uploadMeasured: Boolean = true,
        /** معرّف المصدر الذي قِيس عليه التنزيل. */
        val sourceLabel: String? = null,
    )

    /** إعدادات الاختبار والمراصد الزمنية. */
    data class Config(
        val overallTimeoutMillis: Long = SpeedTestWatchdog.DEFAULT_OVERALL_TIMEOUT_MILLIS,
        val stallTimeoutMillis: Long = SpeedTestWatchdog.DEFAULT_STALL_TIMEOUT_MILLIS,
        /** مدة معايرة المصادر المتوازية. */
        val probeMillis: Long = 4_500L,
        /** عدد البايتات الذي يعتبر المصدر ناجحًا عند بلوغه أثناء المعايرة. */
        val probeMinBytes: Long = 400_000L,
        /** مهلة إضافية نمنحها لمصدر fast.com قبل اختيار مصدر آخر. */
        val preferredGraceMillis: Long = 1_500L,
        /** نافذة القياس الفعلية. */
        val measureMillis: Long = 8_000L,
        val uploadMillis: Long = 6_000L,
        val probeConnections: Int = 1,
        val streams: Int = 5,
        val connectTimeoutMillis: Int = 8_000,
        val readTimeoutMillis: Int = 4_000,
        val metadataReadTimeoutMillis: Int = 12_000,
        val reportIntervalMillis: Long = 300L,
    )

    /** مصدر تنزيل: معرّف + دالة تُرجع روابطه (بعضها يُجلب من واجهة خارجية). */
    internal data class DownloadSource(
        val id: String,
        val urls: suspend (Config) -> List<String>,
        val headers: Map<String, String> = emptyMap(),
    )

    /** مصدر رفع: عنوان خام أو multipart. */
    internal data class UploadSource(
        val id: String,
        val url: String,
        val multipart: Boolean = false,
    )

    private const val FAST_COM_ID = "fast.com"
    private const val FAST_HOME = "https://fast.com/"
    private const val REFERER = "https://fast.com/"
    private const val ORIGIN = "https://fast.com"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private const val WATCH_INTERVAL_MILLIS = 500L
    private val TOKEN_REGEX = Regex("""token\s*[:=]\s*["']([^"']+)["']""")
    private val SCRIPT_REGEX = Regex("""src=["']([^"']*app-[^"']+\.js)["']""")

    /**
     * مصادر التنزيل بالترتيب: fast.com أولًا (المصدر المفضّل)، ثم خوادم عامة
     * متنوعة على شبكات توصيل مختلفة لتجاوز الحجب الجزئي.
     */
    internal val DOWNLOAD_SOURCES: List<DownloadSource> = listOf(
        DownloadSource(
            id = FAST_COM_ID,
            headers = mapOf("Referer" to REFERER, "Origin" to ORIGIN),
            urls = { config -> fastComUrls(config) },
        ),
        DownloadSource(
            id = "Google",
            urls = {
                listOf(
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                )
            },
        ),
        DownloadSource(
            id = "jsDelivr",
            urls = {
                listOf(
                    "https://cdn.jsdelivr.net/npm/monaco-editor@0.45.0/min/vs/editor/editor.main.js",
                )
            },
        ),
        DownloadSource(
            id = "cdnjs",
            urls = {
                listOf(
                    "https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.45.0/min/vs/editor/editor.main.js",
                )
            },
        ),
        DownloadSource(
            id = "Cloudflare",
            urls = { listOf("https://speed.cloudflare.com/__down?bytes=100000000") },
        ),
        DownloadSource(
            id = "Hetzner-DE",
            urls = { listOf("https://speed.hetzner.de/100MB.bin") },
        ),
        DownloadSource(
            id = "Hetzner-US",
            urls = { listOf("https://ash-speed.hetzner.com/100MB.bin") },
        ),
        DownloadSource(
            id = "Tele2",
            urls = { listOf("https://speedtest.tele2.net/100MB.zip") },
        ),
        DownloadSource(
            id = "OVH",
            urls = { listOf("https://proof.ovh.net/files/100Mb.dat") },
        ),
        DownloadSource(
            id = "ThinkBroadband",
            urls = { listOf("https://ipv4.download.thinkbroadband.com/50MB.zip") },
        ),
    )

    /** مصادر الرفع بالترتيب (fast.com لا يوفّر واجهة رفع عامة موثوقة). */
    internal val UPLOAD_SOURCES: List<UploadSource> = listOf(
        UploadSource(id = "Cloudflare", url = "https://speed.cloudflare.com/__up"),
        UploadSource(id = "Tele2", url = "https://speedtest.tele2.net/upload.php", multipart = true),
    )

    /**
     * تنفيذ الاختبار كاملًا (تنزيل ثم رفع).
     *
     * @param isOnline فحص حيّ للاتصال يستدعيه المرصد كل نصف ثانية.
     * @param onProgress تُنادى أثناء العمل من خيوط الخلفية لتحديث الواجهة.
     */
    suspend fun run(
        config: Config = Config(),
        isOnline: () -> Boolean = { true },
        onProgress: (Progress) -> Unit = {},
    ): Result = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        if (!isOnlineSafely(isOnline)) {
            throw SpeedTestException(Failure.NO_CONNECTION, "no connectivity before start")
        }

        val watchdog = SpeedTestWatchdog(
            startedAtMillis = startedAt,
            overallTimeoutMillis = config.overallTimeoutMillis,
            stallTimeoutMillis = config.stallTimeoutMillis,
        )
        val tracker = Tracker(startedAt, config, watchdog, onProgress)
        val log = AttemptLog()

        coroutineScope {
            val stallArmed = AtomicBoolean(false)
            val watcher = launch {
                while (true) {
                    delay(WATCH_INTERVAL_MILLIS)
                    val now = System.currentTimeMillis()
                    val failure = when {
                        !isOnlineSafely(isOnline) -> Failure.CONNECTION_LOST
                        stallArmed.get() &&
                            now - watchdog.lastProgressMillis >= config.stallTimeoutMillis ->
                            Failure.STALLED

                        now - startedAt >= config.overallTimeoutMillis -> Failure.TIMEOUT
                        else -> null
                    }
                    if (failure != null) {
                        throw SpeedTestException(failure, "watchdog: $failure", log.summary())
                    }
                }
            }

            try {
                val download = download(config, tracker, stallArmed, log)
                val upload = upload(config, tracker, stallArmed, log)
                tracker.finish()
                Result(
                    downloadBitsPerSecond = download.rate,
                    uploadBitsPerSecond = upload,
                    usedFastCom = download.sourceId == FAST_COM_ID,
                    durationMillis = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L),
                    uploadMeasured = upload > 0.0,
                    sourceLabel = download.sourceId,
                )
            } finally {
                watcher.cancel()
            }
        }
    }

    /** نتيجة التنزيل: السرعة المقيسة ومعرّف المصدر الذي قِيس عليه. */
    private data class DownloadOutcome(val rate: Double, val sourceId: String)

    /**
     * التنزيل: فكّ روابط كل المصادر بالتوازي، ثم معايرة متوازية قصيرة لاختيار
     * أسرع مصدر عامل، ثم قياس فعلي عليه بنافذة كاملة.
     */
    private suspend fun download(
        config: Config,
        tracker: Tracker,
        stallArmed: AtomicBoolean,
        log: AttemptLog,
    ): DownloadOutcome {
        tracker.startPhase(Phase.PREPARING)

        // 1) فكّ الروابط (fast.com يحتاج جلب رمز من الواجهة).
        val resolved = ConcurrentHashMap<String, List<String>>()
        coroutineScope {
            DOWNLOAD_SOURCES.forEach { source ->
                launch(Dispatchers.IO) {
                    try {
                        val urls = source.urls(config)
                        if (urls.isEmpty()) log.record(source.id, REASON_NO_URLS) else resolved[source.id] = urls
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        log.record(source.id, reasonOf(e))
                    }
                }
            }
        }
        if (resolved.isEmpty()) {
            throw SpeedTestException(Failure.ERROR, "no download source", log.summary())
        }

        // 2) معايرة متوازية: من يصل أولًا (والأسرع) يحدَّد مصدر القياس.
        val order = DOWNLOAD_SOURCES.map { it.id }.filter { resolved.containsKey(it) }
        val probed = probe(resolved, order, config, tracker, log)

        val winner = SourceSelector.pick(
            order = order,
            bytes = probed,
            minBytes = config.probeMinBytes,
            preferredId = FAST_COM_ID.takeIf { resolved.containsKey(it) },
        ) ?: throw SpeedTestException(Failure.ERROR, "no reachable source", log.summary())

        // 3) القياس الفعلي على المصدر الفائز.
        val source = DOWNLOAD_SOURCES.first { it.id == winner }
        val urls = resolved.getValue(winner)
        stallArmed.set(true)
        return try {
            val rate = measure(source, urls, config, tracker, log)
            DownloadOutcome(rate = rate, sourceId = winner)
        } finally {
            stallArmed.set(false)
        }
    }

    /** معايرة كل المصادر في وقت واحد؛ تُلغى البقية فور تحديد الفائز. */
    private suspend fun probe(
        resolved: Map<String, List<String>>,
        order: List<String>,
        config: Config,
        tracker: Tracker,
        log: AttemptLog,
    ): Map<String, Long> {
        val bytes = ConcurrentHashMap<String, Long>()
        val probeStartedAt = System.currentTimeMillis()

        coroutineScope {
            val jobs = order.map { id ->
                launch(Dispatchers.IO) {
                    val source = DOWNLOAD_SOURCES.first { it.id == id }
                    val counter = AtomicLong(0L)
                    try {
                        val end = System.currentTimeMillis() + config.probeMillis
                        readStreams(
                            urls = resolved.getValue(id),
                            connections = config.probeConnections,
                            deadline = end,
                            config = config,
                            headers = source.headers,
                            onBytes = { count -> counter.addAndGet(count); tracker.addBytes(count) },
                        )
                        if (counter.get() <= 0L) log.record(id, REASON_NO_BYTES)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        log.record(id, reasonOf(e))
                    } finally {
                        bytes[id] = counter.get()
                    }
                }
            }

            val limit = probeStartedAt + config.probeMillis + config.preferredGraceMillis
            while (System.currentTimeMillis() < limit) {
                delay(PROBE_POLL_MILLIS)
                val preferred = FAST_COM_ID.takeIf { resolved.containsKey(it) }
                val preferredReady = preferred != null &&
                    (bytes[preferred] ?: 0L) >= config.probeMinBytes
                val otherReady = SourceSelector.anyOtherReached(
                    order = order,
                    bytes = bytes,
                    minBytes = config.probeMinBytes,
                    preferredId = preferred,
                )
                // نتوقف فور جهوزية fast.com، أو بعد مهلة السماح إن جهز مصدر آخر.
                val elapsed = System.currentTimeMillis() - probeStartedAt
                if (preferredReady || (otherReady && elapsed >= config.preferredGraceMillis)) break
            }

            jobs.forEach { it.cancel() }
        }

        return bytes.toMap()
    }

    /** قياس فعلي على مصدر واحد بعدة اتصالات متوازية. */
    private suspend fun measure(
        source: DownloadSource,
        urls: List<String>,
        config: Config,
        tracker: Tracker,
        log: AttemptLog,
    ): Double {
        tracker.startPhase(Phase.DOWNLOAD)
        val end = System.currentTimeMillis() + config.measureMillis
        readStreams(
            urls = urls,
            connections = config.streams,
            deadline = end,
            config = config,
            headers = source.headers,
            onBytes = { count -> tracker.addBytes(count) },
        )
        if (tracker.phaseByteCount <= 0L) {
            log.record(source.id, REASON_NO_BYTES)
            throw SpeedTestException(Failure.ERROR, "${source.id}: no bytes received", log.summary())
        }
        return tracker.currentBitsPerSecond()
    }

    /**
     * يقرأ [connections] اتصالات متوازية من [urls] حتى [deadline]، ويبلّغ
     * [onBytes] بكل قراءة. كل اتصال يُفصل في `finally` ولا يُوقف خطأ اتصال
     * واحد بقية الاتصالات.
     */
    private suspend fun readStreams(
        urls: List<String>,
        connections: Int,
        deadline: Long,
        config: Config,
        headers: Map<String, String>,
        onBytes: (Long) -> Unit,
    ) {
        if (urls.isEmpty() || connections <= 0) return
        coroutineScope {
            repeat(connections) { index ->
                launch(Dispatchers.IO) {
                    val url = urls[index % urls.size]
                    while (System.currentTimeMillis() < deadline) {
                        coroutineContext.ensureActive()
                        val conn = open(url, config, headers)
                        try {
                            val code = conn.responseCode
                            if (code != 200 && code != 206) throw IOException("HTTP $code")
                            conn.inputStream.use { input ->
                                val buffer = ByteArray(64 * 1024)
                                while (System.currentTimeMillis() < deadline) {
                                    coroutineContext.ensureActive()
                                    var read = 0
                                    var timedOut = false
                                    try {
                                        read = input.read(buffer)
                                    } catch (e: SocketTimeoutException) {
                                        // لا بيانات الآن: المرصد يقرر، وإن كان كل شيء سليمًا نتابع.
                                        timedOut = true
                                    }
                                    if (timedOut) continue
                                    if (read <= 0) break
                                    onBytes(read.toLong())
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: IOException) {
                            // فشل هذا الاتصال وحده ولا يوقف بقية الاتصالات.
                            if (System.currentTimeMillis() >= deadline) break
                        } finally {
                            conn.disconnect()
                        }
                    }
                }
            }
        }
    }

    /** الرفع: المحاولة على مصادر الرفع بالترتيب حتى ينجح أحدها. */
    private suspend fun upload(
        config: Config,
        tracker: Tracker,
        stallArmed: AtomicBoolean,
        log: AttemptLog,
    ): Double {
        UPLOAD_SOURCES.forEach { source ->
            tracker.startPhase(Phase.UPLOAD)
            stallArmed.set(true)
            try {
                sendUpload(source, config, tracker)
                if (tracker.phaseByteCount > 0L) return tracker.currentBitsPerSecond()
                log.record(source.id, REASON_NO_BYTES)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                log.record(source.id, reasonOf(e))
            } finally {
                stallArmed.set(false)
            }
        }
        return 0.0
    }

    /** إرسال بيانات الرفع حتى انتهاء المدة (خام أو multipart). */
    private suspend fun sendUpload(source: UploadSource, config: Config, tracker: Tracker) {
        val deadline = System.currentTimeMillis() + config.uploadMillis
        val conn = URL(source.url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = config.connectTimeoutMillis
            conn.readTimeout = config.metadataReadTimeoutMillis
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept-Encoding", "identity")
            conn.setChunkedStreamingMode(128 * 1024)
            val boundary = "----mywifi${System.currentTimeMillis()}"
            conn.setRequestProperty(
                "Content-Type",
                if (source.multipart) {
                    "multipart/form-data; boundary=$boundary"
                } else {
                    "application/octet-stream"
                },
            )
            val chunk = ByteArray(256 * 1024)
            conn.outputStream.use { output ->
                if (source.multipart) {
                    output.write(
                        (
                            "--$boundary\r\n" +
                                "Content-Disposition: form-data; name=\"file\"; " +
                                "filename=\"speedtest.bin\"\r\n" +
                                "Content-Type: application/octet-stream\r\n\r\n"
                            ).toByteArray(),
                    )
                }
                while (System.currentTimeMillis() < deadline) {
                    coroutineContext.ensureActive()
                    output.write(chunk)
                    tracker.addBytes(chunk.size.toLong())
                }
                if (source.multipart) output.write("\r\n--$boundary--\r\n".toByteArray())
                output.flush()
            }
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
        } finally {
            conn.disconnect()
        }
    }

    /** روابط fast.com: نجلب الرمز من الصفحة (أو سكربت التطبيق) ثم واجهة القياس. */
    private suspend fun fastComUrls(config: Config): List<String> {
        val token = fetchFastToken(config)
        val api = "https://api.fast.com/netflix/speedtest/v2" +
            "?https=true&token=$token&urlCount=${config.streams}"
        val body = httpGet(
            api,
            config,
            mapOf("Referer" to REFERER, "Origin" to ORIGIN),
        )
        val urls = runCatching { parseUrls(body) }.getOrElse { error ->
            throw SpeedTestException(Failure.ERROR, "fast.com: bad response", "رد غير متوقع من api.fast.com", error)
        }
        if (urls.isEmpty()) throw SpeedTestException(Failure.ERROR, "fast.com: no urls")
        return urls
    }

    /** جلب رمز fast.com من الصفحة ثم سكربت التطبيق. */
    private fun fetchFastToken(config: Config): String {
        val html = httpGet(FAST_HOME, config)
        TOKEN_REGEX.find(html)?.let { return it.groupValues[1] }
        val script = SCRIPT_REGEX.find(html)?.groupValues?.get(1)
            ?: throw SpeedTestException(Failure.ERROR, "fast.com: script not found")
        val scriptUrl = when {
            script.startsWith("http") -> script
            script.startsWith("/") -> "https://fast.com$script"
            else -> "https://fast.com/$script"
        }
        val js = httpGet(scriptUrl, config)
        TOKEN_REGEX.find(js)?.let { return it.groupValues[1] }
        throw SpeedTestException(Failure.ERROR, "fast.com: token not found")
    }

    /** تفسير رد api.fast.com (مصفوفة كائنات أو نصوص). */
    private fun parseUrls(body: String): List<String> {
        val array = JSONObject(body).getJSONArray("urls")
        return (0 until array.length()).mapNotNull { index ->
            when (val item = array.get(index)) {
                is String -> item
                is JSONObject -> item.optString("url").ifBlank { null }
                else -> null
            }
        }
    }

    private fun httpGet(
        address: String,
        config: Config,
        headers: Map<String, String> = emptyMap(),
    ): String {
        val conn = open(
            address = address,
            config = config,
            headers = headers,
            readTimeoutMillis = config.metadataReadTimeoutMillis,
        )
        try {
            val code = conn.responseCode
            if (code != 200) throw SpeedTestException(Failure.ERROR, "HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: SpeedTestException) {
            throw e
        } catch (e: IOException) {
            throw SpeedTestException(Failure.ERROR, e.message ?: "http error", null, e)
        } finally {
            conn.disconnect()
        }
    }

    private fun open(
        address: String,
        config: Config,
        headers: Map<String, String> = emptyMap(),
        readTimeoutMillis: Int = config.readTimeoutMillis,
    ): HttpURLConnection {
        val conn = URL(address).openConnection() as HttpURLConnection
        conn.connectTimeout = config.connectTimeoutMillis
        conn.readTimeout = readTimeoutMillis
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept-Encoding", "identity")
        headers.forEach { (name, value) -> conn.setRequestProperty(name, value) }
        return conn
    }

    /** فحص الاتصال لا يفشل الاختبار حتى لو رمى استثناءً (نعتبره متصلًا). */
    private fun isOnlineSafely(isOnline: () -> Boolean): Boolean =
        try {
            isOnline()
        } catch (e: Exception) {
            true
        }

    /** وصف مختصر لسبب فشل مصدر واحد، يُعرض في التفاصيل. */
    private fun reasonOf(error: Throwable): String = when {
        error is SpeedTestException && error.detail != null -> error.detail
        error.message?.startsWith("HTTP ") == true -> error.message!!
        error is SocketTimeoutException -> REASON_TIMEOUT
        error is java.net.UnknownHostException -> REASON_DNS
        error is IOException -> REASON_NETWORK
        else -> REASON_FAILED
    }

    /** سجل محاولات المصادر (آمن من تعدد الخيوط) لعرضه عند الفشل. */
    internal class AttemptLog {
        private val entries = Collections.synchronizedList(mutableListOf<String>())

        fun record(source: String, reason: String) {
            entries.add("$source: $reason")
        }

        fun summary(max: Int = 5): String? {
            val snapshot = synchronized(entries) { entries.toList() }
            if (snapshot.isEmpty()) return null
            return snapshot.takeLast(max).joinToString(" • ")
        }
    }

    /** متابعة كمّية البيانات في كل مرحلة لحساب السرعة الجارية. */
    private class Tracker(
        private val startedAt: Long,
        private val config: Config,
        private val watchdog: SpeedTestWatchdog,
        private val onProgress: (Progress) -> Unit,
    ) {

        private val phase = AtomicReference(Phase.PREPARING)
        private val phaseStartedAt = AtomicLong(startedAt)
        private val phaseBytes = AtomicLong(0L)
        private val lastReportAt = AtomicLong(0L)

        val phaseByteCount: Long get() = phaseBytes.get()

        fun startPhase(next: Phase) {
            val now = System.currentTimeMillis()
            phase.set(next)
            phaseStartedAt.set(now)
            phaseBytes.set(0L)
            lastReportAt.set(0L)
            watchdog.onProgress(now)
            report(now, force = true)
        }

        fun addBytes(count: Long) {
            if (count <= 0L) return
            val now = System.currentTimeMillis()
            phaseBytes.addAndGet(count)
            watchdog.onProgress(now)
            report(now, force = false)
        }

        fun finish() {
            phase.set(Phase.FINISHED)
        }

        /** السرعة الجارية للمرحلة الحالية بالبت/ثانية. */
        fun currentBitsPerSecond(): Double {
            val elapsed = (System.currentTimeMillis() - phaseStartedAt.get()).coerceAtLeast(1L)
            return phaseBytes.get() * 8.0 * 1000.0 / elapsed
        }

        private fun report(now: Long, force: Boolean) {
            val previous = lastReportAt.get()
            if (!force && now - previous < config.reportIntervalMillis) return
            if (!lastReportAt.compareAndSet(previous, now)) return
            val elapsed = (now - phaseStartedAt.get()).coerceAtLeast(1L)
            val transferred = phaseBytes.get()
            onProgress(
                Progress(
                    phase = phase.get(),
                    elapsedMillis = (now - startedAt).coerceAtLeast(0L),
                    bytesTransferred = transferred,
                    bitsPerSecond = transferred * 8.0 * 1000.0 / elapsed,
                ),
            )
        }
    }

    private const val PROBE_POLL_MILLIS = 120L
    private const val REASON_NO_URLS = "بلا روابط"
    private const val REASON_NO_BYTES = "بلا بيانات"
    private const val REASON_TIMEOUT = "مهلة"
    private const val REASON_NETWORK = "فشل الاتصال"
    private const val REASON_DNS = "تعذّر تحليل العنوان"
    private const val REASON_FAILED = "فشل الطلب"
}
