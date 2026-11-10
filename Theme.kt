package com.psychovolume.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Night = Color(0xFF140A2E)
val NightSoft = Color(0xFF221446)
val ElectricPink = Color(0xFFFF2BD6)
val AcidGreen = Color(0xFFA6FF2E)
val Turquoise = Color(0xFF1EE8D8)
val Sun = Color(0xFFFFE83B)
val Grape = Color(0xFF9B3DFF)

private val scheme = darkColorScheme(
    primary = ElectricPink,
    onPrimary = Color.White,
    secondary = Turquoise,
    onSecondary = Night,
    tertiary = Sun,
    background = Night,
    onBackground = Color.White,
    surface = NightSoft,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2E1D5A),
    onSurfaceVariant = Color(0xFFD8CCFF),
    outline = Color(0xFF7A62C9),
)

private val typography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 40.sp, letterSpacing = (-1).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 17.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp),
)

@Composable
fun PsychoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
