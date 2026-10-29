package com.jon.pinchazos.dominio

import com.jon.pinchazos.data.Pinchazo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class Periodo(val etiqueta: String, val dias: Int?) {
    HOY("Hoy", 1),
    SIETE_DIAS("7 días", 7),
    TREINTA_DIAS("30 días", 30),
    TODO("Todo", null),
}

/** Franjas del día; [hasta] es exclusivo. */
enum class Franja(val nombre: String, val desde: Int, val hasta: Int) {
    MADRUGADA("Madrugada", 0, 6),
    MANANA("Mañana", 6, 12),
    MEDIODIA("Mediodía", 12, 16),
    TARDE("Tarde", 16, 20),
    NOCHE("Noche", 20, 24);

    val rango: String get() = "$desde–$hasta h"

    companion object {
        fun de(hora: Int): Franja = entries.first { hora >= it.desde && hora < it.hasta }
    }
}

data class Resumen(val pinchazos: Int, val unidades: Double, val mediaDiaria: Double)

data class ResumenFranja(val franja: Franja, val pinchazos: Int, val unidades: Double)

data class DiaPinchazos(val fecha: LocalDate, val pinchazos: List<Pinchazo>) {
    val unidades: Double get() = pinchazos.sumOf { it.dosis }
}

object Informes {
    const val MAX_DIAS_GRAFICO = 14

    fun primerDia(periodo: Periodo, hoy: LocalDate): LocalDate? =
        periodo.dias?.let { hoy.minusDays(it - 1L) }

    fun filtrar(pinchazos: List<Pinchazo>, periodo: Periodo, hoy: LocalDate, zona: ZoneId): List<Pinchazo> {
        val desde = primerDia(periodo, hoy) ?: return pinchazos
        val desdeMs = desde.atStartOfDay(zona).toInstant().toEpochMilli()
        return pinchazos.filter { it.timestamp >= desdeMs }
    }

    /**
     * Totales del periodo. La media diaria divide entre los días del periodo, pero sin contar
     * días anteriores al primer registro (si se empezó a usar la app hace 3 días, "7 días"
     * divide entre 3).
     */
    fun resumen(todos: List<Pinchazo>, periodo: Periodo, hoy: LocalDate, zona: ZoneId): Resumen {
        val delPeriodo = filtrar(todos, periodo, hoy, zona)
        if (delPeriodo.isEmpty()) return Resumen(0, 0.0, 0.0)
        val unidades = delPeriodo.sumOf { it.dosis }
        val dias = rangoDeDias(todos, periodo, hoy, zona)?.let { ChronoUnit.DAYS.between(it.first, it.second) + 1 } ?: 1
        return Resumen(delPeriodo.size, unidades, unidades / dias.coerceAtLeast(1))
    }

    /** Las cinco franjas, en orden, con sus totales (también las vacías). */
    fun porFranja(pinchazos: List<Pinchazo>, zona: ZoneId): List<ResumenFranja> {
        val agrupados = pinchazos.groupBy { Franja.de(hora(it, zona)) }
        return Franja.entries.map { franja ->
            val lista = agrupados[franja].orEmpty()
            ResumenFranja(franja, lista.size, lista.sumOf { it.dosis })
        }
    }

    /** Días con pinchazos, del más reciente al más antiguo; dentro de cada día, también descendente. */
    fun agruparPorDia(pinchazos: List<Pinchazo>, zona: ZoneId): List<DiaPinchazos> =
        pinchazos
            .groupBy { Formatos.fecha(it.timestamp, zona) }
            .map { (fecha, lista) -> DiaPinchazos(fecha, lista.sortedByDescending { it.timestamp }) }
            .sortedByDescending { it.fecha }

    /**
     * Filas del gráfico "Cuándo y cuánto": un día por fila (incluidos los días sin pinchazos,
     * para que se vean los huecos), desde el inicio del periodo o el primer registro hasta hoy.
     * Como mucho [MAX_DIAS_GRAFICO] días; el primero de la lista es el más reciente.
     */
    fun diasGrafico(todos: List<Pinchazo>, periodo: Periodo, hoy: LocalDate, zona: ZoneId): List<DiaPinchazos> {
        val (inicio, fin) = rangoDeDias(todos, periodo, hoy, zona) ?: return emptyList()
        val porDia = filtrar(todos, periodo, hoy, zona).groupBy { Formatos.fecha(it.timestamp, zona) }
        return generateSequence(fin) { it.minusDays(1) }
            .takeWhile { !it.isBefore(inicio) }
            .take(MAX_DIAS_GRAFICO)
            .map { dia -> DiaPinchazos(dia, porDia[dia].orEmpty().sortedBy { it.timestamp }) }
            .toList()
    }

    /**
     * Posiciones horizontales para los círculos de una fila del gráfico de modo que no se tapen
     * (dos pinchazos con minutos de diferencia caerían en el mismo sitio y se vería uno solo).
     * [xs] van en orden de hora; cada círculo se mueve lo mínimo hacia la derecha y, si se sale
     * por [maxX], la fila se recoloca hacia la izquierda.
     */
    fun repartirSinSolapar(xs: List<Float>, radios: List<Float>, hueco: Float, maxX: Float): List<Float> {
        if (xs.isEmpty()) return xs
        val r = xs.indices.map { radios[it] }
        val salida = xs.toMutableList()
        for (i in 1 until salida.size) {
            salida[i] = maxOf(salida[i], salida[i - 1] + r[i - 1] + r[i] + hueco)
        }
        val ultimo = salida.lastIndex
        salida[ultimo] = minOf(salida[ultimo], maxX - r[ultimo])
        for (i in ultimo - 1 downTo 0) {
            salida[i] = minOf(salida[i], salida[i + 1] - r[i + 1] - r[i] - hueco)
        }
        return salida
    }

    /** Del primer día con datos (sin salir del periodo) a hoy; null si no hay datos en el periodo. */
    private fun rangoDeDias(
        todos: List<Pinchazo>,
        periodo: Periodo,
        hoy: LocalDate,
        zona: ZoneId,
    ): Pair<LocalDate, LocalDate>? {
        if (filtrar(todos, periodo, hoy, zona).isEmpty()) return null
        val primerRegistro = Formatos.fecha(todos.minOf { it.timestamp }, zona)
        val inicioPeriodo = primerDia(periodo, hoy)
        val inicio = if (inicioPeriodo == null) primerRegistro else maxOf(inicioPeriodo, primerRegistro)
        return minOf(inicio, hoy) to hoy
    }

    private fun hora(p: Pinchazo, zona: ZoneId): Int =
        Instant.ofEpochMilli(p.timestamp).atZone(zona).hour
}
