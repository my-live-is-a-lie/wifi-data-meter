package com.mywifiadata.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** ألوان دلالية إضافية: التنزيل والرفع. */
val LocalDownloadColor = staticCompositionLocalOf { Color(0xFF3F51D8) }
val LocalUploadColor = staticCompositionLocalOf { Color(0xFF9C27B0) }

/**
 * ثيم التطبيق المبني على Material 3 Expressive.
 *
 * - يختار مخطط الألوان الديناميكي من خلفية الشاشة عند توفره (أندرويد 12+) إن كان مُفعّلًا.
 * - يستخدم [expressiveLightColorScheme] في الوضع الفاتح و [darkColorScheme] في الوضع الداكن.
 * - عند تفعيل [amoled] في الوضع الداكن تصبح الخلفيات سوداء بالكامل (وضع «مظلم»).
 * - يمرّر [MotionScheme.expressive] لحركات أكثر حيوية.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val baseScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme()
        else -> expressiveLightColorScheme()
    }

    // وضع «مظلم» (AMOLED): أسود بالكامل للخلفيات مع تدرّجات شبه سوداء للتباين.
    val colorScheme: ColorScheme = if (amoled && darkTheme) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceVariant = Color(0xFF121212),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF050505),
            surfaceContainer = Color(0xFF0A0A0A),
            surfaceContainerHigh = Color(0xFF101010),
            surfaceContainerHighest = Color(0xFF161616),
        )
    } else {
        baseScheme
    }

    val downloadColor = if (darkTheme) Color(0xFFB7C4FF) else Color(0xFF2A3EB1)
    val uploadColor = if (darkTheme) Color(0xFFE3C4F5) else Color(0xFF7B1FA2)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalDownloadColor provides downloadColor,
        LocalUploadColor provides uploadColor,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = AppShapes,
            typography = AppTypography,
            content = content,
        )
    }
}
