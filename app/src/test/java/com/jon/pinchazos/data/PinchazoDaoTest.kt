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

@RunWith(RobolectricTestRunner::class)
class PinchazoDaoTest {
    private lateinit var db: PinchazosDatabase
    private lateinit var dao: PinchazoDao

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), PinchazosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.pinchazoDao()
    }

    @After
    fun cerrar() = db.close()

    private fun p(timestamp: Long, dosis: Double) =
        Pinchazo(timestamp = timestamp, dosis = dosis, insulina = Insulina.HUMALOG_JUNIOR)

    @Test
    fun observarTodosDevuelveDelMasRecienteAlMasAntiguo() = runTest {
        dao.insertar(p(2_000, 2.0))
        dao.insertar(p(3_000, 3.0))
        dao.insertar(p(1_000, 1.0))

        assertEquals(listOf(3_000L, 2_000L, 1_000L), dao.observarTodos().first().map { it.timestamp })
        assertEquals(listOf(1_000L, 2_000L, 3_000L), dao.todos().map { it.timestamp })
    }

    @Test
    fun insertarAsignaIdsYGuardaTodosLosCampos() = runTest {
        val id = dao.insertar(p(1_000, 4.5))
        assertTrue(id > 0)
        assertEquals(Pinchazo(id, 1_000, 4.5, Insulina.HUMALOG_JUNIOR), dao.todos().single())
    }

    @Test
    fun actualizarYBorrar() = runTest {
        val id = dao.insertar(p(1_000, 2.0))
        dao.insertar(p(2_000, 3.0))

        dao.actualizar(Pinchazo(id, 1_500, 2.5, Insulina.HUMALOG_JUNIOR))
        assertEquals(Pinchazo(id, 1_500, 2.5, Insulina.HUMALOG_JUNIOR), dao.todos().first())

        dao.borrar(dao.todos().first())
        assertEquals(listOf(2_000L), dao.todos().map { it.timestamp })
    }

    @Test
    fun insertarTodos() = runTest {
        dao.insertarTodos(listOf(p(1_000, 1.0), p(2_000, 2.0)))
        assertEquals(2, dao.todos().size)
    }
}
