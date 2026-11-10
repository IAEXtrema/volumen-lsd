package com.psychovolume.app.engine

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.isActive

/**
 * Lienzo animado. El bucle de fotogramas solo corre mientras el ciclo de vida está en STARTED
 * (pantalla visible / servicio activo), así no consume batería en segundo plano.
 * Cada fotograma solo invalida la fase de dibujo (no hay recomposición).
 */
@Composable
fun PsychoScene(
    level: Float,
    params: RenderParams,
    modifier: Modifier = Modifier,
) {
    val engine = remember { PsychoEngine(initialLevel = level) }
    val currentLevel by rememberUpdatedState(level)
    val currentParams by rememberUpdatedState(params)
    var frame by remember { mutableLongStateOf(0L) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(engine, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var last = 0L
            while (isActive) {
                withFrameNanos { now ->
                    if (last == 0L) last = now
                    // Ahorro de batería: ~30 FPS (se omite un fotograma de cada dos)
                    val minInterval = if (currentParams.batterySaver) 30_000_000L else 0L
                    if (now - last >= minInterval) {
                        val dt = (now - last) / 1_000_000_000f
                        last = now
                        engine.update(dt, currentLevel, currentParams)
                        frame = now
                    }
                }
            }
        }
    }

    Canvas(modifier) {
        if (frame >= 0L) engine.draw(this)
    }
}
