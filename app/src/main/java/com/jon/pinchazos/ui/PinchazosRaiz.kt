package com.jon.pinchazos.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jon.pinchazos.data.Pinchazo
import com.jon.pinchazos.dominio.Formatos
import com.jon.pinchazos.ui.comun.EditarPinchazo
import com.jon.pinchazos.ui.comun.Iconos
import com.jon.pinchazos.ui.informes.EventoInformes
import com.jon.pinchazos.ui.informes.InformesScreen
import com.jon.pinchazos.ui.informes.InformesViewModel
import com.jon.pinchazos.ui.registrar.EventoRegistro
import com.jon.pinchazos.ui.registrar.RegistrarScreen
import com.jon.pinchazos.ui.registrar.RegistrarViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun PinchazosRaiz() {
    val registrarVm: RegistrarViewModel = viewModel(factory = RegistrarViewModel.Factory)
    val informesVm: InformesViewModel = viewModel(factory = InformesViewModel.Factory)
    val snackbar = remember { SnackbarHostState() }
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    // Pinchazo abierto en la hoja de edición (desde el historial o desde "Corregir o borrar").
    var editando by remember { mutableStateOf<Pinchazo?>(null) }

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
            SnackbarHost(snackbar, Modifier.padding(bottom = if (pestana == 0) 112.dp else 0.dp))
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pestana == 0,
                    onClick = { pestana = 0 },
                    icon = { Icon(Iconos.Gota, contentDescription = null) },
                    label = { Text("Registrar") },
                )
                NavigationBarItem(
                    selected = pestana == 1,
                    onClick = { pestana = 1 },
                    icon = { Icon(Iconos.Barras, contentDescription = null) },
                    label = { Text("Informes") },
                )
            }
        },
    ) { relleno ->
        Box(
            Modifier
                .padding(relleno)
                .consumeWindowInsets(relleno),
        ) {
            when (pestana) {
                0 -> {
                    val estado by registrarVm.estado.collectAsStateWithLifecycle()
                    RegistrarScreen(
                        estado = estado,
                        onDosis = registrarVm::elegirDosis,
                        onInsulina = registrarVm::elegirInsulina,
                        onHora = registrarVm::elegirHora,
                        onRegistrar = registrarVm::registrar,
                        onCorregir = { editando = it },
                    )
                }
                else -> {
                    val estado by informesVm.estado.collectAsStateWithLifecycle()
                    InformesScreen(informesVm, estado, onEditar = { editando = it })
                }
            }
        }
    }

    editando?.let { p ->
        EditarPinchazo(
            pinchazo = p,
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
}
