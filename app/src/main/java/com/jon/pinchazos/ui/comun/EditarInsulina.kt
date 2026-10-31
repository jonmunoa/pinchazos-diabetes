package com.jon.pinchazos.ui.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jon.pinchazos.data.Insulina
import com.jon.pinchazos.dominio.Formatos
import kotlinx.coroutines.launch

/**
 * Hoja inferior para dar de alta una insulina ([insulina] null) o cambiarla: nombre, de cuánto en
 * cuánto va la pluma y la dosis máxima. Las que tienen pinchazos se pueden archivar (dejan de salir
 * en Registrar y sus pinchazos se quedan) o borrar del todo.
 *
 * [onGuardar] devuelve el error a enseñar, o null si se ha guardado (y entonces se cierra).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarInsulina(
    insulina: Insulina?,
    onGuardar: suspend (Insulina) -> String?,
    tienePinchazos: suspend (Insulina) -> Boolean,
    onBorrar: (Insulina) -> Unit,
    onCerrar: () -> Unit,
) {
    val inicial = insulina ?: Insulina(nombre = "")
    var nombre by remember { mutableStateOf(inicial.nombre) }
    var paso by remember { mutableDoubleStateOf(inicial.paso) }
    var maximo by remember { mutableStateOf(Formatos.numero(inicial.maximo)) }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var confirmandoBorrado by remember { mutableStateOf(false) }
    // null mientras se comprueba; solo se ofrece "Archivar" a las que tienen pinchazos.
    var conPinchazos by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    if (insulina != null) {
        LaunchedEffect(insulina.id) { conPinchazos = tienePinchazos(insulina) }
    }

    fun guardar(cambiada: Insulina) {
        if (guardando) return
        guardando = true
        scope.launch {
            error = onGuardar(cambiada)
            guardando = false
            if (error == null) onCerrar()
        }
    }

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
            Text(
                if (insulina == null) "Nueva insulina" else "Editar insulina",
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it
                    error = null
                },
                label = { Text("Nombre") },
                placeholder = { Text("Humalog, Lantus, Tresiba…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "La pluma marca",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Insulina.PASOS.forEach { p ->
                        FilterChip(
                            selected = paso == p,
                            onClick = { paso = p },
                            label = {
                                Text(
                                    if (p == 0.5) "Medias unidades" else "Unidades enteras",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.heightIn(min = 40.dp),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = maximo,
                onValueChange = {
                    maximo = it
                    error = null
                },
                label = { Text("Dosis máxima") },
                suffix = { Text("u") },
                supportingText = { Text("Lo más que se pincha de una vez. Hasta ahí llegan el + y los botones rápidos.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            }

            if (confirmandoBorrado && insulina != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("¿Borrar ${insulina.nombre}?", style = MaterialTheme.typography.titleMedium)
                        if (conPinchazos == true) {
                            Text(
                                "Se borran también todos sus pinchazos. No se puede deshacer. " +
                                    "Si solo quieres que deje de salir, archívala.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { confirmandoBorrado = false },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) { Text("Cancelar") }
                            Button(
                                onClick = { onBorrar(insulina) },
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val max = maximo.trim().replace(',', '.').toDoubleOrNull()
                            if (max == null || max <= 0) {
                                error = "Pon una dosis máxima, por ejemplo 12"
                            } else {
                                guardar(inicial.copy(nombre = nombre, paso = paso, maximo = max))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Guardar") }
                    if (insulina != null && conPinchazos != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            if (insulina.archivada) {
                                TextButton(
                                    onClick = { guardar(insulina.copy(archivada = false)) },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                ) { Text("Recuperar") }
                            } else if (conPinchazos == true) {
                                TextButton(
                                    onClick = { guardar(insulina.copy(archivada = true)) },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                ) { Text("Archivar") }
                            }
                            TextButton(
                                onClick = { confirmandoBorrado = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) { Text("Borrar") }
                        }
                    }
                }
            }
        }
    }
}
