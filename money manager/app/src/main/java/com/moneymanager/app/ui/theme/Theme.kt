package com.moneymanager.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moneymanager.app.model.ThemeMode
import com.moneymanager.app.model.UiAccent
import com.moneymanager.app.model.UiSurface

var Navy950 = Color(0xFF000000)
var Navy900 = Color(0xFF111111)
var Navy850 = Color(0xFF1A1A1A)
var Navy800 = Color(0xFF242424)
var TextPrimary = Color(0xFFF5F5F5)
var TextMuted = Color(0xFFC9CDD6)
var TextDim = Color(0xFF8F95A3)
var PrimaryBlue = Color(0xFF7C8CFF)
var PrimarySoft = Color(0xFFAFD2FF)
var MoneyGreen = Color(0xFF38E68B)
var WarningAmber = Color(0xFFFFC857)
var LossRed = Color(0xFFFF6B7A)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun MoneyManagerTheme(
    themeMode: ThemeMode = ThemeMode.Dark,
    uiAccent: UiAccent = UiAccent.Sky,
    uiSurface: UiSurface = UiSurface.Midnight,
    content: @Composable () -> Unit
) {
    val dark = themeMode == ThemeMode.Dark
    applyThemeTokens(dark, uiAccent, uiSurface)
    val colorScheme = expressiveColorScheme(dark)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MoneyTypography,
        shapes = ExpressiveShapes,
        content = content
    )
}

private fun expressiveColorScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        background = Navy950,
        surface = Navy950,
        surfaceContainerLowest = Navy950,
        surfaceContainerLow = Navy900,
        surfaceContainer = Navy850,
        surfaceContainerHigh = Navy800,
        surfaceContainerHighest = Navy800,
        primary = PrimarySoft,
        onPrimary = Color(0xFF10295C),
        primaryContainer = PrimaryBlue,
        onPrimaryContainer = Color(0xFFEAF1FF),
        secondary = MoneyGreen,
        onSecondary = Color(0xFF003823),
        tertiary = WarningAmber,
        error = LossRed,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        onSurfaceVariant = TextMuted,
        outline = TextDim,
        outlineVariant = Navy800
    )
} else {
    lightColorScheme(
        background = Navy950,
        surface = Navy950,
        surfaceContainerLowest = Navy900,
        surfaceContainerLow = Navy850,
        surfaceContainer = Navy850,
        surfaceContainerHigh = Navy800,
        surfaceContainerHighest = Navy800,
        primary = PrimaryBlue,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDDE8FF),
        onPrimaryContainer = Color(0xFF001B3F),
        secondary = MoneyGreen,
        onSecondary = Color.White,
        tertiary = WarningAmber,
        error = LossRed,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        onSurfaceVariant = TextMuted,
        outline = TextDim,
        outlineVariant = Navy800
    )
}

private fun applyThemeTokens(dark: Boolean, uiAccent: UiAccent, uiSurface: UiSurface) {
    if (dark) {
        Navy950 = uiSurface.darkBackgroundHex.toColorOr(Color(0xFF0C0F17))
        Navy900 = uiSurface.darkCardHex.toColorOr(Color(0xFF111620))
        Navy850 = uiSurface.darkPanelHex.toColorOr(Color(0xFF171D2A))
        Navy800 = uiSurface.darkChipHex.toColorOr(Color(0xFF202737))
        TextPrimary = Color(0xFFF8FAFF)
        TextMuted = Color(0xFFC7CEDC)
        TextDim = Color(0xFF94A0B4)
        PrimaryBlue = uiAccent.darkHex.toColorOr(Color(0xFF7C9DFF))
        PrimarySoft = uiAccent.softDarkHex.toColorOr(Color(0xFFAFC6FF))
        MoneyGreen = Color(0xFF61E6A4)
        WarningAmber = Color(0xFFFFD166)
        LossRed = Color(0xFFFF7A8A)
    } else {
        Navy950 = uiSurface.lightBackgroundHex.toColorOr(Color(0xFFF8F9FF))
        Navy900 = uiSurface.lightCardHex.toColorOr(Color.White)
        Navy850 = uiSurface.lightPanelHex.toColorOr(Color(0xFFF0F4FF))
        Navy800 = uiSurface.lightChipHex.toColorOr(Color(0xFFE3EAF8))
        TextPrimary = Color(0xFF151923)
        TextMuted = Color(0xFF4A5568)
        TextDim = Color(0xFF6D7688)
        PrimaryBlue = uiAccent.lightHex.toColorOr(Color(0xFF345CA8))
        PrimarySoft = uiAccent.softLightHex.toColorOr(Color(0xFF1F65C8))
        MoneyGreen = Color(0xFF00875A)
        WarningAmber = Color(0xFFC87400)
        LossRed = Color(0xFFC43A4E)
    }
}

private fun String.toColorOr(fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(fallback)
}
