package com.jon.pinchazos.ui.registrar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jon.pinchazos.data.Insulinas
import com.jon.pinchazos.data.Pinchazo
import com.jon.pinchazos.dominio.Dosis
import com.jon.pinchazos.dominio.Formatos
import com.jon.pinchazos.ui.comun.SelectorDosis
import com.jon.pinchazos.ui.comun.SelectorFechaHora
import com.jon.pinchazos.ui.comun.encogerAlPulsar
import com.jon.pinchazos.ui.theme.LocalColoresExtra
import java.time.ZoneId

@Composable
fun RegistrarScreen(
    estado: RegistrarUiState,
    onDosis: (Double) -> Unit,
    onInsulina: (String) -> Unit,
    onHora: (Long?) -> Unit,
    onRegistrar: () -> Unit,
    onCorregir: (Pinchazo) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            TarjetaUltimo(estado, onCorregir)
            SelectorInsulina(estado.insulina, onInsulina)
            // Mientras se lee la última dosis no se enseña ningún número, para no mostrar uno que no es.
            SelectorDosis(
                dosis = estado.dosis ?: Dosis.POR_DEFECTO,
                onCambiar = onDosis,
                modifier = Modifier.alpha(if (estado.dosis == null) 0f else 1f),
            )
            FilaHora(estado.horaElegida, estado.ahora, onHora)
            Spacer(Modifier.height(4.dp))
        }
        BotonRegistrar(estado, onRegistrar)
    }
}

@Composable
private fun TarjetaUltimo(estado: RegistrarUiState, onCorregir: (Pinchazo) -> Unit) {
    val extra = LocalColoresExtra.current
    val fondo by animateColorAsState(
        if (estado.reciente) extra.reciente else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(250),
        label = "fondoUltimo",
    )
    val texto = if (estado.reciente) extra.onReciente else MaterialTheme.colorScheme.onSurface
    val zona = ZoneId.systemDefault()
    val hoy = Formatos.fecha(estado.ahora, zona)

    Card(
        colors = CardDefaults.cardColors(containerColor = fondo, contentColor = texto),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val ultimo = estado.ultimo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Último pinchazo",
                    style = MaterialTheme.typography.labelLarge,
                    color = texto.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                )
                if (ultimo != null) {
                    TextButton(
                        onClick = { onCorregir(ultimo) },
                        colors = ButtonDefaults.textButtonColors(contentColor = texto),
                        modifier = Modifier.height(40.dp),
                    ) { Text("Corregir o borrar") }
                }
            }
            if (ultimo == null) {
                Text("Todavía no hay ninguno", style = MaterialTheme.typography.headlineSmall)
            } else {
                Text(
                    Formatos.transcurrido(ultimo.timestamp, estado.ahora),
                    style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                )
                Text(
                    "${Formatos.unidades(ultimo.dosis)} ${Formatos.cuando(ultimo.timestamp, hoy, zona)}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = texto.copy(alpha = 0.15f))
            Text(
                "Hoy: ${Formatos.pinchazos(estado.hoyPinchazos)} · ${Formatos.unidades(estado.hoyUnidades)}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun SelectorInsulina(elegida: String, onElegir: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Insulinas.TODAS.forEach { insulina ->
            FilterChip(
                selected = insulina == elegida,
                onClick = { onElegir(insulina) },
                label = { Text(insulina, style = MaterialTheme.typography.titleMedium) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp),
            )
        }
    }
}

@Composable
private fun FilaHora(horaElegida: Long?, ahora: Long, onHora: (Long?) -> Unit) {
    var eligiendo by rememberSaveable { mutableStateOf(false) }
    val zona = ZoneId.systemDefault()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Hora", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (horaElegida == null) {
                    "Ahora"
                } else {
                    val hoy = Formatos.fecha(ahora, zona)
                    "${Formatos.etiquetaCorta(Formatos.fecha(horaElegida, zona), hoy)}, ${Formatos.hora(horaElegida, zona)}"
                },
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (horaElegida != null) {
            TextButton(onClick = { onHora(null) }, modifier = Modifier.height(48.dp)) { Text("Ahora") }
        }
        OutlinedButton(onClick = { eligiendo = true }, modifier = Modifier.height(48.dp)) {
            Text("Cambiar hora")
        }
    }
    if (eligiendo) {
        SelectorFechaHora(
            inicial = horaElegida ?: System.currentTimeMillis(),
            onElegido = {
                onHora(it)
                eligiendo = false
            },
            onCancelar = { eligiendo = false },
        )
    }
}

/**
 * El botón dice exactamente lo que se va a guardar ("4,5 u · ahora"), así la hora cambiada
 * nunca pasa desapercibida aunque la fila "Hora" quede fuera de la pantalla.
 */
@Composable
private fun BotonRegistrar(estado: RegistrarUiState, onRegistrar: () -> Unit) {
    val haptica = LocalHapticFeedback.current
    val interaccion = remember { MutableInteractionSource() }
    val zona = ZoneId.systemDefault()
    val boton = estado.boton

    Button(
        onClick = {
            if (boton == EstadoBoton.LISTO) {
                haptica.performHapticFeedback(HapticFeedbackType.Confirm)
                onRegistrar()
            }
        },
        enabled = boton != EstadoBoton.CARGANDO,
        interactionSource = interaccion,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(80.dp)
            .encogerAlPulsar(interaccion, escala = 0.97f),
    ) {
        if (boton == EstadoBoton.REGISTRADO) {
            Text("✓ Registrado", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Registrar pinchazo", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                val dosis = estado.dosis
                if (dosis != null) {
                    val cuando = estado.horaElegida?.let {
                        Formatos.cuando(it, Formatos.fecha(estado.ahora, zona), zona)
                    } ?: "ahora"
                    Text(
                        "${Formatos.unidades(dosis)} · $cuando",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
