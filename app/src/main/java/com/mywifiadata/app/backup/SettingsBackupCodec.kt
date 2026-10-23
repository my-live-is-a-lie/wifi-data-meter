package com.mywifiadata.app.backup

import org.json.JSONArray
import org.json.JSONObject

/** Versioned JSON codec for the user-configurable values in SettingsStore. */
internal object SettingsBackupCodec {
    const val FORMAT_ID = "com.mywifiadata.settings-backup"
    const val SCHEMA_VERSION = 1
    const val MAX_BACKUP_BYTES = 2 * 1024 * 1024

    /** Device/runtime state is intentionally kept on the device, not restored from a file. */
    private val excludedKeys = setOf(
        "quick_settings_tile_enabled",
        "quick_settings_tile_added",
        "app_icon_hidden",
        "tracking_enabled",
        "plan_logo_path",
        "mobile_plan_logo_path",
        "app_search_selection_history",
        "monitor_target_uid",
        "monitor_target_package",
        "monitor_target_label",
        "monitor_target_icon",
        "monitor_target_history",
        "session_start",
        "session_baseline_rx",
        "session_baseline_tx",
        "alert_cycle",
        "alert_level",
        "speed_test_download",
        "speed_test_upload",
        "speed_test_time",
        "speed_test_source_fast",
        "speed_test_upload_ok",
        "speed_test_data_bytes",
        "speed_test_warning_snoozed_until",
        "speed_test_history",
    )

    fun isExcluded(key: String): Boolean = key in excludedKeys

    fun encode(preferences: Map<String, *>): String {
        val settings = JSONObject()
        preferences.toSortedMap().forEach { (key, value) ->
            if (isExcluded(key)) return@forEach
            val entry = value?.let { encodeEntry(it) } ?: return@forEach
            settings.put(key, entry)
        }
        require(settings.length() <= MAX_PREFERENCE_COUNT) { "There are too many settings to back up." }
        val json = JSONObject()
            .put("format", FORMAT_ID)
            .put("schemaVersion", SCHEMA_VERSION)
            .put("createdAtMillis", System.currentTimeMillis())
            .put("settings", settings)
            .toString(2)
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) {
            "Settings backup is too large."
        }
        return json
    }

    /** Validates and decodes a backup; excluded device-specific keys are ignored even if present. */
    fun decode(json: String): Map<String, Any> {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) {
            "Backup file is too large."
        }
        val root = runCatching { JSONObject(json) }
            .getOrElse { throw IllegalArgumentException("Backup file is not valid JSON.", it) }
        require(root.optString("format") == FORMAT_ID) { "This is not a Wi-Fi Data Meter settings backup." }
        val schemaVersion = root.optInt("schemaVersion", -1)
        require(schemaVersion in 1..SCHEMA_VERSION) { "This backup version is not supported." }
        val settings = root.optJSONObject("settings")
            ?: throw IllegalArgumentException("Backup settings are missing.")
        require(settings.length() <= MAX_PREFERENCE_COUNT) { "Backup contains too many settings." }

        val restored = LinkedHashMap<String, Any>()
        val keys = settings.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (isExcluded(key)) continue
            require(key.isNotBlank() && key.length <= MAX_KEY_LENGTH) { "Backup contains an invalid setting." }
            val entry = settings.optJSONObject(key)
                ?: throw IllegalArgumentException("Backup contains an invalid setting.")
            restored[key] = decodeEntry(entry)
        }
        return restored
    }

    private fun encodeEntry(value: Any): JSONObject? {
        return when (value) {
            is String -> JSONObject().put("type", "string").put("value", value)
            is Boolean -> JSONObject().put("type", "boolean").put("value", value)
            is Int -> JSONObject().put("type", "int").put("value", value)
            is Long -> JSONObject().put("type", "long").put("value", value)
            is Float -> if (value.isFinite()) {
                JSONObject().put("type", "float").put("value", value.toDouble())
            } else {
                null
            }
            is Double -> if (value.isFinite()) {
                JSONObject().put("type", "double").put("value", value)
            } else {
                null
            }
            is Set<*> -> {
                if (value.any { it !is String }) return null
                val strings = value.filterIsInstance<String>()
                val array = JSONArray().apply { strings.sorted().forEach { put(it) } }
                JSONObject().put("type", "string_set").put("value", array)
            }
            else -> null
        }
    }

    private fun decodeEntry(entry: JSONObject): Any {
        val value = entry.opt("value")
        return when (entry.optString("type")) {
            "string" -> value as? String ?: invalidEntry()
            "boolean" -> value as? Boolean ?: invalidEntry()
            "int" -> (value as? Number)?.toInt() ?: invalidEntry()
            "long" -> (value as? Number)?.toLong() ?: invalidEntry()
            "float" -> (value as? Number)?.toFloat()?.takeIf(Float::isFinite) ?: invalidEntry()
            "double" -> (value as? Number)?.toDouble()?.takeIf(Double::isFinite) ?: invalidEntry()
            "string_set" -> {
                val array = value as? JSONArray ?: invalidEntry()
                buildSet {
                    for (index in 0 until array.length()) {
                        add(array.get(index) as? String ?: invalidEntry())
                    }
                }
            }
            else -> invalidEntry()
        }
    }

    private fun invalidEntry(): Nothing = throw IllegalArgumentException("Backup contains an invalid setting.")

    private const val MAX_PREFERENCE_COUNT = 512
    private const val MAX_KEY_LENGTH = 120
}
