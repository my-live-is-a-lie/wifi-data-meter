package com.mywifiadata.app.data

import com.mywifiadata.app.data.model.AppSearchSelection
import com.mywifiadata.app.data.model.AppUsage
import java.text.Normalizer
import java.util.Base64
import java.util.Locale

/** Only chosen apps are stored locally, never the user's query text. */
internal object AppSearchHistory {
    const val LIMIT = 100

    fun key(app: AppUsage): String = app.packageName ?: "uid:${app.uid}"

    fun record(
        history: List<AppSearchSelection>,
        app: AppUsage,
        now: Long,
    ): List<AppSearchSelection> {
        val key = key(app)
        val previous = history.firstOrNull { it.appKey == key }
        val count = (previous?.selectionCount ?: 0L).coerceAtMost(Long.MAX_VALUE - 1) + 1
        return (listOf(AppSearchSelection(key, count, now)) + history.filterNot { it.appKey == key })
            .sortedByDescending { it.lastSelectedMillis }
            .take(LIMIT)
    }

    /** Blank input promotes prior selections, then includes every remaining app.
     * Input order breaks ties for apps with no history, preserving the usage sort choice. */
    fun search(
        apps: List<AppUsage>,
        query: String,
        history: List<AppSearchSelection>,
    ): List<AppUsage> {
        val needle = normalize(query.trim())
        val selections = history.associateBy { it.appKey }
        return apps.filter {
            needle.isEmpty() || normalize(it.label).contains(needle) ||
                normalize(it.packageName.orEmpty()).contains(needle)
        }.sortedWith(
            compareByDescending<AppUsage> { selections[key(it)]?.selectionCount ?: 0L }
                .thenByDescending { selections[key(it)]?.lastSelectedMillis ?: 0L },
        )
    }

    private fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)

    fun encode(history: List<AppSearchSelection>): String = history.take(LIMIT).joinToString(";") {
        val key = Base64.getEncoder().encodeToString(it.appKey.toByteArray(Charsets.UTF_8))
        "$key,${it.selectionCount},${it.lastSelectedMillis}"
    }

    fun decode(encoded: String?): List<AppSearchSelection> = encoded.orEmpty().split(';').mapNotNull {
        val parts = it.split(',')
        if (parts.size != 3) return@mapNotNull null
        val key = runCatching { String(Base64.getDecoder().decode(parts[0]), Charsets.UTF_8) }
            .getOrNull()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        val count = parts[1].toLongOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        val timestamp = parts[2].toLongOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        AppSearchSelection(key, count, timestamp)
    }.sortedByDescending { it.lastSelectedMillis }.distinctBy { it.appKey }.take(LIMIT)
}
