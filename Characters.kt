package com.psychovolume.app.engine

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * Personajes originales dibujados por código, en estilo "sticker":
 * borde blanco grueso + contorno de tinta + relleno con degradado + brillos.
 * Cada personaje se dibuja en coordenadas locales (~100 unidades) y se posiciona con withTransform.
 * Todos dependen solo de (tiempo, nivel) => las transformaciones son 100 % reversibles.
 */

// ---------------------------------------------------------------- utilidades de estilo

/** Contorno por debajo del relleno: funciona con formas compuestas (uniones de óvalos). */
internal fun DrawScope.sticker(path: Path, fill: Brush, ink: Color, w: Float = 3f, border: Boolean = true) {
    if (border) drawPath(path, Color.White, style = Stroke(width = w * 2f + 7f, join = StrokeJoin.Round))
    drawPath(path, ink, style = Stroke(width = w * 2f, join = StrokeJoin.Round))
    drawPath(path, fill)
}

internal fun DrawScope.sticker(path: Path, fill: Color, ink: Color, w: Float = 3f, border: Boolean = true) {
    if (border) drawPath(path, Color.White, style = Stroke(width = w * 2f + 7f, join = StrokeJoin.Round))
    drawPath(path, ink, style = Stroke(width = w * 2f, join = StrokeJoin.Round))
    drawPath(path, fill)
}

/** Ojo kawaii: abierto (pupila negra con dos brillos), dormido (‿) o feliz (^). */
internal fun DrawScope.kawaiiEye(
    c: Offset, r: Float, open: Float, ink: Color, pp: PathPool, happy: Boolean = false,
) {
    if (happy || open < 0.2f) {
        val p = pp.get()
        if (happy) {
            p.moveTo(c.x - r, c.y + r * 0.35f)
            p.quadraticBezierTo(c.x, c.y - r * 1.1f, c.x + r, c.y + r * 0.35f)
        } else {
            p.moveTo(c.x - r, c.y - r * 0.1f)
            p.quadraticBezierTo(c.x, c.y + r * 0.9f, c.x + r, c.y - r * 0.1f)
        }
        drawPath(p, ink, style = Stroke(width = r * 0.5f, cap = StrokeCap.Round))
    } else {
        val ry = r * 1.18f * open
        drawOval(ink, topLeft = Offset(c.x - r, c.y - ry), size = Size(r * 2f, ry * 2f))
        drawCircle(Color.White, r * 0.38f, Offset(c.x - r * 0.32f, c.y - ry * 0.4f))
        drawCircle(Color.White, r * 0.17f, Offset(c.x + r * 0.36f, c.y + ry * 0.36f))
    }
}

internal fun DrawScope.blush(c: Offset, w: Float, color: Color) {
    drawOval(color.copy(alpha = 0.55f), topLeft = Offset(c.x - w / 2f, c.y - w / 4f), size = Size(w, w / 2f))
}

internal fun DrawScope.smile(c: Offset, w: Float, depth: Float, ink: Color, stroke: Float, pp: PathPool) {
    val p = pp.get()
    p.moveTo(c.x - w / 2f, c.y)
    p.quadraticBezierTo(c.x, c.y + depth, c.x + w / 2f, c.y)
    drawPath(p, ink, style = Stroke(width = stroke, cap = StrokeCap.Round))
}

/** Boquita abierta en forma de "D" para personajes cantando o bailando. */
internal fun DrawScope.openMouth(c: Offset, w: Float, h: Float, ink: Color, tongue: Color, pp: PathPool) {
    val p = pp.get()
    p.moveTo(c.x - w / 2f, c.y)
    p.quadraticBezierTo(c.x, c.y + h * 2f, c.x + w / 2f, c.y)
    p.close()
    drawPath(p, ink)
    drawOval(tongue, topLeft = Offset(c.x - w * 0.22f, c.y + h * 0.45f), size = Size(w * 0.44f, h * 0.5f))
}

/** "Z" de criatura dormida. */
internal fun DrawScope.sleepZ(x: Float, y: Float, s: Float, color: Color, pp: PathPool) {
    val p = pp.get()
    p.moveTo(x - s, y - s)
    p.lineTo(x + s, y - s)
    p.lineTo(x - s, y + s)
    p.lineTo(x + s, y + s)
    drawPath(p, color, style = Stroke(width = s * 0.45f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

internal fun starPath(p: Path, cx: Float, cy: Float, ro: Float, ri: Float, points: Int, rotRad: Float) {
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) ro else ri
        val a = rotRad + i * (TAU / (points * 2)) - TAU / 4f
        val x = cx + cos(a) * r
        val y = cy + sin(a) * r
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
}

internal fun DrawScope.musicNote(x: Float, y: Float, s: Float, color: Color, ink: Color, pp: PathPool) {
    val stem = pp.get()
    stem.moveTo(x + 5f * s, y)
    stem.lineTo(x + 5f * s, y - 22f * s)
    stem.quadraticBezierTo(x + 14f * s, y - 18f * s, x + 15f * s, y - 10f * s)
    drawPath(stem, ink, style = Stroke(width = 5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(stem, color, style = Stroke(width = 2.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawOval(ink, topLeft = Offset(x - 6.5f * s, y - 5f * s), size = Size(13f * s, 10f * s))
    drawOval(color, topLeft = Offset(x - 5f * s, y - 3.6f * s), size = Size(10f * s, 7.2f * s))
}

// ---------------------------------------------------------------- nube feliz

internal fun DrawScope.happyCloud(
    cx: Float, cy: Float, sc: Float, t: Float, level: Float, seed: Float,
    pal: PsychoPalette, pp: PathPool,
) {
    val bob = sin(t * 0.9f + seed) * 5f
    val breathe = sin(t * 1.7f + seed) * 0.03f
    val open = smooth(0.14f, 0.32f, level)
    val drift = sin(t * 0.25f + seed) * 18f
    withTransform({
        translate(cx + drift, cy + bob)
        scale(sc * (1f + breathe), sc * (1f - breathe), Offset.Zero)
    }) {
        val p = pp.get()
        p.addOval(Rect(-70f, -18f, -10f, 30f))
        p.addOval(Rect(-40f, -44f, 30f, 22f))
        p.addOval(Rect(5f, -30f, 65f, 26f))
        p.addOval(Rect(-55f, 0f, 55f, 34f))
        val top = lerp(Color(0xFFE6DDFF), Color.White, open)
        val bottom = lerp(Color(0xFFA996E0), lerp(Color.White, pal.turquoise, 0.55f), 0.3f + 0.6f * level)
        sticker(p, Brush.verticalGradient(listOf(top, bottom), startY = -44f, endY = 34f), pal.ink, 2.5f)
        drawOval(Color.White.copy(alpha = 0.7f), topLeft = Offset(-28f, -36f), size = Size(26f, 9f))

        val happy = level > 0.78f && sin(t * 3f + seed) > 0f
        kawaiiEye(Offset(-17f, 5f), 5.5f, open, pal.ink, pp, happy)
        kawaiiEye(Offset(17f, 5f), 5.5f, open, pal.ink, pp, happy)
        if (level > 0.55f) {
            openMouth(Offset(0f, 13f), 14f, 5f + 2f * sin(t * 6f), pal.ink, pal.pink, pp)
        } else {
            smile(Offset(0f, 14f), 10f, 4f + 4f * open, pal.ink, 2.4f, pp)
        }
        blush(Offset(-30f, 14f), 12f, pal.pink)
        blush(Offset(30f, 14f), 12f, pal.pink)
    }
}

// ---------------------------------------------------------------- boca, lengua y papel

internal fun DrawScope.heroMouth(
    cx: Float, cy: Float, sc: Float, t: Float, level: Float, fx: Float,
    pal: PsychoPalette, pp: PathPool,
) {
    val sing = smooth(0.5f, 1f, level)
    val open = 0.15f + 0.85f * smooth(0.12f, 0.45f, level) + 0.1f * sin(t * 5f) * sing
    val pulse = 1f + 0.05f * sin(t * 6f) * smooth(0.6f, 1f, level) * fx
    val lowerY = 34f + 30f * open
    val vivid = smooth(0.0f, 0.4f, level)

    // Brillo detrás de la boca (fase 3+)
    val glow = smooth(0.45f, 1f, level) * fx
    if (glow > 0.01f) {
        drawCircle(
            Brush.radialGradient(
                listOf(pal.pink.copy(alpha = 0.55f * glow), Color.Transparent),
                center = Offset(cx, cy), radius = 190f * sc,
            ),
            radius = 190f * sc, center = Offset(cx, cy),
        )
    }

    withTransform({
        translate(cx, cy)
        scale(sc * pulse, sc * (2f - pulse), Offset.Zero)
    }) {
        // Labios
        val lips = pp.get()
        lips.moveTo(-100f, 0f)
        lips.cubicTo(-80f, -30f, -45f, -50f, -20f, -40f)
        lips.quadraticBezierTo(-8f, -35f, 0f, -26f)
        lips.quadraticBezierTo(8f, -35f, 20f, -40f)
        lips.cubicTo(45f, -50f, 80f, -30f, 100f, 0f)
        lips.cubicTo(78f, lowerY * 0.78f, 42f, lowerY, 0f, lowerY)
        lips.cubicTo(-42f, lowerY, -78f, lowerY * 0.78f, -100f, 0f)
        lips.close()
        val pink = lerp(Color(0xFFC97AAB), pal.pink, vivid)
        sticker(
            lips,
            Brush.verticalGradient(
                listOf(lerp(pink, Color.White, 0.4f), pink, lerp(pink, pal.purple, 0.5f)),
                startY = -50f, endY = lowerY,
            ),
            pal.ink, 3.5f,
        )

        // Interior oscuro
        val inside = pp.get()
        inside.moveTo(-80f, 0f)
        inside.quadraticBezierTo(0f, -16f - 6f * open, 80f, 0f)
        inside.quadraticBezierTo(0f, 8f + 52f * open, -80f, 0f)
        inside.close()
        drawPath(
            inside,
            Brush.verticalGradient(listOf(Color(0xFF240620), Color(0xFF5A0F3E)), startY = -14f, endY = 30f * open + 4f),
        )
        clipPath(inside) {
            drawRoundRect(
                Color.White, topLeft = Offset(-56f, -22f), size = Size(112f, 15f),
                cornerRadius = CornerRadius(6f, 6f),
            )
            drawLine(Color(0xFFE6D8F0), Offset(-18f, -22f), Offset(-18f, -8f), strokeWidth = 1.6f)
            drawLine(Color(0xFFE6D8F0), Offset(18f, -22f), Offset(18f, -8f), strokeWidth = 1.6f)
        }
        drawPath(inside, pal.ink, style = Stroke(width = 3f, join = StrokeJoin.Round))

        // Brillos de los labios
        drawOval(Color.White.copy(alpha = 0.65f), topLeft = Offset(-66f, -36f), size = Size(30f, 9f))
        drawOval(Color.White.copy(alpha = 0.45f), topLeft = Offset(36f, -36f), size = Size(18f, 6f))
        drawOval(Color.White.copy(alpha = 0.55f), topLeft = Offset(14f, lowerY - 17f), size = Size(36f, 9f))

        // Lengua
        val out = smooth(0.15f, 0.4f, level)
        val len = 4f + 92f * out + 14f * smooth(0.8f, 1f, level)
        val wa = 3f + 14f * smooth(0.2f, 0.45f, level) + 12f * smooth(0.7f, 1f, level) * fx
        val baseY = 2f + 20f * open
        val n = 14
        val xs = FloatArray(n + 1)
        val ys = FloatArray(n + 1)
        val hw = FloatArray(n + 1)
        for (i in 0..n) {
            val s = i / n.toFloat()
            xs[i] = sin(t * 3.2f - s * 2.6f) * wa * s
            ys[i] = baseY + s * len
            hw[i] = 22f - 5f * s
        }
        val tongue = pp.get()
        tongue.moveTo(xs[0] - hw[0], ys[0])
        for (i in 1..n) tongue.lineTo(xs[i] - hw[i], ys[i])
        val tipBulge = hw[n] * 1.3f
        tongue.cubicTo(
            xs[n] - hw[n], ys[n] + tipBulge, xs[n] + hw[n], ys[n] + tipBulge, xs[n] + hw[n], ys[n],
        )
        for (i in n - 1 downTo 0) tongue.lineTo(xs[i] + hw[i], ys[i])
        tongue.close()
        sticker(
            tongue,
            Brush.verticalGradient(
                listOf(Color(0xFFFF86AC), Color(0xFFE8386D)), startY = baseY, endY = baseY + len + 25f,
            ),
            pal.ink, 3f, border = false,
        )
        val groove = pp.get()
        groove.moveTo(xs[1], ys[1])
        for (i in 2..(n * 7 / 10)) groove.lineTo(xs[i], ys[i])
        drawPath(groove, Color(0xFFB81E52).copy(alpha = 0.7f), style = Stroke(width = 3f, cap = StrokeCap.Round))
        drawOval(Color.White.copy(alpha = 0.4f), topLeft = Offset(xs[n / 2] - 14f, ys[n / 2] - 4f), size = Size(9f, 14f))

        // Papel psicodélico: pegado a la lengua -> salta -> orbita alrededor de la boca
        val appear = smooth(0.18f, 0.32f, level)
        if (appear > 0.01f) {
            val k = (n * 0.72f).toInt()
            val onX = xs[k]
            val onY = ys[k]
            val slope = (xs[k + 1] - xs[k - 1]) / (ys[k + 1] - ys[k - 1])
            val hopAmt = smooth(0.45f, 0.65f, level)
            val hop = abs(sin(t * 2.4f))
            val orbit = smooth(0.82f, 0.97f, level)
            val orbX = cos(t * 1.5f) * 150f
            val orbY = sin(t * 1.5f) * 70f - 20f
            val px = mixf(onX, orbX, orbit)
            val py = mixf(onY - hop * 70f * hopAmt, orbY, orbit)
            val spin = -slope * 40f + hopAmt * hop * 220f * (1f - orbit) + orbit * t * 160f
            val paperSize = easeOutBack(appear) * (1f + 0.6f * orbit)
            psychePaper(px, py, paperSize, spin, t, pal, pp)
        }
    }
}

/** Pequeño papel ilustrado: franjas arcoíris, retícula perforada y un sol sonriente. */
internal fun DrawScope.psychePaper(x: Float, y: Float, s: Float, rotDeg: Float, t: Float, pal: PsychoPalette, pp: PathPool) {
    if (s <= 0.01f) return
    withTransform({
        translate(x, y)
        rotate(rotDeg, Offset.Zero)
        scale(s, s, Offset.Zero)
    }) {
        val sq = pp.get()
        sq.addRoundRect(RoundRect(-17f, -17f, 17f, 17f, 3f, 3f))
        val shift = fract(t * 0.2f) * 34f
        sticker(
            sq,
            Brush.linearGradient(
                pal.rainbow, start = Offset(-17f - shift, -17f), end = Offset(17f - shift + 34f, 17f),
            ),
            pal.ink, 1.8f,
        )
        val dash = PathEffect.dashPathEffect(floatArrayOf(2f, 2.5f), 0f)
        for (k in -1..1 step 2) {
            drawLine(Color.White.copy(alpha = 0.7f), Offset(k * 8.5f, -16f), Offset(k * 8.5f, 16f), 0.9f, pathEffect = dash)
            drawLine(Color.White.copy(alpha = 0.7f), Offset(-16f, k * 8.5f), Offset(16f, k * 8.5f), 0.9f, pathEffect = dash)
        }
        val sun = pp.get()
        starPath(sun, 0f, 0f, 10f, 6f, 8, t * 1.5f)
        sticker(sun, pal.yellow, pal.ink, 1.2f, border = false)
        drawCircle(pal.ink, 1.3f, Offset(-2.6f, -1f))
        drawCircle(pal.ink, 1.3f, Offset(2.6f, -1f))
        smile(Offset(0f, 1.8f), 4.5f, 2.6f, pal.ink, 1.1f, pp)
    }
}

// ---------------------------------------------------------------- ajolote

internal fun DrawScope.axolotl(
    cx: Float, cy: Float, sc: Float, t: Float, level: Float, appear: Float, fx: Float,
    pal: PsychoPalette, pp: PathPool,
) {
    if (appear <= 0.01f) return
    val pop = easeOutBack(appear)
    val swim = smooth(0.7f, 1f, level) * fx.coerceAtMost(1f)
    val x = cx + sin(t * 0.9f) * 55f * swim
    val y = cy + sin(t * 1.3f) * 10f + sin(t * 1.8f) * 18f * swim + (1f - appear) * 60f
    val rot = sin(t * 0.8f) * 7f + sin(t * 1.8f) * 10f * swim

    withTransform({
        translate(x, y)
        rotate(rot, Offset.Zero)
        scale(sc * pop, sc * pop, Offset.Zero)
    }) {
        val body = lerp(Color(0xFFFFB8DC), pal.pink, 0.22f)
        val deep = lerp(pal.pink, pal.purple, 0.35f)
        val light = lerp(body, Color.White, 0.45f)

        // Cola
        val tw = sin(t * 3f) * 10f
        val tail = pp.get()
        tail.moveTo(14f, 58f)
        tail.cubicTo(50f, 72f, 82f + tw, 62f, 94f + tw, 28f)
        tail.cubicTo(80f + tw, 50f, 50f, 52f, 26f, 44f)
        tail.close()
        sticker(tail, Brush.linearGradient(listOf(body, deep), Offset(14f, 58f), Offset(94f, 28f)), pal.ink, 2.5f)

        // Branquias plumosas (3 por lado)
        val wig = 0.4f + level
        for (side in intArrayOf(-1, 1)) {
            for (k in 0..2) {
                val bx = side * (52f + 4f * (k % 2))
                val by = -22f + k * 14f
                val baseAng = (-150f - k * 30f) + sin(t * 4f + k + side) * 9f * wig
                val angRad = Math.toRadians(baseAng.toDouble()).toFloat()
                val dx = cos(angRad) * 36f * side * -1f
                val dy = sin(angRad) * 36f
                val ex = bx + dx
                val ey = by + dy
                drawLine(pal.ink, Offset(bx, by), Offset(ex, ey), 13f, cap = StrokeCap.Round)
                drawLine(deep, Offset(bx, by), Offset(ex, ey), 8f, cap = StrokeCap.Round)
                for (f in 1..3) {
                    val s = 0.3f + f * 0.22f
                    val fx2 = bx + dx * s
                    val fy2 = by + dy * s
                    val off = if (f % 2 == 0) 5f else -5f
                    drawCircle(pal.ink, 6.4f, Offset(fx2 - dy / 36f * off, fy2 + dx / 36f * off))
                    drawCircle(light, 4.6f, Offset(fx2 - dy / 36f * off, fy2 + dx / 36f * off))
                }
            }
        }

        // Cuerpo, bracitos y pancita
        val b = pp.get()
        b.addOval(Rect(-32f, 26f, 32f, 82f))
        sticker(b, body, pal.ink, 2.5f)
        val arms = pp.get()
        arms.addOval(Rect(-46f, 46f, -26f, 62f))
        arms.addOval(Rect(26f, 46f, 46f, 62f))
        sticker(arms, body, pal.ink, 2.2f, border = false)
        drawOval(light, topLeft = Offset(-17f, 42f), size = Size(34f, 32f))

        // Cabeza
        val head = pp.get()
        head.addOval(Rect(-60f, -46f, 60f, 40f))
        sticker(
            head,
            Brush.radialGradient(listOf(light, body, deep), center = Offset(-12f, -18f), radius = 90f),
            pal.ink, 3f,
        )
        drawCircle(deep.copy(alpha = 0.5f), 4f, Offset(-14f, -34f))
        drawCircle(deep.copy(alpha = 0.5f), 3f, Offset(4f, -38f))
        drawCircle(deep.copy(alpha = 0.5f), 3.5f, Offset(20f, -32f))
        drawOval(Color.White.copy(alpha = 0.55f), topLeft = Offset(-40f, -38f), size = Size(26f, 9f))

        // Cara
        val blink = if (fract(t * 0.23f) < 0.04f) 0.1f else 1f
        val joy = level > 0.85f
        kawaiiEye(Offset(-27f, -4f), 7f, blink, pal.ink, pp, happy = joy)
        kawaiiEye(Offset(27f, -4f), 7f, blink, pal.ink, pp, happy = joy)
        if (joy) openMouth(Offset(0f, 12f), 22f, 6f, pal.ink, Color(0xFFFF5C8A), pp)
        else smile(Offset(0f, 12f), 30f, 12f, pal.ink, 3.4f, pp)
        blush(Offset(-40f, 12f), 16f, pal.pink)
        blush(Offset(40f, 12f), 16f, pal.pink)
    }
}

// ---------------------------------------------------------------- honguito bailarín

internal fun DrawScope.mushroom(
    cx: Float, cy: Float, sc: Float, t: Float, level: Float, phase: Float, cap: Color, fx: Float,
    pal: PsychoPalette, pp: PathPool,
) {
    val wake = smooth(0.15f, 0.38f, level)
    val dance = smooth(0.4f, 0.6f, level) * fx.coerceAtMost(1f)
    val grow = 1f + 0.25f * smooth(0.8f, 1f, level)
    val beat = sin(t * 5f + phase)
    val sy = (1f + 0.12f * beat * dance) * grow
    val sx = (1f - 0.08f * beat * dance) * grow
    val hop = abs(sin(t * 2.5f + phase)) * 14f * dance
    val sway = sin(t * 2.5f + phase) * 12f * dance + sin(t * 0.6f + phase) * 3f

    withTransform({
        translate(cx, cy - hop)
        rotate(sway, Offset.Zero)
        scale(sc * sx, sc * sy, Offset.Zero)
    }) {
        val stem = pp.get()
        stem.moveTo(-14f, -50f)
        stem.quadraticBezierTo(-21f, -20f, -20f, -2f)
        stem.quadraticBezierTo(0f, 4f, 20f, -2f)
        stem.quadraticBezierTo(21f, -20f, 14f, -50f)
        stem.close()
        sticker(stem, Brush.verticalGradient(listOf(Color(0xFFFFF6E6), Color(0xFFF2D3C2)), -50f, 0f), pal.ink, 2.5f)

        val capColor = lerp(Color(0xFF8C6FA8), cap, smooth(0.08f, 0.5f, level))
        val capPath = pp.get()
        capPath.moveTo(-52f, -46f)
        capPath.cubicTo(-52f, -98f, 52f, -98f, 52f, -46f)
        capPath.quadraticBezierTo(0f, -34f, -52f, -46f)
        capPath.close()
        sticker(
            capPath,
            Brush.verticalGradient(listOf(lerp(capColor, Color.White, 0.35f), capColor, lerp(capColor, pal.ink, 0.25f)), -86f, -40f),
            pal.ink, 2.8f,
        )
        drawCircle(Color.White, 8f, Offset(-24f, -64f))
        drawCircle(Color.White, 9.5f, Offset(4f, -78f))
        drawCircle(Color.White, 6f, Offset(28f, -60f))
        drawCircle(Color.White, 4f, Offset(-6f, -56f))
        drawOval(Color.White.copy(alpha = 0.5f), topLeft = Offset(-38f, -86f), size = Size(22f, 8f))

        val happy = dance > 0.5f && beat > 0.55f
        kawaiiEye(Offset(-8f, -30f), 4.5f, wake, pal.ink, pp, happy)
        kawaiiEye(Offset(8f, -30f), 4.5f, wake, pal.ink, pp, happy)
        if (dance > 0.3f) openMouth(Offset(0f, -20f), 9f, 3.5f, pal.ink, pal.pink, pp)
        else smile(Offset(0f, -20f), 7f, 2f + 3f * wake, pal.ink, 1.8f, pp)
        blush(Offset(-14f, -22f), 8f, pal.pink)
        blush(Offset(14f, -22f), 8f, pal.pink)
    }
}

// ---------------------------------------------------------------- gatito DJ

internal fun DrawScope.djCat(
    cx: Float, cy: Float, sc: Float, t: Float, appear: Float, fur: Color, phones: Color, seed: Float,
    pal: PsychoPalette, pp: PathPool,
) {
    if (appear <= 0.01f) return
    val rise = (1f - easeOutBack(appear)) * 130f
    val bob = abs(sin(t * 4f + seed)) * 6f
    val tilt = sin(t * 4f + seed) * 9f
    val furDark = lerp(fur, pal.ink, 0.3f)

    withTransform({
        translate(cx, cy + rise)
        scale(sc, sc, Offset.Zero)
    }) {
        val bodyP = pp.get()
        bodyP.addOval(Rect(-34f, -44f, 34f, 20f))
        sticker(bodyP, fur, pal.ink, 2.5f)

        withTransform({
            translate(0f, -66f - bob)
            rotate(tilt, Offset.Zero)
        }) {
            val ears = pp.get()
            ears.moveTo(-38f, -10f); ears.lineTo(-32f, -52f); ears.lineTo(-8f, -30f); ears.close()
            ears.moveTo(38f, -10f); ears.lineTo(32f, -52f); ears.lineTo(8f, -30f); ears.close()
            sticker(ears, fur, pal.ink, 2.5f)
            val inner = pp.get()
            inner.moveTo(-31f, -18f); inner.lineTo(-28f, -42f); inner.lineTo(-15f, -30f); inner.close()
            inner.moveTo(31f, -18f); inner.lineTo(28f, -42f); inner.lineTo(15f, -30f); inner.close()
            drawPath(inner, pal.pink.copy(alpha = 0.8f))

            val head = pp.get()
            head.addOval(Rect(-46f, -38f, 46f, 32f))
            sticker(head, Brush.radialGradient(listOf(lerp(fur, Color.White, 0.35f), fur), Offset(-10f, -14f), 60f), pal.ink, 3f)
            for (k in -1..1) {
                drawLine(furDark, Offset(k * 9f, -36f), Offset(k * 7f, -24f), 4f, cap = StrokeCap.Round)
            }

            // Audífonos
            val band = pp.get()
            band.moveTo(-48f, -4f)
            band.cubicTo(-50f, -66f, 50f, -66f, 48f, -4f)
            drawPath(band, pal.ink, style = Stroke(width = 12f, cap = StrokeCap.Round))
            drawPath(band, phones, style = Stroke(width = 7f, cap = StrokeCap.Round))
            val cups = pp.get()
            cups.addRoundRect(RoundRect(-58f, -18f, -38f, 20f, 9f, 9f))
            cups.addRoundRect(RoundRect(38f, -18f, 58f, 20f, 9f, 9f))
            sticker(cups, phones, pal.ink, 2.5f, border = false)
            val led = 0.5f + 0.5f * sin(t * 8f + seed)
            drawCircle(Color.White.copy(alpha = 0.4f + 0.6f * led), 3.2f, Offset(-48f, 0f))
            drawCircle(Color.White.copy(alpha = 0.4f + 0.6f * led), 3.2f, Offset(48f, 0f))

            // Cara
            kawaiiEye(Offset(-16f, -4f), 7f, 1f, pal.ink, pp, happy = true)
            kawaiiEye(Offset(16f, -4f), 7f, 1f, pal.ink, pp, happy = true)
            val nose = pp.get()
            nose.moveTo(-4f, 5f); nose.lineTo(4f, 5f); nose.lineTo(0f, 10f); nose.close()
            drawPath(nose, pal.pink)
            val mouth = pp.get()
            mouth.moveTo(-9f, 11f)
            mouth.quadraticBezierTo(-4.5f, 18f, 0f, 11f)
            mouth.quadraticBezierTo(4.5f, 18f, 9f, 11f)
            drawPath(mouth, pal.ink, style = Stroke(width = 2.6f, cap = StrokeCap.Round))
            for (side in intArrayOf(-1, 1)) {
                drawLine(pal.ink, Offset(side * 22f, 8f), Offset(side * 42f, 3f), 1.6f, cap = StrokeCap.Round)
                drawLine(pal.ink, Offset(side * 22f, 13f), Offset(side * 42f, 16f), 1.6f, cap = StrokeCap.Round)
            }
            blush(Offset(-28f, 12f), 12f, pal.pink)
            blush(Offset(28f, 12f), 12f, pal.pink)
        }

        // Tornamesa
        val deck = pp.get()
        deck.addRoundRect(RoundRect(-72f, -6f, 72f, 34f, 10f, 10f))
        sticker(deck, Brush.verticalGradient(listOf(lerp(pal.purple, Color.White, 0.2f), lerp(pal.purple, pal.ink, 0.5f)), -6f, 34f), pal.ink, 2.5f)
        drawOval(Color(0xFF1A1030), topLeft = Offset(-56f, -2f), size = Size(64f, 18f))
        drawOval(phones, topLeft = Offset(-30f, 4f), size = Size(12f, 6f))
        rotate(t * 260f + seed * 50f, Offset(-24f, 7f)) {
            drawLine(Color.White.copy(alpha = 0.35f), Offset(-46f, 7f), Offset(-32f, 7f), 2f, cap = StrokeCap.Round)
        }
        for (i in 0..3) {
            val on = sin(t * 7f + i * 1.7f + seed) > 0f
            drawCircle(pal.rainbow[(i * 2) % pal.rainbow.size].copy(alpha = if (on) 1f else 0.35f), 3.6f, Offset(22f + i * 12f, 8f))
        }
        drawLine(Color.White.copy(alpha = 0.5f), Offset(20f, 22f), Offset(60f, 22f), 3f, cap = StrokeCap.Round)
        drawCircle(pal.yellow, 4f, Offset(40f + sin(t * 2f + seed) * 16f, 22f))

        // Patitas rascando el disco
        val scratch = sin(t * 8f + seed) * 6f
        val paws = pp.get()
        paws.addOval(Rect(-34f + scratch, -10f, -14f + scratch, 4f))
        paws.addOval(Rect(10f, -12f, 30f, 2f))
        sticker(paws, fur, pal.ink, 2f, border = false)
    }

    // Notas musicales que flotan
    for (i in 0..1) {
        val ph = fract(t * 0.3f + i * 0.5f + seed * 0.1f)
        val nx = cx + (if (seed > 1f) -1f else 1f) * (40f + ph * 45f) * sc + sin(ph * 9f) * 6f
        val ny = cy + rise - 150f * sc - ph * 90f * sc
        val a = (1f - ph) * appear
        musicNote(nx, ny, 1.1f * sc, pal.rainbow[(i * 3 + seed.toInt()) % pal.rainbow.size].copy(alpha = a), pal.ink.copy(alpha = a), pp)
    }
}

// ---------------------------------------------------------------- extraterrestre en platillo

internal fun DrawScope.ufoAlien(
    cx: Float, cy: Float, sc: Float, t: Float, level: Float, appear: Float, skin: Color, seed: Float,
    pal: PsychoPalette, pp: PathPool,
) {
    if (appear <= 0.01f) return
    val pop = easeOutBack(appear)
    val x = cx + sin(t * 0.45f + seed) * 70f
    val y = cy + sin(t * 1.2f + seed) * 8f - (1f - appear) * 140f
    val tilt = sin(t * 0.9f + seed) * 8f

    withTransform({
        translate(x, y)
        rotate(tilt, Offset.Zero)
        scale(sc * pop, sc * pop, Offset.Zero)
    }) {
        val beam = smooth(0.85f, 1f, level)
        if (beam > 0.01f) {
            val bp = pp.get()
            bp.moveTo(-22f, 10f); bp.lineTo(22f, 10f); bp.lineTo(66f, 150f); bp.lineTo(-66f, 150f); bp.close()
            drawPath(bp, Brush.verticalGradient(listOf(pal.yellow.copy(alpha = 0.5f * beam), Color.Transparent), 10f, 150f))
        }

        // Alien dentro de la cúpula
        drawLine(pal.ink, Offset(-10f, -44f), Offset(-18f, -64f), 3f, cap = StrokeCap.Round)
        drawLine(pal.ink, Offset(10f, -44f), Offset(18f, -64f), 3f, cap = StrokeCap.Round)
        drawCircle(pal.ink, 5.5f, Offset(-18f, -64f))
        drawCircle(pal.pink, 4f, Offset(-18f, -64f))
        drawCircle(pal.ink, 5.5f, Offset(18f, -64f))
        drawCircle(pal.yellow, 4f, Offset(18f, -64f))
        val head = pp.get()
        head.addOval(Rect(-24f, -48f, 24f, -6f))
        sticker(head, Brush.radialGradient(listOf(lerp(skin, Color.White, 0.4f), skin), Offset(-6f, -34f), 30f), pal.ink, 2.2f, border = false)
        val blink = if (fract(t * 0.31f + seed) < 0.05f) 0.1f else 1f
        kawaiiEye(Offset(-10f, -26f), 4.8f, blink, pal.ink, pp)
        kawaiiEye(Offset(10f, -26f), 4.8f, blink, pal.ink, pp)
        kawaiiEye(Offset(0f, -38f), 3.8f, 1f, pal.ink, pp)
        smile(Offset(0f, -16f), 10f, 5f, pal.ink, 2f, pp)

        // Cúpula de cristal
        val dome = pp.get()
        dome.moveTo(-34f, -6f)
        dome.cubicTo(-34f, -64f, 34f, -64f, 34f, -6f)
        dome.close()
        drawPath(dome, pal.turquoise.copy(alpha = 0.22f))
        drawPath(dome, Color.White.copy(alpha = 0.8f), style = Stroke(width = 2.5f))
        drawArc(
            Color.White.copy(alpha = 0.7f), startAngle = 200f, sweepAngle = 50f, useCenter = false,
            topLeft = Offset(-26f, -50f), size = Size(52f, 70f), style = Stroke(width = 3f, cap = StrokeCap.Round),
        )

        // Platillo
        val saucer = pp.get()
        saucer.addOval(Rect(-66f, -16f, 66f, 14f))
        sticker(
            saucer,
            Brush.verticalGradient(listOf(Color(0xFFF3EEFF), Color(0xFFB7A6E6), lerp(pal.purple, pal.ink, 0.3f)), -16f, 14f),
            pal.ink, 2.6f,
        )
        drawOval(lerp(pal.purple, pal.ink, 0.4f), topLeft = Offset(-26f, 8f), size = Size(52f, 12f))
        for (i in 0..6) {
            val lx = -48f + i * 16f
            val on = sin(t * 6f + i + seed) > -0.2f
            drawCircle(pal.ink, 4.6f, Offset(lx, 2f))
            drawCircle(pal.rainbow[i % pal.rainbow.size].copy(alpha = if (on) 1f else 0.3f), 3.4f, Offset(lx, 2f))
        }
    }
}

// ---------------------------------------------------------------- ojomedusa (criatura surrealista)

internal fun DrawScope.eyeJelly(
    cx: Float, cy: Float, sc: Float, t: Float, appear: Float, pal: PsychoPalette, pp: PathPool,
) {
    if (appear <= 0.01f) return
    val pop = easeOutBack(appear)
    val bob = sin(t * 1.4f) * 12f
    val pulse = 1f + 0.08f * sin(t * 3f)
    withTransform({
        translate(cx, cy + bob)
        scale(sc * pop * pulse, sc * pop / pulse, Offset.Zero)
    }) {
        for (i in 0..4) {
            val x0 = -32f + i * 16f
            val tp = pp.get()
            tp.moveTo(x0, 10f)
            for (s in 1..10) {
                val yy = 10f + s * 8f
                tp.lineTo(x0 + sin(t * 3.5f + s * 0.7f + i) * (2f + s * 1.2f), yy)
            }
            drawPath(tp, pal.ink, style = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(tp, pal.rainbow[(i + 2) % pal.rainbow.size], style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        val bell = pp.get()
        bell.moveTo(-46f, 10f)
        bell.cubicTo(-46f, -58f, 46f, -58f, 46f, 10f)
        bell.quadraticBezierTo(34.5f, 20f, 23f, 10f)
        bell.quadraticBezierTo(11.5f, 20f, 0f, 10f)
        bell.quadraticBezierTo(-11.5f, 20f, -23f, 10f)
        bell.quadraticBezierTo(-34.5f, 20f, -46f, 10f)
        bell.close()
        sticker(
            bell,
            Brush.radialGradient(listOf(lerp(pal.purple, Color.White, 0.5f), pal.purple, lerp(pal.purple, pal.pink, 0.6f)), Offset(-10f, -26f), 60f),
            pal.ink, 2.8f,
        )
        drawOval(Color.White.copy(alpha = 0.5f), topLeft = Offset(-30f, -36f), size = Size(20f, 8f))

        val blink = if (fract(t * 0.19f) < 0.05f) 0.15f else 1f
        val eyeH = 34f * blink
        drawOval(pal.ink, topLeft = Offset(-23f, -17f - eyeH / 2f - 3f), size = Size(46f, eyeH + 6f))
        drawOval(Color.White, topLeft = Offset(-20f, -17f - eyeH / 2f), size = Size(40f, eyeH))
        if (blink > 0.5f) {
            val lx = sin(t * 0.7f) * 6f
            val ly = cos(t * 0.9f) * 4f
            drawCircle(pal.turquoise, 11f, Offset(lx, -17f + ly))
            drawCircle(pal.ink, 6f, Offset(lx, -17f + ly))
            drawCircle(Color.White, 2.6f, Offset(lx - 3f, -20f + ly))
        }
        smile(Offset(0f, 4f), 12f, 4f, pal.ink, 2.2f, pp)
    }
}

// ---------------------------------------------------------------- gotitas saltarinas

internal fun DrawScope.slime(
    cx: Float, cy: Float, sc: Float, t: Float, appear: Float, color: Color, phase: Float,
    pal: PsychoPalette, pp: PathPool,
) {
    if (appear <= 0.01f) return
    val pop = easeOutBack(appear)
    val hop = abs(sin(t * 3f + phase))
    val squash = 1f + (1f - hop) * 0.2f
    withTransform({
        translate(cx, cy - hop * 22f * appear)
        scale(sc * pop * squash, sc * pop / squash, Offset.Zero)
    }) {
        val p = pp.get()
        p.moveTo(-20f, 0f)
        p.cubicTo(-24f, -22f, -4f, -30f, 0f, -40f)
        p.cubicTo(4f, -30f, 24f, -22f, 20f, 0f)
        p.quadraticBezierTo(0f, 5f, -20f, 0f)
        p.close()
        sticker(p, Brush.verticalGradient(listOf(lerp(color, Color.White, 0.45f), color), -40f, 0f), pal.ink, 2.2f)
        drawOval(Color.White.copy(alpha = 0.6f), topLeft = Offset(-12f, -26f), size = Size(6f, 10f))
        kawaiiEye(Offset(-7f, -13f), 3.6f, 1f, pal.ink, pp)
        kawaiiEye(Offset(7f, -13f), 3.6f, 1f, pal.ink, pp)
        drawOval(pal.ink, topLeft = Offset(-2.5f, -7f), size = Size(5f, 4f + 2f * (1f - hop)))
    }
}
