package com.jon.pinchazos.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "preferencias")

/** Recuerda la última dosis usada con cada insulina (separado en interfaz para poder probar el ViewModel). */
interface MemoriaDosis {
    /** Por id de insulina. */
    val ultimasDosis: Flow<Map<Long, Double>>
    suspend fun guardarUltimaDosis(insulinaId: Long, dosis: Double)
}

class Preferencias(private val context: Context) : MemoriaDosis {
    override val ultimasDosis: Flow<Map<Long, Double>> = context.dataStore.data.map { prefs ->
        val porInsulina = prefs.asMap().mapNotNull { (clave, valor) ->
            val id = clave.name.removePrefix(PREFIJO).takeIf { clave.name.startsWith(PREFIJO) }?.toLongOrNull()
            if (id != null && valor is Double) id to valor else null
        }.toMap()
        // Antes solo había Humalog Junior (id 1) y una única "última dosis".
        val antigua = prefs[ULTIMA_DOSIS_ANTIGUA]
        if (antigua != null && 1L !in porInsulina) porInsulina + (1L to antigua) else porInsulina
    }

    override suspend fun guardarUltimaDosis(insulinaId: Long, dosis: Double) {
        context.dataStore.edit { it[doublePreferencesKey("$PREFIJO$insulinaId")] = dosis }
    }

    private companion object {
        const val PREFIJO = "ultima_dosis_"
        val ULTIMA_DOSIS_ANTIGUA = doublePreferencesKey("ultima_dosis")
    }
}
