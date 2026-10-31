package com.jon.pinchazos.ui.informes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jon.pinchazos.data.Pinchazo
import com.jon.pinchazos.dominio.DiaPinchazos
import com.jon.pinchazos.dominio.Formatos
import com.jon.pinchazos.dominio.Periodo
import com.jon.pinchazos.ui.comun.Iconos
import com.jon.pinchazos.ui.comun.TextoCambiante
import com.jon.pinchazos.ui.comun.entradaEscalonada
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun InformesScreen(
    viewModel: InformesViewModel,
    estado: InformesUiState,
    onEditar: (Pinchazo) -> Unit,
) {
    // Con varias insulinas se ve cuál es cada pinchazo; con una sola sobra.
    val conNombre = estado.filtro == null && estado.filtrables.size > 1
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Informes", style = MaterialTheme.typography.headlineMedium) }

        if (estado.filtrables.size > 1) {
            item { FiltroInsulina(estado, onElegir = viewModel::filtrar) }
        }

        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Periodo.entries.forEachIndexed { i, periodo ->
                    SegmentedButton(
                        selected = estado.periodo == periodo,
                        onClick = { viewModel.elegirPeriodo(periodo) },
                        shape = SegmentedButtonDefaults.itemShape(i, Periodo.entries.size),
                        icon = {},
                    ) { Text(periodo.etiqueta, maxLines = 1) }
                }
            }
        }

        item { Resumen(estado, Modifier.entradaEscalonada(0)) }

        item {
            Seccion("Momento del día", Modifier.entradaEscalonada(1)) { BarrasFranjas(estado.franjas) }
        }

        item {
            Seccion("Cuándo y cuánto", Modifier.entradaEscalonada(2)) {
                if (estado.diasGrafico.isEmpty()) {
                    TextoVacio()
                } else {
                    GraficoCuandoCuanto(estado.diasGrafico, estado.hoy)
                }
            }
        }

        item {
            Column(Modifier.padding(top = 8.dp)) {
                Text("Historial", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Toca un pinchazo para cambiar la dosis o la hora.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (estado.historial.isEmpty()) {
            item { TextoVacio() }
        }

        estado.historial.forEachIndexed { i, dia ->
            item(key = "dia-${dia.fecha}") {
                TarjetaDia(
                    dia,
                    estado.hoy,
                    conNombre,
                    onEditar = onEditar,
                    onBorrar = viewModel::borrar,
                    modifier = Modifier.animateItem().entradaEscalonada(3 + i),
                )
            }
        }
    }
}

/** "Todas" y una por insulina con pinchazos. */
@Composable
private fun FiltroInsulina(estado: InformesUiState, onElegir: (String?) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (listOf<String?>(null) + estado.filtrables).forEach { nombre ->
            FilterChip(
                selected = nombre == estado.filtro,
                onClick = { onElegir(nombre) },
                label = {
                    Text(
                        nombre ?: "Todas",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 200.dp),
                    )
                },
                shape = RoundedCornerShape(10.dp),
            )
        }
    }
}

/** Tres cifras en una sola tarjeta: pinchazos, unidades y media al día. */
@Composable
private fun Resumen(estado: InformesUiState, modifier: Modifier) {
    val r = estado.resumen
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier,
    ) {
        Row(Modifier.padding(vertical = 14.dp)) {
            Cifra("${r.pinchazos}", if (r.pinchazos == 1) "pinchazo" else "pinchazos", Modifier.weight(1f))
            Cifra(Formatos.numero(r.unidades), "unidades", Modifier.weight(1f))
            Cifra(Formatos.numero(r.mediaDiaria), "u al día de media", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Cifra(valor: String, etiqueta: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TextoCambiante(valor, MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"))
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Seccion(titulo: String, modifier: Modifier, contenido: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            contenido()
        }
    }
}

@Composable
private fun TextoVacio() {
    Text(
        "No hay pinchazos en este periodo",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Un día del historial: cabecera con el total y, debajo, sus pinchazos del más reciente al más antiguo. */
@Composable
private fun TarjetaDia(
    dia: DiaPinchazos,
    hoy: LocalDate,
    conNombre: Boolean,
    onEditar: (Pinchazo) -> Unit,
    onBorrar: (Pinchazo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier,
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
            ) {
                Text(
                    Formatos.etiquetaDia(dia.fecha, hoy),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${Formatos.pinchazos(dia.pinchazos.size)} · ${Formatos.unidades(dia.unidades)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            dia.pinchazos.forEachIndexed { i, p ->
                if (i > 0) {
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
                FilaPinchazo(p, conNombre, onEditar = { onEditar(p) }, onBorrar = { onBorrar(p) })
            }
        }
    }
}

/** Hora | dosis (e insulina) | papelera. Al pulsar la papelera, la propia fila pide confirmación. */
@Composable
private fun FilaPinchazo(p: Pinchazo, conNombre: Boolean, onEditar: () -> Unit, onBorrar: () -> Unit) {
    val zona = ZoneId.systemDefault()
    val hora = Formatos.hora(p.timestamp, zona)
    var confirmando by remember(p.id) { mutableStateOf(false) }

    if (confirmando) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 12.dp, end = 4.dp).heightIn(min = 52.dp),
            ) {
                Text(
                    "¿Borrar ${Formatos.unidades(p.dosis)} de las $hora?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { confirmando = false }) { Text("No") }
                Button(
                    onClick = {
                        confirmando = false
                        onBorrar()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Borrar") }
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditar)
                .heightIn(min = 52.dp)
                .padding(start = 16.dp, end = 4.dp)
                .semantics { contentDescription = "$hora, ${Formatos.unidades(p.dosis)} de ${p.insulina}. Toca para cambiarlo." },
        ) {
            Text(
                hora,
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(64.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(Formatos.unidades(p.dosis), style = MaterialTheme.typography.bodyLarge)
                if (conNombre) {
                    Text(
                        p.insulina,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = { confirmando = true }) {
                Icon(
                    Iconos.Papelera,
                    contentDescription = "Borrar el pinchazo de las $hora",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
