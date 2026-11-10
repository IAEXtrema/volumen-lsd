package com.psychovolume.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.psychoDataStore: DataStore<Preferences> by preferencesDataStore(name = "psycho_settings")

/** Preferencias visuales persistentes. */
data class VisualSettings(
    /** Intensidad de efectos (0.3 – 1.5). No cambia las fases, solo la fuerza de los efectos. */
    val intensity: Float = 1f,
    /** Velocidad global de animación (0.25 – 2). */
    val speed: Float = 1f,
    /** Multiplicador de partículas (0 – 2). */
    val particles: Float = 1f,
    /** Destellos de pantalla en la fase 5. */
    val flashes: Boolean = true,
    /** Limita a ~30 FPS y reduce partículas. */
    val batterySaver: Boolean = false,
    /** Accesibilidad: menos movimiento, sin destellos, caleidoscopio suave. */
    val reduceMotion: Boolean = false,
    /** Sensibilidad: qué tan rápido reacciona la animación al volumen (0 – 1). */
    val sensitivity: Float = 0.55f,
    /** Tamaño de la cápsula principal (0.6 – 1). */
    val capsuleScale: Float = 1f,
    /** Opacidad de la cápsula (0.35 – 1). */
    val opacity: Float = 1f,
    /** Controlador flotante activado. */
    val floating: Boolean = false,
    /** Índice de paleta cromática. */
    val palette: Int = 0,
)

class SettingsRepository private constructor(context: Context) {

    private val store = context.applicationContext.psychoDataStore

    private object Keys {
        val intensity = floatPreferencesKey("intensity")
        val speed = floatPreferencesKey("speed")
        val particles = floatPreferencesKey("particles")
        val flashes = booleanPreferencesKey("flashes")
        val batterySaver = booleanPreferencesKey("battery_saver")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val sensitivity = floatPreferencesKey("sensitivity")
        val capsuleScale = floatPreferencesKey("capsule_scale")
        val opacity = floatPreferencesKey("opacity")
        val floating = booleanPreferencesKey("floating")
        val palette = intPreferencesKey("palette")
    }

    private fun Preferences.toSettings(): VisualSettings {
        val d = VisualSettings()
        return VisualSettings(
            intensity = this[Keys.intensity] ?: d.intensity,
            speed = this[Keys.speed] ?: d.speed,
            particles = this[Keys.particles] ?: d.particles,
            flashes = this[Keys.flashes] ?: d.flashes,
            batterySaver = this[Keys.batterySaver] ?: d.batterySaver,
            reduceMotion = this[Keys.reduceMotion] ?: d.reduceMotion,
            sensitivity = this[Keys.sensitivity] ?: d.sensitivity,
            capsuleScale = this[Keys.capsuleScale] ?: d.capsuleScale,
            opacity = this[Keys.opacity] ?: d.opacity,
            floating = this[Keys.floating] ?: d.floating,
            palette = this[Keys.palette] ?: d.palette,
        )
    }

    val settings: Flow<VisualSettings> = store.data.map { it.toSettings() }.distinctUntilChanged()

    suspend fun update(transform: (VisualSettings) -> VisualSettings) {
        store.edit { p ->
            val n = transform(p.toSettings())
            p[Keys.intensity] = n.intensity
            p[Keys.speed] = n.speed
            p[Keys.particles] = n.particles
            p[Keys.flashes] = n.flashes
            p[Keys.batterySaver] = n.batterySaver
            p[Keys.reduceMotion] = n.reduceMotion
            p[Keys.sensitivity] = n.sensitivity
            p[Keys.capsuleScale] = n.capsuleScale
            p[Keys.opacity] = n.opacity
            p[Keys.floating] = n.floating
            p[Keys.palette] = n.palette
        }
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context).also { instance = it }
            }
    }
}
