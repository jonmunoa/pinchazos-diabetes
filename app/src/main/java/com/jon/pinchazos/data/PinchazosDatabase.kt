package com.jon.pinchazos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Pinchazo::class], version = 1, exportSchema = true)
abstract class PinchazosDatabase : RoomDatabase() {
    abstract fun pinchazoDao(): PinchazoDao

    companion object {
        const val NOMBRE = "pinchazos.db"

        // Sin WAL: así el fichero .db siempre está completo y Auto Backup copia un estado coherente.
        fun crear(context: Context): PinchazosDatabase =
            Room.databaseBuilder(context, PinchazosDatabase::class.java, NOMBRE)
                .setJournalMode(JournalMode.TRUNCATE)
                .build()
    }
}
