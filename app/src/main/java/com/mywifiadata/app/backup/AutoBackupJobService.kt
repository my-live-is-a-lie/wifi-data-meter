package com.mywifiadata.app.backup

import android.app.job.JobParameters
import android.app.job.JobService
import com.mywifiadata.app.MyWifiApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Runs scheduled settings backups away from the UI process lifecycle. */
class AutoBackupJobService : JobService() {
    private var activeJob: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val app = application as? MyWifiApplication ?: return false
        val manager = SettingsBackupManager(this, app.repository.settingsStore)
        activeJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                manager.runScheduledBackup()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                manager.markAttemptFailed()
            }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        activeJob?.cancel()
        activeJob = null
        return true
    }
}
