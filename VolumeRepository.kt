package com.psychovolume.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/** Volumen multimedia actual del dispositivo. */
data class VolumeState(
    val index: Int,
    val min: Int,
    val max: Int,
    val fixed: Boolean,
) {
    /** Volumen normalizado entre 0.0 y 1.0 */
    val fraction: Float
        get() = if (max <= min) 0f else ((index - min).toFloat() / (max - min)).coerceIn(0f, 1f)
}

/**
 * Observa el volumen de STREAM_MUSIC con dos mecanismos complementarios:
 *  1. ContentObserver sobre Settings.System: el framework escribe ahí cada cambio de volumen
 *     (botones físicos, panel del sistema, otras apps).
 *  2. Broadcast "android.media.VOLUME_CHANGED_ACTION": llega antes en muchos fabricantes.
 * Ambos solo disparan una relectura de AudioManager, que es la fuente de verdad, de modo que un
 * broadcast falso no puede inyectar valores.
 *
 * Se usa por conteo de referencias: la Activity y el servicio flotante llaman acquire()/release().
 */
class VolumeRepository private constructor(context: Context) {

    private val app = context.applicationContext
    private val audio: AudioManager = app.getSystemService(AudioManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(read())
    val state: StateFlow<VolumeState> = _state.asStateFlow()

    private var users = 0

    private val observer = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) = refresh()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refresh()
    }

    private fun read(): VolumeState = VolumeState(
        index = audio.getStreamVolume(STREAM),
        min = audio.getStreamMinVolume(STREAM),
        max = audio.getStreamMaxVolume(STREAM),
        fixed = audio.isVolumeFixed,
    )

    fun refresh() {
        _state.value = read()
    }

    @MainThread
    fun acquire() {
        if (users++ == 0) {
            app.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
            ContextCompat.registerReceiver(
                app, receiver, IntentFilter(ACTION_VOLUME_CHANGED), ContextCompat.RECEIVER_EXPORTED
            )
        }
        refresh()
    }

    @MainThread
    fun release() {
        if (users <= 0) return
        if (--users == 0) {
            app.contentResolver.unregisterContentObserver(observer)
            runCatching { app.unregisterReceiver(receiver) }
        }
    }

    /** Fija el volumen desde un valor 0..1 (slider). No muestra el panel del sistema. */
    fun setFraction(fraction: Float) {
        val s = _state.value
        val target = (s.min + (fraction.coerceIn(0f, 1f) * (s.max - s.min))).roundToInt()
        if (target != audio.getStreamVolume(STREAM)) {
            runCatching { audio.setStreamVolume(STREAM, target, 0) }
        }
        refresh()
    }

    fun step(up: Boolean) {
        runCatching {
            audio.adjustStreamVolume(
                STREAM,
                if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                0
            )
        }
        refresh()
    }

    companion object {
        const val STREAM = AudioManager.STREAM_MUSIC
        private const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"

        @Volatile
        private var instance: VolumeRepository? = null

        fun get(context: Context): VolumeRepository =
            instance ?: synchronized(this) {
                instance ?: VolumeRepository(context).also { instance = it }
            }
    }
}
