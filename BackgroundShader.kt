package com.psychovolume.app.engine

import android.graphics.RuntimeShader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush

/**
 * Fondo procedural en GPU (AGSL, disponible desde Android 13).
 * Mezcla continuamente: cielo nocturno -> plasma líquido -> espirales -> túnel caleidoscópico.
 * Todos los uniforms se usan en la salida (AGSL elimina los no usados y setFloatUniform fallaría).
 */
class BackgroundShader {

    private val shader = RuntimeShader(SOURCE)
    val brush = ShaderBrush(shader)

    fun update(
        width: Float, height: Float, time: Float, level: Float,
        kaleido: Float, flash: Float, palette: PsychoPalette,
    ) {
        shader.setFloatUniform("res", width, height)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("level", level)
        shader.setFloatUniform("kaleido", kaleido)
        shader.setFloatUniform("flash", flash)
        setColor("c0", palette.pink)
        setColor("c1", palette.purple)
        setColor("c2", palette.turquoise)
        setColor("c3", palette.blue)
        setColor("c4", palette.yellow)
        setColor("night", palette.night)
    }

    private fun setColor(name: String, c: Color) = shader.setFloatUniform(name, c.red, c.green, c.blue)

    private companion object {
        const val SOURCE = """
uniform float2 res;
uniform float time;
uniform float level;
uniform float kaleido;
uniform float flash;
uniform float3 c0;
uniform float3 c1;
uniform float3 c2;
uniform float3 c3;
uniform float3 c4;
uniform float3 night;

float3 pal(float t) {
    float x = fract(t) * 5.0;
    float3 a = c0;
    float3 b = c1;
    float f = x;
    if (x < 1.0) { a = c0; b = c1; f = x; }
    else if (x < 2.0) { a = c1; b = c2; f = x - 1.0; }
    else if (x < 3.0) { a = c2; b = c3; f = x - 2.0; }
    else if (x < 4.0) { a = c3; b = c4; f = x - 3.0; }
    else { a = c4; b = c0; f = x - 4.0; }
    return mix(a, b, smoothstep(0.0, 1.0, f));
}

half4 main(float2 fc) {
    float2 uv = (fc - 0.5 * res) / res.y;
    float L = level;
    float r = length(uv);
    float a = atan(uv.y, uv.x);

    // Plasma líquido
    float p = sin(uv.x * 3.1 + time * 0.35)
            + sin(uv.y * 4.3 - time * 0.27)
            + sin((uv.x + uv.y) * 2.7 + time * 0.21)
            + sin(r * 7.0 - time * 0.6);
    float3 col = pal(p * 0.12 + time * 0.02);

    // Espirales (fase 2+)
    float sp = sin(a * 3.0 + r * 18.0 - time * 2.2);
    float spiralAmt = smoothstep(0.18, 0.5, L);
    col = mix(col, pal(r * 0.8 - time * 0.05 + 0.33), spiralAmt * 0.38 * smoothstep(0.2, 0.9, sp));

    // Túnel caleidoscópico (fase 4-5)
    float n = 8.0;
    float seg = 6.2831853 / n;
    float ka = abs(mod(a + time * 0.15, seg) - 0.5 * seg);
    float z = 0.35 / (r + 0.06) + time * (0.6 + L * 1.4);
    float rings = sin(z * 3.0) * 0.5 + 0.5;
    float petals = sin(ka * 14.0 + z * 1.5) * 0.5 + 0.5;
    float3 tunnel = pal(z * 0.08 + ka * 0.6) * (0.55 + 0.6 * rings * petals);
    col = mix(col, tunnel, kaleido);

    // Calma nocturna -> saturación total
    float wake = smoothstep(0.0, 0.55, L);
    float3 calm = mix(night, mix(night, c1, 0.4), fc.y / res.y) + 0.06 * col;
    col = mix(calm, col, 0.22 + 0.78 * wake);
    col *= 0.6 + 0.55 * smoothstep(0.0, 0.8, L);

    // Viñeta y destello
    col *= 1.0 - 0.35 * smoothstep(0.35, 1.1, r);
    col += flash * 0.2;
    return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
}
"""
    }
}
