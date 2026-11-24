package com.example.juntavecinal

import com.example.juntavecinal.data.remote.*
import com.example.juntavecinal.model.dao.ReservaDao
import com.example.juntavecinal.model.entity.Reserva
import com.example.juntavecinal.model.entity.ReservaCache
import com.example.juntavecinal.model.repository.*
import com.example.juntavecinal.ui.components.SelectableDiasDesde
import com.example.juntavecinal.util.Fechas
import com.example.juntavecinal.util.Rut
import com.example.juntavecinal.viewmodel.*
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class LogicaMovilTest {
    private val dispatcher = StandardTestDispatcher()
    private val vecino = Sesion("token-vecino", Perfil(4L, "Vecina", Rol.VECINO))
    private val directiva = Sesion("token-directiva", Perfil(5L, "Directiva", Rol.DIRECTIVA))
    private val tesoreria = Sesion("token-tesoreria", Perfil(6L, "Tesorería", Rol.TESORERO))

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        SessionManager.cerrarSesion()
    }

    @After
    fun limpiar() {
        SessionManager.cerrarSesion()
        Dispatchers.resetMain()
    }

    @Test
    fun elLoginValidaYGuardaUnaSolaSesion() = runTest(dispatcher) {
        val api = AuthPrueba(vecino)
        val vm = LoginViewModel(UsuarioRepository(api))
        vm.login { fail() }
        assertNotNull(vm.uiState.errores.rut)
        assertNotNull(vm.uiState.errores.password)
        vm.onRutChange("123456786")
        assertNotNull(vm.uiState.errores.rut)
        vm.onPasswordChange("x".repeat(129))
        assertNotNull(vm.uiState.errores.password)
        vm.informarError("Permiso denegado")
        assertEquals("Permiso denegado", vm.uiState.error)
        vm.onRutChange("123456785")
        vm.onPasswordChange("clave")
        api.error = http(401)
        vm.login { fail() }
        vm.login { fail() }
        advanceUntilIdle()
        assertEquals(1, api.logins.size)
        assertEquals("RUT o contraseña incorrectos", vm.uiState.error)
        api.error = IOException()
        vm.login { fail() }
        advanceUntilIdle()
        assertTrue(vm.uiState.error!!.contains("conectar"))
        api.error = null
        var completado = false
        vm.login { completado = true }
        advanceUntilIdle()
        assertEquals(Credenciales("12.345.678-5", "clave"), api.logins.last())
        assertEquals(vecino, SessionManager.sesion.value)
        assertEquals(LoginUiState(), vm.uiState)
        assertTrue(completado)
    }

    @Test
    fun elRegistroValidaCamposYConservaLaCuentaCuandoElRutEstaRepetido() = runTest(dispatcher) {
        val api = AuthPrueba(vecino)
        val vm = RegisterViewModel(UsuarioRepository(api))
        vm.registrar { fail() }
        with(vm.uiState.errores) {
            assertTrue(listOf(nombre, rut, telefono, region, comuna, password, confirmacion).all { it != null })
        }
        vm.onNombreChange("ab")
        assertNotNull(vm.uiState.errores.nombre)
        vm.onNombreChange("x".repeat(121))
        assertNotNull(vm.uiState.errores.nombre)
        vm.onNombreChange("Vecina de prueba")
        vm.onRutChange("123456786")
        assertNotNull(vm.uiState.errores.rut)
        vm.onRutChange("123456785")
        vm.onTelefonoChange("98")
        assertNotNull(vm.uiState.errores.telefono)
        vm.onTelefonoChange("9 1234 5678")
        val region = vm.regionesDisponibles().first { it.contains("Metropolitana") }
        vm.onRegionChange(region)
        vm.onFiltroComunaChange("Cerr")
        assertTrue("Cerrillos" in vm.comunasDisponibles())
        vm.onComunaChange("Cerrillos")
        vm.onPasswordChange("corta")
        assertNotNull(vm.uiState.errores.password)
        vm.onPasswordChange("x".repeat(129))
        assertNotNull(vm.uiState.errores.password)
        vm.onConfirmacionChange("diferente")
        vm.onPasswordChange("Clave123")
        assertNotNull(vm.uiState.errores.confirmacion)
        vm.onConfirmacionChange("Clave123")
        api.error = http(400, """{"message":"Ese RUT ya está registrado"}""")
        vm.registrar { fail() }
        vm.registrar { fail() }
        advanceUntilIdle()
        assertEquals(1, api.registros.size)
        assertEquals("Ese RUT ya está registrado", vm.uiState.errores.rut)
        assertEquals("Cerrillos", vm.uiState.comuna)
        api.error = IOException()
        vm.registrar { fail() }
        advanceUntilIdle()
        assertNotNull(vm.uiState.errorGeneral)
        vm.informarError("Permiso denegado")
        assertEquals("Permiso denegado", vm.uiState.errorGeneral)
        api.error = null
        var completado = false
        vm.registrar { completado = true }
        advanceUntilIdle()
        assertEquals("9 1234 5678", api.registros.last().telefono)
        assertEquals(vecino, SessionManager.sesion.value)
        assertTrue(completado)
    }

    @Test
    fun elFormularioCreaReintentaYEditaSinPerderLaFechaLocal() = runTest(dispatcher) {
        val api = ReservasPrueba()
        val dao = CachePrueba()
        val hoy = Fechas.hoy()
        val feriados = FeriadosPrueba(
            listOf(hoy.plusDays(1).year, hoy.plusDays(2).year).distinct().map { anio ->
                val fecha = LocalDate.of(anio, 7, 1)
                val otroDia = if (fecha in hoy.plusDays(1)..hoy.plusDays(2)) fecha.plusDays(3) else fecha
                FeriadoRemoto(otroDia.toString(), "Feriado de prueba", "Nacional")
            }
        )
        val vm = ReservaFormViewModel(ReservaRepository(dao, api), FeriadosRepository(feriados))
        SessionManager.iniciarSesion(vecino)
        vm.preparar(null)
        advanceUntilIdle()
        vm.guardar()
        with(vm.errores.value) {
            assertTrue(listOf(nombre, fecha, horaInicio, horaFin, asistentes, aceptaReglamento).all { it != null })
        }
        vm.actualizarNombre("ab")
        assertNotNull(vm.errores.value.nombre)
        vm.actualizarNombre("x".repeat(121))
        assertNotNull(vm.errores.value.nombre)
        vm.actualizarNombre("Taller")
        vm.actualizarFechaTexto("31022026")
        assertNotNull(vm.errores.value.fecha)
        vm.actualizarFechaTexto(Fechas.formatearFecha(Fechas.hoy().minusDays(1)))
        assertNotNull(vm.errores.value.fecha)
        val fecha = Fechas.hoy().plusDays(1)
        vm.actualizarFechaTexto(Fechas.formatearFecha(fecha))
        vm.actualizarHoraInicioTexto("25")
        vm.actualizarHoraFinTexto("13")
        assertFalse(vm.validarFormulario())
        assertNotNull(vm.errores.value.horaInicio)
        assertNotNull(vm.errores.value.horaFin)
        vm.actualizarHoraInicioTexto("2460")
        vm.actualizarHoraFinTexto("1270")
        assertNotNull(vm.errores.value.horaInicio)
        assertNotNull(vm.errores.value.horaFin)
        vm.actualizarHoraInicioTexto("1000")
        vm.actualizarHoraFinTexto("0930")
        assertNotNull(vm.errores.value.horaFin)
        vm.actualizarHoraFinTexto("1100")
        vm.actualizarAsistentes("51")
        assertNotNull(vm.errores.value.asistentes)
        vm.actualizarAsistentes("no")
        assertEquals("51", vm.formulario.value.asistentes)
        vm.actualizarAsistentes("15")
        vm.actualizarObservaciones("x".repeat(201))
        assertNotNull(vm.errores.value.observaciones)
        vm.actualizarObservaciones("con mesas")
        vm.actualizarRequiereProyector(true)
        vm.actualizarAceptaReglamento(true)
        assertTrue(vm.validarFormulario())
        assertEquals(vm.inicioDeHoy(), vm.textoAFecha(Fechas.formatearFecha(Fechas.hoy())))
        assertEquals(Fechas.formatearFecha(fecha), vm.fechaATexto(vm.textoAFecha(Fechas.formatearFecha(fecha))))
        assertEquals("", vm.fechaATexto(null))

        SessionManager.cerrarSesion()
        vm.guardar()
        assertNotNull(vm.mensajeGeneral.value)
        SessionManager.iniciarSesion(vecino)
        api.error = IOException()
        vm.guardar()
        vm.guardar()
        advanceUntilIdle()
        assertEquals(1, api.solicitudes.size)
        assertNotNull(vm.mensajeGeneral.value)
        api.error = null
        vm.guardar()
        advanceUntilIdle()
        assertEquals(api.solicitudes[0], api.solicitudes[1])
        assertEquals(Fechas.fechaPicker(fecha), api.solicitudes.last().fecha)
        assertEquals(600, api.solicitudes.last().horaInicioMin)
        assertEquals(vecino.usuario.id, api.solicitudes.last().usuarioId)
        assertTrue(vm.guardado.value)
        vm.mostrarAvisoExito()
        assertTrue(vm.mostrarExito.value)
        vm.ocultarAvisoExito()
        vm.consumirGuardado()
        assertFalse(vm.mostrarExito.value)
        assertFalse(vm.guardado.value)
        val reserva = dao.guardadas(vecino.usuario.id).single().reserva
        vm.preparar(reserva)
        advanceUntilIdle()
        assertEquals("Taller", vm.formulario.value.nombre)
        vm.actualizarFechaTexto(Fechas.formatearFecha(fecha.plusDays(1)))
        vm.actualizarHoraFinTexto("1130")
        vm.guardar()
        advanceUntilIdle()
        assertEquals(reserva.id, api.ultimoEditado)
        assertEquals(HorarioReserva(vecino.usuario.id, 2, fecha.plusDays(1).toString(), "10:00", "11:30"), api.horario)
        val editada = dao.guardadas(vecino.usuario.id).single().reserva
        assertEquals("11:30", editada.horaFin)
        assertEquals("Taller", editada.nombre)
        assertEquals("con mesas", editada.observaciones)
    }

    @Test
    fun lasReservasSeSeparanPorCuentaYElErrorConservaLaCache() = runTest(dispatcher) {
        val api = ReservasPrueba()
        val dao = CachePrueba()
        val vm = ReservaListViewModel(ReservaRepository(dao, api))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.reservas.collect {} }
        val reserva = reserva(1L, Fechas.hoy().toString())
        vm.actualizar()
        assertFalse(vm.estado.value.cargando)
        SessionManager.iniciarSesion(vecino)
        api.lista = listOf(reservaMovil(reserva))
        vm.actualizar()
        vm.actualizar()
        advanceUntilIdle()
        assertEquals(listOf(reserva), vm.reservas.value)
        assertEquals(listOf(vecino.usuario.id), api.usuariosConsultados)
        api.error = IOException()
        vm.actualizar()
        advanceUntilIdle()
        assertNotNull(vm.estado.value.error)
        assertEquals(listOf(reserva), vm.reservas.value)
        api.error = null
        vm.cancelar(reserva)
        advanceUntilIdle()
        assertEquals("CANCELADA", vm.reservas.value.single().estado)
        SessionManager.iniciarSesion(directiva)
        advanceUntilIdle()
        assertEquals(emptyList<Reserva>(), vm.reservas.value)
        val otra = reserva(2, Fechas.hoy().toString(), usuario = directiva.usuario.id)
        api.lista = listOf(reservaMovil(otra))
        vm.actualizar()
        advanceUntilIdle()
        assertEquals(directiva.usuario.id, api.usuariosConsultados.last())
        assertEquals("CANCELADA", dao.guardadas(vecino.usuario.id).single().reserva.estado)
        assertEquals(otra, vm.reservas.value.single())
        api.error = http(401)
        vm.actualizar()
        advanceUntilIdle()
        assertNull(SessionManager.sesion.value)
    }

    @Test
    fun feriadosBloqueanElCalendarioLaCreacionYLaEdicionDeUnaReserva() = runTest(dispatcher) {
        val hoy = Fechas.hoy()
        val feriado = hoy.plusDays(1)
        val diaRegional = hoy.plusDays(2)
        val api = ReservasPrueba()
        val dao = CachePrueba()
        val feriados = FeriadosPrueba(
            listOf(
                FeriadoRemoto(feriado.toString(), "Feriado nacional de prueba", "Nacional"),
                FeriadoRemoto(diaRegional.toString(), "Feriado de otra región", "Regional"),
                FeriadoRemoto(diaRegional.plusMonths(3).withYear(diaRegional.year).toString(), "Otro feriado", "Nacional")
            )
        )
        feriados.error = IOException()
        val vm = ReservaFormViewModel(ReservaRepository(dao, api), FeriadosRepository(feriados))

        SessionManager.iniciarSesion(vecino)
        vm.preparar(null)
        vm.actualizarNombre("Taller de prueba")
        vm.actualizarFechaTexto(Fechas.formatearFecha(feriado))
        vm.actualizarHoraInicioTexto("1000")
        vm.actualizarHoraFinTexto("1100")
        vm.actualizarAsistentes("15")
        vm.actualizarAceptaReglamento(true)
        advanceUntilIdle()

        vm.guardar()
        advanceUntilIdle()
        assertTrue(api.solicitudes.isEmpty())
        assertTrue(vm.mensajeGeneral.value!!.contains("No pudimos consultar"))

        feriados.error = null
        vm.guardar()
        advanceUntilIdle()
        assertTrue(api.solicitudes.isEmpty())
        assertTrue(vm.errores.value.fecha!!.contains("Feriado nacional de prueba"))

        val dias = SelectableDiasDesde(vm.inicioDeHoy(), checkNotNull(vm.calendario.value))
        assertFalse(dias.isSelectableDate(Fechas.fechaPicker(feriado)))
        assertTrue(dias.isSelectableDate(Fechas.fechaPicker(diaRegional)))
        assertFalse(dias.isSelectableDate(Fechas.fechaPicker(hoy.minusDays(1))))
        assertFalse(dias.isSelectableYear(diaRegional.year + 1))

        vm.actualizarFechaTexto(Fechas.formatearFecha(diaRegional))
        vm.guardar()
        advanceUntilIdle()
        assertEquals(1, api.solicitudes.size)
        assertEquals(Fechas.fechaPicker(diaRegional), api.solicitudes.single().fecha)

        vm.preparar(dao.guardadas(vecino.usuario.id).single().reserva)
        vm.actualizarFechaTexto(Fechas.formatearFecha(feriado))
        vm.guardar()
        advanceUntilIdle()
        assertTrue(vm.errores.value.fecha!!.contains("Feriado nacional de prueba"))
        assertNull(api.ultimoEditado)

        val fueraDeCobertura = diaRegional.plusYears(2)
        vm.actualizarFechaTexto(Fechas.formatearFecha(fueraDeCobertura))
        vm.guardar()
        assertTrue(vm.errores.value.fecha!!.contains("no cubre el año"))
        assertNull(api.ultimoEditado)
    }

    @Test
    fun inicioMuestraElProximoFeriadoYOrdenaLasReservasVigentesDelUsuario() = runTest(dispatcher) {
        val fechaActual = Fechas.hoy()
        val feriado = fechaActual.plusDays(3)
        val api = FeriadosPrueba(
            listOf(
                FeriadoRemoto(feriado.plusDays(2).toString(), "Feriado posterior", "Nacional"),
                FeriadoRemoto(fechaActual.minusDays(1).toString(), "Feriado pasado", "Nacional"),
                FeriadoRemoto(fechaActual.plusDays(1).toString(), "Feriado de otra región", "Regional"),
                FeriadoRemoto(feriado.toString(), "Próximo feriado", "Nacional")
            )
        )
        api.error = IOException()
        val vm = HomeViewModel(FeriadosRepository(api))
        vm.consultarFeriados()
        vm.consultarFeriados()
        advanceUntilIdle()
        assertEquals(1, api.consultas)
        assertNull(vm.feriados.value.fecha)
        assertFalse(vm.feriados.value.cargando)

        api.error = null
        vm.consultarFeriados()
        advanceUntilIdle()
        assertEquals(feriado, vm.feriados.value.fecha)
        assertEquals("Próximo feriado", vm.feriados.value.nombre)
        vm.consultarFeriados()
        advanceUntilIdle()
        assertEquals(2, api.consultas)
        val hoy = "2026-11-24"
        val vigentes = vm.proximas(
            listOf(reserva(3, "2026-11-25"), reserva(2, hoy, horaFin = "11:00"),
                reserva(4, hoy, horaFin = "10:00"), reserva(5, "2026-11-25", estado = "CANCELADA"),
                reserva(7, "2026-11-23"),
                reserva(8, hoy, usuario = directiva.usuario.id)),
            vecino.usuario.id, LocalDateTime.parse("2026-11-24T10:30")
        )
        assertEquals(listOf(2L, 3L), vigentes.map { it.id })
    }

    @Test
    fun finanzasConsultaPaginasDelPeriodoYRespetaPermisos() = runTest(dispatcher) {
        val api = FinanzasPrueba()
        val repo = FinanzasRepository(api)
        val periodo = YearMonth.of(2026, 11)
        api.lista = listOf(movimiento(1, "2026-11-01"), movimiento(2, "2026-11-30"))
        val datos = repo.consultar(directiva, periodo)
        assertEquals(listOf(1L, 2L), datos.movimientos.map { it.id })
        assertEquals("2026-11-01" to "2026-11-30", api.periodo)
        assertEquals(listOf(0, 1), api.paginas)
        assertEquals(3L, datos.cuotasPendientes.single().id)
        val secretariado = Sesion("secretaria", Perfil(7L, "Secretaría", Rol.SECRETARIO))
        assertTrue(repo.consultar(secretariado, periodo).cuotasPendientes.isEmpty())
        assertFalse(vecino.usuario.rol.consultaFinanzas)
        assertTrue(secretariado.usuario.rol.consultaFinanzas)
        assertFalse(secretariado.usuario.rol.gestionaFinanzas)
        assertTrue(directiva.usuario.rol.gestionaFinanzas)
        assertTrue(tesoreria.usuario.rol.gestionaFinanzas)
    }

    @Test
    fun finanzasValidaYRegistraMovimientosYPagosDeCuotas() = runTest(dispatcher) {
        val api = FinanzasPrueba()
        val vm = FinanzasViewModel(FinanzasRepository(api))
        SessionManager.iniciarSesion(tesoreria)
        vm.actualizar()
        vm.cambiarMes(1)
        advanceUntilIdle()
        val mes = vm.estado.value.mes
        vm.cambiarMes(1)
        advanceUntilIdle()
        assertEquals(mes.plusMonths(1), vm.estado.value.mes)
        val form = FormularioMovimiento(TipoMovimiento.EGRESO, " Servicios ", " Agua ", "15,25")
        listOf(form.copy(categoria = ""), form.copy(descripcion = ""), form.copy(fecha = "--"),
            form.copy(monto = "0"), form.copy(monto = "1.023"), form.copy(monto = "1e20"),
            form.copy(fecha = "0999-01-02"), form.copy(fecha = Fechas.hoy().plusDays(1).toString())
        ).forEach {
            vm.guardarMovimiento(it) { fail() }
            assertNotNull(vm.estado.value.error)
        }
        vm.limpiarError()
        assertNull(vm.estado.value.error)
        api.error = IOException()
        vm.guardarMovimiento(form) { fail() }
        advanceUntilIdle()
        assertNotNull(vm.estado.value.error)
        api.error = null
        var guardado = false
        vm.guardarMovimiento(form) { guardado = true }
        advanceUntilIdle()
        assertTrue(guardado)
        assertEquals(BigDecimal("15.25"), api.datosMovimiento!!.monto)
        assertEquals("Agua", api.datosMovimiento!!.descripcion)
        assertEquals("Servicios", api.datosMovimiento!!.categoria)
        assertEquals(TipoMovimiento.EGRESO, api.datosMovimiento!!.tipo)

        val pago = FormularioPago(3, "2000", medio = " Efectivo ")
        listOf(pago.copy(cuotaId = null), pago.copy(monto = "abc"), pago.copy(monto = "9999"),
            pago.copy(medio = "")).forEach {
            vm.registrarPago(it) { fail() }
            assertNotNull(vm.estado.value.error)
        }
        vm.registrarPago(pago) {}
        advanceUntilIdle()
        assertEquals(DatosPago(3, BigDecimal("2000"), "Efectivo"), api.datosPago)
        SessionManager.cerrarSesion()
        vm.actualizar()
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun interpretaLosErroresHttpYNoCancelaUnaSesionNueva() = runTest(dispatcher) {
        for (codigo in listOf(400, 401, 403, 404, 409, 500)) {
            val resultado = peticion<String> { throw http(codigo, """{"detail":"error esperado"}""") }
            assertEquals(codigo, (resultado.exceptionOrNull() as ErrorApi).codigo)
        }
        assertEquals("error esperado", mensajeHttp(409, "error esperado"))
        assertNotEquals("error esperado", mensajeHttp(500, "error esperado"))
        assertTrue(peticion<String> { throw http(404, "<html>") }.isFailure)
        assertTrue(peticion<String> { throw com.google.gson.JsonSyntaxException("JSON") }.isFailure)
        SessionManager.iniciarSesion(directiva)
        SessionManager.solicitar(vecino) { throw http(401) }
        assertEquals(directiva, SessionManager.sesion.value)
        try {
            peticion<String> { throw CancellationException() }
            fail()
        } catch (_: CancellationException) {
            assertEquals(directiva, SessionManager.sesion.value)
        }
    }

    @Test
    fun fechasRutYPermisosDeUnaReservaSonConsistentes() {
        assertEquals(LocalDate.of(2028, 2, 29), Fechas.leerFecha("29/02/2028"))
        assertNull(Fechas.leerFecha("29/02/2027"))
        assertNull(Fechas.leerIso("2026-02-30"))
        assertEquals("09:30", Fechas.formatearHora("09:30:00"))
        assertEquals("1", Rut.formatear("1"))
        assertEquals("12.345.678-5", Rut.formatear("123456785"))
        assertTrue(Rut.esValido("12.345.678-5"))
        assertFalse(Rut.esValido("123"))
        assertFalse(Rut.esValido("1K3456785"))
        assertFalse(Rut.esValido("123456786"))
        assertTrue(Rut.esValido("12.345.670-K"))
        assertTrue(Rut.esValido("12.345.675-0"))

        val reserva = reserva(3, Fechas.hoy().toString())
        assertTrue(reserva.modificablePor(vecino.usuario))
        assertFalse(reserva.modificablePor(directiva.usuario))
        for ((estado, visible) in mapOf(
            "PENDIENTE" to "Pendiente", "CONFIRMADA" to "Confirmada", "CANCELADA" to "Cancelada", "OTRO" to "OTRO"
        )) {
            val conEstado = reserva.copy(estado = estado)
            assertEquals(visible, conEstado.estadoVisible)
            assertEquals(estado != "CANCELADA", conEstado.modificablePor(vecino.usuario))
            assertFalse(conEstado.modificablePor(directiva.usuario))
        }
    }

    private class AuthPrueba(private val sesion: Sesion) : AuthApi {
        val registros = mutableListOf<Registro>()
        val logins = mutableListOf<Credenciales>()
        var error: Exception? = null

        override suspend fun registrar(datos: Registro) {
            registros.add(datos)
            error?.let { throw it }
        }

        override suspend fun login(datos: Credenciales): Acceso {
            logins.add(datos)
            error?.let { throw it }
            return Acceso(sesion.usuario.id, datos.rut, sesion.usuario.nombre, sesion.usuario.rol, sesion.token)
        }
    }

    private class CachePrueba : ReservaDao {
        private val cuentas = mutableMapOf<Long, MutableStateFlow<List<ReservaCache>>>()
        private fun cuenta(id: Long) = cuentas.getOrPut(id) { MutableStateFlow(emptyList()) }
        fun guardadas(id: Long): List<ReservaCache> = cuenta(id).value
        override fun observar(cuentaId: Long): Flow<List<ReservaCache>> = cuenta(cuentaId)
        override suspend fun borrarCache(cuentaId: Long) { cuenta(cuentaId).value = emptyList() }
        override suspend fun guardar(reservas: List<ReservaCache>) {
            reservas.groupBy { it.cuentaId }.forEach { (id, nuevas) ->
                cuenta(id).value = (cuenta(id).value + nuevas).associateBy { it.reserva.id }.values.toList()
            }
        }
        override suspend fun marcarCancelada(cuentaId: Long, id: Long) {
            cuenta(cuentaId).value = cuenta(cuentaId).value.map {
                if (it.reserva.id == id) it.copy(reserva = it.reserva.copy(estado = "CANCELADA"))
                else it
            }
        }
    }

    private class ReservasPrueba : ReservasApi {
        var lista = emptyList<ReservaMovil>()
        var error: Exception? = null
        var ultimoEditado: Long? = null
        var horario: HorarioReserva? = null
        val usuariosConsultados = mutableListOf<Long>()
        val solicitudes = mutableListOf<SolicitudReserva>()

        override suspend fun listar(auth: String, usuarioId: Long): List<ReservaMovil> {
            usuariosConsultados.add(usuarioId)
            error?.let { throw it }
            return lista
        }

        override suspend fun crear(auth: String, datos: SolicitudReserva): ReservaMovil {
            solicitudes.add(datos)
            error?.let { throw it }
            return ReservaMovil(20, datos.usuarioId, datos.nombre, datos.fecha, datos.horaInicioMin,
                datos.horaFinMin, datos.asistentes, datos.observaciones, datos.requiereProyector,
                datos.aceptaReglamento, "PENDIENTE")
        }

        override suspend fun detalle(auth: String, id: Long): DetalleReserva =
            DetalleReserva(2, "2026-11-24", "10:00", "11:00", "PENDIENTE")

        override suspend fun editar(auth: String, id: Long, datos: HorarioReserva): DetalleReserva {
            ultimoEditado = id
            horario = datos
            return DetalleReserva(datos.espacioId, datos.fecha, datos.horaInicio, datos.horaFin, "PENDIENTE")
        }

        override suspend fun cancelar(auth: String, id: Long) = Unit
    }

    private class FeriadosPrueba(private val datos: List<FeriadoRemoto>) : FeriadosApi {
        var consultas = 0
        var error: Exception? = null

        override suspend fun listar(): RespuestaFeriados {
            consultas++
            error?.let { throw it }
            return RespuestaFeriados(datos)
        }
    }

    private class FinanzasPrueba : FinanzasApi {
        var lista = emptyList<Movimiento>()
        var periodo: Pair<String, String>? = null
        val paginas = mutableListOf<Int>()
        var error: Exception? = null
        var datosMovimiento: DatosMovimiento? = null
        var datosPago: DatosPago? = null

        override suspend fun resumen(auth: String, desde: String, hasta: String): Resumen {
            periodo = desde to hasta
            return Resumen(BigDecimal.TEN, BigDecimal.ONE, BigDecimal("9"))
        }

        override suspend fun movimientos(auth: String, desde: String, hasta: String, pagina: Int): Pagina<Movimiento> {
            assertEquals(periodo, desde to hasta)
            paginas.add(pagina)
            return Pagina(lista.drop(pagina).take(1), lista.size)
        }

        override suspend fun cuotasPendientes(auth: String, pagina: Int): Pagina<CuotaPendiente> {
            assertEquals(0, pagina)
            return Pagina(listOf(CuotaPendiente(3, 7, "Cuota mensual", BigDecimal("3000"))), 1)
        }

        override suspend fun crear(auth: String, datos: DatosMovimiento) {
            error?.let { throw it }
            datosMovimiento = datos
        }

        override suspend fun registrarPago(auth: String, datos: DatosPago) {
            datosPago = datos
        }
    }

    private companion object {
        fun http(codigo: Int, cuerpo: String = ""): HttpException =
            HttpException(Response.error<Unit>(codigo, cuerpo.toResponseBody()))

        fun reserva(id: Long, fecha: String, horaFin: String = "11:00", estado: String = "PENDIENTE", usuario: Long = 4) =
            Reserva(id, usuario, "Reserva $id", fecha, "10:00", horaFin, 10, null, false, true, estado)

        fun reservaMovil(reserva: Reserva) = ReservaMovil(
            reserva.id, reserva.usuarioId, reserva.nombre, Fechas.fechaPicker(LocalDate.parse(reserva.fecha)),
            600, 660, reserva.asistentes, reserva.observaciones, reserva.requiereProyector,
            reserva.aceptaReglamento, reserva.estado
        )

        fun movimiento(id: Long, fecha: String) =
            Movimiento(id, TipoMovimiento.EGRESO, "Servicios", "Agua", BigDecimal.TEN, fecha)
    }
}
