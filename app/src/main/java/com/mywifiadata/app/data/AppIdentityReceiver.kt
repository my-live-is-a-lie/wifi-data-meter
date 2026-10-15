package com.mywifiadata.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mywifiadata.app.MyWifiApplication
import kotlinx.coroutines.launch

/** System-only runtime receiver; package updates must not be mistaken for uninstall. */
class AppIdentityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val removed = intent.action == Intent.ACTION_PACKAGE_REMOVED
        if (removed && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
        if (!removed && intent.action != Intent.ACTION_PACKAGE_ADDED && intent.action != Intent.ACTION_PACKAGE_REPLACED) return
        val application = context.applicationContext as? MyWifiApplication ?: return
        val pending = goAsync()
        application.applicationScope.launch {
            try {
                runCatching {
                    val cache = application.repository.appIdentities
                    if (removed) cache.packageRemoved(packageName) else cache.packageChanged(packageName)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
