package com.moneymanager.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy950 = Color(0xFF0B1326)
val Navy900 = Color(0xFF131B2E)
val Navy850 = Color(0xFF171F33)
val Navy800 = Color(0xFF222A3D)
val TextPrimary = Color(0xFFDAE2FD)
val TextMuted = Color(0xFFC2C6D6)
val TextDim = Color(0xFF8C909F)
val PrimaryBlue = Color(0xFF4D8EFF)
val PrimarySoft = Color(0xFFADC6FF)
val MoneyGreen = Color(0xFF4EDEA3)
val WarningAmber = Color(0xFFFFB95F)
val LossRed = Color(0xFFFFB4AB)

private val ColorScheme = darkColorScheme(
    background = Navy950,
    surface = Navy950,
    surfaceContainer = Navy850,
    surfaceContainerHigh = Navy800,
    primary = PrimaryBlue,
    onPrimary = Color(0xFF002E6A),
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = Color(0xFF00285D),
    secondary = MoneyGreen,
    onSecondary = Color(0xFF003824),
    tertiary = WarningAmber,
    onTertiary = Color(0xFF472A00),
    error = LossRed,
    onError = Color(0xFF690005),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextMuted,
    outline = Color(0xFF424754)
)

@Composable
fun MoneyManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = MoneyTypography,
        content = content
    )
}
