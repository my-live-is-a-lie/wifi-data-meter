package com.mywifiadata.app.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import com.mywifiadata.app.R
import com.mywifiadata.app.data.model.AppUsage
import com.mywifiadata.app.data.model.TrafficTotals
import java.io.ByteArrayOutputStream
import java.io.File

/** Capture while installed; keep names and small PNG icons after uninstall and process restarts. */
class AppIdentityCache(private val context: Context) {
    private val packageManager = context.packageManager
    // UIDs are device-local. Never restore this mapping from a backup made on another device.
    private val store = AppIdentityStore(File(context.noBackupFilesDir, "app-identities"))

    fun captureInstalledApps() {
        val apps = runCatching { packageManager.getInstalledApplications(0) }.getOrNull() ?: return
        val installedUids = apps.map { it.uid }.filter { it >= 0 }.toSet()
        // Re-query each UID rather than trusting an ApplicationInfo that may have gone stale
        // during a long scan. Do not hold a global lock while scanning the entire device.
        installedUids.forEach { uid -> runCatching { resolve(uid, TrafficTotals.Empty) } }
        // Catch removals that happened while our process was not alive.
        store.all().filter { it.isInstalled && it.uid !in installedUids }.forEach { record ->
            runCatching { resolve(record.uid, TrafficTotals.Empty) }
        }
    }

    @Synchronized
    fun packageRemoved(packageName: String) = store.markRemoved(packageName)

    @Synchronized
    fun packageChanged(packageName: String) {
        val info = runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull() ?: return
        resolve(info.uid, TrafficTotals.Empty)
    }

    @Synchronized
    fun resolve(uid: Int, totals: TrafficTotals): AppUsage {
        val previous = store.get(uid)
        val currentPackage = runCatching { packageManager.getPackagesForUid(uid)?.sorted()?.firstOrNull() }.getOrNull()
        if (currentPackage != null) {
            val live = runCatching { packageManager.getApplicationInfo(currentPackage, 0) }.getOrNull()
            if (live != null && live.uid == uid) return capture(live).asUsage(totals)
            // A known new owner must never inherit the old owner's name/icon on UID reuse.
            if (previous?.packageName != currentPackage) {
                val newOwner = CachedAppIdentity(uid, currentPackage, currentPackage, false, true, 0L)
                store.put(newOwner)
                return newOwner.asUsage(totals)
            }
        }
        if (previous != null) {
            try {
                val info = packageManager.getApplicationInfo(previous.packageName, 0)
                if (info.uid == uid) return capture(info).asUsage(totals)
                store.put(previous.copy(isInstalled = false)) // Reinstalled with a different UID.
            } catch (_: PackageManager.NameNotFoundException) {
                store.put(previous.copy(isInstalled = false))
            } catch (_: Exception) {
                // Keep the last known status on transient/security failures.
            }
            return (store.get(uid) ?: previous).asUsage(totals)
        }
        return AppUsage(uid, null, context.getString(R.string.unknown_app_label), true, totals)
    }

    /**
     * كل التطبيقات المثبّتة المعروفة محليًا، مرتّبة بالاسم.
     * تُستخدم لقائمة اختيار هدف النافذة العائمة حتى تظهر التطبيقات بلا استهلاك سابق.
     */
    @Synchronized
    fun installed(): List<AppUsage> =
        store.all()
            .filter { it.isInstalled && it.packageName.isNotBlank() }
            .map { identity -> identity.asUsage(TrafficTotals.Empty) }
            .sortedBy { it.label.lowercase() }

    @Synchronized
    fun clear() = store.clear()

    private fun capture(info: ApplicationInfo): CachedAppIdentity {
        val old = store.get(info.uid)?.takeIf { it.packageName == info.packageName }
        val updateTime = runCatching { packageManager.getPackageInfo(info.packageName, 0).lastUpdateTime }
            .getOrDefault(old?.lastUpdateTime ?: 0L)
        val label = runCatching { packageManager.getApplicationLabel(info).toString() }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: old?.label ?: info.packageName
        val record = CachedAppIdentity(
            uid = info.uid,
            packageName = info.packageName,
            label = label,
            isSystem = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            isInstalled = true,
            lastUpdateTime = updateTime,
        )
        if (old?.lastUpdateTime != updateTime || old?.isInstalled != true || !store.iconFile(record).isFile) {
            runCatching {
                val bitmap = packageManager.getApplicationIcon(info).toBitmap(width = 96, height = 96)
                val png = ByteArrayOutputStream().use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                    output.toByteArray()
                }
                store.writeIcon(record, png)
            }
        }
        store.put(record)
        return record
    }

    private fun CachedAppIdentity.asUsage(totals: TrafficTotals): AppUsage = AppUsage(
        uid = uid,
        packageName = packageName,
        label = label,
        isSystem = isSystem,
        totals = totals,
        isInstalled = isInstalled,
        cachedIconPath = store.iconFile(this).takeIf { it.isFile }?.absolutePath,
        iconVersion = lastUpdateTime,
    )
}
