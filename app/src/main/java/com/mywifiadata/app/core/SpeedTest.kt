package com.mywifiadata.app.core

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong
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
 */
object SpeedTestEngine {

    /** نتيجة الاختبار: قيمتان بالبت/ثانية + مصدر القياس. */
    data class Result(
        val downloadBitsPerSecond: Double,
        val uploadBitsPerSecond: Double,
        val usedFastCom: Boolean,
    )

    private const val FAST_HOME = "https://fast.com/"
    private const val REFERER = "https://fast.com/"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private const val STREAMS = 5
    private const val DOWNLOAD_MILLIS = 10_000L
    private const val UPLOAD_MILLIS = 6_000L
    private const val CLOUDFLARE_CHUNK = 25_000_000
    private val TOKEN_REGEX = Regex("""token\s*[:=]\s*["']([^"']+)["']""")
    private val SCRIPT_REGEX = Regex("""src=["']([^"']*app-[^"']+\.js)["']""")

    /** تنفيذ الاختبار كاملًا (تنزيل ثم رفع) — يُلغى تلقائيًا مع الـJob الحامل له. */
    suspend fun run(): Result = withContext(Dispatchers.IO) {
        val download = try {
            fastDownload() to true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            cloudflareDownload() to false
        }
        val upload = try {
            cloudflareUpload()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            0.0
        }
        Result(
            downloadBitsPerSecond = download.first,
            uploadBitsPerSecond = upload,
            usedFastCom = download.second,
        )
    }

    /** تنزيل من خوادم fast.com عبر خمسة تدفقات متوازية. */
    private suspend fun fastDownload(): Double {
        val token = fetchFastToken()
        val api = "https://api.fast.com/netflix/speedtest/v2?https=true&token=$token&urlCount=$STREAMS"
        val body = httpGet(
            api,
            headers = mapOf("Referer" to REFERER, "Origin" to "https://fast.com"),
        )
        val urls = parseUrls(body)
        if (urls.isEmpty()) throw IOException("fast.com: no test urls")
        val totalBytes = AtomicLong(0)
        val startedAt = System.currentTimeMillis()
        val deadline = startedAt + DOWNLOAD_MILLIS
        coroutineScope {
            urls.forEach { address ->
                launch(Dispatchers.IO) {
                    runCatching { stream(address, deadline) }
                        .onSuccess { bytes -> totalBytes.addAndGet(bytes) }
                }
            }
        }
        val elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
        return totalBytes.get() * 8.0 * 1000.0 / elapsed
    }

    /** قراءة تدفق واحد حتى الموعد النهائي. */
    private suspend fun stream(address: String, deadline: Long): Long {
        val ranged = if (address.contains("/range/")) {
            address
        } else {
            address.trimEnd('/') + "/range/0-26214400"
        }
        val conn = open(
            ranged,
            headers = mapOf("Referer" to REFERER, "Origin" to "https://fast.com"),
        )
        var count = 0L
        try {
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            conn.inputStream.use { input ->
                val buffer = ByteArray(64 * 1024)
                while (System.currentTimeMillis() < deadline) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer)
                    if (read <= 0) break
                    count += read
                }
            }
        } finally {
            conn.disconnect()
        }
        return count
    }

    /** جلب رمز fast.com من الصفحة ثم سكربت التطبيق. */
    private fun fetchFastToken(): String {
        val html = httpGet(FAST_HOME)
        TOKEN_REGEX.find(html)?.let { return it.groupValues[1] }
        val script = SCRIPT_REGEX.find(html)?.groupValues?.get(1)
            ?: throw IOException("fast.com: script not found")
        val scriptUrl = when {
            script.startsWith("http") -> script
            script.startsWith("/") -> "https://fast.com$script"
            else -> "https://fast.com/$script"
        }
        val js = httpGet(scriptUrl)
        TOKEN_REGEX.find(js)?.let { return it.groupValues[1] }
        throw IOException("fast.com: token not found")
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
    private suspend fun cloudflareDownload(): Double {
        val startedAt = System.currentTimeMillis()
        val endAt = startedAt + 8_000L
        var totalBytes = 0L
        while (System.currentTimeMillis() < endAt) {
            coroutineContext.ensureActive()
            val conn = open("https://speed.cloudflare.com/__down?bytes=$CLOUDFLARE_CHUNK")
            try {
                if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
                conn.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        totalBytes += read
                    }
                }
            } finally {
                conn.disconnect()
            }
        }
        val elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
        return totalBytes * 8.0 * 1000.0 / elapsed
    }

    /** رفع عبر Cloudflare (fast.com لا يوفّر واجهة رفع عامة موثوقة). */
    private suspend fun cloudflareUpload(): Double {
        val startedAt = System.currentTimeMillis()
        val endAt = startedAt + UPLOAD_MILLIS
        val chunk = ByteArray(256 * 1024)
        var totalBytes = 0L
        val conn = URL("https://speed.cloudflare.com/__up").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Content-Type", "application/octet-stream")
            conn.setChunkedStreamingMode(64 * 1024)
            conn.outputStream.use { output ->
                while (System.currentTimeMillis() < endAt) {
                    coroutineContext.ensureActive()
                    output.write(chunk)
                    totalBytes += chunk.size
                }
            }
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
        } finally {
            conn.disconnect()
        }
        val elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
        return totalBytes * 8.0 * 1000.0 / elapsed
    }

    private fun httpGet(address: String, headers: Map<String, String> = emptyMap()): String {
        val conn = open(address, headers)
        try {
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun open(address: String, headers: Map<String, String> = emptyMap()): HttpURLConnection {
        val conn = URL(address).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", USER_AGENT)
        headers.forEach { (name, value) -> conn.setRequestProperty(name, value) }
        return conn
    }
}
