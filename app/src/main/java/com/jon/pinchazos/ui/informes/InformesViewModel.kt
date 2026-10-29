package com.jon.pinchazos.ui.informes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jon.pinchazos.PinchazosApp
import com.jon.pinchazos.data.Pinchazo
import com.jon.pinchazos.data.PinchazoRepository
import com.jon.pinchazos.dominio.DiaPinchazos
import com.jon.pinchazos.dominio.Formatos
import com.jon.pinchazos.dominio.Informes
import com.jon.pinchazos.dominio.Periodo
import com.jon.pinchazos.dominio.Resumen
import com.jon.pinchazos.dominio.ResumenFranja
import com.jon.pinchazos.dominio.relojCada
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class InformesUiState(
    val periodo: Periodo = Periodo.SIETE_DIAS,
    val hoy: LocalDate = LocalDate.now(),
    val resumen: Resumen = Resumen(0, 0.0, 0.0),
    val franjas: List<ResumenFranja> = emptyList(),
    val diasGrafico: List<DiaPinchazos> = emptyList(),
    val historial: List<DiaPinchazos> = emptyList(),
)

sealed interface EventoInformes {
    data class Mensaje(val texto: String) : EventoInformes
    data class Borrado(val pinchazo: Pinchazo) : EventoInformes
}

class InformesViewModel(
    private val repositorio: PinchazoRepository,
    reloj: Flow<Long> = relojCada(60_000),
    private val zona: () -> ZoneId = ZoneId::systemDefault,
) : ViewModel() {

    private val periodo = MutableStateFlow(Periodo.SIETE_DIAS)
    private val canal = Channel<EventoInformes>(Channel.BUFFERED)
    val eventos: Flow<EventoInformes> = canal.receiveAsFlow()

    val estado: StateFlow<InformesUiState> = combine(
        repositorio.pinchazos,
        periodo,
        reloj,
    ) { todos, periodo, ahora ->
        val zona = zona()
        val hoy = Formatos.fecha(ahora, zona)
        val delPeriodo = Informes.filtrar(todos, periodo, hoy, zona)
        InformesUiState(
            periodo = periodo,
            hoy = hoy,
            resumen = Informes.resumen(todos, periodo, hoy, zona),
            franjas = Informes.porFranja(delPeriodo, zona),
            diasGrafico = Informes.diasGrafico(todos, periodo, hoy, zona),
            historial = Informes.agruparPorDia(delPeriodo, zona),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InformesUiState())

    fun elegirPeriodo(nuevo: Periodo) {
        periodo.value = nuevo
    }

    fun actualizar(pinchazo: Pinchazo) {
        viewModelScope.launch {
            repositorio.actualizar(pinchazo)
            canal.send(EventoInformes.Mensaje("Pinchazo actualizado"))
        }
    }

    fun borrar(pinchazo: Pinchazo) {
        viewModelScope.launch {
            repositorio.borrar(pinchazo)
            canal.send(EventoInformes.Borrado(pinchazo))
        }
    }

    fun restaurar(pinchazo: Pinchazo) {
        viewModelScope.launch { repositorio.restaurar(pinchazo) }
    }

    suspend fun exportarCsv(): String = repositorio.exportarCsv()

    fun importarCsv(texto: String) {
        viewModelScope.launch {
            val mensaje = try {
                val r = repositorio.importarCsv(texto)
                buildString {
                    append(if (r.importados == 1) "Importado 1 pinchazo" else "Importados ${r.importados} pinchazos")
                    if (r.duplicados > 0) append(" · ${r.duplicados} ya estaban")
                    if (r.lineasConError > 0) append(" · ${r.lineasConError} con errores")
                }
            } catch (e: IllegalArgumentException) {
                "No se pudo importar: ${e.message}"
            }
            canal.send(EventoInformes.Mensaje(mensaje))
        }
    }

    fun avisar(texto: String) {
        canal.trySend(EventoInformes.Mensaje(texto))
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PinchazosApp
                InformesViewModel(app.repositorio)
            }
        }
    }
}
