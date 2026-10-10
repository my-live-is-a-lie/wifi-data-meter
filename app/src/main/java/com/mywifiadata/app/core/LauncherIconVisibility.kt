package com.mywifiadata.app.core

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Controls the launcher-only activity alias without disabling MainActivity or the QS entry point. */
object LauncherIconVisibility {

    private const val LAUNCHER_ALIAS_SUFFIX = ".LauncherActivityAlias"

    /** Returns false if the package manager could not update the launcher alias. */
    fun setVisible(context: Context, visible: Boolean): Boolean = runCatching {
        val packageManager = context.packageManager
        val alias = ComponentName(
            context.packageName,
            context.packageName + LAUNCHER_ALIAS_SUFFIX,
        )
        val currentState = packageManager.getComponentEnabledSetting(alias)
        val currentlyVisible = currentState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED &&
            currentState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER &&
            currentState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED

        if (currentlyVisible != visible) {
            packageManager.setComponentEnabledSetting(
                alias,
                if (visible) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP,
            )
        }
    }.isSuccess
}
