package com.psychovolume.app.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Campo de estrellas con centelleo; se desplaza más rápido conforme sube el volumen. */
internal class StarField(private val count: Int = 90) {
    private val sx = FloatArray(count) { hash(it * 3 + 1) }
    private val sy = FloatArray(count) { hash(it * 3 + 2) * 0.8f }
    private val sz = FloatArray(count) { 1f + hash(it * 3 + 3) * 3.2f }
    private val ph = FloatArray(count) { hash(it * 7 + 11) * TAU }

    fun draw(scope: DrawScope, vis: Rect, t: Float, level: Float, pal: PsychoPalette, pp: PathPool): Unit = with(scope) {
        val w = vis.width
        val h = vis.height
        for (i in 0 until count) {
            val x = vis.left + fract(sx[i] + t * 0.004f * (0.3f + sz[i] * 0.3f)) * w
            val y = vis.top + sy[i] * h
            val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * (1.1f + sz[i] * 0.3f) + ph[i]))
            val tint = if (level > 0.3f) pal.rainbow[i % pal.rainbow.size] else Color.White
            val c = lerp(Color.White, tint, smooth(0.3f, 0.7f, level) * 0.7f).copy(alpha = tw)
            val r = sz[i] * (1f + 0.5f * smooth(0.6f, 1f, level))
            if (sz[i] > 3f) {
                val p = pp.get()
                starPath(p, x, y, r * 2.2f, r * 0.55f, 4, t * 0.5f + ph[i])
                drawPath(p, c)
            } else {
                drawCircle(c, r * 0.7f, Offset(x, y))
            }
        }
    }
}

/** Geometría sagrada alegre: flor de la vida + hexagrama, en capas que se multiplican y giran. */
internal fun DrawScope.sacredGeometry(
    cx: Float, cy: Float, t: Float, level: Float, fx: Float, quality: Float,
    pal: PsychoPalette, pp: PathPool,
) {
    val alpha = smooth(0.38f, 0.62f, level) * (0.35f + 0.35f * fx).coerceAtMost(0.8f)
    if (alpha < 0.01f) return
    val multi = smooth(0.6f, 0.8f, level)
    val max = smooth(0.82f, 1f, level)
    val breathe = 1f + 0.05f * sin(t * 1.6f)

    flower(cx, cy, 58f * breathe * smooth(0.38f, 0.6f, level), t * 14f, alpha, 0, pal, pp)
    if (multi > 0.01f) flower(cx, cy, 58f * 1.9f * multi, -t * 9f, alpha * 0.8f, 2, pal, pp)
    if (max > 0.01f && quality > 0.5f) flower(cx, cy, 58f * 2.9f * max, t * 6f, alpha * 0.65f, 4, pal, pp)
}

private fun DrawScope.flower(
    cx: Float, cy: Float, r: Float, rotDeg: Float, alpha: Float, colorShift: Int,
    pal: PsychoPalette, pp: PathPool,
) {
    if (r < 1f) return
    val cols = pal.rainbow
    withTransform({
        translate(cx, cy)
        rotate(rotDeg, Offset.Zero)
    }) {
        val sw = 2.4f
        drawCircle(cols[colorShift % cols.size].copy(alpha = alpha), r, Offset.Zero, style = Stroke(sw))
        for (i in 0 until 6) {
            val a = i * TAU / 6f
            drawCircle(
                cols[(i + colorShift + 1) % cols.size].copy(alpha = alpha), r,
                Offset(cos(a) * r, sin(a) * r), style = Stroke(sw),
            )
        }
        // Hexagrama
        val hex = pp.get()
        for (k in 0..1) {
            for (i in 0..2) {
                val a = k * (TAU / 6f) + i * TAU / 3f - TAU / 4f
                val x = cos(a) * r * 2f
                val y = sin(a) * r * 2f
                if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
            }
            hex.close()
        }
        drawPath(hex, cols[(colorShift + 3) % cols.size].copy(alpha = alpha), style = Stroke(sw, join = StrokeJoin.Round))
        drawCircle(cols[(colorShift + 5) % cols.size].copy(alpha = alpha), r * 2f, Offset.Zero, style = Stroke(sw * 1.4f))
        for (i in 0 until 12) {
            val a = i * TAU / 12f
            drawCircle(cols[(i + colorShift) % cols.size].copy(alpha = alpha * 1.2f), 4.2f, Offset(cos(a) * r * 2f, sin(a) * r * 2f))
        }
    }
}

/** Arcoíris líquido que se revela de izquierda a derecha y ondula. */
internal fun DrawScope.liquidRainbow(
    cx: Float, baseY: Float, rx: Float, ry: Float, t: Float, reveal: Float, wobble: Float,
    pal: PsychoPalette, pp: PathPool, bandW: Float = 12f,
) {
    if (reveal < 0.01f) return
    val cols = pal.rainbow
    val n = 48
    // Sombra suave para dar profundidad
    for (i in cols.indices) {
        val rr = i * bandW
        val p = pp.get()
        for (j in 0..n) {
            val th = PI.toFloat() + PI.toFloat() * reveal * (j / n.toFloat())
            val wav = 1f + wobble * 0.035f * sin(th * 5f + t * 2.5f + i * 0.6f)
            val x = cx + cos(th) * (rx - rr) * wav
            val y = baseY + sin(th) * (ry - rr) * wav
            if (j == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        if (i == 0) drawPath(p, Color.White.copy(alpha = 0.35f), style = Stroke(bandW + 10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p, cols[i], style = Stroke(bandW + 1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Colinas onduladas que respiran; su color y patrón evolucionan con el volumen. */
internal object Landscape {
    fun backY(x: Float, t: Float, wob: Float): Float =
        340f + sin(x * 0.012f + 1f) * 18f + sin(x * 0.031f + t * 0.8f) * wob * 0.6f

    fun frontY(x: Float, t: Float, wob: Float): Float =
        392f + sin(x * 0.010f + 3f) * 12f + sin(x * 0.027f - t * 0.9f) * wob

    fun wobble(level: Float, fx: Float): Float = 3f + 16f * smooth(0.6f, 1f, level) * fx

    fun drawBack(scope: DrawScope, vis: Rect, t: Float, level: Float, fx: Float, pal: PsychoPalette, pp: PathPool): Unit = with(scope) {
        val vivid = smooth(0.1f, 0.6f, level)
        val wob = wobble(level, fx)
        val p = pp.get()
        p.moveTo(vis.left, vis.bottom)
        var x = vis.left
        while (x <= vis.right + 20f) {
            p.lineTo(x, backY(x, t, wob)); x += 16f
        }
        p.lineTo(vis.right + 20f, vis.bottom)
        p.close()
        val top = lerp(Color(0xFF3C2A63), pal.purple, vivid)
        val bottom = lerp(Color(0xFF231A40), pal.blue, vivid)
        drawPath(p, pal.ink, style = Stroke(6f, join = StrokeJoin.Round))
        drawPath(p, Brush.verticalGradient(listOf(top, bottom), 320f, vis.bottom))
    }

    fun drawFront(scope: DrawScope, vis: Rect, t: Float, level: Float, fx: Float, pal: PsychoPalette, pp: PathPool): Unit = with(scope) {
        val vivid = smooth(0.1f, 0.6f, level)
        val wob = wobble(level, fx)
        val p = pp.get()
        p.moveTo(vis.left, vis.bottom)
        var x = vis.left
        while (x <= vis.right + 20f) {
            p.lineTo(x, frontY(x, t, wob)); x += 14f
        }
        p.lineTo(vis.right + 20f, vis.bottom)
        p.close()
        val top = lerp(Color(0xFF2E4A55), pal.green, vivid)
        val bottom = lerp(Color(0xFF1B2B3A), pal.turquoise, vivid)
        drawPath(p, Color.White.copy(alpha = 0.6f), style = Stroke(12f, join = StrokeJoin.Round))
        drawPath(p, pal.ink, style = Stroke(6f, join = StrokeJoin.Round))
        drawPath(p, Brush.verticalGradient(listOf(top, bottom), 370f, vis.bottom))

        // Lunares psicodélicos (fase 4+)
        val dots = smooth(0.6f, 0.8f, level)
        if (dots > 0.01f) {
            var i = 0
            var dx = vis.left + 30f
            while (dx < vis.right) {
                val dy = frontY(dx, t, wob) + 16f + hash(i) * 22f
                val col = pal.rainbow[i % pal.rainbow.size]
                drawCircle(col.copy(alpha = 0.85f * dots), (4f + hash(i + 50) * 5f) * dots, Offset(dx, dy))
                dx += 38f + hash(i + 9) * 30f
                i++
            }
        }
        Unit
    }
}
