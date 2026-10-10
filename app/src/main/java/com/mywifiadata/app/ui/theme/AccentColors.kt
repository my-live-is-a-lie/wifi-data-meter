package com.mywifiadata.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.mywifiadata.app.data.model.AccentColor

/** الأدوار اللونية التي تتغير مع لون التمييز المختار. */
internal data class AccentPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
)

private data class AccentColorVariants(
    val light: AccentPalette,
    val dark: AccentPalette,
    val swatch: Color,
)

private fun accentVariants(
    lightPrimary: Color,
    lightContainer: Color,
    lightOnContainer: Color,
    darkPrimary: Color,
    darkOnPrimary: Color,
    darkContainer: Color,
    darkOnContainer: Color,
): AccentColorVariants = AccentColorVariants(
    light = AccentPalette(
        primary = lightPrimary,
        onPrimary = Color.White,
        primaryContainer = lightContainer,
        onPrimaryContainer = lightOnContainer,
        inversePrimary = darkPrimary,
    ),
    dark = AccentPalette(
        primary = darkPrimary,
        onPrimary = darkOnPrimary,
        primaryContainer = darkContainer,
        onPrimaryContainer = darkOnContainer,
        inversePrimary = lightPrimary,
    ),
    swatch = lightPrimary,
)

private val accentColorVariants = mapOf(
    AccentColor.INDIGO to accentVariants(
        lightPrimary = Color(0xFF3949AB),
        lightContainer = Color(0xFFE0E3FF),
        lightOnContainer = Color(0xFF111B50),
        darkPrimary = Color(0xFFB7C4FF),
        darkOnPrimary = Color(0xFF172457),
        darkContainer = Color(0xFF303F78),
        darkOnContainer = Color(0xFFDDE2FF),
    ),
    AccentColor.BLUE to accentVariants(
        lightPrimary = Color(0xFF1565C0),
        lightContainer = Color(0xFFD1E4FF),
        lightOnContainer = Color(0xFF001C3A),
        darkPrimary = Color(0xFF90CAF9),
        darkOnPrimary = Color(0xFF003258),
        darkContainer = Color(0xFF004A78),
        darkOnContainer = Color(0xFFCFE5FF),
    ),
    AccentColor.CYAN to accentVariants(
        lightPrimary = Color(0xFF007C91),
        lightContainer = Color(0xFFB9EDFA),
        lightOnContainer = Color(0xFF001F25),
        darkPrimary = Color(0xFF80DEEA),
        darkOnPrimary = Color(0xFF00363D),
        darkContainer = Color(0xFF004F56),
        darkOnContainer = Color(0xFFB9F3FA),
    ),
    AccentColor.TEAL to accentVariants(
        lightPrimary = Color(0xFF00796B),
        lightContainer = Color(0xFFB2DFDB),
        lightOnContainer = Color(0xFF00201C),
        darkPrimary = Color(0xFF80CBC4),
        darkOnPrimary = Color(0xFF003731),
        darkContainer = Color(0xFF005047),
        darkOnContainer = Color(0xFFA2F2E7),
    ),
    AccentColor.GREEN to accentVariants(
        lightPrimary = Color(0xFF2E7D32),
        lightContainer = Color(0xFFC8E6C9),
        lightOnContainer = Color(0xFF00210A),
        darkPrimary = Color(0xFFA5D6A7),
        darkOnPrimary = Color(0xFF123B17),
        darkContainer = Color(0xFF23512A),
        darkOnContainer = Color(0xFFC3E9C2),
    ),
    AccentColor.AMBER to accentVariants(
        lightPrimary = Color(0xFF9A5C00),
        lightContainer = Color(0xFFFFE0B2),
        lightOnContainer = Color(0xFF291700),
        darkPrimary = Color(0xFFFFCA72),
        darkOnPrimary = Color(0xFF472900),
        darkContainer = Color(0xFF674000),
        darkOnContainer = Color(0xFFFFE0B2),
    ),
    AccentColor.ORANGE to accentVariants(
        lightPrimary = Color(0xFFB23C00),
        lightContainer = Color(0xFFFFDBCA),
        lightOnContainer = Color(0xFF371200),
        darkPrimary = Color(0xFFFFB680),
        darkOnPrimary = Color(0xFF542000),
        darkContainer = Color(0xFF793500),
        darkOnContainer = Color(0xFFFFDCC6),
    ),
    AccentColor.RED to accentVariants(
        lightPrimary = Color(0xFFB3261E),
        lightContainer = Color(0xFFFFDAD6),
        lightOnContainer = Color(0xFF410002),
        darkPrimary = Color(0xFFF2B8B5),
        darkOnPrimary = Color(0xFF601410),
        darkContainer = Color(0xFF7A2822),
        darkOnContainer = Color(0xFFFFDAD6),
    ),
    AccentColor.PINK to accentVariants(
        lightPrimary = Color(0xFFAD1457),
        lightContainer = Color(0xFFFFD9E2),
        lightOnContainer = Color(0xFF3E001D),
        darkPrimary = Color(0xFFFFB1C8),
        darkOnPrimary = Color(0xFF5A1132),
        darkContainer = Color(0xFF6D1E3F),
        darkOnContainer = Color(0xFFFFD9E2),
    ),
    AccentColor.PURPLE to accentVariants(
        lightPrimary = Color(0xFF6A1B9A),
        lightContainer = Color(0xFFEAD7F2),
        lightOnContainer = Color(0xFF2E004F),
        darkPrimary = Color(0xFFE1BEE7),
        darkOnPrimary = Color(0xFF461D5C),
        darkContainer = Color(0xFF603768),
        darkOnContainer = Color(0xFFF5D9FB),
    ),
)

private val defaultSwatch = Color(0xFF5263B8)

internal fun AccentColor.palette(darkTheme: Boolean): AccentPalette? =
    accentColorVariants[this]?.let { variants -> if (darkTheme) variants.dark else variants.light }

/** اللون الظاهر في لوحة الاختيار؛ يظل ثابتًا في الوضعين الفاتح والداكن. */
internal fun AccentColor.swatchColor(): Color =
    accentColorVariants[this]?.swatch ?: defaultSwatch

internal fun ColorScheme.withAccentColor(accentColor: AccentColor, darkTheme: Boolean): ColorScheme {
    val palette = accentColor.palette(darkTheme) ?: return this
    return copy(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = palette.onPrimaryContainer,
        inversePrimary = palette.inversePrimary,
    )
}
