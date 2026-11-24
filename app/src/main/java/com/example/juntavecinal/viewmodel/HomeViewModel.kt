package com.example.juntavecinal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juntavecinal.model.entity.Reserva
import com.example.juntavecinal.model.repository.FeriadosRepository
import com.example.juntavecinal.util.Fechas
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstadoFeriadosInicio(
    val cargando: Boolean = false,
    val fecha: LocalDate? = null,
    val nombre: String? = null
)

class HomeViewModel(
    private val feriadosRepository: FeriadosRepository = FeriadosRepository.compartido
) : ViewModel() {
    private val _feriados = MutableStateFlow(EstadoFeriadosInicio())
    val feriados = _feriados.asStateFlow()

    fun consultarFeriados() {
        if (_feriados.value.cargando) return
        _feriados.value = EstadoFeriadosInicio(cargando = true)
        viewModelScope.launch {
            val proximo = feriadosRepository.obtener().getOrNull()?.proximo(Fechas.hoy())
            _feriados.value = EstadoFeriadosInicio(fecha = proximo?.key, nombre = proximo?.value)
        }
    }

    fun proximas(
        reservas: List<Reserva>, usuarioId: Long, ahora: LocalDateTime = LocalDateTime.now(Fechas.SANTIAGO)
    ): List<Reserva> {
        val hoy = ahora.toLocalDate().toString()
        return reservas.filter { reserva ->
            reserva.usuarioId == usuarioId && reserva.estado != "CANCELADA" &&
                (reserva.fecha > hoy || reserva.fecha == hoy && LocalTime.parse(reserva.horaFin) > ahora.toLocalTime())
        }.sortedWith(compareBy(Reserva::fecha, Reserva::horaInicio))
    }
}
