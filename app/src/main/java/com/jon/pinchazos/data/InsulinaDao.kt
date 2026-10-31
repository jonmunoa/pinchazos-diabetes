package com.jon.pinchazos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InsulinaDao {
    /** En orden de alta. */
    @Query("SELECT * FROM insulinas ORDER BY id")
    fun observarTodas(): Flow<List<Insulina>>

    @Query("SELECT * FROM insulinas ORDER BY id")
    suspend fun todas(): List<Insulina>

    @Insert
    suspend fun insertar(insulina: Insulina): Long

    @Update
    suspend fun actualizar(insulina: Insulina)

    @Delete
    suspend fun borrar(insulina: Insulina)
}
