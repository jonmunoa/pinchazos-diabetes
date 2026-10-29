package com.jon.pinchazos.ui.informes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jon.pinchazos.data.Pinchazo
import com.jon.pinchazos.dominio.DiaPinchazos
import com.jon.pinchazos.dominio.Formatos
import com.jon.pinchazos.dominio.Periodo
import com.jon.pinchazos.ui.comun.Iconos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun InformesScreen(
    viewModel: InformesViewModel,
    estado: InformesUiState,
    onEditar: (Pinchazo) -> Unit,
) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()

    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val texto = withContext(Dispatchers.IO) {
                runCatching {
                    contexto.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (texto == null) viewModel.avisar("No se pudo leer el archivo") else viewModel.importarCsv(texto)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Periodo.entries.forEachIndexed { i, periodo ->
                    SegmentedButton(
                        selected = estado.periodo == periodo,
                        onClick = { viewModel.elegirPeriodo(periodo) },
                        shape = SegmentedButtonDefaults.itemShape(i, Periodo.entries.size),
                        icon = {},
                        modifier = Modifier.height(48.dp),
                    ) { Text(periodo.etiqueta, maxLines = 1) }
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(IntrinsicSize.Min),
            ) {
                val n = estado.resumen.pinchazos
                val celda = Modifier.weight(1f).fillMaxHeight()
                Cifra("$n", if (n == 1) "pinchazo" else "pinchazos", celda)
                Cifra(Formatos.numero(estado.resumen.unidades), "unidades", celda)
                Cifra(Formatos.numero(estado.resumen.mediaDiaria), "u de media al día", celda)
            }
        }

        item {
            Seccion("Momento del día") { BarrasFranjas(estado.franjas) }
        }

        item {
            Seccion("Cuándo y cuánto") {
                if (estado.diasGrafico.isEmpty()) {
                    TextoVacio()
                } else {
                    GraficoCuandoCuanto(estado.diasGrafico, estado.hoy)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val csv = viewModel.exportarCsv()
                            runCatching { CompartirCsv.compartir(contexto, csv) }
                                .onFailure { viewModel.avisar("No se pudo exportar") }
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Exportar CSV") }
                OutlinedButton(
                    onClick = { importar.launch(CompartirCsv.TIPOS_IMPORTABLES) },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Importar CSV") }
            }
        }

        item {
            Column(Modifier.padding(top = 8.dp)) {
                Text("Historial", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Toca un pinchazo para cambiar la dosis o la hora.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (estado.historial.isEmpty()) {
            item { TextoVacio() }
        }

        estado.historial.forEach { dia ->
            item(key = "dia-${dia.fecha}") {
                TarjetaDia(dia, estado.hoy, onEditar = onEditar, onBorrar = viewModel::borrar)
            }
        }
    }
}

@Composable
private fun Cifra(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 16.dp).heightIn(min = 72.dp)) {
            Text(
                valor,
                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                maxLines = 1,
            )
            Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge)
            contenido()
        }
    }
}

@Composable
private fun TextoVacio() {
    Text(
        "No hay pinchazos en este periodo",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Un día del historial: cabecera con el total y, debajo, sus pinchazos del más reciente al más antiguo. */
@Composable
private fun TarjetaDia(
    dia: DiaPinchazos,
    hoy: LocalDate,
    onEditar: (Pinchazo) -> Unit,
    onBorrar: (Pinchazo) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    Formatos.etiquetaDia(dia.fecha, hoy),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        "${Formatos.pinchazos(dia.pinchazos.size)} · ${Formatos.unidades(dia.unidades)}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            dia.pinchazos.forEachIndexed { i, p ->
                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                FilaPinchazo(p, onEditar = { onEditar(p) }, onBorrar = { onBorrar(p) })
            }
        }
    }
}

/** Hora | dosis | papelera. Al pulsar la papelera, la propia fila pide confirmación. */
@Composable
private fun FilaPinchazo(p: Pinchazo, onEditar: () -> Unit, onBorrar: () -> Unit) {
    val zona = ZoneId.systemDefault()
    val hora = Formatos.hora(p.timestamp, zona)
    var confirmando by remember(p.id) { mutableStateOf(false) }

    if (confirmando) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 12.dp, end = 4.dp).heightIn(min = 64.dp),
            ) {
                Text(
                    "¿Borrar ${Formatos.unidades(p.dosis)} de las $hora?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { confirmando = false }, modifier = Modifier.height(48.dp)) {
                    Text("No")
                }
                Button(
                    onClick = {
                        confirmando = false
                        onBorrar()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.height(48.dp),
                ) { Text("Borrar") }
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditar)
                .heightIn(min = 64.dp)
                .padding(start = 16.dp, end = 4.dp),
        ) {
            Text(
                hora,
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Normal,
                modifier = Modifier.width(84.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    Formatos.unidades(p.dosis),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    p.insulina,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { confirmando = true }, modifier = Modifier.height(48.dp).width(48.dp)) {
                Icon(
                    Iconos.Papelera,
                    contentDescription = "Borrar el pinchazo de las $hora",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
