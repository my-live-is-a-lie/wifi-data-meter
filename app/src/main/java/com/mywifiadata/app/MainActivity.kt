package com.mywifiadata.app

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
import com.mywifiadata.app.data.model.ThemeMode
import com.mywifiadata.app.ui.AppViewModel
import com.mywifiadata.app.ui.screens.MainScreen
import com.mywifiadata.app.ui.theme.AppTheme
import com.mywifiadata.app.widget.UsageWidgetProvider

/**
 * الشاشة الوحيدة في التطبيق: تستضيف واجهة Compose بكاملها.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as MyWifiApplication).repository
        // فتح الأداة اختيار صريح لشكلها؛ أما التشغيل العادي فيستعيد القيمة دون الكتابة فوقها.
        val hasWidgetStyle = intent.hasExtra(UsageWidgetProvider.EXTRA_OPEN_USAGE_CARD_STYLE)
        if (savedInstanceState == null && hasWidgetStyle) {
            val openedStyle = intent.getIntExtra(
                UsageWidgetProvider.EXTRA_OPEN_USAGE_CARD_STYLE,
                UsageWidgetProvider.DEFAULT_USAGE_CARD_STYLE,
            ).coerceIn(
                UsageWidgetProvider.DEFAULT_USAGE_CARD_STYLE,
                UsageWidgetProvider.MAX_USAGE_CARD_STYLE_INDEX,
            )
            repository.settingsStore.update { it.copy(usageCardStyleIndex = openedStyle) }
        }

        setContent {
            val settings by repository.settingsStore.settings.collectAsStateWithLifecycle()
            val configuration = LocalConfiguration.current
            val language = configuration.locales[0].language

            // النصوص الافتراضية عربية، لذا نفرض اتجاهًا من اليمين لليسار
            // في كل اللغات عدا الإنجليزية.
            val layoutDirection = when (language) {
                "en" -> LayoutDirection.Ltr
                else -> LayoutDirection.Rtl
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
