package com.mywifiadata.app.monitor

import android.app.Dialog
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import com.mywifiadata.app.MyWifiApplication
import com.mywifiadata.app.R
import com.mywifiadata.app.core.AppLanguage
import com.mywifiadata.app.core.LauncherIconVisibility

/** زر الإعدادات السريعة لتشغيل مراقبة الشبكة وإيقافها. */
class MonitoringTileService : TileService() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.localizedContext(newBase))
    }

    private val repository by lazy { (application as MyWifiApplication).repository }

    /** يشغّل إطارات تحريك الأيقونة على الخيط الرئيسي أثناء فتح لوحة الإعدادات السريعة. */
    private val tileAnimationHandler = Handler(Looper.getMainLooper())

    /** الحركة الجارية حاليًا (إن وُجدت). */
    private var tileAnimation: TileIconAnimation? = null

    override fun onTileAdded() {
        super.onTileAdded()
        repository.settingsStore.update {
            it.copy(quickSettingsTileEnabled = true, quickSettingsTileAdded = true)
        }
        updateTileState()
    }

    override fun onTileRemoved() {
        // لا تترك المستخدم بلا مدخل: إذا أزال البلاطة، أعد أيقونة المشغّل تلقائيًا.
        LauncherIconVisibility.setVisible(this, visible = true)
        repository.settingsStore.update {
            it.copy(
                quickSettingsTileEnabled = false,
                quickSettingsTileAdded = false,
                appIconHidden = false,
            )
        }
        super.onTileRemoved()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onStopListening() {
        // انتهى ظهور البلاطة: أوقف الحركة وأعد الأيقونة الثابتة إن كانت الحركة في منتصفها.
        if (stopTileIconAnimation()) updateTileState()
        super.onStopListening()
    }

    override fun onDestroy() {
        stopTileIconAnimation()
        super.onDestroy()
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
            stopMonitoring()
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14 may reject direct foreground-service starts from a tile callback.
            // Showing a temporary dialog makes this user action eligible to start the service.
            if (isLocked) {
                unlockAndRun { startMonitoringThroughDialog() }
            } else {
                startMonitoringThroughDialog()
            }
        } else {
            startMonitoring()
        }
    }

    private fun startMonitoringThroughDialog() {
        val dialog = object : Dialog(this) {
            init {
                window?.apply {
                    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    setDimAmount(0f)
                }
                setOnShowListener {
                    startMonitoring()
                    dismiss()
                }
            }

            override fun show() {
                super.show()
                // Android 14 otherwise collapses the shade (and can bring the app forward)
                // after showing the temporary dialog. Throw after showing it; this is caught
                // below while the service gets its short foreground-start exemption.
                throw TemporaryTileDialogShownException()
            }
        }

        val failure = runCatching { showDialog(dialog) }.exceptionOrNull()
        if (failure !is TemporaryTileDialogShownException) {
            failure?.let { Log.w(TAG, "Temporary tile dialog failed; trying the direct start", it) }
            startMonitoring()
        }
    }

    private fun startMonitoring() {
        val started = runCatching {
            repository.settingsStore.update { it.copy(trackingEnabled = true) }
            MonitorService.start(this)
        }.onFailure { exception ->
            Log.e(TAG, "Could not start monitoring from Quick Settings", exception)
            repository.settingsStore.update { it.copy(trackingEnabled = false) }
            Toast.makeText(this, R.string.qs_tile_start_failed, Toast.LENGTH_LONG).show()
        }.isSuccess

        if (!started) {
            MonitoringTileService.requestRefresh(this)
        }
        updateTileState()
        if (started) playTileIconAnimation(activating = true)
    }

    private fun stopMonitoring() {
        repository.settingsStore.update { it.copy(trackingEnabled = false) }
        MonitorService.stop(this)
        updateTileState()
        playTileIconAnimation(activating = false)
    }

    // ——————————————————————————— تحريك أيقونة البلاطة ———————————————————————————

    /**
     * يشغّل حركة الأيقونة: الدائرة تتقلص للداخل ثم تعود إلى مكانها، وشرطات الواي فاي
     * تُرسَم من الشرطة السفلية إلى العليا عند التشغيل، وبالعكس عند الإيقاف.
     */
    private fun playTileIconAnimation(activating: Boolean) {
        stopTileIconAnimation()
        val animation = TileIconAnimation(activating)
        tileAnimation = animation
        tileAnimationHandler.post(animation)
    }

    /** يوقف أي حركة جارية، ويعيد true إن كانت هناك حركة تعمل فعلًا. */
    private fun stopTileIconAnimation(): Boolean {
        val wasRunning = tileAnimation != null
        tileAnimation?.let { tileAnimationHandler.removeCallbacks(it) }
        tileAnimation = null
        return wasRunning
    }

    /**
     * يحدّث أيقونة البلاطة بإطار تلو الآخر: النظام لا يشغّل حركة الأيقونة بنفسه،
     * لذا نرسل كل إطار كصورة مستقلة بينما لوحة الإعدادات السريعة مفتوحة.
     */
    private inner class TileIconAnimation(private val activating: Boolean) : Runnable {

        private val frameCount = if (activating) {
            TileIconFrames.ACTIVATION_FRAMES
        } else {
            TileIconFrames.DEACTIVATION_FRAMES
        }
        private var index = 0

        override fun run() {
            val tile = qsTile
            if (tile == null) {
                tileAnimation = null
                return
            }
            val progress = if (frameCount > 1) index.toFloat() / (frameCount - 1) else 1f
            val frame = if (activating) {
                TileIconFrames.activation(progress)
            } else {
                TileIconFrames.deactivation(progress)
            }
            tile.icon = frame.toIcon(this@MonitoringTileService)
            tile.updateTile()

            index += 1
            if (index < frameCount) {
                tileAnimationHandler.postDelayed(this, TileIconFrames.FRAME_DELAY_MS)
            } else {
                // انتهت الحركة: أعد الأيقونة المتجهة الثابتة (أحدّ من الصورة النقطية).
                tileAnimation = null
                updateTileState()
            }
        }
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
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_network_monitor)
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
        private const val TAG = "MonitoringTileService"
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

private class TemporaryTileDialogShownException : RuntimeException()
