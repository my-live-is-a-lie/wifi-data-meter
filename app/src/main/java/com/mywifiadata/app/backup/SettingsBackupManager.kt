package com.mywifiadata.app.backup

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.mywifiadata.app.data.SettingsStore

/** Cadence used by Android's inexact, battery-aware periodic job scheduler. */
enum class AutoBackupFrequency(val intervalMillis: Long) {
    DAILY(TimeUnit.DAYS.toMillis(1)),
    WEEKLY(TimeUnit.DAYS.toMillis(7)),
    MONTHLY(TimeUnit.DAYS.toMillis(30));

    companion object {
        fun fromStoredName(name: String?): AutoBackupFrequency =
            entries.firstOrNull { it.name == name } ?: WEEKLY
    }
}

data class AutomaticBackupSettings(
    val enabled: Boolean = false,
    val frequency: AutoBackupFrequency = AutoBackupFrequency.WEEKLY,
    val folderUri: String? = null,
    val lastBackupMillis: Long = 0L,
    val lastAttemptFailed: Boolean = false,
)

/** Persists automatic-backup preferences and writes JSON settings backups through SAF. */
class SettingsBackupManager(
    context: Context,
    private val settingsStore: SettingsStore,
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun currentAutomaticSettings(): AutomaticBackupSettings = AutomaticBackupSettings(
        enabled = preferences.getBoolean(KEY_ENABLED, false),
        frequency = AutoBackupFrequency.fromStoredName(preferences.getString(KEY_FREQUENCY, null)),
        folderUri = preferences.getString(KEY_FOLDER_URI, null)?.takeIf(String::isNotBlank),
        lastBackupMillis = preferences.getLong(KEY_LAST_BACKUP, 0L),
        lastAttemptFailed = preferences.getBoolean(KEY_LAST_ATTEMPT_FAILED, false),
    )

    fun saveAutomaticSettings(settings: AutomaticBackupSettings) {
        val previous = currentAutomaticSettings()
        try {
            persistConfiguration(settings)
            syncSchedule()
        } catch (error: Exception) {
            runCatching { persistConfiguration(previous) }
            runCatching { syncSchedule() }
            throw error
        }
    }

    private fun persistConfiguration(settings: AutomaticBackupSettings) {
        val saved = preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putString(KEY_FREQUENCY, settings.frequency.name)
            .putString(KEY_FOLDER_URI, settings.folderUri)
            .commit()
        check(saved) { "Could not save automatic-backup settings." }
    }

    /** Reconciles the persisted choice with JobScheduler, including after an app update. */
    fun syncSchedule() {
        val scheduler = appContext.getSystemService(JobScheduler::class.java) ?: return
        val config = currentAutomaticSettings()
        if (!config.enabled || config.folderUri == null) {
            scheduler.cancel(AUTO_BACKUP_JOB_ID)
            return
        }
        val existing = scheduler.getPendingJob(AUTO_BACKUP_JOB_ID)
        if (existing != null && existing.intervalMillis == config.frequency.intervalMillis) return

        val job = JobInfo.Builder(
            AUTO_BACKUP_JOB_ID,
            ComponentName(appContext, AutoBackupJobService::class.java),
        )
            .setPersisted(true)
            .setPeriodic(config.frequency.intervalMillis)
            .build()
        check(scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS) {
            "Android could not schedule automatic backups."
        }
    }

    fun clearAutomaticSettings() {
        appContext.getSystemService(JobScheduler::class.java)?.cancel(AUTO_BACKUP_JOB_ID)
        currentAutomaticSettings().folderUri?.let { storedUri ->
            runCatching {
                appContext.contentResolver.releasePersistableUriPermission(
                    Uri.parse(storedUri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        preferences.edit().clear().apply()
    }

    fun createSettingsBackupJson(): String = settingsStore.createSettingsBackupJson()

    /** Backs up to the user-selected folder; the same file is replaced on each run. */
    fun backupToSelectedFolder(): Long = synchronized(BACKUP_WRITE_LOCK) {
        try {
            val folderUri = currentAutomaticSettings().folderUri?.let(Uri::parse)
                ?: throw IOException("Choose a backup folder first.")
            val json = createSettingsBackupJson()
            writeToFolder(folderUri, json)
            val completedAt = System.currentTimeMillis()
            val saved = preferences.edit()
                .putLong(KEY_LAST_BACKUP, completedAt)
                .putBoolean(KEY_LAST_ATTEMPT_FAILED, false)
                .commit()
            if (!saved) throw IOException("Could not save the last-backup timestamp.")
            completedAt
        } catch (error: Exception) {
            markAttemptFailed()
            throw error
        }
    }

    /** Called by the scheduled job; a disabled setting is always a no-op. */
    fun runScheduledBackup() {
        if (!currentAutomaticSettings().enabled) return
        backupToSelectedFolder()
    }

    fun markAttemptFailed() {
        preferences.edit().putBoolean(KEY_LAST_ATTEMPT_FAILED, true).apply()
    }

    fun folderDisplayName(folderUri: String?): String? {
        if (folderUri.isNullOrBlank()) return null
        return runCatching {
            val treeUri = Uri.parse(folderUri)
            val treeId = DocumentsContract.getTreeDocumentId(treeUri)
            treeId.substringAfter(':', treeId)
                .replace('/', '›')
                .ifBlank { treeId }
        }.getOrElse {
            Uri.parse(folderUri).lastPathSegment?.takeIf(String::isNotBlank) ?: folderUri
        }
    }

    private fun writeToFolder(treeUri: Uri, json: String) {
        val resolver = appContext.contentResolver
        val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val folderDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )

        val childrenCursor = resolver.query(childrenUri, projection, null, null, null)
            ?: throw IOException("Could not read the selected backup folder.")
        val existingBackupUri = childrenCursor.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            var match: Uri? = null
            while (cursor.moveToNext()) {
                if (cursor.getString(nameColumn) == AUTO_BACKUP_FILE_NAME) {
                    match = DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(idColumn))
                    break
                }
            }
            match
        }

        val destination = existingBackupUri ?: DocumentsContract.createDocument(
            resolver,
            folderDocumentUri,
            JSON_MIME_TYPE,
            AUTO_BACKUP_FILE_NAME,
        ) ?: throw IOException("Could not create a backup file in the selected folder.")

        val output = resolver.openOutputStream(destination, "wt")
            ?: throw IOException("Could not open the backup file for writing.")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
    }

    companion object {
        const val PREFERENCES_NAME = "automatic_settings_backup"
        const val AUTO_BACKUP_FILE_NAME = "wifi-data-meter-settings-backup.json"
        const val JSON_MIME_TYPE = "application/json"
        const val AUTO_BACKUP_JOB_ID = 0x57494649

        private val BACKUP_WRITE_LOCK = Any()
        private const val KEY_ENABLED = "enabled"
        private const val KEY_FREQUENCY = "frequency"
        private const val KEY_FOLDER_URI = "folder_uri"
        private const val KEY_LAST_BACKUP = "last_backup_millis"
        private const val KEY_LAST_ATTEMPT_FAILED = "last_attempt_failed"
    }
}
