package com.example.juntavecinal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.juntavecinal.data.AppDatabase
import com.example.juntavecinal.model.entity.Reserva
import com.example.juntavecinal.model.repository.ReservaRepository
import com.example.juntavecinal.util.Fechas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReservaRepository(
        AppDatabase.getInstance(application).reservaDao()
    )

    /** Reservas del usuario que aún no terminan, ordenadas: la primera es la próxima. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val proximas: StateFlow<List<Reserva>> = SessionManager.usuarioId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.proximas(id, Fechas.inicioDeHoy())
        }
        .map { lista ->
            val hoy = Fechas.inicioDeHoy()
            val ahora = Fechas.minutosActuales()
            // Si es hoy, descarta las que ya terminaron
            lista.filter { it.fecha > hoy || it.horaFinMin > ahora }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}