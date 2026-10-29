package com.jon.pinchazos.dominio

import com.jon.pinchazos.data.Insulinas
import com.jon.pinchazos.data.Pinchazo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class InformesTest {
    private val zona = ZoneId.of("Europe/Madrid")
    private val hoy = LocalDate.of(2026, 10, 28)

    private fun p(fecha: LocalDate, hora: String, dosis: Double) = Pinchazo(
        timestamp = LocalDateTime.of(fecha, java.time.LocalTime.parse(hora)).atZone(zona).toInstant().toEpochMilli(),
        dosis = dosis,
        insulina = Insulinas.HUMALOG_JUNIOR,
    )

    private fun haceDias(n: Long) = hoy.minusDays(n)

    // --- Franjas ---

    @Test
    fun franjaDeCadaHoraRespetaLosLimites() {
        val esperado = mapOf(
            0 to Franja.MADRUGADA, 5 to Franja.MADRUGADA,
            6 to Franja.MANANA, 11 to Franja.MANANA,
            12 to Franja.MEDIODIA, 15 to Franja.MEDIODIA,
            16 to Franja.TARDE, 19 to Franja.TARDE,
            20 to Franja.NOCHE, 23 to Franja.NOCHE,
        )
        esperado.forEach { (hora, franja) -> assertEquals("hora $hora", franja, Franja.de(hora)) }
    }

    @Test
    fun porFranjaSumaUnidadesYCuentaPinchazosIncluidasLasVacias() {
        val lista = listOf(
            p(hoy, "05:59", 2.0),
            p(hoy, "06:00", 3.0),
            p(hoy, "13:30", 4.5),
            p(haceDias(1), "13:45", 1.0),
            p(hoy, "23:59", 2.0),
        )
        val r = Informes.porFranja(lista, zona)

        assertEquals(Franja.entries.toList(), r.map { it.franja })
        assertEquals(ResumenFranja(Franja.MADRUGADA, 1, 2.0), r[0])
        assertEquals(ResumenFranja(Franja.MANANA, 1, 3.0), r[1])
        assertEquals(ResumenFranja(Franja.MEDIODIA, 2, 5.5), r[2])
        assertEquals(ResumenFranja(Franja.TARDE, 0, 0.0), r[3])
        assertEquals(ResumenFranja(Franja.NOCHE, 1, 2.0), r[4])
    }

    // --- Totales y media diaria ---

    @Test
    fun resumenDeHoySoloCuentaLosDeHoy() {
        val lista = listOf(p(hoy, "08:00", 2.0), p(hoy, "14:00", 3.5), p(haceDias(1), "22:00", 5.0))
        assertEquals(Resumen(2, 5.5, 5.5), Informes.resumen(lista, Periodo.HOY, hoy, zona))
    }

    @Test
    fun resumenDeSieteDiasExcluyeElOctavoDiaYDivideEntreSiete() {
        val lista = listOf(
            p(haceDias(40), "10:00", 2.0),
            p(haceDias(7), "10:00", 5.0), // fuera: el periodo empieza hace 6 días
            p(haceDias(6), "00:00", 3.0), // dentro: primer minuto del periodo
            p(hoy, "09:00", 4.0),
        )
        val r = Informes.resumen(lista, Periodo.SIETE_DIAS, hoy, zona)
        assertEquals(2, r.pinchazos)
        assertEquals(7.0, r.unidades, 0.0)
        assertEquals(1.0, r.mediaDiaria, 1e-9)
    }

    @Test
    fun mediaDiariaNoCuentaDiasAnterioresAlPrimerRegistro() {
        val lista = listOf(p(haceDias(2), "08:00", 3.0), p(hoy, "08:00", 3.0))
        val r = Informes.resumen(lista, Periodo.SIETE_DIAS, hoy, zona)
        assertEquals(6.0, r.unidades, 0.0)
        assertEquals(2.0, r.mediaDiaria, 1e-9) // 6 u / 3 días
    }

    @Test
    fun mediaDiariaDeTreintaDiasConHuecosDivideEntreTodosLosDias() {
        val lista = listOf(p(haceDias(60), "08:00", 1.0), p(haceDias(10), "08:00", 6.0), p(hoy, "08:00", 6.0))
        val r = Informes.resumen(lista, Periodo.TREINTA_DIAS, hoy, zona)
        assertEquals(2, r.pinchazos)
        assertEquals(0.4, r.mediaDiaria, 1e-9) // 12 u / 30 días
    }

    @Test
    fun resumenDeTodoDivideDesdeElPrimerRegistro() {
        val lista = listOf(p(haceDias(9), "08:00", 2.0), p(hoy, "08:00", 4.0))
        val r = Informes.resumen(lista, Periodo.TODO, hoy, zona)
        assertEquals(Resumen(2, 6.0, 0.6), r.copy(mediaDiaria = Math.round(r.mediaDiaria * 1e9) / 1e9))
    }

    @Test
    fun resumenSinDatosEsCero() {
        assertEquals(Resumen(0, 0.0, 0.0), Informes.resumen(emptyList(), Periodo.TODO, hoy, zona))
        val soloAntiguos = listOf(p(haceDias(3), "08:00", 2.0))
        assertEquals(Resumen(0, 0.0, 0.0), Informes.resumen(soloAntiguos, Periodo.HOY, hoy, zona))
    }

    // --- Agrupación por días y gráfico ---

    @Test
    fun agruparPorDiaOrdenaDescendenteYSumaElDia() {
        val lista = listOf(
            p(haceDias(1), "08:00", 2.0),
            p(hoy, "08:00", 1.5),
            p(hoy, "20:00", 3.0),
        )
        val dias = Informes.agruparPorDia(lista, zona)
        assertEquals(listOf(hoy, haceDias(1)), dias.map { it.fecha })
        assertEquals(4.5, dias[0].unidades, 0.0)
        assertEquals(listOf("20:00", "08:00"), dias[0].pinchazos.map { Formatos.hora(it.timestamp, zona) })
    }

    @Test
    fun graficoIncluyeDiasVaciosDesdeElPrimerRegistro() {
        val lista = listOf(p(haceDias(3), "08:00", 2.0), p(hoy, "08:00", 1.0))
        val dias = Informes.diasGrafico(lista, Periodo.SIETE_DIAS, hoy, zona)
        assertEquals(listOf(hoy, haceDias(1), haceDias(2), haceDias(3)), dias.map { it.fecha })
        assertTrue(dias[1].pinchazos.isEmpty())
    }

    @Test
    fun graficoTieneComoMaximoCatorceDias() {
        val lista = (0L until 20L).map { p(haceDias(it), "08:00", 1.0) }
        val dias = Informes.diasGrafico(lista, Periodo.TODO, hoy, zona)
        assertEquals(Informes.MAX_DIAS_GRAFICO, dias.size)
        assertEquals(hoy, dias.first().fecha)
        assertEquals(haceDias(13), dias.last().fecha)
    }

    @Test
    fun circulosCercanosSeSeparanSinTaparse() {
        // Dos pinchazos casi a la misma hora (x 100 y 102) con radio 10: el segundo se aparta.
        assertEquals(listOf(100f, 122f), Informes.repartirSinSolapar(listOf(100f, 102f), listOf(10f, 10f), 2f, 500f))
        // Separados de sobra: no se mueven.
        assertEquals(listOf(100f, 300f), Informes.repartirSinSolapar(listOf(100f, 300f), listOf(10f, 10f), 2f, 500f))
        // Al final del día no se salen del gráfico: se recolocan hacia la izquierda.
        assertEquals(listOf(468f, 490f), Informes.repartirSinSolapar(listOf(489f, 490f), listOf(10f, 10f), 2f, 500f))
    }

    // --- Dosis y formatos ---

    @Test
    fun dosisSeAjustaAPasosDeMediaUnidadDentroDelRango() {
        assertEquals(25, Dosis.VALORES.size)
        assertEquals(12.0, Dosis.VALORES.last(), 0.0)
        assertEquals(4.5, Dosis.ajustar(4.3), 0.0)
        assertEquals(12.0, Dosis.ajustar(12.7), 0.0)
        assertEquals(0.0, Dosis.ajustar(-1.0), 0.0)
        assertEquals(0.0, Dosis.sumarPasos(0.0, -1), 0.0)
        assertEquals(5.0, Dosis.sumarPasos(4.5, 1), 0.0)
    }

    @Test
    fun formatosEnEspanol() {
        assertEquals("4,5", Formatos.numero(4.5))
        assertEquals("4", Formatos.numero(4.0))
        assertEquals("4,5 u", Formatos.unidades(4.5))
        assertEquals("1 pinchazo", Formatos.pinchazos(1))
        assertEquals("3 pinchazos", Formatos.pinchazos(3))
    }

    @Test
    fun tiempoTranscurrido() {
        val min = 60_000L
        assertEquals("hace menos de 1 min", Formatos.transcurrido(0, 30_000))
        assertEquals("hace 5 min", Formatos.transcurrido(0, 5 * min))
        assertEquals("hace 2 h 15 min", Formatos.transcurrido(0, 135 * min))
        assertEquals("hace 1 d 2 h", Formatos.transcurrido(0, 26 * 60 * min))
    }

    @Test
    fun etiquetasDeDia() {
        assertEquals("Hoy", Formatos.etiquetaDia(hoy, hoy))
        assertEquals("Ayer", Formatos.etiquetaDia(haceDias(1), hoy))
        assertEquals("Lunes, 26 de octubre", Formatos.etiquetaDia(haceDias(2), hoy))
        assertEquals("Miércoles, 31 de diciembre de 2025", Formatos.etiquetaDia(LocalDate.of(2025, 12, 31), hoy))
    }
}
