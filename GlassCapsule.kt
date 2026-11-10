package com.psychovolume.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cápsula de cristal: recorta el universo y le dibuja encima reflejos, borde iridiscente
 * y una sombra interior para dar volumen.
 */
@Composable
fun GlassCapsule(
    modifier: Modifier = Modifier,
    corner: Dp? = null,
    glow: Color = Color(0xFFFF2BD6),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = if (corner == null) RoundedCornerShape(50) else RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .shadow(elevation = 22.dp, shape = shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .drawWithContent {
                drawContent()
                val r = if (corner == null) size.height / 2f else corner.toPx()
                val cr = CornerRadius(r, r)
                // Sombra interior
                drawRoundRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                        startY = size.height * 0.55f, endY = size.height,
                    ),
                    cornerRadius = cr,
                )
                // Reflejo superior del cristal
                drawRoundRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.04f), Color.Transparent),
                        startY = 0f, endY = size.height * 0.45f,
                    ),
                    topLeft = Offset(size.width * 0.06f, size.height * 0.05f),
                    size = Size(size.width * 0.88f, size.height * 0.38f),
                    cornerRadius = CornerRadius(r * 0.8f, r * 0.8f),
                )
                drawRoundRect(
                    Color.White.copy(alpha = 0.55f),
                    topLeft = Offset(size.width * 0.12f, size.height * 0.09f),
                    size = Size(size.width * 0.18f, size.height * 0.05f),
                    cornerRadius = CornerRadius(size.height, size.height),
                )
                // Borde iridiscente
                val bw = 3.dp.toPx()
                drawRoundRect(
                    Brush.sweepGradient(
                        listOf(
                            Color.White.copy(alpha = 0.9f), Color(0xFF1EE8D8), Color(0xFFFF2BD6),
                            Color(0xFFFFE83B), Color(0xFF9B3DFF), Color.White.copy(alpha = 0.9f),
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                    ),
                    topLeft = Offset(bw / 2f, bw / 2f),
                    size = Size(size.width - bw, size.height - bw),
                    cornerRadius = CornerRadius(r - bw / 2f, r - bw / 2f),
                    style = Stroke(bw),
                )
            },
        content = content,
    )
}
