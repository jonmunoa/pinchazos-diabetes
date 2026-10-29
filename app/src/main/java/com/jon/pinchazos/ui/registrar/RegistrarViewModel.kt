package com.jon.pinchazos.ui.registrar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jon.pinchazos.PinchazosApp
import com.jon.pinchazos.data.Insulinas
import com.jon.pinchazos.data.MemoriaDosis
import com.jon.pinchazos.data.Pinchazo
import com.jon.pinchazos.data.PinchazoRepository
import com.jon.pinchazos.dominio.Dosis
import com.jon.pinchazos.dominio.Formatos
import com.jon.pinchazos.dominio.relojCada
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId

/** Qué puede hacer ahora el botón "Registrar pinchazo". */
enum class EstadoBoton {
    /** Aún no se ha leído la última dosis: registrar ahora guardaría una dosis que no es la que se ve. */
    CARGANDO,
    LISTO,
    GUARDANDO,

    /** Recién registrado: se ignoran toques durante un momento para evitar dobles registros. */
    REGISTRADO,
}

data class RegistrarUiState(
    val ultimo: Pinchazo? = null,
    val ahora: Long = System.currentTimeMillis(),
    val hoyPinchazos: Int = 0,
    val hoyUnidades: Double = 0.0,
    val insulina: String = Insulinas.POR_DEFECTO,
    /** null mientras se lee la última dosis guardada. */
    val dosis: Double? = null,
    /** null = "Ahora". */
    val horaElegida: Long? = null,
    val boton: EstadoBoton = EstadoBoton.CARGANDO,
) {
    val reciente: Boolean
        get() = ultimo != null && ahora - ultimo.timestamp < TRES_HORAS_MS

    private companion object {
        const val TRES_HORAS_MS = 3 * 60 * 60 * 1000L
    }
}

sealed interface EventoRegistro {
    data class Registrado(val pinchazo: Pinchazo) : EventoRegistro
    data class Error(val texto: String) : EventoRegistro
}

class RegistrarViewModel(
    private val repositorio: PinchazoRepository,
    private val memoria: MemoriaDosis,
    reloj: Flow<Long> = relojCada(30_000),
    private val ahora: () -> Long = System::currentTimeMillis,
    private val zona: () -> ZoneId = ZoneId::systemDefault,
) : ViewModel() {

    private data class Seleccion(
        val insulina: String = Insulinas.POR_DEFECTO,
        val dosis: Double? = null,
        val horaElegida: Long? = null,
        val boton: EstadoBoton = EstadoBoton.LISTO,
    )

    private val seleccion = MutableStateFlow(Seleccion())
    private val canal = Channel<EventoRegistro>(Channel.BUFFERED)

    /** Avisos para el Snackbar: registrado (con "Deshacer") o error al guardar. */
    val eventos: Flow<EventoRegistro> = canal.receiveAsFlow()

    val estado: StateFlow<RegistrarUiState> = combine(
        repositorio.pinchazos,
        reloj,
        seleccion,
    ) { pinchazos, ahora, sel ->
        val zona = zona()
        val hoy = Formatos.fecha(ahora, zona)
        val deHoy = pinchazos.filter { Formatos.fecha(it.timestamp, zona) == hoy }
        RegistrarUiState(
            ultimo = pinchazos.firstOrNull(),
            ahora = ahora,
            hoyPinchazos = deHoy.size,
            hoyUnidades = deHoy.sumOf { it.dosis },
            insulina = sel.insulina,
            dosis = sel.dosis,
            horaElegida = sel.horaElegida,
            boton = if (sel.dosis == null) EstadoBoton.CARGANDO else sel.boton,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RegistrarUiState())

    init {
        viewModelScope.launch {
            val ultima = try {
                memoria.ultimaDosis.first()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Dosis.POR_DEFECTO // preferencias ilegibles: mejor un valor por defecto que no poder registrar
            }
            // Si ya ha tocado una dosis mientras cargaba, manda la suya.
            seleccion.update { if (it.dosis == null) it.copy(dosis = Dosis.ajustar(ultima)) else it }
        }
    }

    fun elegirDosis(dosis: Double) = seleccion.update { it.copy(dosis = Dosis.ajustar(dosis)) }

    fun elegirInsulina(insulina: String) = seleccion.update { it.copy(insulina = insulina) }

    fun elegirHora(timestamp: Long?) = seleccion.update { it.copy(horaElegida = timestamp) }

    /**
     * Guarda lo que se ve en pantalla. Ignora la llamada si la dosis aún no está cargada o si ya
     * hay un registro en curso o recién hecho (doble toque).
     */
    fun registrar() {
        val sel = seleccion.value
        val dosis = sel.dosis ?: return
        if (sel.boton != EstadoBoton.LISTO) return
        seleccion.update { it.copy(boton = EstadoBoton.GUARDANDO) }

        viewModelScope.launch {
            try {
                val momento = sel.horaElegida?.let { minOf(it, ahora()) } ?: ahora()
                val pinchazo = repositorio.registrar(dosis, sel.insulina, momento)
                seleccion.update { it.copy(horaElegida = null, boton = EstadoBoton.REGISTRADO) }
                canal.send(EventoRegistro.Registrado(pinchazo))
                try {
                    memoria.guardarUltimaDosis(dosis)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // No recordar la dosis no es grave; el pinchazo sí está guardado.
                }
                delay(BLOQUEO_TRAS_REGISTRAR_MS)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                canal.send(EventoRegistro.Error("No se ha podido guardar. Vuelve a intentarlo."))
            } finally {
                seleccion.update { it.copy(boton = EstadoBoton.LISTO) }
            }
        }
    }

    fun deshacer(pinchazo: Pinchazo) {
        viewModelScope.launch { repositorio.borrar(pinchazo) }
    }

    companion object {
        const val BLOQUEO_TRAS_REGISTRAR_MS = 1_500L

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PinchazosApp
                RegistrarViewModel(app.repositorio, app.preferencias)
            }
        }
    }
}
