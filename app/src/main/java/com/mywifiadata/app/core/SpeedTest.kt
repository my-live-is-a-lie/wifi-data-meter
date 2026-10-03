package com.mywifiadata.app.core

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
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
 * قياس سرعة الإنترنت بالخوادم خلف موقع fast.com (Netflix Open Connect)،
 * مع رجوع تلقائي إلى خوادم Cloudflare إذا حُجب api.fast.com في منطقة
 * المستخدم (بعض الدول تحجبه برمز 403).
 *
 * مزايا السلامة المضافة:
 * - **رسالة عند انقطاع الاتصال**: مرصد [SpeedTestWatchdog] يفحص الشبكة كل نصف
 *   ثانية أثناء الاختبار، وعند فقدانها يتوقف كل شيء فورًا مع سبب واضح.
 * - **رسالة عند مرور وقت طويل دون نتيجة**: إذا لم تصل أي بيانات خلال
 *   [Config.stallTimeoutMillis] أو تجاوز الاختبار [Config.overallTimeoutMillis]
 *   يُغلق التحميل (تُفصل الاتصالات) ويُعاد سبب الفشل إلى الواجهة.
 * - **إغلاق التحميل دائمًا**: كل اتصال يُفصل في `finally`، وإلغاء الـCoroutine
 *   (زر الإيقاف) يوقف القراءة/الكتابة بدل تركها في الخلفية.
 */
object SpeedTestEngine {

    /** مراحل الاختبار كما تُعرض في الواجهة. */
    enum class Phase { PREPARING, DOWNLOAD, UPLOAD, FINISHED }

    /** أسباب فشل الاختبار (تُترجم إلى رسائل عربية في الواجهة). */
    enum class Failure { NO_CONNECTION, CONNECTION_LOST, STALLED, TIMEOUT, ERROR }

    /** فشل اختبار السرعة مصحوبًا بسببه. */
    class SpeedTestException(
        val failure: Failure,
        message: String,
        cause: Throwable? = null,
    ) : IOException(message, cause)

    /** تقدّم لحظي أثناء الاختبار (لعرض السرعة الجارية). */
    data class Progress(
        val phase: Phase,
        val elapsedMillis: Long,
        val bytesTransferred: Long,
        val bitsPerSecond: Double,
    )

    /** نتيجة الاختبار: قيمتان بالبت/ثانية + مصدر القياس. */
    data class Result(
        val downloadBitsPerSecond: Double,
        val uploadBitsPerSecond: Double,
        val usedFastCom: Boolean,
        val durationMillis: Long,
    )

    /** إعدادات الاختبار والمراصد الزمنية. */
    data class Config(
        val overallTimeoutMillis: Long = SpeedTestWatchdog.DEFAULT_OVERALL_TIMEOUT_MILLIS,
        val stallTimeoutMillis: Long = SpeedTestWatchdog.DEFAULT_STALL_TIMEOUT_MILLIS,
        val downloadMillis: Long = 10_000L,
        val uploadMillis: Long = 6_000L,
        val streams: Int = 5,
        val connectTimeoutMillis: Int = 10_000,
        /** مهلة قراءة التدفقات: قصيرة ليتحرك المرصد سريعًا عند التوقف. */
        val readTimeoutMillis: Int = 4_000,
        /** مهلة الطلبات الصغيرة (صفحة fast.com وواجهة الرموز). */
        val metadataReadTimeoutMillis: Int = 12_000,
        val reportIntervalMillis: Long = 300L,
    )

    private const val FAST_HOME = "https://fast.com/"
    private const val REFERER = "https://fast.com/"
    private const val ORIGIN = "https://fast.com"
    private const val CLOUDFLARE_DOWN =
        "https://speed.cloudflare.com/__down?bytes=25000000"
    private const val CLOUDFLARE_UP = "https://speed.cloudflare.com/__up"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private const val WATCH_INTERVAL_MILLIS = 500L
    private val TOKEN_REGEX = Regex("""token\s*[:=]\s*["']([^"']+)["']""")
    private val SCRIPT_REGEX = Regex("""src=["']([^"']*app-[^"']+\.js)["']""")

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

        // 1) التنزيل: fast.com أولًا، وإن فشل الخادم نفسه (حجب/403) ننتقل إلى Cloudflare.
        //    أما انقطاع الاتصال أو توقّف البيانات فيوقف الاختبار فورًا برسالته.
        var usedFastCom = true
        val download = try {
            phase(watchdog, isOnline) { fastDownload(config, tracker) }
        } catch (e: SpeedTestException) {
            if (e.failure != Failure.ERROR) throw e
            usedFastCom = false
            phase(watchdog, isOnline) { cloudflareDownload(config, tracker) }
        }

        // 2) الرفع عبر Cloudflare (fast.com لا يوفّر واجهة رفع عامة موثوقة).
        val upload = try {
            phase(watchdog, isOnline) { cloudflareUpload(config, tracker) }
        } catch (e: SpeedTestException) {
            when (e.failure) {
                Failure.ERROR -> 0.0
                else -> throw e
            }
        }

        val finishedAt = System.currentTimeMillis()
        tracker.finish()
        Result(
            downloadBitsPerSecond = download,
            uploadBitsPerSecond = upload,
            usedFastCom = usedFastCom,
            durationMillis = (finishedAt - startedAt).coerceAtLeast(1L),
        )
    }

    /**
     * ينفّذ مرحلة واحدة مع مرصد يفحص الاتصال وتدفّق البيانات، ويحوّل أخطاء
     * الشبكة إلى [SpeedTestException] بسبب واضح.
     */
    private suspend fun <T> phase(
        watchdog: SpeedTestWatchdog,
        isOnline: () -> Boolean,
        block: suspend () -> T,
    ): T = try {
        coroutineScope {
            val watcher = launch {
                while (true) {
                    delay(WATCH_INTERVAL_MILLIS)
                    val failure = watchdog.check(
                        nowMillis = System.currentTimeMillis(),
                        isOnline = isOnlineSafely(isOnline),
                    )
                    if (failure != null) {
                        throw SpeedTestException(failure, "watchdog: $failure")
                    }
                }
            }
            try {
                block()
            } finally {
                watcher.cancel()
            }
        }
    } catch (e: SpeedTestException) {
        throw e
    } catch (e: CancellationException) {
        throw e
    } catch (e: SocketTimeoutException) {
        throw SpeedTestException(Failure.STALLED, "socket timeout", e)
    } catch (e: IOException) {
        val failure = if (isOnlineSafely(isOnline)) Failure.ERROR else Failure.CONNECTION_LOST
        throw SpeedTestException(failure, e.message ?: "io error", e)
    }

    /** تنزيل من خوادم fast.com عبر عدة تدفقات متوازية. */
    private suspend fun fastDownload(config: Config, tracker: Tracker): Double {
        tracker.startPhase(Phase.PREPARING)
        val token = fetchFastToken(config)
        val api = "https://api.fast.com/netflix/speedtest/v2" +
            "?https=true&token=$token&urlCount=${config.streams}"
        val body = httpGet(api, config, mapOf("Referer" to REFERER, "Origin" to ORIGIN))
        val urls = parseUrls(body)
        if (urls.isEmpty()) throw SpeedTestException(Failure.ERROR, "fast.com: no test urls")
        return multiStreamDownload(urls, config, tracker)
    }

    /** قراءة عدة تدفقات حتى انتهاء المدة، وفشل تدفق واحد لا يوقف البقية. */
    private suspend fun multiStreamDownload(
        urls: List<String>,
        config: Config,
        tracker: Tracker,
    ): Double {
        tracker.startPhase(Phase.DOWNLOAD)
        val deadline = System.currentTimeMillis() + config.downloadMillis
        val failures = AtomicInteger(0)

        coroutineScope {
            urls.forEach { address ->
                launch(Dispatchers.IO) {
                    try {
                        val bytes = stream(address, deadline, config, tracker)
                        if (bytes <= 0L) failures.incrementAndGet()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: SpeedTestException) {
                        // توقّف/انقطاع حقيقي: يوقف الاختبار كله برسالته.
                        throw e
                    } catch (e: IOException) {
                        failures.incrementAndGet()
                    }
                }
            }
        }

        if (failures.get() >= urls.size) {
            throw SpeedTestException(Failure.ERROR, "fast.com: all streams failed")
        }
        if (tracker.phaseByteCount <= 0L) {
            throw SpeedTestException(Failure.ERROR, "fast.com: no bytes received")
        }
        return tracker.currentBitsPerSecond()
    }

    /** قراءة تدفق واحد حتى الموعد النهائي (يُفصل الاتصال دائمًا في finally). */
    private suspend fun stream(
        address: String,
        deadline: Long,
        config: Config,
        tracker: Tracker,
    ): Long {
        val target = if (address.contains("/range/")) {
            address
        } else {
            address.trimEnd('/') + "/range/0-26214400"
        }
        val conn = open(target, config, mapOf("Referer" to REFERER, "Origin" to ORIGIN))
        var count = 0L
        try {
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            conn.inputStream.use { input ->
                val buffer = ByteArray(64 * 1024)
                while (System.currentTimeMillis() < deadline) {
                    coroutineContext.ensureActive()
                    var read = 0
                    var timedOut = false
                    try {
                        read = input.read(buffer)
                    } catch (e: SocketTimeoutException) {
                        timedOut = true
                    }
                    // لا بيانات الآن: المرصد يقرر (توقّف/مهلة)، وإن كان كل شيء سليمًا نتابع.
                    if (timedOut) continue
                    if (read <= 0) break
                    count += read
                    tracker.addBytes(read.toLong())
                }
            }
        } finally {
            conn.disconnect()
        }
        return count
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

    /** تنزيل احتياطي من Cloudflare عند تعذّر fast.com. */
    private suspend fun cloudflareDownload(config: Config, tracker: Tracker): Double {
        tracker.startPhase(Phase.DOWNLOAD)
        val deadline = System.currentTimeMillis() + config.downloadMillis
        while (System.currentTimeMillis() < deadline) {
            coroutineContext.ensureActive()
            val conn = open(CLOUDFLARE_DOWN, config)
            try {
                if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
                conn.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (System.currentTimeMillis() < deadline) {
                        coroutineContext.ensureActive()
                        var read = 0
                        var timedOut = false
                        try {
                            read = input.read(buffer)
                        } catch (e: SocketTimeoutException) {
                            timedOut = true
                        }
                        if (timedOut) continue
                        if (read <= 0) break
                        tracker.addBytes(read.toLong())
                    }
                }
            } catch (e: SpeedTestException) {
                throw e
            } catch (e: IOException) {
                // انتهت المدة أثناء المحاولة: نكتفي بما جُمع.
                if (System.currentTimeMillis() >= deadline) break
                throw e
            } finally {
                conn.disconnect()
            }
        }
        if (tracker.phaseByteCount <= 0L) {
            throw SpeedTestException(Failure.ERROR, "cloudflare: no bytes received")
        }
        return tracker.currentBitsPerSecond()
    }

    /** رفع عبر Cloudflare (fast.com لا يوفّر واجهة رفع عامة موثوقة). */
    private suspend fun cloudflareUpload(config: Config, tracker: Tracker): Double {
        tracker.startPhase(Phase.UPLOAD)
        val deadline = System.currentTimeMillis() + config.uploadMillis
        val chunk = ByteArray(256 * 1024)
        val conn = URL(CLOUDFLARE_UP).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = config.connectTimeoutMillis
            conn.readTimeout = config.metadataReadTimeoutMillis
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Content-Type", "application/octet-stream")
            conn.setChunkedStreamingMode(64 * 1024)
            conn.outputStream.use { output ->
                while (System.currentTimeMillis() < deadline) {
                    coroutineContext.ensureActive()
                    output.write(chunk)
                    tracker.addBytes(chunk.size.toLong())
                }
                output.flush()
            }
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
        } finally {
            conn.disconnect()
        }
        if (tracker.phaseByteCount <= 0L) {
            throw SpeedTestException(Failure.ERROR, "cloudflare: no bytes sent")
        }
        return tracker.currentBitsPerSecond()
    }

    private fun httpGet(
        address: String,
        config: Config,
        headers: Map<String, String> = emptyMap(),
    ): String {
        val conn = open(address, config, headers, config.metadataReadTimeoutMillis)
        try {
            if (conn.responseCode != 200) {
                throw SpeedTestException(Failure.ERROR, "HTTP ${conn.responseCode}")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: SpeedTestException) {
            throw e
        } catch (e: IOException) {
            throw SpeedTestException(Failure.ERROR, e.message ?: "http error", e)
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
}
