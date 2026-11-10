package com.psychovolume.app.engine

import com.psychovolume.app.data.VisualSettings

/** Parámetros de render ya resueltos (ajustes del usuario + accesibilidad del sistema). */
data class RenderParams(
    val intensity: Float = 1f,
    val speed: Float = 1f,
    val particles: Float = 1f,
    val flashes: Boolean = true,
    val batterySaver: Boolean = false,
    val reduceMotion: Boolean = false,
    val sensitivity: Float = 0.55f,
    val paletteIndex: Int = 0,
) {
    companion object {
        fun from(s: VisualSettings, systemReduceMotion: Boolean): RenderParams {
            val reduce = s.reduceMotion || systemReduceMotion
            return RenderParams(
                intensity = if (reduce) minOf(s.intensity, 0.6f) else s.intensity,
                speed = if (reduce) s.speed * 0.5f else s.speed,
                particles = if (s.batterySaver) s.particles * 0.45f else s.particles,
                flashes = s.flashes && !reduce,
                batterySaver = s.batterySaver,
                reduceMotion = reduce,
                sensitivity = s.sensitivity,
                paletteIndex = s.palette,
            )
        }
    }
}
