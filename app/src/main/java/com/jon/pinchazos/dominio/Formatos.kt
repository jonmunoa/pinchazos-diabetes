package com.jon.pinchazos.dominio

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Textos con formato español: coma decimal, "hace 2 h 15 min", "Lunes, 26 de octubre"… */
object Formatos {
    val ES: Locale = Locale.forLanguageTag("es-ES")

    private val SIMBOLOS = DecimalFormatSymbols(ES)
    private val HORA = DateTimeFormatter.ofPattern("HH:mm", ES)
    private val DIA_LARGO = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ES)
    private val DIA_LARGO_CON_ANO = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", ES)
    private val DIA_CORTO = DateTimeFormatter.ofPattern("EEE d", ES)
    private val DIA_MES = DateTimeFormatter.ofPattern("d/M", ES)

    /** 4.5 → "4,5"; 4.0 → "4" */
    fun numero(valor: Double): String = DecimalFormat("0.#", SIMBOLOS).format(valor)

    fun unidades(valor: Double): String = "${numero(valor)} u"

    fun pinchazos(n: Int): String = if (n == 1) "1 pinchazo" else "$n pinchazos"

    fun hora(timestamp: Long, zona: ZoneId): String =
        HORA.format(Instant.ofEpochMilli(timestamp).atZone(zona))

    fun fecha(timestamp: Long, zona: ZoneId): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zona).toLocalDate()

    /** "hace 2 h 15 min", "hace 5 min", "hace 1 d 3 h" */
    fun transcurrido(desde: Long, ahora: Long): String {
        val minutos = ((ahora - desde) / 60_000).coerceAtLeast(0)
        if (minutos < 1) return "hace menos de 1 min"
        val dias = minutos / (24 * 60)
        val horas = (minutos % (24 * 60)) / 60
        val mins = minutos % 60
        return when {
            dias > 0 -> "hace $dias d $horas h"
            horas > 0 -> "hace $horas h $mins min"
            else -> "hace $mins min"
        }
    }

    /** "a las 14:10" (hoy), "ayer a las 22:10", "el 26/10 a las 09:05" */
    fun cuando(timestamp: Long, hoy: LocalDate, zona: ZoneId): String {
        val dia = fecha(timestamp, zona)
        val hora = hora(timestamp, zona)
        return when (dia) {
            hoy -> "a las $hora"
            hoy.minusDays(1) -> "ayer a las $hora"
            else -> "el ${DIA_MES.format(dia)} a las $hora"
        }
    }

    /** "Hoy", "Ayer", "Lunes, 26 de octubre" (con año si no es el actual). */
    fun etiquetaDia(dia: LocalDate, hoy: LocalDate): String = when (dia) {
        hoy -> "Hoy"
        hoy.minusDays(1) -> "Ayer"
        else -> (if (dia.year == hoy.year) DIA_LARGO else DIA_LARGO_CON_ANO).format(dia).capitalizada()
    }

    /** "Hoy", "Ayer", "Lun 26" */
    fun etiquetaCorta(dia: LocalDate, hoy: LocalDate): String = when (dia) {
        hoy -> "Hoy"
        hoy.minusDays(1) -> "Ayer"
        else -> DIA_CORTO.format(dia).replace(".", "").capitalizada()
    }

    private fun String.capitalizada() = replaceFirstChar { it.titlecase(ES) }
}
