package com.jon.pinchazos.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jon.pinchazos.BuildConfig
import com.jon.pinchazos.data.Insulina
import com.jon.pinchazos.data.Pinchazo
import com.jon.pinchazos.dominio.Formatos
import com.jon.pinchazos.ui.ajustes.AjustesScreen
import com.jon.pinchazos.ui.comun.EditarInsulina
import com.jon.pinchazos.ui.comun.EditarPinchazo
import com.jon.pinchazos.ui.comun.Iconos
import com.jon.pinchazos.ui.comun.Movimiento
import com.jon.pinchazos.ui.informes.CompartirCsv
import com.jon.pinchazos.ui.informes.EventoInformes
import com.jon.pinchazos.ui.informes.InformesScreen
import com.jon.pinchazos.ui.informes.InformesViewModel
import com.jon.pinchazos.ui.registrar.EventoRegistro
import com.jon.pinchazos.ui.registrar.RegistrarScreen
import com.jon.pinchazos.ui.registrar.RegistrarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun PinchazosRaiz() {
    val registrarVm: RegistrarViewModel = viewModel(factory = RegistrarViewModel.Factory)
    val informesVm: InformesViewModel = viewModel(factory = InformesViewModel.Factory)
    val snackbar = remember { SnackbarHostState() }
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    var pestana by rememberSaveable { mutableIntStateOf(PESTANA_REGISTRAR) }
    // Pinchazo abierto en la hoja de edición (desde el historial o desde "Corregir o borrar").
    var editando by remember { mutableStateOf<Pinchazo?>(null) }
    // Insulina abierta en su hoja: null = cerrada; HojaInsulina(null) = alta.
    var hojaInsulina by remember { mutableStateOf<HojaInsulina?>(null) }
    val estadoInformes by informesVm.estado.collectAsStateWithLifecycle()

    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val texto = withContext(Dispatchers.IO) {
                runCatching {
                    contexto.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (texto == null) informesVm.avisar("No se pudo leer el archivo") else informesVm.importarCsv(texto)
        }
    }

    // Los avisos se recogen aquí para que no se pierdan al cambiar de pestaña.
    LaunchedEffect(Unit) {
        launch {
            registrarVm.eventos.collect { evento ->
                snackbar.currentSnackbarData?.dismiss()
                launch {
                    when (evento) {
                        is EventoRegistro.Registrado -> {
                            val p = evento.pinchazo
                            val zona = ZoneId.systemDefault()
                            val texto = "Registrado: ${Formatos.unidades(p.dosis)} " +
                                Formatos.cuando(p.timestamp, LocalDate.now(zona), zona)
                            val r = snackbar.showSnackbar(texto, actionLabel = "Deshacer", duration = SnackbarDuration.Long)
                            if (r == SnackbarResult.ActionPerformed) registrarVm.deshacer(p)
                        }
                        is EventoRegistro.Error -> snackbar.showSnackbar(evento.texto, duration = SnackbarDuration.Long)
                    }
                }
            }
        }
        launch {
            informesVm.eventos.collect { evento ->
                snackbar.currentSnackbarData?.dismiss()
                launch {
                    when (evento) {
                        is EventoInformes.Mensaje -> snackbar.showSnackbar(evento.texto)
                        is EventoInformes.Borrado -> {
                            val p = evento.pinchazo
                            val zona = ZoneId.systemDefault()
                            val r = snackbar.showSnackbar(
                                "Borrado: ${Formatos.unidades(p.dosis)} " +
                                    Formatos.cuando(p.timestamp, LocalDate.now(zona), zona),
                                actionLabel = "Deshacer",
                                duration = SnackbarDuration.Long,
                            )
                            if (r == SnackbarResult.ActionPerformed) informesVm.restaurar(p)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        // En Registrar, el aviso va por encima del botón grande para no taparlo.
        snackbarHost = {
            SnackbarHost(snackbar, Modifier.padding(bottom = if (pestana == PESTANA_REGISTRAR) 96.dp else 0.dp))
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                Pestana(PESTANA_REGISTRAR, pestana, Iconos.Gota, "Registrar") { pestana = it }
                Pestana(PESTANA_INFORMES, pestana, Iconos.Barras, "Informes") { pestana = it }
                Pestana(PESTANA_AJUSTES, pestana, Iconos.Ajustes, "Ajustes") { pestana = it }
            }
        },
    ) { relleno ->
        Box(
            Modifier
                .padding(relleno)
                .consumeWindowInsets(relleno),
        ) {
            // Cambio de pestaña: fundido corto con un leve desplazamiento hacia el lado al que se va.
            // Se ve muchas veces al día, así que rápido y discreto.
            AnimatedContent(
                targetState = pestana,
                transitionSpec = {
                    val haciaDerecha = targetState > initialState
                    (fadeIn(Movimiento.rapido(220)) + slideInHorizontally(Movimiento.rapido(260)) { (if (haciaDerecha) 1 else -1) * it / 12 }) togetherWith
                        fadeOut(Movimiento.rapido(120))
                },
                label = "pestana",
            ) { actual ->
                when (actual) {
                    PESTANA_REGISTRAR -> {
                        val estado by registrarVm.estado.collectAsStateWithLifecycle()
                        RegistrarScreen(
                            estado = estado,
                            onInsulina = registrarVm::elegirInsulina,
                            onDosis = registrarVm::elegirDosis,
                            onHora = registrarVm::elegirHora,
                            onRegistrar = registrarVm::registrar,
                            onCorregir = { editando = it },
                            onNuevaInsulina = { hojaInsulina = HojaInsulina(null) },
                            onEditarInsulina = { hojaInsulina = HojaInsulina(it) },
                        )
                    }
                    PESTANA_INFORMES -> InformesScreen(informesVm, estadoInformes, onEditar = { editando = it })
                    else -> AjustesScreen(
                        insulinas = estadoInformes.insulinas,
                        version = BuildConfig.VERSION_NAME,
                        onAnadirInsulina = { hojaInsulina = HojaInsulina(null) },
                        onEditarInsulina = { hojaInsulina = HojaInsulina(it) },
                        onExportar = {
                            scope.launch {
                                val csv = informesVm.exportarCsv()
                                runCatching { CompartirCsv.compartir(contexto, csv) }
                                    .onFailure { informesVm.avisar("No se pudo exportar") }
                            }
                        },
                        onImportar = { importar.launch(CompartirCsv.TIPOS_IMPORTABLES) },
                    )
                }
            }
        }
    }

    editando?.let { p ->
        // Si su insulina ya no está (borrada o importada sin alta), se edita de media en media unidad.
        val insulina = estadoInformes.insulinas.firstOrNull { it.nombre == p.insulina }
            ?: Insulina(nombre = p.insulina, maximo = maxOf(12.0, p.dosis))
        EditarPinchazo(
            pinchazo = p,
            insulina = insulina,
            hoy = LocalDate.now(),
            onGuardar = {
                informesVm.actualizar(it)
                editando = null
            },
            onBorrar = {
                informesVm.borrar(it)
                editando = null
            },
            onCerrar = { editando = null },
        )
    }

    hojaInsulina?.let { hoja ->
        EditarInsulina(
            insulina = hoja.insulina,
            onGuardar = registrarVm::guardarInsulina,
            tienePinchazos = registrarVm::tienePinchazos,
            onBorrar = {
                registrarVm.borrarInsulina(it)
                hojaInsulina = null
            },
            onCerrar = { hojaInsulina = null },
        )
    }
}

/** Envoltorio para distinguir "hoja cerrada" (null) de "hoja de alta" (insulina null). */
private data class HojaInsulina(val insulina: Insulina?)

@Composable
private fun androidx.compose.foundation.layout.RowScope.Pestana(
    indice: Int,
    actual: Int,
    icono: ImageVector,
    texto: String,
    onElegir: (Int) -> Unit,
) {
    NavigationBarItem(
        selected = actual == indice,
        onClick = { onElegir(indice) },
        icon = { IconoPestana(icono, actual == indice) },
        label = { Text(texto) },
    )
}

/** Icono de pestaña que da un pequeño salto al elegirla. */
@Composable
private fun IconoPestana(icono: ImageVector, elegida: Boolean) {
    val escala = remember { Animatable(1f) }
    LaunchedEffect(elegida) {
        if (elegida) {
            escala.snapTo(0.82f)
            escala.animateTo(1f, Movimiento.pop())
        }
    }
    Icon(
        icono,
        contentDescription = null,
        modifier = Modifier.graphicsLayer {
            scaleX = escala.value
            scaleY = escala.value
        },
    )
}

private const val PESTANA_REGISTRAR = 0
private const val PESTANA_INFORMES = 1
private const val PESTANA_AJUSTES = 2
