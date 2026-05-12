package com.moneymanager.app.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moneymanager.app.model.ThemeMode

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

private val ExpressiveDarkColorScheme = darkColorScheme(
    background = Color(0xFF0C0F17),
    surface = Color(0xFF0C0F17),
    surfaceContainerLowest = Color(0xFF090B10),
    surfaceContainerLow = Color(0xFF111620),
    surfaceContainer = Color(0xFF171D2A),
    surfaceContainerHigh = Color(0xFF202737),
    surfaceContainerHighest = Color(0xFF2A3345),
    primary = Color(0xFFAFC6FF),
    onPrimary = Color(0xFF10295C),
    primaryContainer = Color(0xFF29477F),
    onPrimaryContainer = Color(0xFFD9E4FF),
    secondary = Color(0xFF61E6A4),
    onSecondary = Color(0xFF003823),
    secondaryContainer = Color(0xFF155C3D),
    onSecondaryContainer = Color(0xFFB8FFD7),
    tertiary = Color(0xFFFFD166),
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF6A4500),
    onTertiaryContainer = Color(0xFFFFE2A3),
    error = Color(0xFFFF7A8A),
    onError = Color(0xFF69000B),
    errorContainer = Color(0xFF8E1A28),
    onErrorContainer = Color(0xFFFFDADF),
    onBackground = Color(0xFFF8FAFF),
    onSurface = Color(0xFFF8FAFF),
    onSurfaceVariant = Color(0xFFC7CEDC),
    outline = Color(0xFF596274),
    outlineVariant = Color(0xFF343D4E)
)

private val ExpressiveLightColorScheme = lightColorScheme(
    background = Color(0xFFF8F9FF),
    surface = Color(0xFFF8F9FF),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F4FF),
    surfaceContainer = Color(0xFFEAF0FC),
    surfaceContainerHigh = Color(0xFFE3EAF8),
    surfaceContainerHighest = Color(0xFFDCE4F3),
    primary = Color(0xFF345CA8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E4FF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF006C49),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB7F4D0),
    onSecondaryContainer = Color(0xFF002114),
    tertiary = Color(0xFF885200),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDFA2),
    onTertiaryContainer = Color(0xFF2B1700),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    onBackground = Color(0xFF151923),
    onSurface = Color(0xFF151923),
    onSurfaceVariant = Color(0xFF4A5568),
    outline = Color(0xFF747C8C),
    outlineVariant = Color(0xFFC8D0DF)
)

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
    content: @Composable () -> Unit
) {
    val dark = themeMode == ThemeMode.Dark
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> ExpressiveDarkColorScheme
        else -> ExpressiveLightColorScheme
    }
    applyThemeTokens(dark)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MoneyTypography,
        shapes = ExpressiveShapes,
        content = content
    )
}

private fun applyThemeTokens(dark: Boolean) {
    if (dark) {
        Navy950 = Color(0xFF0C0F17)
        Navy900 = Color(0xFF111620)
        Navy850 = Color(0xFF171D2A)
        Navy800 = Color(0xFF202737)
        TextPrimary = Color(0xFFF8FAFF)
        TextMuted = Color(0xFFC7CEDC)
        TextDim = Color(0xFF94A0B4)
        PrimaryBlue = Color(0xFF7C9DFF)
        PrimarySoft = Color(0xFFAFC6FF)
        MoneyGreen = Color(0xFF61E6A4)
        WarningAmber = Color(0xFFFFD166)
        LossRed = Color(0xFFFF7A8A)
    } else {
        Navy950 = Color(0xFFF8F9FF)
        Navy900 = Color(0xFFFFFFFF)
        Navy850 = Color(0xFFF0F4FF)
        Navy800 = Color(0xFFE3EAF8)
        TextPrimary = Color(0xFF151923)
        TextMuted = Color(0xFF4A5568)
        TextDim = Color(0xFF6D7688)
        PrimaryBlue = Color(0xFF345CA8)
        PrimarySoft = Color(0xFF1F65C8)
        MoneyGreen = Color(0xFF00875A)
        WarningAmber = Color(0xFFC87400)
        LossRed = Color(0xFFC43A4E)
    }
}
