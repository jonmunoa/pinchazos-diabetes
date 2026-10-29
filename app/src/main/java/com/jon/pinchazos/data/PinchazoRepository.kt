package com.jon.pinchazos.data

import com.jon.pinchazos.dominio.Dosis
import kotlinx.coroutines.flow.Flow
import java.time.ZoneId

data class ResultadoImportacion(val importados: Int, val duplicados: Int, val lineasConError: Int)

class PinchazoRepository(
    private val dao: PinchazoDao,
    private val zona: () -> ZoneId = { ZoneId.systemDefault() },
) {
    /** Del más reciente al más antiguo. */
    val pinchazos: Flow<List<Pinchazo>> = dao.observarTodos()

    suspend fun registrar(dosis: Double, insulina: String, timestamp: Long): Pinchazo {
        val pinchazo = Pinchazo(timestamp = timestamp, dosis = Dosis.ajustar(dosis), insulina = insulina)
        return pinchazo.copy(id = dao.insertar(pinchazo))
    }

    suspend fun actualizar(pinchazo: Pinchazo) =
        dao.actualizar(pinchazo.copy(dosis = Dosis.ajustar(pinchazo.dosis)))

    suspend fun borrar(pinchazo: Pinchazo) = dao.borrar(pinchazo)

    /** Vuelve a insertar un pinchazo borrado, con su mismo id (para "Deshacer"). */
    suspend fun restaurar(pinchazo: Pinchazo) {
        dao.insertar(pinchazo)
    }

    suspend fun exportarCsv(): String = CsvPinchazos.escribir(dao.todos(), zona())

    /**
     * Añade los pinchazos del CSV que no estén ya guardados (mismo minuto, insulina y dosis),
     * así importar dos veces la misma copia no duplica nada.
     */
    suspend fun importarCsv(texto: String): ResultadoImportacion {
        val lectura = CsvPinchazos.leer(texto, zona())
        val existentes = dao.todos().mapTo(mutableSetOf()) { clave(it) }
        val nuevos = lectura.pinchazos.filter { existentes.add(clave(it)) }
        if (nuevos.isNotEmpty()) dao.insertarTodos(nuevos)
        return ResultadoImportacion(
            importados = nuevos.size,
            duplicados = lectura.pinchazos.size - nuevos.size,
            lineasConError = lectura.lineasConError,
        )
    }

    private fun clave(p: Pinchazo) = Triple(p.timestamp / 60_000, p.insulina, p.dosis)
}
