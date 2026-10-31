package com.jon.pinchazos

import android.app.Application
import androidx.room.withTransaction
import com.jon.pinchazos.data.PinchazoRepository
import com.jon.pinchazos.data.PinchazosDatabase
import com.jon.pinchazos.data.Preferencias

/** Contenedor de dependencias de la app (sin framework de inyección). */
class PinchazosApp : Application() {
    private val database by lazy { PinchazosDatabase.crear(this) }
    val repositorio by lazy {
        PinchazoRepository(
            database.pinchazoDao(),
            database.insulinaDao(),
            enTransaccion = { bloque -> database.withTransaction { bloque() } },
        )
    }
    val preferencias by lazy { Preferencias(this) }
}
