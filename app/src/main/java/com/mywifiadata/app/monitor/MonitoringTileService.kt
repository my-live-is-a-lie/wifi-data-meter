package com.mywifiadata.app.monitor

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mywifiadata.app.MyWifiApplication
import com.mywifiadata.app.R

/** زر الإعدادات السريعة لتشغيل مراقبة الشبكة وإيقافها. */
class MonitoringTileService : TileService() {

    private val repository by lazy { (application as MyWifiApplication).repository }

    override fun onTileAdded() {
        super.onTileAdded()
        repository.settingsStore.update { it.copy(quickSettingsTileEnabled = true) }
        updateTileState()
    }

    override fun onTileRemoved() {
        repository.settingsStore.update { it.copy(quickSettingsTileEnabled = false) }
        super.onTileRemoved()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val settings = repository.settingsStore.current
        if (!settings.quickSettingsTileEnabled) {
            updateTileState()
            return
        }

        if (!settings.trackingEnabled && !repository.hasUsageAccess()) {
            openUsageAccessSettings()
            return
        }

        if (settings.trackingEnabled) {
            repository.settingsStore.update { it.copy(trackingEnabled = false) }
            MonitorService.stop(this)
        } else {
            repository.settingsStore.update { it.copy(trackingEnabled = true) }
            val started = runCatching { MonitorService.start(this) }.isSuccess
            if (!started) {
                repository.settingsStore.update { it.copy(trackingEnabled = false) }
            }
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val settings = repository.settingsStore.current
        val stateDescription = when {
            !settings.quickSettingsTileEnabled -> getString(R.string.qs_tile_state_disabled)
            settings.trackingEnabled -> getString(R.string.qs_tile_state_running)
            !repository.hasUsageAccess() -> getString(R.string.qs_tile_state_permission_needed)
            else -> getString(R.string.qs_tile_state_stopped)
        }

        tile.label = getString(R.string.qs_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_usage)
        tile.state = when {
            !settings.quickSettingsTileEnabled -> Tile.STATE_UNAVAILABLE
            settings.trackingEnabled -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = stateDescription
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = stateDescription
        }
        tile.updateTile()
    }

    private fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                USAGE_ACCESS_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        private const val USAGE_ACCESS_REQUEST_CODE = 5401

        /** يطلب من النظام إعادة قراءة حالة البلاطة بعد تغييرها من واجهة التطبيق. */
        fun requestRefresh(context: Context) {
            runCatching {
                requestListeningState(
                    context.applicationContext,
                    ComponentName(context, MonitoringTileService::class.java),
                )
            }
        }
    }
}
