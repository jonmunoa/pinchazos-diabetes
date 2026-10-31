package com.jon.pinchazos.ui.informes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jon.pinchazos.dominio.DiaPinchazos
import com.jon.pinchazos.dominio.Formatos
import com.jon.pinchazos.dominio.Informes
import com.jon.pinchazos.dominio.ResumenFranja
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.sqrt

/** Barras horizontales de unidades por franja del día. */
@Composable
fun BarrasFranjas(franjas: List<ResumenFranja>) {
    val maximo = franjas.maxOfOrNull { it.unidades }?.takeIf { it > 0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        franjas.forEach { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(112.dp)) {
                    Text(f.franja.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(
                        f.franja.rango,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    val fraccion = (f.unidades / maximo).toFloat()
                    if (fraccion > 0f) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraccion)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
                Text(
                    "${Formatos.unidades(f.unidades)} · ${f.pinchazos}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(84.dp),
                )
            }
        }
    }
}

/**
 * Una fila por día con un eje de 0 a 24 h; cada pinchazo es un círculo en su hora, con
 * el tamaño según la dosis y las unidades dentro.
 */
@Composable
fun GraficoCuandoCuanto(dias: List<DiaPinchazos>, hoy: LocalDate, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer()
    val colores = MaterialTheme.colorScheme
    val estiloEje = MaterialTheme.typography.labelMedium.copy(color = colores.onSurfaceVariant)
    val estiloDia = MaterialTheme.typography.labelLarge.copy(color = colores.onSurface)
    val estiloDosis = MaterialTheme.typography.labelMedium.copy(
        color = colores.onPrimary,
        fontWeight = FontWeight.Bold,
    )
    val zona = ZoneId.systemDefault()
    // El círculo más grande es la dosis más alta del gráfico (12 u como poco, como la pluma de siempre).
    val dosisMaxima = maxOf(12.0, dias.maxOfOrNull { d -> d.pinchazos.maxOfOrNull { it.dosis } ?: 0.0 } ?: 0.0)
    val alturaFila = 46.dp
    val alturaCabecera = 22.dp
    val descripcion = dias.joinToString(". ") { d ->
        "${Formatos.etiquetaDia(d.fecha, hoy)}: " + if (d.pinchazos.isEmpty()) {
            "sin pinchazos"
        } else {
            d.pinchazos.joinToString { "${Formatos.unidades(it.dosis)} a las ${Formatos.hora(it.timestamp, zona)}" }
        }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(alturaCabecera + alturaFila * dias.size)
            .semantics { contentDescription = descripcion },
    ) {
        val anchoEtiqueta = 52.dp.toPx()
        val radioMin = 11.dp.toPx()
        val radioMax = 21.dp.toPx()
        val cabecera = alturaCabecera.toPx()
        val fila = alturaFila.toPx()
        val x0 = anchoEtiqueta + radioMax
        val anchoUtil = size.width - x0 - radioMax
        fun x(horas: Float) = x0 + horas / 24f * anchoUtil

        for (h in 0..24 step 6) {
            val xh = x(h.toFloat())
            drawLine(colores.outlineVariant, Offset(xh, cabecera), Offset(xh, size.height), strokeWidth = 1.dp.toPx())
            val t = medidor.measure("$h", estiloEje)
            drawText(t, topLeft = Offset(xh - t.size.width / 2f, 0f))
        }

        dias.forEachIndexed { i, dia ->
            val y = cabecera + fila * i + fila / 2f
            val etiqueta = medidor.measure(Formatos.etiquetaCorta(dia.fecha, hoy), estiloDia)
            drawText(etiqueta, topLeft = Offset(0f, y - etiqueta.size.height / 2f))
            drawLine(colores.outlineVariant, Offset(x(0f), y), Offset(x(24f), y), strokeWidth = 1.dp.toPx())

            val reales = dia.pinchazos.map { p ->
                val momento = Instant.ofEpochMilli(p.timestamp).atZone(zona)
                x(momento.hour + momento.minute / 60f)
            }
            val radios = dia.pinchazos.map { radioMin + (radioMax - radioMin) * sqrt((it.dosis / dosisMaxima).toFloat()) }
            val centros = Informes.repartirSinSolapar(reales, radios, hueco = 3.dp.toPx(), maxX = size.width)

            // Punto en la hora exacta: se ve cuando el círculo ha tenido que apartarse.
            reales.forEach { drawCircle(colores.primary, 3.dp.toPx(), Offset(it, y)) }
            dia.pinchazos.forEachIndexed { j, p ->
                val centro = Offset(centros[j], y)
                drawCircle(colores.surfaceContainerLowest, radios[j] + 2.dp.toPx(), centro)
                drawCircle(colores.primary, radios[j], centro)
                val t = medidor.measure(Formatos.numero(p.dosis), estiloDosis)
                drawText(t, topLeft = Offset(centro.x - t.size.width / 2f, centro.y - t.size.height / 2f))
            }
        }
    }
}
