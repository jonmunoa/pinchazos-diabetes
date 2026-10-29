package com.jon.pinchazos.ui.comun

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.jon.pinchazos.dominio.Dosis
import com.jon.pinchazos.dominio.Formatos

private val EaseOutFuerte = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/** Encoge un poco el elemento mientras se pulsa, para que se note que responde. */
@Composable
fun Modifier.encogerAlPulsar(interaccion: InteractionSource, escala: Float = 0.96f): Modifier {
    val pulsado by interaccion.collectIsPressedAsState()
    val actual by animateFloatAsState(
        targetValue = if (pulsado) escala else 1f,
        animationSpec = tween(durationMillis = 140, easing = EaseOutFuerte),
        label = "encoger",
    )
    return graphicsLayer {
        scaleX = actual
        scaleY = actual
    }
}

/** Número grande con − y +, y debajo la cuadrícula con todos los valores de 0 a 12. */
@Composable
fun SelectorDosis(dosis: Double, onCambiar: (Double) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BotonPaso("−", "Restar media unidad", enabled = dosis > Dosis.MIN) {
                onCambiar(Dosis.sumarPasos(dosis, -1))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    Formatos.numero(dosis),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        lineHeight = 76.sp,
                        fontFeatureSettings = "tnum",
                    ),
                )
                Text(
                    " u",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            BotonPaso("+", "Sumar media unidad", enabled = dosis < Dosis.MAX) {
                onCambiar(Dosis.sumarPasos(dosis, 1))
            }
        }
        CuadriculaDosis(dosis, onCambiar)
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
            .size(72.dp)
            .encogerAlPulsar(interaccion)
            .semantics { contentDescription = descripcion },
    ) {
        Text(texto, fontSize = 36.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CuadriculaDosis(dosis: Double, onCambiar: (Double) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Dosis.VALORES.chunked(5).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { valor ->
                    val elegido = valor == dosis
                    Surface(
                        onClick = { onCambiar(valor) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (elegido) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        contentColor = if (elegido) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .semantics { selected = elegido },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                Formatos.numero(valor),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (elegido) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}
