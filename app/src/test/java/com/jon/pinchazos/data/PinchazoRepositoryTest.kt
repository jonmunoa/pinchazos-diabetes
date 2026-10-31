package com.jon.pinchazos.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class PinchazoRepositoryTest {
    private val zona = ZoneId.of("Europe/Madrid")
    private lateinit var db: PinchazosDatabase
    private lateinit var repo: PinchazoRepository

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), PinchazosDatabase::class.java)
            .addCallback(PinchazosDatabase.SEMBRAR)
            .allowMainThreadQueries()
            .build()
        repo = PinchazoRepository(db.pinchazoDao(), db.insulinaDao()) { zona }
    }

    @After
    fun cerrar() = db.close()

    private fun ts(texto: String) = LocalDateTime.parse(texto).atZone(zona).toInstant().toEpochMilli()

    @Test
    fun registrarAjustaLaDosisYDevuelveElId() = runTest {
        val p = repo.registrar(dosis = 4.3, insulina = Insulina.humalogJunior(), timestamp = 1_000)
        assertTrue(p.id > 0)
        assertEquals(4.5, p.dosis, 0.0)
        assertEquals(listOf(p), repo.pinchazos.first())
    }

    @Test
    fun borrarYRestaurarRecuperaElMismoRegistro() = runTest {
        val p = repo.registrar(2.0, Insulina.humalogJunior(), 1_000)
        repo.borrar(p)
        assertTrue(repo.pinchazos.first().isEmpty())
        repo.restaurar(p)
        assertEquals(listOf(p), repo.pinchazos.first())
    }

    @Test
    fun actualizarCambiaDosisYHora() = runTest {
        val p = repo.registrar(2.0, Insulina.humalogJunior(), 1_000)
        repo.actualizar(p.copy(dosis = 6.0, timestamp = 5_000))
        assertEquals(listOf(p.copy(dosis = 6.0, timestamp = 5_000)), repo.pinchazos.first())
    }

    @Test
    fun exportarEImportarNoDuplica() = runTest {
        repo.registrar(4.5, Insulina.humalogJunior(), ts("2026-10-28T14:10"))
        repo.registrar(2.0, Insulina.humalogJunior(), ts("2026-10-27T08:05"))
        val csv = repo.exportarCsv()

        val otraVez = repo.importarCsv(csv)
        assertEquals(ResultadoImportacion(importados = 0, duplicados = 2, lineasConError = 0), otraVez)

        val nuevo = csv + "2026-10-26;22:30;Humalog Junior;3\r\n"
        assertEquals(ResultadoImportacion(1, 2, 0), repo.importarCsv(nuevo))
        assertEquals(3, repo.pinchazos.first().size)
    }

    @Test
    fun importarEnBaseVaciaRestauraLaCopia() = runTest {
        val csv = "fecha;hora;insulina;dosis\n2026-10-28;09:00;Humalog Junior;3,5\n2026-10-28;13:00;Humalog Junior;5\n"
        assertEquals(ResultadoImportacion(2, 0, 0), repo.importarCsv(csv))
        assertEquals(listOf(5.0, 3.5), repo.pinchazos.first().map { it.dosis })
    }

    @Test
    fun laBaseNuevaEmpiezaConHumalogJunior() = runTest {
        assertEquals(listOf(Insulina.HUMALOG_JUNIOR), repo.insulinas.first().map { it.nombre })
    }

    @Test
    fun cambiarElNombreDeUnaInsulinaSeLlevaSusPinchazos() = runTest {
        repo.registrar(4.5, Insulina.humalogJunior(), 1_000)
        val humalog = repo.insulinas.first().single()
        assertNull(repo.guardarInsulina(humalog.copy(nombre = "  Humalog  ")))
        assertEquals("Humalog", repo.insulinas.first().single().nombre)
        assertEquals("Humalog", repo.pinchazos.first().single().insulina)
    }

    @Test
    fun noDejaDosInsulinasConElMismoNombreNiSinNombre() = runTest {
        assertEquals("Ya hay una insulina que se llama así", repo.guardarInsulina(Insulina(nombre = "humalog junior")))
        assertEquals("Ponle un nombre", repo.guardarInsulina(Insulina(nombre = " ")))
        assertNull(repo.guardarInsulina(Insulina(nombre = "Lantus", paso = 1.0, maximo = 40.0)))
        assertEquals(2, repo.insulinas.first().size)
    }

    @Test
    fun registrarUsaLosPasosYElMaximoDeSuInsulina() = runTest {
        repo.guardarInsulina(Insulina(nombre = "Lantus", paso = 1.0, maximo = 40.0))
        val lantus = repo.insulinas.first().last()
        assertEquals(23.0, repo.registrar(22.6, lantus, 1_000).dosis, 0.0)
        assertEquals(40.0, repo.registrar(55.0, lantus, 2_000).dosis, 0.0)
    }

    @Test
    fun borrarUnaInsulinaBorraSusPinchazos() = runTest {
        repo.guardarInsulina(Insulina(nombre = "Lantus", paso = 1.0, maximo = 40.0))
        val lantus = repo.insulinas.first().last()
        repo.registrar(20.0, lantus, 1_000)
        repo.registrar(3.0, Insulina.humalogJunior(), 2_000)
        assertTrue(repo.tienePinchazos(lantus))

        repo.borrarInsulina(lantus)
        assertEquals(listOf(Insulina.HUMALOG_JUNIOR), repo.insulinas.first().map { it.nombre })
        assertEquals(listOf(Insulina.HUMALOG_JUNIOR), repo.pinchazos.first().map { it.insulina })
    }

    @Test
    fun importarDaDeAltaLasInsulinasQueNoExisten() = runTest {
        val csv = "fecha;hora;insulina;dosis\n2026-10-28;09:00;Tresiba;24\n2026-10-28;13:00;Humalog Junior;5\n"
        assertEquals(ResultadoImportacion(2, 0, 0), repo.importarCsv(csv))
        val tresiba = repo.insulinas.first().single { it.nombre == "Tresiba" }
        assertEquals(24.0, tresiba.maximo, 0.0)
    }
}
