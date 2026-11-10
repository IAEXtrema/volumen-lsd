package com.psychovolume.app.engine

import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

internal const val TAU = (PI * 2.0).toFloat()

internal fun clamp01(x: Float): Float = x.coerceIn(0f, 1f)

/** smoothstep clásico: 0 antes de e0, 1 después de e1, curva suave entre ambos. */
internal fun smooth(e0: Float, e1: Float, x: Float): Float {
    val t = clamp01((x - e0) / (e1 - e0))
    return t * t * (3f - 2f * t)
}

internal fun mixf(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** Rebote al aparecer: sobrepasa un poco el tamaño final y vuelve. */
internal fun easeOutBack(t: Float): Float {
    val x = clamp01(t) - 1f
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * x * x * x + c1 * x * x
}

internal fun fract(x: Float): Float = x - floor(x)

/** Pseudoaleatorio determinista en [0,1) para posiciones fijas. */
internal fun hash(i: Int): Float = fract(sin(i * 12.9898f + 78.233f) * 43758.547f)

/** Reutiliza objetos Path entre fotogramas para no generar basura en el GC a 60 FPS. */
class PathPool {
    private val pool = ArrayList<Path>(96)
    private var index = 0

    fun beginFrame() {
        index = 0
    }

    fun get(): Path {
        if (index >= pool.size) pool.add(Path())
        val p = pool[index++]
        p.reset()
        return p
    }
}
