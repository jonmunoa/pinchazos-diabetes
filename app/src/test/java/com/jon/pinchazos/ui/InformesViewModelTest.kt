package com.jon.pinchazos.ui

import com.jon.pinchazos.data.Insulinas
import com.jon.pinchazos.data.Pinchazo
import com.jon.pinchazos.data.PinchazoRepository
import com.jon.pinchazos.dominio.Periodo
import com.jon.pinchazos.ui.informes.EventoInformes
import com.jon.pinchazos.ui.informes.InformesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class InformesViewModelTest {
    private val zona = ZoneId.of("Europe/Madrid")
    private val hoy = LocalDate.of(2026, 10, 28)
    private val ahora = LocalDateTime.of(hoy, java.time.LocalTime.of(14, 10)).atZone(zona).toInstant().toEpochMilli()
    private val hora = 60 * 60 * 1000L
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun preparar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun borrarQuitaElRegistroDelHistorialYDeshacerLoRecupera() = runTest(dispatcher) {
        val dao = FakePinchazoDao()
        val repo = PinchazoRepository(dao) { zona }
        val manana = repo.registrar(2.0, Insulinas.HUMALOG_JUNIOR, ahora - 5 * hora)
        val mediodia = repo.registrar(4.5, Insulinas.HUMALOG_JUNIOR, ahora - hora)

        val vm = InformesViewModel(repo, reloj = flowOf(ahora), zona = { zona })
        val eventos = mutableListOf<EventoInformes>()
        backgroundScope.launch { vm.estado.collect {} }
        backgroundScope.launch { vm.eventos.toList(eventos) }
        vm.elegirPeriodo(Periodo.HOY)
        runCurrent()
        assertEquals(listOf(mediodia, manana), vm.estado.value.historial.single().pinchazos)

        vm.borrar(mediodia)
        runCurrent()
        assertEquals(listOf(EventoInformes.Borrado(mediodia)), eventos)
        assertEquals(listOf(manana), vm.estado.value.historial.single().pinchazos)
        assertEquals(2.0, vm.estado.value.resumen.unidades, 0.0)

        vm.restaurar(mediodia)
        runCurrent()
        assertEquals(listOf(mediodia, manana), vm.estado.value.historial.single().pinchazos)
    }

    @Test
    fun borrarElUnicoPinchazoDelDiaQuitaElDia() = runTest(dispatcher) {
        val dao = FakePinchazoDao()
        val repo = PinchazoRepository(dao) { zona }
        val p = repo.registrar(3.0, Insulinas.HUMALOG_JUNIOR, ahora)
        val vm = InformesViewModel(repo, reloj = flowOf(ahora), zona = { zona })
        backgroundScope.launch { vm.estado.collect {} }
        runCurrent()

        vm.borrar(p)
        runCurrent()
        assertEquals(emptyList<Pinchazo>(), dao.guardados)
        assertEquals(0, vm.estado.value.historial.size)
        assertEquals(0, vm.estado.value.resumen.pinchazos)
    }

    @Test
    fun corregirCambiaDosisYHora() = runTest(dispatcher) {
        val dao = FakePinchazoDao()
        val repo = PinchazoRepository(dao) { zona }
        val p = repo.registrar(3.0, Insulinas.HUMALOG_JUNIOR, ahora)
        val vm = InformesViewModel(repo, reloj = flowOf(ahora), zona = { zona })
        backgroundScope.launch { vm.estado.collect {} }
        runCurrent()

        vm.actualizar(p.copy(dosis = 3.5, timestamp = ahora - 2 * hora))
        runCurrent()
        assertEquals(listOf(p.copy(dosis = 3.5, timestamp = ahora - 2 * hora)), dao.guardados)
    }
}
