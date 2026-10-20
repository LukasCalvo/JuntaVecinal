package com.example.juntavecinal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.juntavecinal.data.AppDatabase
import com.example.juntavecinal.model.entity.Reserva
import com.example.juntavecinal.model.ReservaForm
import com.example.juntavecinal.model.repository.ReservaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

class ReservaFormViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val MAX_ASISTENTES = 50
        private const val PATRON_FECHA = "^\\d{2}/\\d{2}/\\d{4}$"
        private const val PATRON_HORA = "^([01]\\d|2[0-3]):([0-5]\\d)$"
    }

    private val repository = ReservaRepository(
        AppDatabase.getInstance(application).reservaDao()
    )

    private val usuarioIdActual = Sesion.usuarioId

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

    fun textoAFecha(texto: String): Long? {
        if (!Regex(PATRON_FECHA).matches(texto)) return null
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.US)
                .apply {
                    isLenient = false
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                .parse(texto)
                ?.time
        } catch (e: Exception) {
            null
        }
    }

    fun fechaATexto(millis: Long?): String =
        millis?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(Date(it))
        } ?: ""

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
        if (valor.length <= 2 && valor.all { it.isDigit() }) {
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
        else -> null
    }

    private fun errorDeFecha(datos: ReservaForm): String? {
        if (datos.fechaTexto.isBlank()) return "Debes indicar la fecha"

        val millis = textoAFecha(datos.fechaTexto)
        return when {
            millis == null ->
                if (datos.fechaTexto.length == 10) "Fecha inválida (usa DD/MM/AAAA)" else null
            esPasada(datos.fechaTexto) -> "La fecha no puede ser anterior a hoy"
            else -> null
        }
    }

    private fun errorFechaAlGuardar(datos: ReservaForm): String? = when {
        datos.fechaTexto.isBlank() -> "Debes indicar la fecha"
        textoAFecha(datos.fechaTexto) == null -> "Fecha inválida (usa DD/MM/AAAA)"
        esPasada(datos.fechaTexto) -> "La fecha no puede ser anterior a hoy"
        else -> null
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

    private fun errorDeAsistentes(datos: ReservaForm): String? {
        val numero = datos.asistentes.trim().toIntOrNull()
        return when {
            datos.asistentes.isBlank() -> "Indica la cantidad de asistentes"
            numero == null -> "Debe ingresar un número"
            numero < 1 || numero > MAX_ASISTENTES -> "Debe estar entre 1 y $MAX_ASISTENTES"
            else -> null
        }
    }

    private fun errorDeObservaciones(datos: ReservaForm): String? =
        if (datos.observaciones.length > 200) "Máximo 200 caracteres" else null

    private fun errorDeReglamento(datos: ReservaForm): String? =
        if (!datos.aceptaReglamento) "Debes aceptar el reglamento de uso" else null

    private fun calcularErrores(datos: ReservaForm): ReservaErrores = ReservaErrores(
        nombre = errorDeNombre(datos),
        fecha = errorDeFecha(datos),
        horaInicio = errorDeHoraInicio(datos),
        horaFin = errorDeHoraFin(datos),
        asistentes = errorDeAsistentes(datos),
        observaciones = errorDeObservaciones(datos),
        aceptaReglamento = errorDeReglamento(datos)
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

    private fun esPasada(fechaTexto: String): Boolean {
        val hoy = SimpleDateFormat("yyyyMMdd", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("America/Santiago") }
            .format(Date())
        val fecha = fechaTexto.substring(6, 10) + fechaTexto.substring(3, 5) + fechaTexto.substring(0, 2)
        return fecha < hoy
    }

    fun inicioDeHoy(): Long {
        val hoy = SimpleDateFormat("yyyyMMdd", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("America/Santiago") }
            .format(Date())
        val texto = hoy.substring(6, 8) + "/" + hoy.substring(4, 6) + "/" + hoy.substring(0, 4)
        return SimpleDateFormat("dd/MM/yyyy", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(texto)!!.time
    }

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
        _errores.value = calcularErrores(_formulario.value).copy(
            fecha = errorFechaAlGuardar(_formulario.value)
        )
        return _errores.value == ReservaErrores()
    }

    fun guardar() {
        if (!validarFormulario()) return
        val datos = _formulario.value
        val fecha = textoAFecha(datos.fechaTexto) ?: return
        val horaInicio = textoAMinutos(datos.horaInicioTexto) ?: return
        val horaFin = textoAMinutos(datos.horaFinTexto) ?: return
        val asistentes = datos.asistentes.trim().toIntOrNull() ?: return

        viewModelScope.launch {
            _guardando.value = true
            _mensajeGeneral.value = null

            val reserva = Reserva(
                usuarioId = usuarioIdActual,
                nombre = datos.nombre.trim(),
                fecha = fecha,
                horaInicioMin = horaInicio,
                horaFinMin = horaFin,
                asistentes = asistentes,
                observaciones = datos.observaciones.trim(),
                requiereProyector = datos.requiereProyector,
                aceptaReglamento = datos.aceptaReglamento
            )

            val error = repository.guardar(reserva)
            _guardando.value = false

            if (error == null) {
                limpiar()
                _guardado.value = true
            } else {
                _mensajeGeneral.value = error
            }
        }
    }

    fun consumirGuardado() { _guardado.value = false }

    fun mostrarAvisoExito() { _mostrarExito.value = true }

    fun ocultarAvisoExito() { _mostrarExito.value = false }

    private fun limpiar() {
        _formulario.value = ReservaForm()
        _errores.value = ReservaErrores()
        _tocados.value = ReservaTocados()
    }
}