package com.moneymanager.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.moneymanager.app.model.ThemeMode
import com.moneymanager.app.model.UiAccent
import com.moneymanager.app.model.UiSurface

var Navy950 = Color(0xFF141414)
var Navy900 = Color(0xFF1A1A1A)
var Navy850 = Color(0xFF202020)
var Navy800 = Color(0xFF2A2A2A)
var TextPrimary = Color(0xFFF1F1F1)
var TextMuted = Color(0xFFC9C9C9)
var TextDim = Color(0xFF8C8C8C)
var PrimaryBlue = Color(0xFFB4F077)
var PrimarySoft = Color(0xFFB4F077)
var MoneyGreen = Color(0xFFB4F077)
var WarningAmber = Color(0xFFFFC857)
var LossRed = Color(0xFFFF6B4A)
var TransferBlue = Color(0xFF7EA2FF)
var OnAccent = Color(0xFF16220A)
var LineColor = Color(0xFF2A2A2A)
var NavSolid = Color(0xFF242424)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MoneyManagerTheme(
    themeMode: ThemeMode = ThemeMode.Dark,
    uiAccent: UiAccent = UiAccent.Sky,
    uiSurface: UiSurface = UiSurface.Midnight,
    customAccentHex: String? = null,
    content: @Composable () -> Unit
) {
    val dark = themeMode == ThemeMode.Dark
    applyThemeTokens(dark, uiAccent, uiSurface)
    customAccentHex?.let { applyCustomAccent(it) }
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
        primary = PrimaryBlue,
        onPrimary = OnAccent,
        primaryContainer = PrimaryBlue,
        onPrimaryContainer = OnAccent,
        secondary = MoneyGreen,
        onSecondary = Color(0xFF141414),
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
        onPrimary = OnAccent,
        primaryContainer = PrimarySoft,
        onPrimaryContainer = TextPrimary,
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
        Navy950 = uiSurface.darkBackgroundHex.toColorOr(Color(0xFF141414))
        Navy900 = uiSurface.darkCardHex.toColorOr(Color(0xFF1A1A1A))
        Navy850 = uiSurface.darkPanelHex.toColorOr(Color(0xFF202020))
        Navy800 = uiSurface.darkChipHex.toColorOr(Color(0xFF2A2A2A))
        TextPrimary = Color(0xFFF1F1F1)
        TextMuted = Color(0xFFC9C9C9)
        TextDim = Color(0xFF8C8C8C)
        PrimaryBlue = uiAccent.darkHex.toColorOr(Color(0xFFB4F077))
        PrimarySoft = uiAccent.softDarkHex.toColorOr(Color(0xFFB4F077))
        MoneyGreen = Color(0xFFB4F077)
        WarningAmber = Color(0xFFFFD166)
        LossRed = Color(0xFFFF6B4A)
        TransferBlue = Color(0xFF7EA2FF)
        OnAccent = uiAccent.onDarkHex.toColorOr(Color(0xFF16220A))
        LineColor = Navy800
        NavSolid = lerp(Navy850, Navy800, 0.4f)
    } else {
        Navy950 = uiSurface.lightBackgroundHex.toColorOr(Color(0xFFF7FAF2))
        Navy900 = uiSurface.lightCardHex.toColorOr(Color.White)
        Navy850 = uiSurface.lightPanelHex.toColorOr(Color(0xFFEFF6E8))
        Navy800 = uiSurface.lightChipHex.toColorOr(Color(0xFFE1ECD8))
        TextPrimary = Color(0xFF141414)
        TextMuted = Color(0xFF515151)
        TextDim = Color(0xFF737373)
        PrimaryBlue = uiAccent.lightHex.toColorOr(Color(0xFF4E7D1C))
        PrimarySoft = uiAccent.softLightHex.toColorOr(Color(0xFF4E7D1C))
        MoneyGreen = Color(0xFF4E7D1C)
        WarningAmber = Color(0xFFC87400)
        LossRed = Color(0xFFD94A2B)
        TransferBlue = Color(0xFF3B6FD4)
        OnAccent = uiAccent.onLightHex.toColorOr(Color(0xFFF4FBEA))
        LineColor = lerp(Navy800, Navy850, 0.35f)
        NavSolid = Navy900
    }
}

private fun String.toColorOr(fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(fallback)
}

/**
 * Overrides the accent tokens with a user-picked color, deriving the on-accent ink from
 * luminance (dark ink on light accents, white on dark ones) and a soft tint for containers.
 */
private fun applyCustomAccent(hex: String) {
    val accent = hex.toColorOr(PrimaryBlue)
    PrimaryBlue = accent
    PrimarySoft = accent
    OnAccent = if (accent.luminance() > 0.42f) Color(0xFF10131A) else Color(0xFFFFFFFF)
}
