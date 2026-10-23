package com.mywifiadata.app

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.mywifiadata.app.data.AppIdentityReceiver
import com.mywifiadata.app.backup.SettingsBackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.mywifiadata.app.data.SettingsStore
import com.mywifiadata.app.data.UsageRepository
import com.mywifiadata.app.monitor.Notifier

/**
 * نقطة الدخول للتطبيق: تُهيّئ المستودع (Repository) وقنوات الإشعارات مرة واحدة.
 */
class MyWifiApplication : Application() {

    internal val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val repository: UsageRepository by lazy {
        UsageRepository(context = this, settingsStore = SettingsStore(this))
    }

    override fun onCreate() {
        super.onCreate()
        Notifier(this).ensureChannels()
        val backupManager = SettingsBackupManager(this, repository.settingsStore)
        runCatching { backupManager.syncSchedule() }
            .onFailure { backupManager.markAttemptFailed() }
        // Runtime registration observes changes while the UI or monitoring service is alive.
        // The startup scan reconciles changes that occurred while our process was stopped.
        ContextCompat.registerReceiver(
            this,
            AppIdentityReceiver(),
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        applicationScope.launch {
            // يطبّق مدة الاحتفاظ المختارة على الأرشيف حتى قبل فتح شاشة السجل.
            runCatching { repository.pruneUsageArchive() }
            runCatching { repository.appIdentities.captureInstalledApps() }
            runCatching { repository.captureUsageArchive() }
        }
    }
}
