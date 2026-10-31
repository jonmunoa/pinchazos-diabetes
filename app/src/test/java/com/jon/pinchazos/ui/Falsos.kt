package com.jon.pinchazos.ui

import com.jon.pinchazos.data.Insulina
import com.jon.pinchazos.data.InsulinaDao
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

    override suspend fun renombrarInsulina(viejo: String, nuevo: String) =
        datos.update { l -> l.map { if (it.insulina == viejo) it.copy(insulina = nuevo) else it } }

    override suspend fun borrarDeInsulina(insulina: String) = datos.update { l -> l.filterNot { it.insulina == insulina } }

    override suspend fun contarDeInsulina(insulina: String): Int = datos.value.count { it.insulina == insulina }
}

/** DAO de insulinas en memoria; empieza con Humalog Junior (id 1), como la base de datos real. */
class FakeInsulinaDao : InsulinaDao {
    private val datos = MutableStateFlow(listOf(Insulina.humalogJunior().copy(id = 1)))
    private var siguienteId = 2L

    override fun observarTodas(): Flow<List<Insulina>> = datos

    override suspend fun todas(): List<Insulina> = datos.value

    override suspend fun insertar(insulina: Insulina): Long {
        val id = siguienteId++
        datos.update { it + insulina.copy(id = id) }
        return id
    }

    override suspend fun actualizar(insulina: Insulina) =
        datos.update { l -> l.map { if (it.id == insulina.id) insulina else it } }

    override suspend fun borrar(insulina: Insulina) = datos.update { l -> l.filterNot { it.id == insulina.id } }
}

/**
 * [inicial] es la última dosis de Humalog Junior (id 1); null = la lectura nunca termina (simula
 * que aún está cargando).
 */
class FakeMemoria(inicial: Double?, fallar: Boolean = false) : MemoriaDosis {
    val guardadas = mutableMapOf<Long, Double>()
    val guardada: Double? get() = guardadas[1L]

    override val ultimasDosis: Flow<Map<Long, Double>> = when {
        fallar -> flow { throw IOException("preferencias corruptas") }
        inicial == null -> flow { awaitCancellation() }
        else -> flowOf(mapOf(1L to inicial))
    }

    override suspend fun guardarUltimaDosis(insulinaId: Long, dosis: Double) {
        guardadas[insulinaId] = dosis
    }
}
