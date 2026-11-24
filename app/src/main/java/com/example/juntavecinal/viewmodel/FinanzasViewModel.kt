package com.example.juntavecinal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juntavecinal.data.remote.Api
import com.example.juntavecinal.data.remote.DatosMovimiento
import com.example.juntavecinal.data.remote.DatosPago
import com.example.juntavecinal.data.remote.TipoMovimiento
import com.example.juntavecinal.data.remote.Sesion
import com.example.juntavecinal.model.repository.DatosFinanzas
import com.example.juntavecinal.model.repository.FinanzasRepository
import com.example.juntavecinal.util.Fechas
import java.math.BigDecimal
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstadoFinanzas(
    val mes: YearMonth = YearMonth.from(Fechas.hoy()),
    val datos: DatosFinanzas? = null,
    val cargando: Boolean = false,
    val error: String? = null
)

data class FormularioMovimiento(
    val tipo: TipoMovimiento = TipoMovimiento.INGRESO,
    val categoria: String = "",
    val descripcion: String = "",
    val monto: String = "",
    val fecha: String = Fechas.hoy().toString()
)

data class FormularioPago(
    val cuotaId: Long? = null,
    val monto: String = "",
    val medio: String = ""
)

class FinanzasViewModel(
    private val repository: FinanzasRepository = FinanzasRepository(Api.finanzas)
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoFinanzas())
    val estado = _estado.asStateFlow()

    fun actualizar() {
        val mes = _estado.value.mes
        operar({ repository.consultar(it, mes) }) { datos ->
            _estado.value = _estado.value.copy(datos = datos)
        }
    }

    fun cambiarMes(pasos: Long) {
        if (_estado.value.cargando) return
        _estado.value = EstadoFinanzas(mes = _estado.value.mes.plusMonths(pasos))
        actualizar()
    }

    fun limpiarError() {
        _estado.value = _estado.value.copy(error = null)
    }

    fun guardarMovimiento(form: FormularioMovimiento, alGuardar: () -> Unit) {
        val categoria = form.categoria.trim()
        val monto = montoValido(form.monto)
        val descripcion = form.descripcion.trim()
        val fecha = fechaValida(form.fecha)
        if (categoria.length !in 1..80 || monto == null || descripcion.length !in 1..500 || fecha == null) {
            informar("Indica una categoría, una descripción, una fecha y un monto válido.")
            return
        }
        val datos = DatosMovimiento(form.tipo, categoria, descripcion, monto, fecha)
        operar({ repository.guardar(it, datos) }) {
            alGuardar()
            actualizar()
        }
    }

    fun registrarPago(form: FormularioPago, alGuardar: () -> Unit) {
        val cuota = _estado.value.datos?.cuotasPendientes?.find { it.id == form.cuotaId }
        val monto = montoValido(form.monto)
        val medio = form.medio.trim()
        if (cuota == null || monto == null || monto > cuota.saldoPendiente || medio.length !in 1..50) {
            informar("Selecciona una cuota, indica el medio de pago e ingresa un monto que no supere el saldo pendiente.")
            return
        }
        val datos = DatosPago(cuota.id, monto, medio)
        operar({ repository.registrarPago(it, datos) }) {
            alGuardar()
            actualizar()
        }
    }

    private fun montoValido(texto: String): BigDecimal? =
        texto.trim().replace(',', '.').toBigDecimalOrNull()?.takeIf {
            it >= BigDecimal("0.01") && it.scale() <= 2 && it.precision().toLong() - it.scale() <= 12
        }

    private fun fechaValida(texto: String): String? =
        Fechas.leerIso(texto.trim())?.takeIf { it.year >= 1000 && !it.isAfter(Fechas.hoy()) }?.toString()

    private fun informar(error: String?) {
        _estado.value = _estado.value.copy(error = error)
    }

    private fun <T> operar(accion: suspend (Sesion) -> T, alTerminar: (T) -> Unit) {
        val sesion = SessionManager.sesion.value ?: return
        if (_estado.value.cargando) return
        _estado.value = _estado.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            val resultado = SessionManager.solicitar(sesion) { accion(sesion) }
            _estado.value = _estado.value.copy(cargando = false)
            resultado.onSuccess(alTerminar).onFailure { informar(it.message) }
        }
    }
}
