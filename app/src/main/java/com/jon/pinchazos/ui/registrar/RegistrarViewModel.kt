package com.jon.pinchazos.ui.registrar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jon.pinchazos.PinchazosApp
import com.jon.pinchazos.data.Insulina
import com.jon.pinchazos.data.MemoriaDosis
import com.jon.pinchazos.data.Pinchazo
import com.jon.pinchazos.data.PinchazoRepository
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
    /** Las que se pueden registrar (sin las archivadas), en orden de alta. */
    val insulinas: List<Insulina> = emptyList(),
    /** null mientras carga, o si no hay ninguna. */
    val insulina: Insulina? = null,
    /** Ya se sabe qué insulinas hay (para no enseñar "no hay ninguna" mientras carga). */
    val cargado: Boolean = false,
    /** El último pinchazo de la insulina elegida. */
    val ultimo: Pinchazo? = null,
    val ahora: Long = System.currentTimeMillis(),
    val hoyPinchazos: Int = 0,
    val hoyUnidades: Double = 0.0,
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
        /** null = la del último pinchazo (o la primera). */
        val insulinaId: Long? = null,
        /** null = la última usada con esa insulina. */
        val dosis: Double? = null,
        val horaElegida: Long? = null,
        val boton: EstadoBoton = EstadoBoton.LISTO,
    )

    private val seleccion = MutableStateFlow(Seleccion())

    /** Últimas dosis por insulina; null mientras se leen. */
    private val ultimas = MutableStateFlow<Map<Long, Double>?>(null)
    private val canal = Channel<EventoRegistro>(Channel.BUFFERED)

    /** Avisos para el Snackbar: registrado (con "Deshacer") o error al guardar. */
    val eventos: Flow<EventoRegistro> = canal.receiveAsFlow()

    val estado: StateFlow<RegistrarUiState> = combine(
        repositorio.pinchazos,
        repositorio.insulinas,
        reloj,
        seleccion,
        ultimas,
    ) { pinchazos, todas, ahora, sel, ultimas ->
        val zona = zona()
        val hoy = Formatos.fecha(ahora, zona)
        val activas = todas.filterNot { it.archivada }
        val insulina = activas.firstOrNull { it.id == sel.insulinaId }
            ?: pinchazos.firstNotNullOfOrNull { p -> activas.firstOrNull { it.nombre == p.insulina } }
            ?: activas.firstOrNull()
        val suyos = if (insulina == null) emptyList() else pinchazos.filter { it.insulina == insulina.nombre }
        val deHoy = suyos.filter { Formatos.fecha(it.timestamp, zona) == hoy }
        val dosis = when {
            insulina == null || ultimas == null -> null
            sel.dosis != null -> insulina.ajustar(sel.dosis)
            else -> insulina.ajustar(ultimas[insulina.id] ?: Insulina.DOSIS_POR_DEFECTO)
        }
        RegistrarUiState(
            insulinas = activas,
            insulina = insulina,
            cargado = true,
            ultimo = suyos.firstOrNull(),
            ahora = ahora,
            hoyPinchazos = deHoy.size,
            hoyUnidades = deHoy.sumOf { it.dosis },
            dosis = dosis,
            horaElegida = sel.horaElegida,
            boton = if (dosis == null) EstadoBoton.CARGANDO else sel.boton,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RegistrarUiState())

    init {
        viewModelScope.launch {
            ultimas.value = try {
                memoria.ultimasDosis.first()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyMap() // preferencias ilegibles: mejor un valor por defecto que no poder registrar
            }
        }
    }

    fun elegirDosis(dosis: Double) = seleccion.update { it.copy(dosis = dosis) }

    /** Al cambiar de insulina, la dosis vuelve a la última usada con ella. */
    fun elegirInsulina(id: Long) = seleccion.update { it.copy(insulinaId = id, dosis = null) }

    fun elegirHora(timestamp: Long?) = seleccion.update { it.copy(horaElegida = timestamp) }

    /**
     * Guarda lo que se ve en pantalla. Ignora la llamada si la dosis aún no está cargada o si ya
     * hay un registro en curso o recién hecho (doble toque).
     */
    fun registrar() {
        // Lo último tocado manda aunque la pantalla aún no lo haya pintado (toque rápido).
        val visto = estado.value
        val sel = seleccion.value
        val insulina = visto.insulinas.firstOrNull { it.id == sel.insulinaId } ?: visto.insulina ?: return
        val dosis = sel.dosis?.let(insulina::ajustar)
            ?: visto.dosis?.takeIf { visto.insulina?.id == insulina.id }
            ?: return
        if (sel.boton != EstadoBoton.LISTO) return
        seleccion.update { it.copy(boton = EstadoBoton.GUARDANDO) }

        viewModelScope.launch {
            try {
                val momento = sel.horaElegida?.let { minOf(it, ahora()) } ?: ahora()
                val pinchazo = repositorio.registrar(dosis, insulina, momento)
                seleccion.update { it.copy(horaElegida = null, boton = EstadoBoton.REGISTRADO) }
                canal.send(EventoRegistro.Registrado(pinchazo))
                ultimas.update { it.orEmpty() + (insulina.id to pinchazo.dosis) }
                try {
                    memoria.guardarUltimaDosis(insulina.id, pinchazo.dosis)
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

    /** Alta o cambio de una insulina; devuelve el error a enseñar o null. Una nueva queda elegida. */
    suspend fun guardarInsulina(insulina: Insulina): String? {
        val nueva = insulina.id == 0L
        val error = repositorio.guardarInsulina(insulina)
        if (error == null && nueva) {
            val creada = repositorio.insulinas.first().lastOrNull { it.nombre == insulina.nombre.trim() }
            if (creada != null) elegirInsulina(creada.id)
        }
        return error
    }

    suspend fun tienePinchazos(insulina: Insulina): Boolean = repositorio.tienePinchazos(insulina)

    fun borrarInsulina(insulina: Insulina) {
        viewModelScope.launch {
            repositorio.borrarInsulina(insulina)
            seleccion.update { if (it.insulinaId == insulina.id) it.copy(insulinaId = null, dosis = null) else it }
        }
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
