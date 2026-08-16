package com.moneymanager.app.model

/**
 * The shared color library: one saved palette that feeds both the app accent and category
 * colors, so a color the user creates once is available in both places.
 */
object ColorLibrary {
    /** Seeded on first run; matches the colors categories already shipped with. */
    val defaultPalette: List<String> = listOf(
        "#7EA2FF", "#FF8A4C", "#B4F077", "#54D17A",
        "#3FE0C4", "#B794F6", "#FF6FAE", "#F5C542",
        "#FF6B4A", "#9AA7B5", "#C9A66B", "#2F5FD0",
        "#087F7A", "#6D28D9", "#138A4E"
    )

    /** Human label derived from hue, so saved colors read as names without a naming UI. */
    fun nameOf(hex: String): String {
        val argb = runCatching { android.graphics.Color.parseColor(hex) }.getOrNull()
            ?: return "Custom"
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        val (hue, sat, value) = Triple(hsv[0], hsv[1], hsv[2])
        if (value < 0.15f) return "Ink"
        if (sat < 0.12f) return if (value > 0.75f) "Chalk" else "Slate"
        return when (hue.toInt()) {
            in 0..14 -> "Coral"
            in 15..40 -> "Ember"
            in 41..54 -> "Sand"
            in 55..69 -> "Amber"
            in 70..95 -> "Lime"
            in 96..150 -> "Fern"
            in 151..175 -> "Teal"
            in 176..200 -> "Aqua"
            in 201..240 -> "Sky"
            in 241..275 -> "Indigo"
            in 276..300 -> "Lilac"
            in 301..330 -> "Orchid"
            else -> "Rose"
        }
    }

    fun normalize(hex: String): String = hex.trim().uppercase().let {
        if (it.startsWith("#")) it else "#$it"
    }
}
