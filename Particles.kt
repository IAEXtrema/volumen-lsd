package com.psychovolume.app.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sistema de partículas sin asignaciones por fotograma (arrays planos + pool fijo).
 * Tipos: destello de 4 puntas, burbuja, punto luminoso y estallido radial (fase 5).
 * La tasa de emisión depende del nivel: al bajar el volumen dejan de nacer y las vivas se apagan solas.
 */
internal class ParticleSystem(private val capacity: Int = 360) {

    private val x = FloatArray(capacity)
    private val y = FloatArray(capacity)
    private val vx = FloatArray(capacity)
    private val vy = FloatArray(capacity)
    private val life = FloatArray(capacity)
    private val maxLife = FloatArray(capacity)
    private val psize = FloatArray(capacity)
    private val seed = FloatArray(capacity)
    private val kind = IntArray(capacity)
    private val color = IntArray(capacity)

    private var spawnAcc = 0f
    private var burstAcc = 0f
    private var cursor = 0
    private val rnd = Random(42)

    fun update(dt: Float, level: Float, rateMul: Float, speed: Float, vis: Rect, burstOrigin: Offset) {
        val move = (0.6f + level) * speed
        for (i in 0 until capacity) {
            if (life[i] <= 0f) continue
            life[i] -= dt
            x[i] += (vx[i] + sin(life[i] * 3f + seed[i]) * 12f) * dt * move
            y[i] += vy[i] * dt * move
        }

        val rate = (3f + 190f * level.toDouble().pow(1.6).toFloat()) * rateMul
        spawnAcc += rate * dt
        while (spawnAcc >= 1f) {
            spawnAcc -= 1f
            spawnAmbient(vis, level)
        }

        val burst = smooth(0.8f, 1f, level) * 70f * rateMul
        burstAcc += burst * dt
        while (burstAcc >= 1f) {
            burstAcc -= 1f
            spawnBurst(burstOrigin)
        }
    }

    private fun slot(): Int {
        for (k in 0 until capacity) {
            val i = (cursor + k) % capacity
            if (life[i] <= 0f) {
                cursor = (i + 1) % capacity
                return i
            }
        }
        // Pool lleno: reemplaza la partícula más próxima en el anillo
        val i = cursor
        cursor = (cursor + 1) % capacity
        return i
    }

    private fun spawnAmbient(vis: Rect, level: Float) {
        val i = slot()
        val r = rnd.nextFloat()
        kind[i] = when {
            r < 0.45f -> 0
            r < 0.7f -> 1
            else -> 2
        }
        x[i] = vis.left + rnd.nextFloat() * vis.width
        if (kind[i] == 0) {
            y[i] = vis.top + rnd.nextFloat() * vis.height * 0.85f
            vx[i] = (rnd.nextFloat() - 0.5f) * 10f
            vy[i] = -5f - rnd.nextFloat() * 10f
        } else {
            y[i] = vis.bottom + 10f
            vx[i] = (rnd.nextFloat() - 0.5f) * 20f
            vy[i] = -(35f + rnd.nextFloat() * 50f)
        }
        maxLife[i] = 2.2f + rnd.nextFloat() * 3.5f
        life[i] = maxLife[i]
        psize[i] = (2.5f + rnd.nextFloat() * 6f) * (0.8f + level * 0.6f)
        seed[i] = rnd.nextFloat() * 6.28f
        color[i] = rnd.nextInt(7)
    }

    private fun spawnBurst(o: Offset) {
        val i = slot()
        val a = rnd.nextFloat() * (PI * 2).toFloat()
        val sp = 90f + rnd.nextFloat() * 160f
        kind[i] = 3
        x[i] = o.x
        y[i] = o.y
        vx[i] = cos(a) * sp
        vy[i] = sin(a) * sp
        maxLife[i] = 1.2f + rnd.nextFloat() * 1.4f
        life[i] = maxLife[i]
        psize[i] = 3f + rnd.nextFloat() * 5f
        seed[i] = rnd.nextFloat() * 6.28f
        color[i] = rnd.nextInt(7)
    }

    fun draw(scope: DrawScope, pal: PsychoPalette, alphaMul: Float, pp: PathPool): Unit = with(scope) {
        for (i in 0 until capacity) {
            if (life[i] <= 0f) continue
            val age = 1f - life[i] / maxLife[i]
            val a = sin(age * PI.toFloat()).coerceIn(0f, 1f) * alphaMul
            if (a < 0.02f) continue
            val c = pal.rainbow[color[i] % pal.rainbow.size]
            val p = Offset(x[i], y[i])
            when (kind[i]) {
                0 -> {
                    val path = pp.get()
                    val s = psize[i] * (0.7f + 0.3f * sin(age * 12f + seed[i]))
                    starPath(path, p.x, p.y, s * 1.6f, s * 0.35f, 4, seed[i] + age * 2f)
                    drawCircle(c.copy(alpha = a * 0.35f), s * 1.4f, p, blendMode = BlendMode.Plus)
                    drawPath(path, Color.White.copy(alpha = a))
                }
                1 -> {
                    drawCircle(c.copy(alpha = a * 0.8f), psize[i] * 1.3f, p, style = Stroke(1.8f))
                    drawCircle(Color.White.copy(alpha = a * 0.7f), psize[i] * 0.3f, Offset(p.x - psize[i] * 0.5f, p.y - psize[i] * 0.5f))
                }
                else -> {
                    drawCircle(c.copy(alpha = a * 0.35f), psize[i] * 1.8f, p, blendMode = BlendMode.Plus)
                    drawCircle(lerpWhite(c, 0.5f).copy(alpha = a), psize[i] * 0.6f, p)
                }
            }
        }
    }

    private fun lerpWhite(c: Color, t: Float) = Color(
        red = c.red + (1f - c.red) * t,
        green = c.green + (1f - c.green) * t,
        blue = c.blue + (1f - c.blue) * t,
        alpha = 1f,
    )
}
