package com.jon.pinchazos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CsvPinchazosTest {
    private val zona = ZoneId.of("Europe/Madrid")

    private fun ts(texto: String) = LocalDateTime.parse(texto).atZone(zona).toInstant().toEpochMilli()

    @Test
    fun escribeCabeceraYFilasConComaDecimal() {
        val csv = CsvPinchazos.escribir(
            listOf(
                Pinchazo(1, ts("2026-10-28T14:10"), 4.5, Insulina.HUMALOG_JUNIOR),
                Pinchazo(2, ts("2026-10-27T08:05"), 2.0, Insulina.HUMALOG_JUNIOR),
            ),
            zona,
        )
        val lineas = csv.removePrefix("﻿").trim().lines()
        assertEquals(CsvPinchazos.CABECERA, lineas[0])
        assertEquals("2026-10-27;08:05;Humalog Junior;2", lineas[1])
        assertEquals("2026-10-28;14:10;Humalog Junior;4,5", lineas[2])
    }

    @Test
    fun loExportadoSeVuelveALeerIgual() {
        val originales = listOf(
            Pinchazo(timestamp = ts("2026-10-27T08:05"), dosis = 2.0, insulina = Insulina.HUMALOG_JUNIOR),
            Pinchazo(timestamp = ts("2026-10-28T14:10"), dosis = 4.5, insulina = Insulina.HUMALOG_JUNIOR),
        )
        val leidos = CsvPinchazos.leer(CsvPinchazos.escribir(originales, zona), zona)
        assertEquals(0, leidos.lineasConError)
        assertEquals(originales, leidos.pinchazos)
    }

    @Test
    fun aceptaComaComoSeparadorFechasEspanolasYCuentaErrores() {
        val texto = """
            fecha,hora,insulina,dosis
            28/10/2026,9:30,Humalog Junior,"3,5"
            2026-10-28,14:00,,1.5
            2026-10-28,15:00,Humalog Junior,150
            esto no es una fecha,10:00,Humalog Junior,2
        """.trimIndent()
        val r = CsvPinchazos.leer(texto, zona)
        assertEquals(2, r.lineasConError)
        assertEquals(listOf(ts("2026-10-28T09:30"), ts("2026-10-28T14:00")), r.pinchazos.map { it.timestamp })
        assertEquals(listOf(3.5, 1.5), r.pinchazos.map { it.dosis })
        assertTrue(r.pinchazos.all { it.insulina == Insulina.HUMALOG_JUNIOR })
    }

    @Test(expected = IllegalArgumentException::class)
    fun sinColumnasEsperadasFalla() {
        CsvPinchazos.leer("a;b;c\n1;2;3", zona)
    }
}
