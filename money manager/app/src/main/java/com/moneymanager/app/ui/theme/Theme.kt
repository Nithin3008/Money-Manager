package com.moneymanager.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.moneymanager.app.model.ThemeMode

var Navy950 = Color(0xFF000000)
var Navy900 = Color(0xFF111111)
var Navy850 = Color(0xFF1A1A1A)
var Navy800 = Color(0xFF242424)
var TextPrimary = Color(0xFFF5F5F5)
var TextMuted = Color(0xFFC9CDD6)
var TextDim = Color(0xFF8F95A3)
var PrimaryBlue = Color(0xFF4D8EFF)
var PrimarySoft = Color(0xFFAFC8FF)
var MoneyGreen = Color(0xFF45D69B)
var WarningAmber = Color(0xFFFFB169)
var LossRed = Color(0xFFFFB4AB)

private val AmoledColorScheme = darkColorScheme(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF242424),
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
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFFC9CDD6),
    outline = Color(0xFF41444C)
)

@Composable
fun MoneyManagerTheme(
    themeMode: ThemeMode = ThemeMode.Dark,
    content: @Composable () -> Unit
) {
    val dark = themeMode == ThemeMode.Dark
    applyThemeTokens(dark)

    MaterialTheme(
        colorScheme = if (dark) {
            AmoledColorScheme
        } else {
            lightColorScheme(
                background = Color(0xFFF7F8FD),
                surface = Color(0xFFF7F8FD),
                surfaceContainer = Color.White,
                surfaceContainerHigh = Color.White,
                primary = Color(0xFF0B63CE),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEAF1FF),
                onPrimaryContainer = Color(0xFF002B66),
                secondary = Color(0xFF006D45),
                onSecondary = Color.White,
                tertiary = Color(0xFF8A5200),
                error = Color(0xFFC01818),
                onBackground = Color(0xFF111827),
                onSurface = Color(0xFF111827),
                onSurfaceVariant = Color(0xFF4B5563),
                outline = Color(0xFFC8CDDA)
            )
        },
        typography = MoneyTypography,
        content = content
    )
}

private fun applyThemeTokens(dark: Boolean) {
    if (dark) {
        Navy950 = Color(0xFF000000)
        Navy900 = Color(0xFF111111)
        Navy850 = Color(0xFF1A1A1A)
        Navy800 = Color(0xFF242424)
        TextPrimary = Color(0xFFF5F5F5)
        TextMuted = Color(0xFFC9CDD6)
        TextDim = Color(0xFF8F95A3)
        PrimaryBlue = Color(0xFF4D8EFF)
        PrimarySoft = Color(0xFFAFC8FF)
        MoneyGreen = Color(0xFF45D69B)
        WarningAmber = Color(0xFFFFB169)
        LossRed = Color(0xFFFFB4AB)
    } else {
        Navy950 = Color(0xFFF7F8FD)
        Navy900 = Color(0xFFFFFFFF)
        Navy850 = Color(0xFFFFFFFF)
        Navy800 = Color(0xFFEFF3FB)
        TextPrimary = Color(0xFF111827)
        TextMuted = Color(0xFF4B5563)
        TextDim = Color(0xFF6B7280)
        PrimaryBlue = Color(0xFF0B63CE)
        PrimarySoft = Color(0xFF0B63CE)
        MoneyGreen = Color(0xFF007A4D)
        WarningAmber = Color(0xFFB25C00)
        LossRed = Color(0xFFC01818)
    }
}
