package com.psychovolume.app.engine

import androidx.compose.ui.graphics.Color

data class PsychoPalette(
    val name: String,
    val pink: Color,
    val purple: Color,
    val yellow: Color,
    val green: Color,
    val turquoise: Color,
    val orange: Color,
    val blue: Color,
    /** Color del contorno tipo sticker */
    val ink: Color,
    /** Cielo nocturno de la fase 1 */
    val night: Color,
) {
    val rainbow: List<Color> = listOf(pink, orange, yellow, green, turquoise, blue, purple)
}

object Palettes {
    val all: List<PsychoPalette> = listOf(
        PsychoPalette(
            name = "Neón ácido",
            pink = Color(0xFFFF2BD6), purple = Color(0xFF9B3DFF), yellow = Color(0xFFFFE83B),
            green = Color(0xFFA6FF2E), turquoise = Color(0xFF1EE8D8), orange = Color(0xFFFF8A1F),
            blue = Color(0xFF2F7BFF), ink = Color(0xFF2A0B45), night = Color(0xFF140A2E),
        ),
        PsychoPalette(
            name = "Atardecer cósmico",
            pink = Color(0xFFFF4F8B), purple = Color(0xFF7A2BD9), yellow = Color(0xFFFFC93B),
            green = Color(0xFF6CE38A), turquoise = Color(0xFF3BD1E3), orange = Color(0xFFFF6A2B),
            blue = Color(0xFF3B5BFF), ink = Color(0xFF2B0A2A), night = Color(0xFF1E0B24),
        ),
        PsychoPalette(
            name = "Bosque mágico",
            pink = Color(0xFFF25CC8), purple = Color(0xFF6E44FF), yellow = Color(0xFFF7F052),
            green = Color(0xFF5CFF5C), turquoise = Color(0xFF00D9A5), orange = Color(0xFFFFA62B),
            blue = Color(0xFF1FA2FF), ink = Color(0xFF0E2A1F), night = Color(0xFF07170F),
        ),
        PsychoPalette(
            name = "Algodón de azúcar",
            pink = Color(0xFFFF8FD8), purple = Color(0xFFB59CFF), yellow = Color(0xFFFFF59C),
            green = Color(0xFFC2FFA8), turquoise = Color(0xFF9CF5F0), orange = Color(0xFFFFC59C),
            blue = Color(0xFF9CC8FF), ink = Color(0xFF4A2A6A), night = Color(0xFF2A2245),
        ),
    )

    fun get(index: Int): PsychoPalette = all[index.coerceIn(0, all.lastIndex)]
}
