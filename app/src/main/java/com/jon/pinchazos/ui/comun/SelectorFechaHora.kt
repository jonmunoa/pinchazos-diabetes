package com.jon.pinchazos.ui.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Elegir fecha y luego hora. No deja elegir días futuros, y si el día es hoy tampoco
 * una hora posterior a la actual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFechaHora(inicial: Long, onElegido: (Long) -> Unit, onCancelar: () -> Unit) {
    val zona = remember { ZoneId.systemDefault() }
    val inicialLocal = remember(inicial) { Instant.ofEpochMilli(inicial).atZone(zona) }
    val hoy = remember { LocalDate.now(zona) }
    var fecha by remember { mutableStateOf<LocalDate?>(null) }

    val dia = fecha
    if (dia == null) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = inicialLocal.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(hoy)

                override fun isSelectableYear(year: Int): Boolean = year <= hoy.year
            },
        )
        DatePickerDialog(
            onDismissRequest = onCancelar,
            confirmButton = {
                TextButton(
                    enabled = estado.selectedDateMillis != null,
                    onClick = {
                        fecha = estado.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                    },
                ) { Text("Siguiente") }
            },
            dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
        ) {
            DatePicker(state = estado)
        }
    } else {
        val estado = rememberTimePickerState(
            initialHour = inicialLocal.hour,
            initialMinute = inicialLocal.minute,
            is24Hour = true,
        )
        val elegido = dia.atTime(estado.hour, estado.minute).atZone(zona).toInstant().toEpochMilli()
        val futuro = elegido > System.currentTimeMillis()
        AlertDialog(
            onDismissRequest = onCancelar,
            title = { Text("Hora del pinchazo") },
            text = {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TimePicker(state = estado)
                    if (futuro) {
                        Text(
                            "No puede ser una hora futura",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !futuro, onClick = { onElegido(elegido) }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { fecha = null }) { Text("Atrás") } },
        )
    }
}
