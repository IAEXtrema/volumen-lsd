package com.psychovolume.app.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psychovolume.app.data.VisualSettings
import com.psychovolume.app.engine.Palettes
import com.psychovolume.app.overlay.FloatingController
import com.psychovolume.app.ui.theme.ElectricPink
import com.psychovolume.app.ui.theme.Night
import com.psychovolume.app.ui.theme.Sun
import com.psychovolume.app.ui.theme.Turquoise
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val s by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var canOverlay by remember { mutableStateOf(FloatingController.canDrawOverlays(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        canOverlay = FloatingController.canDrawOverlays(context)
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val update: ((VisualSettings) -> VisualSettings) -> Unit = vm::updateSettings

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Night, Color(0xFF1C0E3E))))
            .safeDrawingPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Text("Ajustes", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Section("Animación")
            SliderSetting("Intensidad de efectos", s.intensity, 0.3f..1.5f, pct(s.intensity)) { v -> update { it.copy(intensity = v) } }
            SliderSetting("Velocidad de animación", s.speed, 0.25f..2f, "×" + "%.2f".format(s.speed)) { v -> update { it.copy(speed = v) } }
            SliderSetting("Cantidad de partículas", s.particles, 0f..2f, pct(s.particles)) { v -> update { it.copy(particles = v) } }
            SliderSetting(
                "Sensibilidad visual", s.sensitivity, 0f..1f, pct(s.sensitivity),
                hint = "Qué tan rápido reacciona el universo a cada cambio de volumen.",
            ) { v -> update { it.copy(sensitivity = v) } }

            Section("Paleta cromática")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Palettes.all.forEachIndexed { i, p ->
                    val selected = s.palette == i
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            Modifier
                                .size(54.dp)
                                .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color.White.copy(alpha = 0.3f), CircleShape)
                                .padding(4.dp)
                                .background(Brush.sweepGradient(p.rainbow + p.rainbow.first()), CircleShape)
                                .clickable { update { it.copy(palette = i) } },
                        )
                        Text(p.name, color = Color.White.copy(alpha = if (selected) 1f else 0.6f), fontSize = 11.sp, maxLines = 2)
                    }
                }
            }

            Section("Accesibilidad y batería")
            SwitchSetting(
                "Reducir movimiento intenso", s.reduceMotion,
                "Desactiva destellos, suaviza el caleidoscopio y baja la velocidad. Se activa solo si el sistema tiene las animaciones desactivadas.",
            ) { v -> update { it.copy(reduceMotion = v) } }
            SwitchSetting("Destellos", s.flashes, "Pulsos de luz en la fase 5 (siempre por debajo de 3 por segundo).") { v -> update { it.copy(flashes = v) } }
            SwitchSetting("Modo ahorro de batería", s.batterySaver, "Limita a ~30 FPS y reduce partículas.") { v -> update { it.copy(batterySaver = v) } }

            Section("Cápsula")
            SliderSetting("Tamaño del controlador", s.capsuleScale, 0.6f..1f, pct(s.capsuleScale)) { v -> update { it.copy(capsuleScale = v) } }
            SliderSetting("Transparencia", 1f - s.opacity, 0f..0.65f, pct(1f - s.opacity)) { v -> update { it.copy(opacity = 1f - v) } }

            Section("Controlador flotante")
            SwitchSetting(
                "Cápsula sobre otras apps", s.floating && canOverlay,
                if (canOverlay) "Arrástrala por la pantalla; tócala para expandirla."
                else "Requiere el permiso «Mostrar sobre otras apps».",
            ) { v ->
                if (v) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    if (!FloatingController.canDrawOverlays(context)) {
                        context.startActivity(FloatingController.overlayPermissionIntent(context))
                    }
                }
                update { it.copy(floating = v) }
            }
            if (s.floating && !canOverlay) {
                TextButton(onClick = { context.startActivity(FloatingController.overlayPermissionIntent(context)) }) {
                    Text("Conceder permiso", color = Sun)
                }
            }
            Text(
                "Android no permite que una app reemplace el panel de volumen del sistema. " +
                    "La cápsula flotante es una ventana propia que reacciona al volumen; el panel nativo " +
                    "seguirá apareciendo al pulsar los botones físicos.",
                color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, lineHeight = 17.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

private fun pct(v: Float) = "${(v * 100).roundToInt()} %"

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(), color = Turquoise, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 2.dp),
    )
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    hint: String? = null,
    onChange: (Float) -> Unit,
) {
    // Valor local mientras se arrastra; se persiste al soltar para no escribir en disco a cada píxel.
    var local by remember(value) { mutableStateOf(value) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(valueLabel, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
        if (hint != null) Text(hint, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = ElectricPink,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f),
            ),
        )
    }
}

@Composable
private fun SwitchSetting(label: String, checked: Boolean, hint: String, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold)
            Text(hint, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElectricPink,
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
            ),
        )
    }
}
