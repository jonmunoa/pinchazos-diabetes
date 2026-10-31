package com.jon.pinchazos.ui.comun

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jon.pinchazos.data.Insulina
import com.jon.pinchazos.dominio.Formatos

/** Encoge un poco el elemento mientras se pulsa, para que se note que responde. */
@Composable
fun Modifier.encogerAlPulsar(interaccion: InteractionSource, escala: Float = 0.96f): Modifier {
    val pulsado by interaccion.collectIsPressedAsState()
    val actual by animateFloatAsState(
        targetValue = if (pulsado) escala else 1f,
        animationSpec = tween(durationMillis = 140, easing = Movimiento.Salida),
        label = "encoger",
    )
    return graphicsLayer {
        scaleX = actual
        scaleY = actual
    }
}

/**
 * Número grande con − y + en los pasos de la insulina y, debajo, los botones rápidos: todos los
 * valores si caben (0 a 12 de media en media) o el 25, 50, 75 y 100 % del máximo.
 */
@Composable
fun SelectorDosis(insulina: Insulina, dosis: Double, onCambiar: (Double) -> Unit, modifier: Modifier = Modifier) {
    val paso = if (insulina.paso == 0.5) "media unidad" else "${Formatos.numero(insulina.paso)} u"
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BotonPaso("−", "Restar $paso", enabled = dosis > 0) {
                onCambiar(insulina.sumarPasos(dosis, -1))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    Formatos.numero(dosis),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        lineHeight = 68.sp,
                        fontFeatureSettings = "tnum",
                    ),
                )
                Text(
                    " u",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            BotonPaso("+", "Sumar $paso", enabled = dosis < insulina.tope) {
                onCambiar(insulina.sumarPasos(dosis, 1))
            }
        }
        val atajos = insulina.atajos
        Cuadricula(atajos, columnas = if (atajos.size > 4) 5 else 4, dosis, onCambiar)
    }
}

@Composable
private fun BotonPaso(texto: String, descripcion: String, enabled: Boolean, onClick: () -> Unit) {
    val interaccion = remember { MutableInteractionSource() }
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaccion,
        modifier = Modifier
            .size(64.dp)
            .encogerAlPulsar(interaccion)
            .semantics { contentDescription = descripcion },
    ) {
        Text(texto, fontSize = 32.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Cuadricula(valores: List<Double>, columnas: Int, dosis: Double, onCambiar: (Double) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        valores.chunked(columnas).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { valor ->
                    val elegido = valor == dosis
                    val interaccion = remember { MutableInteractionSource() }
                    Surface(
                        onClick = { onCambiar(valor) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (elegido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                        contentColor = if (elegido) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        shadowElevation = if (elegido) 0.dp else 1.dp,
                        interactionSource = interaccion,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .encogerAlPulsar(interaccion)
                            .semantics { selected = elegido },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                Formatos.numero(valor),
                                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = if (elegido) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }
                // La última fila incompleta mantiene el ancho de las demás.
                repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
