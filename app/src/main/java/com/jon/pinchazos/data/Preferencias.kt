package com.jon.pinchazos.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.jon.pinchazos.dominio.Dosis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "preferencias")

/** Recuerda la última dosis usada (separado en interfaz para poder probar el ViewModel). */
interface MemoriaDosis {
    val ultimaDosis: Flow<Double>
    suspend fun guardarUltimaDosis(dosis: Double)
}

class Preferencias(private val context: Context) : MemoriaDosis {
    override val ultimaDosis: Flow<Double> =
        context.dataStore.data.map { it[ULTIMA_DOSIS] ?: Dosis.POR_DEFECTO }

    override suspend fun guardarUltimaDosis(dosis: Double) {
        context.dataStore.edit { it[ULTIMA_DOSIS] = dosis }
    }

    private companion object {
        val ULTIMA_DOSIS = doublePreferencesKey("ultima_dosis")
    }
}
