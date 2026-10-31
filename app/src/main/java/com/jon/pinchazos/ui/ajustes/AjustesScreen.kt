package com.jon.pinchazos.ui.ajustes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jon.pinchazos.data.Insulina
import com.jon.pinchazos.dominio.Formatos
import com.jon.pinchazos.ui.comun.Iconos
import com.jon.pinchazos.ui.comun.entradaEscalonada

/** Lo que se configura, en un sitio: las insulinas y los datos. */
@Composable
fun AjustesScreen(
    insulinas: List<Insulina>,
    version: String,
    onAnadirInsulina: () -> Unit,
    onEditarInsulina: (Insulina) -> Unit,
    onExportar: () -> Unit,
    onImportar: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineMedium) }

        item {
            Seccion(
                "Insulinas",
                "Las que te pinchas: si la pluma marca medias unidades y la dosis máxima de cada una.",
                Modifier.entradaEscalonada(0),
            )
        }
        item {
            Tarjeta(Modifier.entradaEscalonada(1)) {
                insulinas.forEachIndexed { i, insulina ->
                    if (i > 0) Separador()
                    Fila(onClick = { onEditarInsulina(insulina) }) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                insulina.nombre,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (insulina.archivada) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (insulina.archivada) {
                                    "Archivada"
                                } else {
                                    val pasos = if (insulina.paso == 0.5) "medias unidades" else "unidades enteras"
                                    "Hasta ${Formatos.unidades(insulina.tope)} · $pasos"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("Editar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (insulinas.isNotEmpty()) Separador()
                Anadir("Añadir insulina", onAnadirInsulina)
            }
        }

        item {
            Seccion(
                "Datos",
                "Todo se guarda solo en este móvil. Puedes sacar una copia en CSV o traer una.",
                Modifier.entradaEscalonada(2),
            )
        }
        item {
            Tarjeta(Modifier.entradaEscalonada(3)) {
                Fila(onClick = onExportar) {
                    Text("Exportar pinchazos (CSV)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
                Separador()
                Fila(onClick = onImportar) {
                    Text("Importar pinchazos (CSV)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Text(
                "Pinchazos $version",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp).entradaEscalonada(4),
            )
        }
    }
}

@Composable
private fun Seccion(titulo: String, texto: String?, modifier: Modifier) {
    Column(modifier.padding(top = 16.dp, bottom = 2.dp)) {
        Text(
            titulo.uppercase(Formatos.ES),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (texto != null) {
            Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Tarjeta(modifier: Modifier, contenido: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) { Column { contenido() } }
}

@Composable
private fun Fila(onClick: () -> Unit, contenido: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) { contenido() }
}

@Composable
private fun Anadir(texto: String, onClick: () -> Unit) {
    Fila(onClick = onClick) {
        Icon(Iconos.Mas, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            texto,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun Separador() {
    HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}
