package com.psychovolume.app.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psychovolume.app.engine.PsychoScene
import com.psychovolume.app.engine.RenderParams
import com.psychovolume.app.ui.GlassCapsule
import com.psychovolume.app.ui.phaseColor
import com.psychovolume.app.ui.phaseOf
import kotlin.math.roundToInt

/**
 * Contenido de la ventana flotante.
 *  - Contraída: mini cápsula animada con el porcentaje. Arrastrar = mover; tocar = expandir.
 *  - Expandida: cápsula grande + slider + botones (contraer, abrir app, cerrar).
 */
@Composable
fun FloatingCapsuleContent(
    level: Float,
    params: RenderParams,
    opacity: Float,
    onDrag: (Float, Float) -> Unit,
    onVolume: (Float) -> Unit,
    onOpenApp: () -> Unit,
    onClose: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val shown = dragValue ?: level
    val drag by rememberUpdatedState(onDrag)

    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures { change, amount ->
            change.consume()
            drag(amount.x, amount.y)
        }
    }

    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            (fadeIn() + scaleIn(spring(dampingRatio = 0.7f), initialScale = 0.8f)) togetherWith
                (fadeOut() + scaleOut(targetScale = 0.8f))
        },
        modifier = Modifier.alpha(opacity.coerceIn(0.35f, 1f)).padding(12.dp),
        label = "floating",
    ) { isExpanded ->
        if (!isExpanded) {
            GlassCapsule(
                modifier = Modifier
                    .size(width = 170.dp, height = 72.dp)
                    .then(dragModifier)
                    .pointerInput(Unit) { detectTapGestures(onTap = { expanded = true }) },
            ) {
                PsychoScene(level = shown, params = params, modifier = Modifier.fillMaxSize())
                Text(
                    "${(shown * 100).roundToInt()}%",
                    color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 6.dp)
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        } else {
            Column(
                Modifier
                    .width(320.dp)
                    .background(Color(0xE6140A2E), RoundedCornerShape(30.dp))
                    .padding(10.dp),
            ) {
                GlassCapsule(
                    modifier = Modifier
                        .size(width = 300.dp, height = 128.dp)
                        .then(dragModifier),
                ) {
                    PsychoScene(level = shown, params = params, modifier = Modifier.fillMaxSize())
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        "${(shown * 100).roundToInt()}%",
                        color = phaseColor(phaseOf(shown).number), fontWeight = FontWeight.Black,
                        modifier = Modifier.width(46.dp).padding(start = 6.dp),
                    )
                    Slider(
                        value = shown,
                        onValueChange = {
                            dragValue = it
                            onVolume(it)
                        },
                        onValueChangeFinished = { dragValue = null },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = phaseColor(phaseOf(shown).number),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                        ),
                    )
                }
                Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        phaseOf(shown).name, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
                        modifier = Modifier.weight(1f).padding(start = 6.dp),
                    )
                    Box {
                        Row {
                            IconButton(onClick = { expanded = false }) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Contraer", tint = Color.White)
                            }
                            IconButton(onClick = onOpenApp) {
                                Icon(Icons.Filled.Home, contentDescription = "Abrir PsychoVolume", tint = Color.White)
                            }
                            IconButton(onClick = onClose) {
                                Icon(Icons.Filled.Close, contentDescription = "Cerrar cápsula", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
