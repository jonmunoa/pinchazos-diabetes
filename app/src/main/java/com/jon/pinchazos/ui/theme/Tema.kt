package com.jon.pinchazos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Clara: fondo gris muy claro, tarjetas blancas y un solo acento granate. Siempre en claro,
 * aunque el móvil esté en modo oscuro.
 */
private val Claro = lightColorScheme(
    primary = Color(0xFF8A2846),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFBE3EA),
    onPrimaryContainer = Color(0xFF3B0A1B),
    inversePrimary = Color(0xFFFFB1C5),
    secondary = Color(0xFF526077),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFCEDF1),
    onSecondaryContainer = Color(0xFF5E1830),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFF3D6),
    onTertiaryContainer = Color(0xFF4A3000),
    error = Color(0xFFB42318),
    errorContainer = Color(0xFFFDE8E6),
    onErrorContainer = Color(0xFF6E140D),
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF4F6F9),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE6EAF0),
    onSurfaceVariant = Color(0xFF526077),
    outline = Color(0xFF8792A3),
    outlineVariant = Color(0xFFE3E8EF),
    // Tarjetas, hojas y barra de navegación: blancas sobre el fondo gris.
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEEF1F5),
    surfaceContainerHighest = Color(0xFFE6EAF0),
)

/** Colores fuera del esquema de Material: el verde de "pinchazo reciente". */
@Immutable
data class ColoresExtra(val reciente: Color, val onReciente: Color)

private val ExtraClaro = ColoresExtra(reciente = Color(0xFFDDF3EA), onReciente = Color(0xFF0B4A36))

val LocalColoresExtra = staticCompositionLocalOf { ExtraClaro }

private val base = Typography()

/** Tamaños de serie de Material 3; solo los títulos van algo más marcados. */
private val Tipografia = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun PinchazosTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalColoresExtra provides ExtraClaro) {
        MaterialTheme(colorScheme = Claro, typography = Tipografia, content = content)
    }
}
