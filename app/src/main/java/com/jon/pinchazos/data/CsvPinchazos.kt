package com.jon.pinchazos.data

import com.jon.pinchazos.dominio.Dosis
import com.jon.pinchazos.dominio.Formatos
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * CSV con columnas fecha;hora;insulina;dosis. Usa punto y coma y coma decimal ("4,5")
 * para que Excel en español lo abra bien. Al leer acepta también coma como separador.
 */
object CsvPinchazos {
    const val CABECERA = "fecha;hora;insulina;dosis"

    private const val BOM = '﻿'
    private val FECHA = DateTimeFormatter.ISO_LOCAL_DATE
    private val FECHA_ES = DateTimeFormatter.ofPattern("d/M/yyyy")
    private val HORA = DateTimeFormatter.ofPattern("HH:mm")

    data class Lectura(val pinchazos: List<Pinchazo>, val lineasConError: Int)

    fun escribir(pinchazos: List<Pinchazo>, zona: ZoneId): String = buildString {
        append(BOM) // para que Excel detecte UTF-8
        append(CABECERA).append("\r\n")
        pinchazos.sortedBy { it.timestamp }.forEach { p ->
            val momento = Instant.ofEpochMilli(p.timestamp).atZone(zona)
            append(FECHA.format(momento)).append(';')
            append(HORA.format(momento)).append(';')
            append(campo(p.insulina)).append(';')
            append(Formatos.numero(p.dosis)).append("\r\n")
        }
    }

    /** @throws IllegalArgumentException si el archivo está vacío o no tiene las columnas esperadas. */
    fun leer(texto: String, zona: ZoneId): Lectura {
        val lineas = texto.removePrefix(BOM.toString()).lines().filter { it.isNotBlank() }
        require(lineas.isNotEmpty()) { "El archivo está vacío" }

        val separador = if (lineas.first().contains(';')) ';' else ','
        val cabecera = dividir(lineas.first(), separador).map { it.trim().lowercase() }
        val iFecha = cabecera.indexOf("fecha")
        val iHora = cabecera.indexOf("hora")
        val iInsulina = cabecera.indexOf("insulina")
        val iDosis = cabecera.indexOf("dosis")
        require(iFecha >= 0 && iHora >= 0 && iDosis >= 0) {
            "El archivo no tiene las columnas fecha, hora y dosis"
        }

        var errores = 0
        val pinchazos = lineas.drop(1).mapNotNull { linea ->
            val campos = dividir(linea, separador)
            runCatching {
                val fecha = leerFecha(campos[iFecha].trim())
                val hora = LocalTime.parse(campos[iHora].trim().take(5).padStart(5, '0'), HORA)
                val dosis = campos[iDosis].trim().replace(',', '.').toDouble()
                require(dosis in Dosis.MIN..Dosis.MAX)
                val insulina = campos.getOrNull(iInsulina)?.trim()?.ifEmpty { null }
                    ?: Insulinas.POR_DEFECTO
                Pinchazo(
                    timestamp = fecha.atTime(hora).atZone(zona).toInstant().toEpochMilli(),
                    dosis = Dosis.ajustar(dosis),
                    insulina = insulina,
                )
            }.getOrElse {
                errores++
                null
            }
        }
        return Lectura(pinchazos, errores)
    }

    private fun leerFecha(texto: String): LocalDate =
        if (texto.contains('/')) LocalDate.parse(texto, FECHA_ES) else LocalDate.parse(texto, FECHA)

    private fun campo(valor: String): String =
        if (valor.any { it == ';' || it == ',' || it == '"' || it == '\n' }) {
            "\"" + valor.replace("\"", "\"\"") + "\""
        } else {
            valor
        }

    /** Divide una línea respetando comillas dobles (y comillas escapadas como ""). */
    private fun dividir(linea: String, separador: Char): List<String> {
        val campos = mutableListOf<String>()
        val actual = StringBuilder()
        var entreComillas = false
        var i = 0
        while (i < linea.length) {
            val c = linea[i]
            when {
                c == '"' && entreComillas && linea.getOrNull(i + 1) == '"' -> {
                    actual.append('"')
                    i++
                }
                c == '"' -> entreComillas = !entreComillas
                c == separador && !entreComillas -> {
                    campos += actual.toString()
                    actual.clear()
                }
                else -> actual.append(c)
            }
            i++
        }
        campos += actual.toString()
        return campos
    }
}
