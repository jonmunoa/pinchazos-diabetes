package com.jon.pinchazos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PinchazoDao {
    /** Todos los pinchazos, del más reciente al más antiguo. */
    @Query("SELECT * FROM pinchazos ORDER BY timestamp DESC")
    fun observarTodos(): Flow<List<Pinchazo>>

    /** Todos los pinchazos en orden cronológico. */
    @Query("SELECT * FROM pinchazos ORDER BY timestamp ASC")
    suspend fun todos(): List<Pinchazo>

    @Insert
    suspend fun insertar(pinchazo: Pinchazo): Long

    @Insert
    suspend fun insertarTodos(pinchazos: List<Pinchazo>)

    @Update
    suspend fun actualizar(pinchazo: Pinchazo)

    @Delete
    suspend fun borrar(pinchazo: Pinchazo)

    /** Al cambiar el nombre de una insulina, sus pinchazos pasan a llevar el nuevo. */
    @Query("UPDATE pinchazos SET insulina = :nuevo WHERE insulina = :viejo")
    suspend fun renombrarInsulina(viejo: String, nuevo: String)

    @Query("DELETE FROM pinchazos WHERE insulina = :insulina")
    suspend fun borrarDeInsulina(insulina: String)

    @Query("SELECT COUNT(*) FROM pinchazos WHERE insulina = :insulina")
    suspend fun contarDeInsulina(insulina: String): Int
}
