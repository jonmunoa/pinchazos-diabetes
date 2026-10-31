package com.jon.pinchazos.ui.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jon.pinchazos.data.Insulina
import com.jon.pinchazos.data.Pinchazo
import com.jon.pinchazos.dominio.Formatos
import java.time.LocalDate
import java.time.ZoneId

/**
 * Hoja inferior para cambiar dosis/hora de un registro o borrarlo (con confirmación en la propia
 * hoja). [insulina] es la suya, para que el selector vaya en sus pasos y hasta su máximo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPinchazo(
    pinchazo: Pinchazo,
    insulina: Insulina,
    hoy: LocalDate,
    onGuardar: (Pinchazo) -> Unit,
    onBorrar: (Pinchazo) -> Unit,
    onCerrar: () -> Unit,
) {
    var dosis by remember { mutableDoubleStateOf(pinchazo.dosis) }
    var timestamp by remember { mutableLongStateOf(pinchazo.timestamp) }
    var confirmandoBorrado by remember { mutableStateOf(false) }
    // Un doble toque en Guardar/Borrar no debe aplicar el cambio dos veces.
    var hecho by remember { mutableStateOf(false) }
    val zona = ZoneId.systemDefault()

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text("Editar pinchazo", style = MaterialTheme.typography.titleLarge)
                Text(
                    pinchazo.insulina,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SelectorDosis(insulina, dosis, onCambiar = { dosis = it })

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hora", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${Formatos.etiquetaCorta(Formatos.fecha(timestamp, zona), hoy)}, ${Formatos.hora(timestamp, zona)}",
                    style = MaterialTheme.typography.titleLarge,
                )
                val ahora = remember { System.currentTimeMillis() }
                ElegirMomento(timestamp, ahora, onElegir = { timestamp = it ?: ahora }, permitirAhora = false)
            }

            if (confirmandoBorrado) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "¿Borrar el pinchazo de ${Formatos.unidades(pinchazo.dosis)} " +
                                "${Formatos.cuando(pinchazo.timestamp, hoy, zona)}?",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { confirmandoBorrado = false },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) { Text("Cancelar") }
                            Button(
                                onClick = {
                                    if (!hecho) {
                                        hecho = true
                                        onBorrar(pinchazo)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) { Text("Sí, borrar") }
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { confirmandoBorrado = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Text("Borrar") }
                    Button(
                        onClick = {
                            if (!hecho) {
                                hecho = true
                                onGuardar(pinchazo.copy(dosis = dosis, timestamp = timestamp))
                            }
                        },
                        modifier = Modifier.weight(2f).heightIn(min = 48.dp),
                    ) { Text("Guardar") }
                }
            }
        }
    }
}
