package com.jon.pinchazos.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pinchazos", indices = [Index("timestamp")])
data class Pinchazo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Momento del pinchazo, en milisegundos desde epoch. */
    val timestamp: Long,
    /** Unidades, de 0 a 12 en pasos de 0,5. */
    val dosis: Double,
    val insulina: String,
)

/** Tipos de insulina disponibles. Para añadir uno nuevo basta con sumarlo a [TODAS]. */
object Insulinas {
    const val HUMALOG_JUNIOR = "Humalog Junior"

    val TODAS: List<String> = listOf(HUMALOG_JUNIOR)
    val POR_DEFECTO: String = TODAS.first()
}
