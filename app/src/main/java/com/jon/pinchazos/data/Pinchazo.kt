package com.jon.pinchazos.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pinchazos", indices = [Index("timestamp")])
data class Pinchazo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Momento del pinchazo, en milisegundos desde epoch. */
    val timestamp: Long,
    /** Unidades, en los pasos de su insulina. */
    val dosis: Double,
    /** Nombre de la [Insulina]. */
    val insulina: String,
)
