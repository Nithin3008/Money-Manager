package com.moneymanager.app.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

/**
 * Widget colors from the MMWidgets design, as day/night providers. Widgets follow the
 * SYSTEM light/dark setting (Glance cannot read the in-app theme picker).
 */
internal object WidgetPalette {
    val bg = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1A1A1A))
    val panel = ColorProvider(day = Color(0xFFEFF6E8), night = Color(0xFF242424))
    val textPrimary = ColorProvider(day = Color(0xFF141414), night = Color(0xFFF1F1F1))
    val textMuted = ColorProvider(day = Color(0xFF515151), night = Color(0xFFC9C9C9))
    val textDim = ColorProvider(day = Color(0xFF737373), night = Color(0xFF8C8C8C))
    val green = ColorProvider(day = Color(0xFF4E7D1C), night = Color(0xFFB4F077))
    val red = ColorProvider(day = Color(0xFFD94A2B), night = Color(0xFFFF6B4A))
    val accent = ColorProvider(day = Color(0xFF4E7D1C), night = Color(0xFFB4F077))
    val accentTint = ColorProvider(
        day = Color(0xFF4E7D1C).copy(alpha = 0.12f),
        night = Color(0xFFB4F077).copy(alpha = 0.16f)
    )
}
