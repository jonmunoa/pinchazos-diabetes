package com.jon.pinchazos.data

import kotlinx.coroutines.flow.Flow
import java.time.ZoneId

data class ResultadoImportacion(val importados: Int, val duplicados: Int, val lineasConError: Int)

class PinchazoRepository(
    private val dao: PinchazoDao,
    private val insulinaDao: InsulinaDao,
    /** Ejecuta el bloque en una transacción de la base de datos (en los tests, tal cual). */
    private val enTransaccion: suspend (suspend () -> Unit) -> Unit = { it() },
    private val zona: () -> ZoneId = { ZoneId.systemDefault() },
) {
    /** Del más reciente al más antiguo. */
    val pinchazos: Flow<List<Pinchazo>> = dao.observarTodos()

    /** Todas, también las archivadas, en orden de alta. */
    val insulinas: Flow<List<Insulina>> = insulinaDao.observarTodas()

    suspend fun registrar(dosis: Double, insulina: Insulina, timestamp: Long): Pinchazo {
        val pinchazo = Pinchazo(timestamp = timestamp, dosis = insulina.ajustar(dosis), insulina = insulina.nombre)
        return pinchazo.copy(id = dao.insertar(pinchazo))
    }

    suspend fun actualizar(pinchazo: Pinchazo) {
        val insulina = insulinaDao.todas().firstOrNull { it.nombre == pinchazo.insulina }
        dao.actualizar(pinchazo.copy(dosis = insulina?.ajustar(pinchazo.dosis) ?: pinchazo.dosis))
    }

    suspend fun borrar(pinchazo: Pinchazo) = dao.borrar(pinchazo)

    /** Vuelve a insertar un pinchazo borrado, con su mismo id (para "Deshacer"). */
    suspend fun restaurar(pinchazo: Pinchazo) {
        dao.insertar(pinchazo)
    }

    /**
     * Da de alta ([Insulina.id] 0) o cambia una insulina. Si cambia de nombre, sus pinchazos se
     * llevan el nuevo. Devuelve el error a enseñar, o null si se ha guardado.
     */
    suspend fun guardarInsulina(insulina: Insulina): String? {
        val nombre = insulina.nombre.trim()
        if (nombre.isEmpty()) return "Ponle un nombre"
        if (insulina.maximo <= 0) return "Pon una dosis máxima, por ejemplo 12"
        val todas = insulinaDao.todas()
        if (todas.any { it.id != insulina.id && it.nombre.equals(nombre, ignoreCase = true) }) {
            return "Ya hay una insulina que se llama así"
        }
        val limpia = insulina.copy(nombre = nombre)
        val anterior = todas.firstOrNull { it.id == insulina.id }
        if (anterior == null) {
            insulinaDao.insertar(limpia.copy(id = 0))
        } else {
            enTransaccion {
                insulinaDao.actualizar(limpia)
                if (anterior.nombre != nombre) dao.renombrarInsulina(anterior.nombre, nombre)
            }
        }
        return null
    }

    suspend fun tienePinchazos(insulina: Insulina): Boolean = dao.contarDeInsulina(insulina.nombre) > 0

    /** Borra la insulina y todos sus pinchazos. */
    suspend fun borrarInsulina(insulina: Insulina) = enTransaccion {
        dao.borrarDeInsulina(insulina.nombre)
        insulinaDao.borrar(insulina)
    }

    suspend fun exportarCsv(): String = CsvPinchazos.escribir(dao.todos(), zona())

    /**
     * Añade los pinchazos del CSV que no estén ya guardados (mismo minuto, insulina y dosis),
     * así importar dos veces la misma copia no duplica nada. Las insulinas que no existan se dan
     * de alta, de media en media unidad.
     */
    suspend fun importarCsv(texto: String): ResultadoImportacion {
        val lectura = CsvPinchazos.leer(texto, zona())
        val existentes = dao.todos().mapTo(mutableSetOf()) { clave(it) }
        val nuevos = lectura.pinchazos.filter { existentes.add(clave(it)) }
        if (nuevos.isNotEmpty()) {
            enTransaccion {
                val conocidas = insulinaDao.todas().mapTo(mutableSetOf()) { it.nombre }
                nuevos.groupBy { it.insulina }
                    .filterKeys { it !in conocidas }
                    .forEach { (nombre, lista) ->
                        insulinaDao.insertar(Insulina(nombre = nombre, maximo = maxOf(12.0, lista.maxOf { it.dosis })))
                    }
                dao.insertarTodos(nuevos)
            }
        }
        return ResultadoImportacion(
            importados = nuevos.size,
            duplicados = lectura.pinchazos.size - nuevos.size,
            lineasConError = lectura.lineasConError,
        )
    }

    private fun clave(p: Pinchazo) = Triple(p.timestamp / 60_000, p.insulina, p.dosis)
}
