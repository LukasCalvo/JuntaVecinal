package com.example.juntavecinal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.juntavecinal.data.AppDatabase
import com.example.juntavecinal.model.entity.Reserva
import com.example.juntavecinal.model.entity.ReservaForm
import com.example.juntavecinal.model.repository.ReservaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Errores por campo: la UI solo los pinta, la lógica vive aquí. */
data class ReservaErrores(
    val nombre: String? = null,
    val fecha: String? = null,
    val horaInicio: String? = null,
    val horaFin: String? = null,
    val asistentes: String? = null,
    val observaciones: String? = null,
    val aceptaReglamento: String? = null
)

class ReservaFormViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val MAX_ASISTENTES = 200
        private const val PATRON_FECHA = "^\\d{2}/\\d{2}/\\d{4}$"
        private const val PATRON_HORA = "^([01]\\d|2[0-3]):([0-5]\\d)$"
    }

    private val repository = ReservaRepository(
        AppDatabase.getInstance(application).reservaDao()
    )

    // TODO: reemplazar por el usuario de la sesión cuando el login lo entregue
    private val usuarioIdActual = 1

    private val _formulario = MutableStateFlow(ReservaForm())
    val formulario: StateFlow<ReservaForm> = _formulario.asStateFlow()

    private val _errores = MutableStateFlow(ReservaErrores())
    val errores: StateFlow<ReservaErrores> = _errores.asStateFlow()

    private val _guardando = MutableStateFlow(false)
    val guardando: StateFlow<Boolean> = _guardando.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    private val _mensajeGeneral = MutableStateFlow<String?>(null)
    val mensajeGeneral: StateFlow<String?> = _mensajeGeneral.asStateFlow()

    // ---------- conversiones (la pantalla las usa para los selectores) ----------

    /** "25/12/2026" -> milis (medianoche UTC). null si la fecha no es válida. */
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

    /** milis -> "25/12/2026" (UTC). */
    fun fechaATexto(millis: Long?): String =
        millis?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(Date(it))
        } ?: ""

    /** "14:30" -> 870 (minutos desde medianoche). null si la hora no es válida. */
    fun textoAMinutos(texto: String): Int? {
        val m = Regex(PATRON_HORA).matchEntire(texto) ?: return null
        return m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()
    }

    // ---------- cambios de campo ----------

    fun actualizarNombre(valor: String) {
        _formulario.value = _formulario.value.copy(nombre = valor)
        _errores.value = _errores.value.copy(nombre = null)
        _mensajeGeneral.value = null
    }

    /** La fecha se puede escribir: solo se conservan los dígitos y se ponen las "/". */
    fun actualizarFechaTexto(valor: String) {
        _formulario.value = _formulario.value.copy(fechaTexto = formatearFechaEntrada(valor))
        _errores.value = _errores.value.copy(fecha = null)
    }

    fun actualizarHoraInicioTexto(valor: String) {
        _formulario.value = _formulario.value.copy(horaInicioTexto = formatearHoraEntrada(valor))
        _errores.value = _errores.value.copy(horaInicio = null)
    }

    fun actualizarHoraFinTexto(valor: String) {
        _formulario.value = _formulario.value.copy(horaFinTexto = formatearHoraEntrada(valor))
        _errores.value = _errores.value.copy(horaFin = null)
    }

    fun actualizarAsistentes(valor: String) {
        if (valor.length <= 3 && valor.all { it.isDigit() }) {
            _formulario.value = _formulario.value.copy(asistentes = valor)
            _errores.value = _errores.value.copy(asistentes = null)
        }
    }

    fun actualizarObservaciones(valor: String) {
        _formulario.value = _formulario.value.copy(observaciones = valor)
        _errores.value = _errores.value.copy(observaciones = null)
    }

    fun actualizarRequiereProyector(valor: Boolean) {
        _formulario.value = _formulario.value.copy(requiereProyector = valor)
    }

    fun actualizarAceptaReglamento(valor: Boolean) {
        _formulario.value = _formulario.value.copy(aceptaReglamento = valor)
        _errores.value = _errores.value.copy(aceptaReglamento = null)
    }

    // ---------- formato automático mientras se escribe ----------

    /** 25122026 -> 25/12/2026 */
    private fun formatearFechaEntrada(valor: String): String {
        val digitos = valor.filter { it.isDigit() }.take(8)
        val sb = StringBuilder()
        digitos.forEachIndexed { i, c ->
            if (i == 2 || i == 4) sb.append('/')
            sb.append(c)
        }
        return sb.toString()
    }

    /** 1430 -> 14:30 */
    private fun formatearHoraEntrada(valor: String): String {
        val digitos = valor.filter { it.isDigit() }.take(4)
        return if (digitos.length <= 2) digitos
        else digitos.substring(0, 2) + ":" + digitos.substring(2)
    }

    // ---------- validación (separada de la UI) ----------

    fun validarFormulario(): Boolean {
        val datos = _formulario.value

        val nombreError = when {
            datos.nombre.isBlank() -> "El nombre es obligatorio"
            datos.nombre.trim().length < 3 -> "Debe tener al menos 3 caracteres"
            else -> null
        }

        val fechaMillis = textoAFecha(datos.fechaTexto)
        val fechaError = when {
            datos.fechaTexto.isBlank() -> "Debes indicar la fecha"
            fechaMillis == null -> "Fecha inválida (usa DD/MM/AAAA)"
            esPasada(fechaMillis) -> "La fecha no puede ser anterior a hoy"
            else -> null
        }

        val inicioMin = textoAMinutos(datos.horaInicioTexto)
        val horaInicioError = when {
            datos.horaInicioTexto.isBlank() -> "Indica la hora de inicio"
            inicioMin == null -> "Hora inválida (usa HH:MM)"
            else -> null
        }

        val finMin = textoAMinutos(datos.horaFinTexto)
        val horaFinError = when {
            datos.horaFinTexto.isBlank() -> "Indica la hora de término"
            finMin == null -> "Hora inválida (usa HH:MM)"
            inicioMin != null && finMin <= inicioMin ->
                "Debe ser posterior a la hora de inicio"
            else -> null
        }

        val asistentes = datos.asistentes.trim().toIntOrNull()
        val asistentesError = when {
            datos.asistentes.isBlank() -> "Indica la cantidad de asistentes"
            asistentes == null -> "Debe ingresar un número"
            asistentes < 1 || asistentes > MAX_ASISTENTES ->
                "Debe estar entre 1 y $MAX_ASISTENTES"
            else -> null
        }
        val observacionesError =
            if (datos.observaciones.length > 200) "Máximo 200 caracteres" else null
        val reglamentoError =
            if (!datos.aceptaReglamento) "Debes aceptar el reglamento de uso" else null

        _errores.value = ReservaErrores(
            nombre = nombreError,
            fecha = fechaError,
            horaInicio = horaInicioError,
            horaFin = horaFinError,
            asistentes = asistentesError,
            observaciones = observacionesError,
            aceptaReglamento = reglamentoError
        )
        return _errores.value == ReservaErrores()
    }

    /** Compara la fecha con hoy usando el formato yyyyMMdd. */
    private fun esPasada(fechaMillis: Long): Boolean {
        val formato = SimpleDateFormat("yyyyMMdd", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
        return formato.format(Date(fechaMillis)) < formato.format(Date())
    }

    // ---------- guardado ----------

    fun guardar() {
        if (!validarFormulario()) return
        viewModelScope.launch {
            _guardando.value = true
            _mensajeGeneral.value = null

            val datos = _formulario.value
            val reserva = Reserva(
                usuarioId = usuarioIdActual,
                nombre = datos.nombre.trim(),
                fecha = textoAFecha(datos.fechaTexto) ?: 0L,
                horaInicioMin = textoAMinutos(datos.horaInicioTexto) ?: 0,
                horaFinMin = textoAMinutos(datos.horaFinTexto) ?: 0,
                asistentes = datos.asistentes.trim().toInt(),
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

    private fun limpiar() {
        _formulario.value = ReservaForm()
        _errores.value = ReservaErrores()
    }
}