package com.mywifiadata.app

import android.app.Application
import com.mywifiadata.app.data.SettingsStore
import com.mywifiadata.app.data.UsageRepository
import com.mywifiadata.app.monitor.Notifier

/**
 * نقطة الدخول للتطبيق: تُهيّئ المستودع (Repository) وقنوات الإشعارات مرة واحدة.
 */
class MyWifiApplication : Application() {

    val repository: UsageRepository by lazy {
        UsageRepository(context = this, settingsStore = SettingsStore(this))
    }

    override fun onCreate() {
        super.onCreate()
        Notifier(this).ensureChannels()
    }
}
