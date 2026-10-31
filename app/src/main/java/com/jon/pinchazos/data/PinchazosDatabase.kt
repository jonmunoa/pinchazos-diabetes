package com.jon.pinchazos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Pinchazo::class, Insulina::class], version = 2, exportSchema = true)
abstract class PinchazosDatabase : RoomDatabase() {
    abstract fun pinchazoDao(): PinchazoDao
    abstract fun insulinaDao(): InsulinaDao

    companion object {
        const val NOMBRE = "pinchazos.db"

        /**
         * 1 → 2: lista de insulinas. Entra Humalog Junior (la única que había, con id 1) y, por si
         * se importó algún CSV con otras, también las que ya tengan pinchazos.
         */
        val CON_INSULINAS = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                crearInsulinas(db)
                db.execSQL(
                    "INSERT OR IGNORE INTO insulinas (nombre, paso, maximo, archivada) " +
                        "SELECT insulina, 0.5, MAX(12.0, MAX(dosis)), 0 FROM pinchazos GROUP BY insulina",
                )
            }
        }

        private fun crearInsulinas(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `insulinas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`nombre` TEXT NOT NULL, `paso` REAL NOT NULL, `maximo` REAL NOT NULL, `archivada` INTEGER NOT NULL)",
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_insulinas_nombre` ON `insulinas` (`nombre`)")
            sembrar(db)
        }

        /** Humalog Junior, como la app de siempre: la primera vez ya se puede registrar. */
        private fun sembrar(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT OR IGNORE INTO insulinas (nombre, paso, maximo, archivada) VALUES (?, 0.5, 12.0, 0)",
                arrayOf(Insulina.HUMALOG_JUNIOR),
            )
        }

        /** Para una base de datos recién creada (también la de los tests). */
        val SEMBRAR = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) = sembrar(db)
        }

        // Sin WAL: así el fichero .db siempre está completo y Auto Backup copia un estado coherente.
        fun crear(context: Context): PinchazosDatabase =
            Room.databaseBuilder(context, PinchazosDatabase::class.java, NOMBRE)
                .setJournalMode(JournalMode.TRUNCATE)
                .addMigrations(CON_INSULINAS)
                .addCallback(SEMBRAR)
                .build()
    }
}
