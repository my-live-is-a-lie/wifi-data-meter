package com.mywifiadata.app.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * فتح شاشات النظام ذات الصلة (صلاحية الاستخدام، إعدادات الواي فاي، إعدادات الإشعارات).
 */
object SystemActions {

    /** شاشة «الوصول إلى الاستخدام» التي تُمنح منها صلاحية قراءة إحصاءات الشبكة. */
    fun openUsageAccessSettings(context: Context) {
        start(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    /** إعدادات الواي فاي. */
    fun openWifiSettings(context: Context) {
        start(context, Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    /** إعدادات تطبيقنا في النظام (للإشعارات والصلاحيات). */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", context.packageName, null))
        start(context, intent)
    }

    private fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (exception: ActivityNotFoundException) {
            // لا توجد شاشة مطابقة على هذا الجهاز — نتجاهل بهدوء.
        }
    }
}
