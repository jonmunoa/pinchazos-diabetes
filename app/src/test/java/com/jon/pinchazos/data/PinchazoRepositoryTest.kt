package com.jon.pinchazos.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
            .allowMainThreadQueries()
            .build()
        repo = PinchazoRepository(db.pinchazoDao()) { zona }
    }

    @After
    fun cerrar() = db.close()

    private fun ts(texto: String) = LocalDateTime.parse(texto).atZone(zona).toInstant().toEpochMilli()

    @Test
    fun registrarAjustaLaDosisYDevuelveElId() = runTest {
        val p = repo.registrar(dosis = 4.3, insulina = Insulinas.HUMALOG_JUNIOR, timestamp = 1_000)
        assertTrue(p.id > 0)
        assertEquals(4.5, p.dosis, 0.0)
        assertEquals(listOf(p), repo.pinchazos.first())
    }

    @Test
    fun borrarYRestaurarRecuperaElMismoRegistro() = runTest {
        val p = repo.registrar(2.0, Insulinas.HUMALOG_JUNIOR, 1_000)
        repo.borrar(p)
        assertTrue(repo.pinchazos.first().isEmpty())
        repo.restaurar(p)
        assertEquals(listOf(p), repo.pinchazos.first())
    }

    @Test
    fun actualizarCambiaDosisYHora() = runTest {
        val p = repo.registrar(2.0, Insulinas.HUMALOG_JUNIOR, 1_000)
        repo.actualizar(p.copy(dosis = 6.0, timestamp = 5_000))
        assertEquals(listOf(p.copy(dosis = 6.0, timestamp = 5_000)), repo.pinchazos.first())
    }

    @Test
    fun exportarEImportarNoDuplica() = runTest {
        repo.registrar(4.5, Insulinas.HUMALOG_JUNIOR, ts("2026-10-28T14:10"))
        repo.registrar(2.0, Insulinas.HUMALOG_JUNIOR, ts("2026-10-27T08:05"))
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
}
