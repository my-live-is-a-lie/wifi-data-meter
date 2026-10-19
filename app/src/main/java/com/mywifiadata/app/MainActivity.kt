package com.mywifiadata.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mywifiadata.app.core.AppLanguage
import com.mywifiadata.app.core.LauncherIconVisibility
import com.mywifiadata.app.data.model.AppLayoutDirection
import com.mywifiadata.app.data.model.ThemeMode
import com.mywifiadata.app.ui.AppViewModel
import com.mywifiadata.app.ui.screens.MainScreen
import com.mywifiadata.app.ui.theme.AppTheme
import com.mywifiadata.app.widget.UsageWidgetUpdater

/**
 * الشاشة الوحيدة في التطبيق: تستضيف واجهة Compose بكاملها.
 */
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.localizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as MyWifiApplication).repository
        LauncherIconVisibility.setVisible(
            this,
            visible = !repository.settingsStore.current.appIconHidden,
        )
        // الأداة تتبع شكل البطاقة المختار؛ نزامنه عند فتح التطبيق أيضًا حتى لو
        // أُضيفت الأداة من قائمة الأدوات دون المرور بزر «إضافة أداة».
        UsageWidgetUpdater.syncStyle(this, repository.settingsStore.current.usageCardStyleIndex)
        setContent {
            val settings by repository.settingsStore.settings.collectAsStateWithLifecycle()
            val configuration = LocalConfiguration.current
            val language = configuration.locales[0].language

            // اتجاه العرض: يتبع الاختيار الصريح إن وُجد، وإلا فحسب لغة التطبيق الحالية.
            val layoutDirection = when (settings.layoutDirection) {
                AppLayoutDirection.RTL -> LayoutDirection.Rtl
                AppLayoutDirection.LTR -> LayoutDirection.Ltr
                AppLayoutDirection.AUTO -> when (language) {
                    "en" -> LayoutDirection.Ltr
                    else -> LayoutDirection.Rtl
                }
            }

            // سمة التطبيق: النظام / فاتح / داكن / مظلم (أسود AMOLED).
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.BLACK -> true
            }

            AppTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColorEnabled,
                amoled = settings.themeMode == ThemeMode.BLACK,
                accentColor = settings.accentColor,
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        val viewModel: AppViewModel = viewModel(
                            factory = AppViewModel.factory(repository),
                        )
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
