package com.psychovolume.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psychovolume.app.engine.PsychoScene
import com.psychovolume.app.engine.RenderParams
import com.psychovolume.app.ui.theme.AcidGreen
import com.psychovolume.app.ui.theme.ElectricPink
import com.psychovolume.app.ui.theme.Grape
import com.psychovolume.app.ui.theme.Night
import com.psychovolume.app.ui.theme.Sun
import com.psychovolume.app.ui.theme.Turquoise
import kotlin.math.roundToInt

data class PhaseInfo(val number: Int, val name: String)

fun phaseOf(level: Float): PhaseInfo = when {
    level < 0.2f -> PhaseInfo(1, "Sueño estelar")
    level < 0.4f -> PhaseInfo(2, "Despertar")
    level < 0.6f -> PhaseInfo(3, "Baile de hongos")
    level < 0.8f -> PhaseInfo(4, "Universo fantástico")
    else -> PhaseInfo(5, "Explosión cósmica")
}

@Composable
fun MainScreen(
    vm: MainViewModel,
    onOpenSettings: () -> Unit,
) {
    val level by vm.displayLevel.collectAsStateWithLifecycle()
    val params by vm.renderParams.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val demo by vm.demo.collectAsStateWithLifecycle()
    val volume by vm.volume.collectAsStateWithLifecycle()
    val fullscreen by vm.fullscreen.collectAsStateWithLifecycle()
    val controls by vm.controlsVisible.collectAsStateWithLifecycle()

    if (fullscreen) {
        FullscreenStage(
            level = level, params = params, demo = demo, controls = controls,
            onToggleControls = vm::toggleControls,
            onSlider = vm::onSliderChange, onSliderDone = vm::onSliderFinished,
            onExit = { vm.setFullscreen(false) },
        )
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Night, Color(0xFF241050), Night)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = ElectricPink)) { append("Psycho") }
                        withStyle(SpanStyle(color = Turquoise)) { append("Volume") }
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { vm.setFullscreen(true) }) { FullscreenGlyph(Color.White) }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = Color.White)
                }
            }

            Spacer(Modifier.weight(1f))

            val interaction = remember { MutableInteractionSource() }
            GlassCapsule(
                modifier = Modifier
                    .fillMaxWidth(settings.capsuleScale.coerceIn(0.6f, 1f))
                    .aspectRatio(2.35f)
                    .alpha(settings.opacity.coerceIn(0.35f, 1f))
                    .clickable(interactionSource = interaction, indication = null) { vm.toggleControls() },
            ) {
                PsychoScene(level = level, params = params, modifier = Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(26.dp))

            AnimatedVisibility(
                visible = controls,
                enter = fadeIn() + slideInVertically { it / 3 },
                exit = fadeOut() + slideOutVertically { it / 3 },
            ) {
                VolumeControls(
                    level = level, demo = demo, fixed = volume.fixed,
                    onSlider = vm::onSliderChange, onSliderDone = vm::onSliderFinished,
                    onStep = vm::stepVolume, onDemo = vm::setDemo,
                )
            }

            Spacer(Modifier.weight(1.3f))
            Text(
                if (controls) "Toca la cápsula para ocultar los controles" else "Toca la cápsula para mostrar los controles",
                color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun VolumeControls(
    level: Float,
    demo: DemoMode,
    fixed: Boolean,
    onSlider: (Float) -> Unit,
    onSliderDone: () -> Unit,
    onStep: (Boolean) -> Unit,
    onDemo: (DemoMode) -> Unit,
) {
    val phase = phaseOf(level)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${(level * 100).roundToInt()}",
                style = MaterialTheme.typography.displaySmall,
                color = phaseColor(phase.number),
            )
            Text("%", color = Color.White.copy(alpha = 0.7f), fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
        }
        Text(
            "Fase ${phase.number} · ${phase.name}" + if (demo != DemoMode.Off) "  (demo)" else "",
            color = phaseColor(phase.number), fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton("–") { onStep(false) }
            Slider(
                value = level,
                onValueChange = onSlider,
                onValueChangeFinished = onSliderDone,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = phaseColor(phase.number),
                    inactiveTrackColor = Color.White.copy(alpha = 0.18f),
                ),
            )
            StepButton("+") { onStep(true) }
        }
        if (fixed) {
            Text(
                "Este dispositivo tiene volumen fijo: usa el modo demo para recorrer las fases.",
                color = Sun, fontSize = 12.sp, textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DemoChip("Volumen real", demo == DemoMode.Off) { onDemo(DemoMode.Off) }
            DemoChip("Demo manual", demo == DemoMode.Manual) { onDemo(DemoMode.Manual) }
            DemoChip("Demo auto", demo == DemoMode.Auto, icon = true) { onDemo(DemoMode.Auto) }
        }
    }
}

@Composable
private fun DemoChip(label: String, selected: Boolean, icon: Boolean = false, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (icon) {
            { Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ElectricPink.copy(alpha = 0.3f),
            selectedLabelColor = Color.White,
            labelColor = Color.White.copy(alpha = 0.75f),
            selectedLeadingIconColor = Sun,
            iconColor = Color.White.copy(alpha = 0.75f),
        ),
    )
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

fun phaseColor(n: Int): Color = when (n) {
    1 -> Color(0xFFB9A8FF)
    2 -> Turquoise
    3 -> AcidGreen
    4 -> Sun
    else -> ElectricPink
}

/** Icono de pantalla completa dibujado (cuatro esquinas). */
@Composable
fun FullscreenGlyph(color: Color, exit: Boolean = false) {
    Canvas(Modifier.size(22.dp)) {
        val s = size.minDimension
        val l = s * 0.32f
        val w = s * 0.11f
        val corners = listOf(Offset(0f, 0f), Offset(s, 0f), Offset(0f, s), Offset(s, s))
        corners.forEach { c ->
            val dx = if (c.x == 0f) 1f else -1f
            val dy = if (c.y == 0f) 1f else -1f
            val p = if (exit) Offset(c.x + dx * l, c.y + dy * l) else c
            val sx = if (exit) -dx else dx
            val sy = if (exit) -dy else dy
            drawLine(color, p, Offset(p.x + sx * l, p.y), w, StrokeCap.Round)
            drawLine(color, p, Offset(p.x, p.y + sy * l), w, StrokeCap.Round)
        }
    }
}

@Composable
private fun FullscreenStage(
    level: Float,
    params: RenderParams,
    demo: DemoMode,
    controls: Boolean,
    onToggleControls: () -> Unit,
    onSlider: (Float) -> Unit,
    onSliderDone: () -> Unit,
    onExit: () -> Unit,
) {
    BackHandler(onBack = onExit)
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GlassCapsule(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .clickable(interactionSource = interaction, indication = null, onClick = onToggleControls),
            corner = 36.dp,
            glow = Grape,
        ) {
            PsychoScene(level = level, params = params, modifier = Modifier.fillMaxSize())
        }
        AnimatedVisibility(
            visible = controls,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            Row(
                Modifier
                    .padding(bottom = 24.dp)
                    .width(460.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${(level * 100).roundToInt()}%",
                    color = phaseColor(phaseOf(level).number), fontWeight = FontWeight.Black, fontSize = 18.sp,
                    modifier = Modifier.width(56.dp),
                )
                Slider(
                    value = level, onValueChange = onSlider, onValueChangeFinished = onSliderDone,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = phaseColor(phaseOf(level).number),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                    ),
                )
                if (demo != DemoMode.Off) Text("demo", color = Sun, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp))
                IconButton(onClick = onExit) {
                    Icon(Icons.Filled.Close, contentDescription = "Salir de pantalla completa", tint = Color.White)
                }
            }
        }
    }
}
