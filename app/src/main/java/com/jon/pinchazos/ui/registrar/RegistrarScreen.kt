package com.jon.pinchazos.ui.registrar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jon.pinchazos.data.Insulina
import com.jon.pinchazos.data.Pinchazo
import com.jon.pinchazos.dominio.Formatos
import com.jon.pinchazos.ui.comun.ElegirMomento
import com.jon.pinchazos.ui.comun.SelectorDosis
import com.jon.pinchazos.ui.comun.TextoCambiante
import com.jon.pinchazos.ui.comun.encogerAlPulsar
import com.jon.pinchazos.ui.theme.LocalColoresExtra
import java.time.ZoneId

@Composable
fun RegistrarScreen(
    estado: RegistrarUiState,
    onInsulina: (Long) -> Unit,
    onDosis: (Double) -> Unit,
    onHora: (Long?) -> Unit,
    onRegistrar: () -> Unit,
    onCorregir: (Pinchazo) -> Unit,
    onNuevaInsulina: () -> Unit,
    onEditarInsulina: (Insulina) -> Unit,
) {
    val insulina = estado.insulina
    if (insulina == null) {
        // Hasta saber si hay insulinas no se enseña nada, para no parpadear el estado vacío.
        if (estado.cargado) SinInsulinas(onNuevaInsulina)
        return
    }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.heightIn(min = 4.dp))
            SelectorInsulina(estado.insulinas, insulina, onInsulina, onEditarInsulina, onNuevaInsulina)
            TarjetaUltimo(estado, onCorregir)
            // Mientras se lee la última dosis no se enseña ningún número, para no mostrar uno que no es.
            SelectorDosis(
                insulina = insulina,
                dosis = estado.dosis ?: 0.0,
                onCambiar = onDosis,
                modifier = Modifier.alpha(if (estado.dosis == null) 0f else 1f),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿Cuándo?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ElegirMomento(estado.horaElegida, estado.ahora, onHora)
            }
            Spacer(Modifier.heightIn(min = 4.dp))
        }
        BotonRegistrar(estado, onRegistrar)
    }
}

@Composable
private fun SinInsulinas(onNueva: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No hay ninguna insulina", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            "Añade la que te pinchas: su nombre, si la pluma marca medias unidades y la dosis máxima.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.heightIn(min = 8.dp))
        Button(onClick = onNueva, modifier = Modifier.heightIn(min = 48.dp)) { Text("Añadir insulina") }
    }
}

@Composable
private fun TarjetaUltimo(estado: RegistrarUiState, onCorregir: (Pinchazo) -> Unit) {
    val extra = LocalColoresExtra.current
    val fondo by animateColorAsState(
        if (estado.reciente) extra.reciente else MaterialTheme.colorScheme.surfaceContainerLowest,
        animationSpec = tween(250),
        label = "fondoUltimo",
    )
    val texto = if (estado.reciente) extra.onReciente else MaterialTheme.colorScheme.onSurface
    val zona = ZoneId.systemDefault()
    val hoy = Formatos.fecha(estado.ahora, zona)

    Card(
        colors = CardDefaults.cardColors(containerColor = fondo, contentColor = texto),
        elevation = CardDefaults.cardElevation(defaultElevation = if (estado.reciente) 0.dp else 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                        modifier = Modifier.heightIn(min = 40.dp),
                    ) { Text("Corregir o borrar") }
                }
            }
            if (ultimo == null) {
                Text("Todavía no hay ninguno", style = MaterialTheme.typography.titleLarge)
            } else {
                TextoCambiante(
                    Formatos.transcurrido(ultimo.timestamp, estado.ahora),
                    MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                )
                Text(
                    "${Formatos.unidades(ultimo.dosis)} ${Formatos.cuando(ultimo.timestamp, hoy, zona)}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = texto.copy(alpha = 0.15f))
            TextoCambiante(
                "Hoy: ${Formatos.pinchazos(estado.hoyPinchazos)} · ${Formatos.unidades(estado.hoyUnidades)}",
                MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Fila de insulinas (con scroll si no caben) y "+ Añadir" al final. Tocar la ya elegida, o
 * mantener pulsada cualquiera, abre su edición.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectorInsulina(
    insulinas: List<Insulina>,
    elegida: Insulina,
    onElegir: (Long) -> Unit,
    onEditar: (Insulina) -> Unit,
    onNueva: () -> Unit,
) {
    val haptica = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        insulinas.forEach { i ->
            val esElegida = i.id == elegida.id
            val interaccion = remember { MutableInteractionSource() }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (esElegida) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (esElegida) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = if (esElegida) null else ButtonDefaults.outlinedButtonBorder(),
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .widthIn(max = 240.dp)
                    .encogerAlPulsar(interaccion)
                    .semantics {
                        selected = esElegida
                        role = Role.Tab
                        onLongClick(label = "Editar ${i.nombre}") {
                            onEditar(i)
                            true
                        }
                    }
                    .combinedClickable(
                        interactionSource = interaccion,
                        indication = ripple(),
                        onClick = { if (esElegida) onEditar(i) else onElegir(i.id) },
                        onLongClick = {
                            haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEditar(i)
                        },
                    ),
            ) {
                Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(i.nombre, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        TextButton(onClick = onNueva) { Text("+ Añadir") }
    }
}

/**
 * El botón dice exactamente lo que se va a guardar ("Registrar 4,5 u · ahora"), así la hora
 * cambiada nunca pasa desapercibida aunque los botones de hora queden fuera de la pantalla.
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
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .heightIn(min = 64.dp)
            .encogerAlPulsar(interaccion, escala = 0.97f),
    ) {
        if (boton == EstadoBoton.REGISTRADO) {
            Text("✓ Registrado", style = MaterialTheme.typography.titleLarge)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Registrar pinchazo", style = MaterialTheme.typography.titleLarge)
                val dosis = estado.dosis
                if (dosis != null) {
                    val cuando = estado.horaElegida?.let {
                        Formatos.cuando(it, Formatos.fecha(estado.ahora, zona), zona)
                    } ?: "ahora"
                    Text(
                        "${Formatos.unidades(dosis)} · $cuando",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
