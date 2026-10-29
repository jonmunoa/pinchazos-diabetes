package com.jon.pinchazos

import android.app.Application
import com.jon.pinchazos.data.PinchazoRepository
import com.jon.pinchazos.data.PinchazosDatabase
import com.jon.pinchazos.data.Preferencias

/** Contenedor de dependencias de la app (sin framework de inyección). */
class PinchazosApp : Application() {
    private val database by lazy { PinchazosDatabase.crear(this) }
    val repositorio by lazy { PinchazoRepository(database.pinchazoDao()) }
    val preferencias by lazy { Preferencias(this) }
}
