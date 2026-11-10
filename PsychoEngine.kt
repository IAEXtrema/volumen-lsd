package com.psychovolume.app.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Motor de la escena. Todo el universo es una función continua de:
 *   - level: volumen suavizado (0..1) con un resorte exponencial,
 *   - animT: tiempo de animación que avanza más rápido con volumen alto.
 * Como no hay estados discretos, subir y bajar el volumen recorre exactamente el mismo camino
 * en ambos sentidos (animación reversible, sin reinicios bruscos).
 *
 * La escena se compone en un lienzo virtual de 1000 x 420 unidades centrado en el área real;
 * fondo, estrellas, colinas y partículas se extienden a toda el área visible.
 */
class PsychoEngine(initialLevel: Float = 0f) {

    private val background = BackgroundShader()
    private val particles = ParticleSystem()
    private val stars = StarField()
    private val pp = PathPool()

    /** Nivel visual suavizado (0..1) */
    var level: Float = initialLevel
        private set

    /** Calidad adaptativa (0.35..1): baja si los fotogramas tardan más de lo esperado. */
    var quality: Float = 1f
        private set

    private var animT = 0f
    private var clock = 0f
    private var flash = 0f
    private var frameMsEma = 16f
    private var qualityTimer = 0f
    private var params = RenderParams()
    private var lastVis = Rect(0f, 0f, VW, VH)

    fun update(rawDt: Float, target: Float, p: RenderParams) {
        params = p
        val dt = rawDt.coerceIn(0f, 0.1f)

        // --- renderizado adaptativo
        val budget = if (p.batterySaver) 36f else 18.5f
        frameMsEma = frameMsEma * 0.9f + rawDt * 1000f * 0.1f
        qualityTimer += dt
        if (qualityTimer > 0.5f) {
            qualityTimer = 0f
            quality = if (frameMsEma > budget * 1.15f) max(0.35f, quality - 0.1f)
            else if (frameMsEma < budget) min(1f, quality + 0.05f)
            else quality
        }

        // --- resorte hacia el volumen objetivo
        val rate = mixf(2.5f, 14f, p.sensitivity)
        level += (target.coerceIn(0f, 1f) - level) * (1f - exp(-dt * rate))

        val tempo = p.speed * (0.45f + 1.25f * level)
        animT += dt * tempo
        clock += dt

        // --- destellos (fase 5), por debajo de 3 Hz por accesibilidad (WCAG 2.3.1)
        flash = if (p.flashes) {
            val pulse = max(0f, sin(clock * TAU * 1.2f)).pow(8)
            smooth(0.85f, 1f, level) * pulse * p.intensity.coerceAtMost(1f)
        } else 0f

        val mouth = Offset(VW / 2f, 270f)
        particles.update(
            dt = dt,
            level = level,
            rateMul = p.particles * quality * p.intensity.coerceAtMost(1.2f),
            speed = p.speed,
            vis = lastVis,
            burstOrigin = mouth,
        )
    }

    fun draw(scope: DrawScope): Unit = with(scope) {
        pp.beginFrame()
        val pal = Palettes.get(params.paletteIndex)
        val fx = params.intensity
        val L = level
        val t = animT

        // 1. Fondo GPU
        val kaleido = smooth(0.6f, 0.95f, L) * (if (params.reduceMotion) 0.35f else 0.85f) * fx.coerceAtMost(1f)
        background.update(size.width, size.height, t, L, kaleido, flash, pal)
        drawRect(background.brush)

        val s = min(size.width / VW, size.height / VH)
        val ox = (size.width - VW * s) / 2f
        val oy = (size.height - VH * s) / 2f
        val vis = Rect(-ox / s, -oy / s, (size.width - ox) / s, (size.height - oy) / s)
        lastVis = vis

        // Distorsión global sutil en las fases altas ("respiración" del universo)
        val warp = smooth(0.7f, 1f, L) * 0.025f * fx.coerceAtMost(1f) * (if (params.reduceMotion) 0.3f else 1f)
        val wx = 1f + warp * sin(t * 1.3f)
        val wy = 1f - warp * sin(t * 1.3f)

        withTransform({
            translate(ox, oy)
            scale(s, s, Offset.Zero)
            scale(wx, wy, Offset(VW / 2f, VH / 2f))
        }) {
            stars.draw(this, vis, t, L, pal, pp)
            sacredGeometry(VW / 2f, 215f, t, L, fx, quality, pal, pp)

            val rainbowReveal = smooth(0.42f, 0.62f, L)
            val wob = (0.6f + 2.4f * smooth(0.75f, 1f, L)) * fx
            liquidRainbow(VW / 2f, 370f, 430f, 285f, t, rainbowReveal, wob, pal, pp)
            if (quality > 0.6f) {
                liquidRainbow(VW * 0.82f, 360f, 150f, 120f, t * 1.2f, smooth(0.85f, 1f, L), wob * 1.4f, pal, pp, 7f)
            }

            // Nubes y criaturas del cielo
            happyCloud(165f, 78f, 0.85f, t, L, 0f, pal, pp)
            happyCloud(850f, 68f, 0.7f, t, L, 2.1f, pal, pp)
            ufoAlien(330f, 92f, 0.75f, t, L, smooth(0.65f, 0.8f, L), pal.green, 0f, pal, pp)
            ufoAlien(655f, 80f, 0.62f, t, L, smooth(0.82f, 0.95f, L), lerpColor(pal.turquoise, pal.yellow, 0.3f), 2.4f, pal, pp)
            eyeJelly(120f, 215f, 0.78f, t, smooth(0.78f, 0.93f, L), pal, pp)

            // Colina trasera + gatitos DJ que se asoman
            Landscape.drawBack(this, vis, t, L, fx, pal, pp)
            djCat(250f, 358f, 0.72f, t, smooth(0.6f, 0.72f, L), pal.orange, pal.turquoise, 0f, pal, pp)
            djCat(752f, 362f, 0.72f, t, smooth(0.66f, 0.8f, L), lerpColor(pal.purple, Color.White, 0.45f), pal.pink, 2.3f, pal, pp)

            // Colina frontal, honguitos y gotitas
            Landscape.drawFront(this, vis, t, L, fx, pal, pp)
            val hw = Landscape.wobble(L, fx)
            mushroom(80f, Landscape.frontY(80f, t, hw) + 6f, 0.62f, t, L, 0f, pal.pink, fx, pal, pp)
            mushroom(160f, Landscape.frontY(160f, t, hw) + 8f, 0.5f, t, L, 1.7f, pal.orange, fx, pal, pp)
            mushroom(920f, Landscape.frontY(920f, t, hw) + 6f, 0.6f, t, L, 3.1f, pal.turquoise, fx, pal, pp)
            slime(385f, Landscape.frontY(385f, t, hw) + 8f, 0.62f, t, smooth(0.45f, 0.6f, L), pal.yellow, 0f, pal, pp)
            slime(620f, Landscape.frontY(620f, t, hw) + 8f, 0.55f, t, smooth(0.5f, 0.65f, L), pal.blue, 1.4f, pal, pp)
            slime(985f, Landscape.frontY(985f, t, hw) + 8f, 0.5f, t, smooth(0.7f, 0.85f, L), pal.green, 2.7f, pal, pp)

            // Criaturas dormidas (fase 1): "Z" flotantes
            val sleepy = 1f - smooth(0.1f, 0.28f, L)
            if (sleepy > 0.01f) {
                for (k in 0..2) {
                    val ph = fract(clock * 0.25f + k / 3f)
                    val col = lerpColor(Color.White, pal.turquoise, 0.4f).copy(alpha = sleepy * (1f - ph) * 0.9f)
                    if (col.alpha > 0.01f) {
                        sleepZ(205f + ph * 30f + k * 4f, 56f - ph * 45f, 5f + ph * 5f, col, pp)
                        sleepZ(100f + ph * 22f, Landscape.frontY(80f, t, hw) - 60f - ph * 40f, 3.5f + ph * 4f, col, pp)
                        sleepZ(950f - ph * 22f, Landscape.frontY(920f, t, hw) - 58f - ph * 40f, 3.5f + ph * 4f, col, pp)
                    }
                }
            }

            // Protagonistas
            axolotl(835f, 205f, 0.82f, t, L, smooth(0.4f, 0.58f, L), fx, pal, pp)
            heroMouth(VW / 2f, 262f, 0.95f, t, L, fx, pal, pp)

            // Partículas encima de todo
            particles.draw(this, pal, (0.5f + 0.5f * fx).coerceAtMost(1f), pp)
        }

        if (flash > 0.01f) {
            drawRect(Color.White.copy(alpha = flash * 0.16f))
        }
        Unit
    }

    private fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)

    companion object {
        const val VW = 1000f
        const val VH = 420f
    }
}
