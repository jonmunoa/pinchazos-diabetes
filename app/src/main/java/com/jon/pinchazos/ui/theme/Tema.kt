package com.jon.pinchazos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Claro = lightColorScheme(
    primary = Color(0xFF8A2846),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E1),
    onPrimaryContainer = Color(0xFF3B0A1B),
    inversePrimary = Color(0xFFFFB1C5),
    secondary = Color(0xFF75565D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E0),
    onSecondaryContainer = Color(0xFF2B151A),
    tertiary = Color(0xFF7B5733),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF22191B),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF22191B),
    surfaceVariant = Color(0xFFF3DDE0),
    onSurfaceVariant = Color(0xFF524346),
    outline = Color(0xFF847376),
    outlineVariant = Color(0xFFD6C2C5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFEF1F2),
    surfaceContainer = Color(0xFFF8EBEC),
    surfaceContainerHigh = Color(0xFFF2E5E7),
    surfaceContainerHighest = Color(0xFFECE0E1),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFE2829F),
    onPrimary = Color(0xFF4F1028),
    primaryContainer = Color(0xFF6B2238),
    onPrimaryContainer = Color(0xFFFFD9E1),
    inversePrimary = Color(0xFF8A2846),
    secondary = Color(0xFFE3BDC4),
    onSecondary = Color(0xFF422930),
    secondaryContainer = Color(0xFF5B3F46),
    onSecondaryContainer = Color(0xFFFFD9E0),
    tertiary = Color(0xFFEFBD94),
    background = Color(0xFF191113),
    onBackground = Color(0xFFEFDFE1),
    surface = Color(0xFF191113),
    onSurface = Color(0xFFEFDFE1),
    surfaceVariant = Color(0xFF524346),
    onSurfaceVariant = Color(0xFFD6C2C5),
    outline = Color(0xFF9F8C8F),
    outlineVariant = Color(0xFF524346),
    surfaceContainerLowest = Color(0xFF140C0E),
    surfaceContainerLow = Color(0xFF22191B),
    surfaceContainer = Color(0xFF261D1F),
    surfaceContainerHigh = Color(0xFF312829),
    surfaceContainerHighest = Color(0xFF3C3234),
)

/** Colores fuera del esquema de Material: el verde de "pinchazo reciente". */
@Immutable
data class ColoresExtra(val reciente: Color, val onReciente: Color)

private val ExtraClaro = ColoresExtra(reciente = Color(0xFFD3F2D8), onReciente = Color(0xFF0B3818))
private val ExtraOscuro = ColoresExtra(reciente = Color(0xFF1F4A2A), onReciente = Color(0xFFCBF0D2))

val LocalColoresExtra = staticCompositionLocalOf { ExtraClaro }

private val base = Typography()

/** Tipografía un punto más grande que la de serie, para leer de un vistazo. */
private val Tipografia = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = base.labelLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
    labelMedium = base.labelMedium.copy(fontSize = 14.sp, lineHeight = 18.sp),
)

@Composable
fun PinchazosTheme(oscuro: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalColoresExtra provides if (oscuro) ExtraOscuro else ExtraClaro,
    ) {
        MaterialTheme(
            colorScheme = if (oscuro) Oscuro else Claro,
            typography = Tipografia,
            content = content,
        )
    }
}
