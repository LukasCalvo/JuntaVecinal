package com.example.juntavecinal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.juntavecinal.data.remote.SolicitudReserva
import com.example.juntavecinal.model.ReservaForm
import com.example.juntavecinal.model.entity.Reserva
import com.example.juntavecinal.model.repository.CalendarioFeriados
import com.example.juntavecinal.model.repository.FeriadosRepository
import com.example.juntavecinal.model.repository.ReservaRepository
import com.example.juntavecinal.util.Fechas
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReservaErrores(
    val nombre: String? = null,
    val fecha: String? = null,
    val horaInicio: String? = null,
    val horaFin: String? = null,
    val asistentes: String? = null,
    val observaciones: String? = null,
    val aceptaReglamento: String? = null
)

data class ReservaTocados(
    val nombre: Boolean = false,
    val fecha: Boolean = false,
    val horaInicio: Boolean = false,
    val horaFin: Boolean = false,
    val asistentes: Boolean = false,
    val observaciones: Boolean = false,
    val aceptaReglamento: Boolean = false
)

class ReservaFormViewModel(
    private val repository: ReservaRepository,
    private val feriadosRepository: FeriadosRepository = FeriadosRepository.compartido
) : ViewModel() {

    companion object {
        private const val MAX_ASISTENTES_SEDE = 50
        private const val PATRON_HORA = "^([01]\\d|2[0-3]):([0-5]\\d)$"
        val Factory = viewModelFactory {
            initializer { ReservaFormViewModel(ReservaRepository.getInstance(checkNotNull(this[APPLICATION_KEY]))) }
        }
    }

    private var original: Reserva? = null

    private val _formulario = MutableStateFlow(ReservaForm())
    val formulario: StateFlow<ReservaForm> = _formulario.asStateFlow()

    private val _errores = MutableStateFlow(ReservaErrores())
    val errores: StateFlow<ReservaErrores> = _errores.asStateFlow()

    private val _tocados = MutableStateFlow(ReservaTocados())
    val tocados: StateFlow<ReservaTocados> = _tocados.asStateFlow()

    private val _guardando = MutableStateFlow(false)
    val guardando: StateFlow<Boolean> = _guardando.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    private val _mostrarExito = MutableStateFlow(false)
    val mostrarExito: StateFlow<Boolean> = _mostrarExito.asStateFlow()

    private val _mensajeGeneral = MutableStateFlow<String?>(null)

    val mensajeGeneral: StateFlow<String?> = _mensajeGeneral.asStateFlow()

    private val _calendario = MutableStateFlow<CalendarioFeriados?>(null)
    val calendario = _calendario.asStateFlow()

    private val _consultandoFeriados = MutableStateFlow(false)
    val consultandoFeriados = _consultandoFeriados.asStateFlow()

    fun consultarFeriados() {
        if (_calendario.value != null || _consultandoFeriados.value) return
        _consultandoFeriados.value = true
        viewModelScope.launch {
            feriadosRepository.obtener().onSuccess { calendario ->
                _calendario.value = calendario
                marcarTocado { it }
            }
            _consultandoFeriados.value = false
        }
    }

    fun preparar(reserva: Reserva?) {
        limpiar()
        _guardado.value = false
        _mostrarExito.value = false
        _mensajeGeneral.value = null
        original = reserva
        if (reserva != null) {
            _formulario.value = ReservaForm(
                nombre = reserva.nombre,
                fechaTexto = Fechas.formatearFecha(reserva.fecha),
                horaInicioTexto = Fechas.formatearHora(reserva.horaInicio),
                horaFinTexto = Fechas.formatearHora(reserva.horaFin)
            )
        }
        consultarFeriados()
    }

    fun textoAFecha(texto: String): Long? = Fechas.leerFecha(texto)?.let(Fechas::fechaPicker)

    fun fechaATexto(millis: Long?): String = millis?.let { Fechas.formatearFecha(Fechas.desdePicker(it)) }.orEmpty()

    fun textoAMinutos(texto: String): Int? {
        val m = Regex(PATRON_HORA).matchEntire(texto) ?: return null
        return m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()
    }

    fun actualizarNombre(valor: String) {
        _formulario.value = _formulario.value.copy(nombre = valor)
        _mensajeGeneral.value = null
        marcarTocado { it.copy(nombre = true) }
    }

    fun actualizarFechaTexto(valor: String) {
        _formulario.value = _formulario.value.copy(fechaTexto = formatearFechaEntrada(valor))
        marcarTocado { it.copy(fecha = true) }
    }

    fun actualizarHoraInicioTexto(valor: String) {
        _formulario.value = _formulario.value.copy(horaInicioTexto = formatearHoraEntrada(valor))
        // No se marca horaFin: si el inicio cambia, su error se recalcula solo.
        marcarTocado { it.copy(horaInicio = true) }
    }

    fun actualizarHoraFinTexto(valor: String) {
        _formulario.value = _formulario.value.copy(horaFinTexto = formatearHoraEntrada(valor))
        marcarTocado { it.copy(horaFin = true) }
    }

    fun actualizarAsistentes(valor: String) {
        if (valor.length <= 9 && valor.all { it in '0'..'9' }) {
            _formulario.value = _formulario.value.copy(asistentes = valor)
            marcarTocado { it.copy(asistentes = true) }
        }
    }

    fun actualizarObservaciones(valor: String) {
        _formulario.value = _formulario.value.copy(observaciones = valor)
        marcarTocado { it.copy(observaciones = true) }
    }

    fun actualizarRequiereProyector(valor: Boolean) {
        _formulario.value = _formulario.value.copy(requiereProyector = valor)
    }

    fun actualizarAceptaReglamento(valor: Boolean) {
        _formulario.value = _formulario.value.copy(aceptaReglamento = valor)
        marcarTocado { it.copy(aceptaReglamento = true) }
    }

    fun formatearFechaEntrada(valor: String): String {
        val digitos = valor.filter { it.isDigit() }.take(8)
        val sb = StringBuilder()
        digitos.forEachIndexed { i, c ->
            if (i == 2 || i == 4) sb.append('/')
            sb.append(c)
        }
        return sb.toString()
    }

    fun formatearHoraEntrada(valor: String): String {
        val digitos = valor.filter { it.isDigit() }.take(4)
        return if (digitos.length <= 2) digitos
        else digitos.substring(0, 2) + ":" + digitos.substring(2)
    }

    private fun errorDeNombre(datos: ReservaForm): String? = when {
        datos.nombre.isBlank() -> "El nombre es obligatorio"
        datos.nombre.trim().length < 3 -> "Debe tener al menos 3 caracteres"
        datos.nombre.trim().length > 120 -> "Máximo 120 caracteres"
        else -> null
    }

    private fun errorDeFecha(datos: ReservaForm, alGuardar: Boolean = false): String? {
        val texto = datos.fechaTexto
        if (texto.isBlank()) return "Debes indicar la fecha"
        val fecha = Fechas.leerFecha(texto)
        if (fecha == null) {
            return if (alGuardar || texto.length == 10) "Fecha inválida (usa DD/MM/AAAA)" else null
        }

        if (fecha.isBefore(Fechas.hoy())) return "La fecha no puede ser anterior a hoy"
        return _calendario.value?.let { errorFeriado(it, fecha) }
    }

    private fun errorFeriado(calendario: CalendarioFeriados, fecha: LocalDate): String? {
        if (!calendario.cubre(fecha.year)) return "El calendario de feriados no cubre el año ${fecha.year}."
        return calendario.nacionales[fecha]?.let { "No se permiten reservas en feriados. $it." }
    }

    private fun errorDeHoraInicio(datos: ReservaForm): String? = when {
        datos.horaInicioTexto.isBlank() -> "Indica la hora de inicio"
        textoAMinutos(datos.horaInicioTexto) == null ->
            if (datos.horaInicioTexto.length == 5) "Hora inválida (usa HH:MM)" else null

        else -> null
    }

    private fun errorDeHoraFin(datos: ReservaForm): String? {
        if (datos.horaFinTexto.isBlank()) return "Indica la hora de término"

        val fin = textoAMinutos(datos.horaFinTexto)
            ?: return if (datos.horaFinTexto.length == 5) "Hora inválida (usa HH:MM)" else null

        val inicio = textoAMinutos(datos.horaInicioTexto)
        return if (inicio != null && fin <= inicio) "Debe ser posterior a la hora de inicio" else null
    }

    private fun errorHoraAlGuardar(texto: String): String? =
        if (texto.isNotBlank() && textoAMinutos(texto) == null) "Hora inválida (usa HH:MM)" else null

    private fun errorDeAsistentes(datos: ReservaForm): String? {
        val numero = datos.asistentes.trim().toIntOrNull()
        val maximo = MAX_ASISTENTES_SEDE
        return when {
            datos.asistentes.isBlank() -> "Indica la cantidad de asistentes"
            numero == null -> "Debe ingresar un número"
            numero < 1 || numero > maximo -> "Debe estar entre 1 y $maximo"
            else -> null
        }
    }

    private fun errorDeObservaciones(datos: ReservaForm): String? =
        if (datos.observaciones.length > 200) "Máximo 200 caracteres" else null

    private fun errorDeReglamento(datos: ReservaForm): String? =
        if (!datos.aceptaReglamento) "Debes aceptar el reglamento de uso" else null

    private fun calcularErrores(datos: ReservaForm): ReservaErrores = ReservaErrores(
        nombre = if (original == null) errorDeNombre(datos) else null,
        fecha = errorDeFecha(datos),
        horaInicio = errorDeHoraInicio(datos),
        horaFin = errorDeHoraFin(datos),
        asistentes = if (original == null) errorDeAsistentes(datos) else null,
        observaciones = if (original == null) errorDeObservaciones(datos) else null,
        aceptaReglamento = if (original == null) errorDeReglamento(datos) else null
    )

    private fun ReservaErrores.soloTocados(tocados: ReservaTocados) = copy(
        nombre = if (tocados.nombre) nombre else null,
        fecha = if (tocados.fecha) fecha else null,
        horaInicio = if (tocados.horaInicio) horaInicio else null,
        horaFin = if (tocados.horaFin) horaFin else null,
        asistentes = if (tocados.asistentes) asistentes else null,
        observaciones = if (tocados.observaciones) observaciones else null,
        aceptaReglamento = if (tocados.aceptaReglamento) aceptaReglamento else null
    )

    private fun marcarTocado(actualizar: (ReservaTocados) -> ReservaTocados) {
        _tocados.value = actualizar(_tocados.value)
        _errores.value = calcularErrores(_formulario.value).soloTocados(_tocados.value)
    }

    fun inicioDeHoy(): Long = Fechas.fechaPicker(Fechas.hoy())

    fun validarFormulario(): Boolean {
        _tocados.value = ReservaTocados(
            nombre = true,
            fecha = true,
            horaInicio = true,
            horaFin = true,
            asistentes = true,
            observaciones = true,
            aceptaReglamento = true
        )
        val datos = _formulario.value
        val base = calcularErrores(datos)
        _errores.value = base.copy(
            fecha = errorDeFecha(datos, alGuardar = true),
            horaInicio = base.horaInicio ?: errorHoraAlGuardar(datos.horaInicioTexto),
            horaFin = base.horaFin ?: errorHoraAlGuardar(datos.horaFinTexto)
        )
        return _errores.value == ReservaErrores()
    }

    fun guardar() {
        if (_guardando.value || !validarFormulario()) return

        val sesion = SessionManager.sesion.value ?: run {
            _mensajeGeneral.value = "Tu sesión expiró. Vuelve a iniciar sesión."
            return
        }

        val formulario = _formulario.value
        val fecha = checkNotNull(Fechas.leerFecha(formulario.fechaTexto))
        val reserva = original
        _guardando.value = true
        _mensajeGeneral.value = null

        viewModelScope.launch {
            val calendario = feriadosRepository.obtener().getOrElse {
                _mensajeGeneral.value = "No pudimos consultar los feriados. Vuelve a intentar para guardar la reserva."
                _guardando.value = false
                return@launch
            }
            _calendario.value = calendario
            errorFeriado(calendario, fecha)?.let { error ->
                _errores.value = _errores.value.copy(fecha = error)
                _guardando.value = false
                return@launch
            }

            val resultado = SessionManager.solicitar(sesion) {
                if (reserva == null) {
                    val datos = SolicitudReserva(
                        usuarioId = sesion.usuario.id,
                        nombre = formulario.nombre.trim(),
                        fecha = Fechas.fechaPicker(fecha),
                        horaInicioMin = checkNotNull(textoAMinutos(formulario.horaInicioTexto)),
                        horaFinMin = checkNotNull(textoAMinutos(formulario.horaFinTexto)),
                        asistentes = formulario.asistentes.trim().toInt(),
                        observaciones = formulario.observaciones.trim(),
                        requiereProyector = formulario.requiereProyector,
                        aceptaReglamento = formulario.aceptaReglamento
                    )
                    repository.crear(sesion, datos)
                } else {
                    repository.editar(sesion, reserva, fecha.toString(), formulario.horaInicioTexto, formulario.horaFinTexto)
                }
            }
            _guardando.value = false

            resultado.onSuccess {
                limpiar()
                _guardado.value = true
            }.onFailure { _mensajeGeneral.value = it.message }
        }
    }

    fun consumirGuardado() {
        _guardado.value = false
    }

    fun mostrarAvisoExito() {
        _mostrarExito.value = true
    }

    fun ocultarAvisoExito() {
        _mostrarExito.value = false
    }

    private fun limpiar() {
        _formulario.value = ReservaForm()
        _errores.value = ReservaErrores()
        _tocados.value = ReservaTocados()
    }
}
