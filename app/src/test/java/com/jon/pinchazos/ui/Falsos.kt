package com.jon.pinchazos.ui

import com.jon.pinchazos.data.MemoriaDosis
import com.jon.pinchazos.data.Pinchazo
import com.jon.pinchazos.data.PinchazoDao
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException

/** DAO en memoria con el mismo orden que el real. */
class FakePinchazoDao(var fallarAlInsertar: Boolean = false) : PinchazoDao {
    private val datos = MutableStateFlow<List<Pinchazo>>(emptyList())
    private var siguienteId = 1L

    val guardados: List<Pinchazo> get() = datos.value.sortedBy { it.timestamp }

    override fun observarTodos(): Flow<List<Pinchazo>> = datos.map { l -> l.sortedByDescending { it.timestamp } }

    override suspend fun todos(): List<Pinchazo> = guardados

    override suspend fun insertar(pinchazo: Pinchazo): Long {
        if (fallarAlInsertar) throw IOException("disco lleno")
        val id = if (pinchazo.id == 0L) siguienteId++ else pinchazo.id
        datos.update { it + pinchazo.copy(id = id) }
        return id
    }

    override suspend fun insertarTodos(pinchazos: List<Pinchazo>) = pinchazos.forEach { insertar(it) }

    override suspend fun actualizar(pinchazo: Pinchazo) =
        datos.update { l -> l.map { if (it.id == pinchazo.id) pinchazo else it } }

    override suspend fun borrar(pinchazo: Pinchazo) = datos.update { l -> l.filterNot { it.id == pinchazo.id } }
}

/** [inicial] null = la lectura nunca termina (simula que aún está cargando). */
class FakeMemoria(inicial: Double?, fallar: Boolean = false) : MemoriaDosis {
    var guardada: Double? = null
        private set

    override val ultimaDosis: Flow<Double> = when {
        fallar -> flow { throw IOException("preferencias corruptas") }
        inicial == null -> flow { awaitCancellation() }
        else -> flowOf(inicial)
    }

    override suspend fun guardarUltimaDosis(dosis: Double) {
        guardada = dosis
    }
}
