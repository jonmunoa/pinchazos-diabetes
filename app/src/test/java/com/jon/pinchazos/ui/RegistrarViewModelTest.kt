package com.jon.pinchazos.ui

import com.jon.pinchazos.data.Insulina
import com.jon.pinchazos.data.MemoriaDosis
import com.jon.pinchazos.data.Pinchazo
import com.jon.pinchazos.data.PinchazoRepository
import com.jon.pinchazos.ui.registrar.EstadoBoton
import com.jon.pinchazos.ui.registrar.EventoRegistro
import com.jon.pinchazos.ui.registrar.RegistrarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrarViewModelTest {
    private val zona = ZoneId.of("Europe/Madrid")
    private val ahora = LocalDateTime.of(2026, 10, 28, 14, 10).atZone(zona).toInstant().toEpochMilli()
    private val hora = 60 * 60 * 1000L

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakePinchazoDao
    private lateinit var insulinas: FakeInsulinaDao
    private lateinit var memoria: FakeMemoria

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        dao = FakePinchazoDao()
        insulinas = FakeInsulinaDao()
        memoria = FakeMemoria(4.5)
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    private class Prueba(val vm: RegistrarViewModel, val eventos: List<EventoRegistro>)

    private fun TestScope.crear(memoria: MemoriaDosis = this@RegistrarViewModelTest.memoria): Prueba {
        val vm = RegistrarViewModel(
            repositorio = PinchazoRepository(dao, insulinas) { zona },
            memoria = memoria,
            reloj = flowOf(ahora),
            ahora = { ahora },
            zona = { zona },
        )
        val eventos = mutableListOf<EventoRegistro>()
        backgroundScope.launch { vm.estado.collect {} }
        backgroundScope.launch { vm.eventos.toList(eventos) }
        runCurrent()
        return Prueba(vm, eventos)
    }

    @Test
    fun arrancaConLaUltimaDosisUsada() = runTest(dispatcher) {
        val vm = crear().vm
        assertEquals(4.5, vm.estado.value.dosis!!, 0.0)
        assertEquals(EstadoBoton.LISTO, vm.estado.value.boton)
    }

    @Test
    fun mientrasNoSeConoceLaDosisNoSeRegistraNada() = runTest(dispatcher) {
        val vm = crear(FakeMemoria(null)).vm
        assertNull(vm.estado.value.dosis)
        assertEquals(EstadoBoton.CARGANDO, vm.estado.value.boton)

        vm.registrar()
        runCurrent()
        assertTrue(dao.guardados.isEmpty())
    }

    @Test
    fun siLasPreferenciasFallanSeUsaLaDosisPorDefecto() = runTest(dispatcher) {
        val vm = crear(FakeMemoria(null, fallar = true)).vm
        assertEquals(Insulina.DOSIS_POR_DEFECTO, vm.estado.value.dosis!!, 0.0)
        assertEquals(EstadoBoton.LISTO, vm.estado.value.boton)
    }

    @Test
    fun registraExactamenteLoQueSeVeYLoRecuerda() = runTest(dispatcher) {
        val p = crear()
        p.vm.elegirDosis(6.0)
        p.vm.registrar()
        runCurrent()

        assertEquals(listOf(Pinchazo(1, ahora, 6.0, Insulina.HUMALOG_JUNIOR)), dao.guardados)
        assertEquals(listOf<EventoRegistro>(EventoRegistro.Registrado(dao.guardados.single())), p.eventos)
        assertEquals(6.0, memoria.guardada!!, 0.0)
        assertEquals(1, p.vm.estado.value.hoyPinchazos)
        assertEquals(6.0, p.vm.estado.value.hoyUnidades, 0.0)
    }

    @Test
    fun unDobleToqueSoloRegistraUnPinchazo() = runTest(dispatcher) {
        val vm = crear().vm
        vm.registrar()
        vm.registrar()
        runCurrent()
        vm.registrar() // aún dentro del bloqueo
        runCurrent()
        assertEquals(1, dao.guardados.size)
        assertEquals(EstadoBoton.REGISTRADO, vm.estado.value.boton)

        advanceTimeBy(RegistrarViewModel.BLOQUEO_TRAS_REGISTRAR_MS + 1)
        runCurrent()
        assertEquals(EstadoBoton.LISTO, vm.estado.value.boton)
        vm.registrar()
        runCurrent()
        assertEquals(2, dao.guardados.size)
    }

    @Test
    fun usaLaHoraElegidaYDespuesVuelveAAhora() = runTest(dispatcher) {
        val vm = crear().vm
        vm.elegirHora(ahora - 3 * hora)
        vm.registrar()
        runCurrent()

        assertEquals(ahora - 3 * hora, dao.guardados.single().timestamp)
        assertNull(vm.estado.value.horaElegida)
    }

    @Test
    fun unaHoraFuturaSeGuardaComoAhora() = runTest(dispatcher) {
        val vm = crear().vm
        vm.elegirHora(ahora + hora)
        vm.registrar()
        runCurrent()
        assertEquals(ahora, dao.guardados.single().timestamp)
    }

    @Test
    fun deshacerBorraElPinchazoRecienRegistrado() = runTest(dispatcher) {
        val p = crear()
        p.vm.registrar()
        runCurrent()
        val registrado = (p.eventos.single() as EventoRegistro.Registrado).pinchazo

        p.vm.deshacer(registrado)
        runCurrent()
        assertTrue(dao.guardados.isEmpty())
        assertNull(p.vm.estado.value.ultimo)
    }

    @Test
    fun siFallaAlGuardarAvisaYSePuedeReintentar() = runTest(dispatcher) {
        dao.fallarAlInsertar = true
        val p = crear()
        p.vm.registrar()
        runCurrent()

        assertTrue(dao.guardados.isEmpty())
        assertTrue(p.eventos.single() is EventoRegistro.Error)
        assertEquals(EstadoBoton.LISTO, p.vm.estado.value.boton)
        assertNull(memoria.guardada)

        dao.fallarAlInsertar = false
        p.vm.registrar()
        runCurrent()
        assertEquals(1, dao.guardados.size)
    }

    @Test
    fun elUltimoEsElMasRecientePorHoraNoPorOrdenDeRegistro() = runTest(dispatcher) {
        val vm = crear().vm
        vm.registrar() // ahora
        runCurrent()
        advanceTimeBy(RegistrarViewModel.BLOQUEO_TRAS_REGISTRAR_MS + 1)
        runCurrent()
        vm.elegirHora(ahora - 5 * hora) // uno olvidado de esta mañana
        vm.registrar()
        runCurrent()

        assertEquals(ahora, vm.estado.value.ultimo!!.timestamp)
        assertEquals(2, vm.estado.value.hoyPinchazos)
    }

    @Test
    fun cadaInsulinaRecuerdaSuDosisYSeRegistraConLaElegida() = runTest(dispatcher) {
        val lantusId = insulinas.insertar(Insulina(nombre = "Lantus", paso = 1.0, maximo = 40.0))
        val p = crear()
        assertEquals(2, p.vm.estado.value.insulinas.size)
        assertEquals(Insulina.HUMALOG_JUNIOR, p.vm.estado.value.insulina!!.nombre)

        p.vm.elegirInsulina(lantusId)
        runCurrent()
        // Sin dosis recordada, la de por defecto; y en sus pasos de unidad entera.
        assertEquals(Insulina.DOSIS_POR_DEFECTO, p.vm.estado.value.dosis!!, 0.0)
        p.vm.elegirDosis(22.4)
        runCurrent()
        assertEquals(22.0, p.vm.estado.value.dosis!!, 0.0)
        p.vm.registrar()
        runCurrent()

        assertEquals("Lantus", dao.guardados.single().insulina)
        assertEquals(22.0, memoria.guardadas[lantusId]!!, 0.0)

        // Al volver a Humalog, su última dosis; su tarjeta no cuenta el pinchazo de Lantus.
        p.vm.elegirInsulina(1)
        runCurrent()
        assertEquals(4.5, p.vm.estado.value.dosis!!, 0.0)
        assertEquals(0, p.vm.estado.value.hoyPinchazos)
        assertNull(p.vm.estado.value.ultimo)
    }

    @Test
    fun arrancaConLaInsulinaDelUltimoPinchazoYSinLasArchivadas() = runTest(dispatcher) {
        val lantusId = insulinas.insertar(Insulina(nombre = "Lantus", paso = 1.0, maximo = 40.0))
        dao.insertar(Pinchazo(timestamp = ahora - hora, dosis = 20.0, insulina = "Lantus"))
        insulinas.insertar(Insulina(nombre = "Vieja", archivada = true))
        val vm = crear().vm
        assertEquals(lantusId, vm.estado.value.insulina!!.id)
        assertEquals(listOf(Insulina.HUMALOG_JUNIOR, "Lantus"), vm.estado.value.insulinas.map { it.nombre })
    }

    @Test
    fun sinInsulinasNoSePuedeRegistrar() = runTest(dispatcher) {
        insulinas.borrar(insulinas.todas().single())
        val vm = crear().vm
        assertTrue(vm.estado.value.cargado)
        assertNull(vm.estado.value.insulina)
        vm.registrar()
        runCurrent()
        assertTrue(dao.guardados.isEmpty())
    }
}
