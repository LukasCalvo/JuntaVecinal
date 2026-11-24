package com.example.juntavecinal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.juntavecinal.data.remote.Sesion
import com.example.juntavecinal.model.entity.Reserva
import com.example.juntavecinal.model.repository.ReservaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EstadoReservas(val cargando: Boolean = false, val error: String? = null)

class ReservaListViewModel(private val repository: ReservaRepository) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoReservas())
    val estado = _estado.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val reservas = SessionManager.sesion.flatMapLatest { sesion ->
        if (sesion == null) flowOf(emptyList()) else repository.observar(sesion.usuario.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun actualizar() = operar { repository.actualizar(it) }

    fun cancelar(reserva: Reserva) = operar { repository.cancelar(it, reserva.id) }

    private fun operar(accion: suspend (Sesion) -> Unit) {
        val sesion = SessionManager.sesion.value ?: return
        if (_estado.value.cargando) return
        _estado.value = EstadoReservas(cargando = true)
        viewModelScope.launch {
            val resultado = SessionManager.solicitar(sesion) { accion(sesion) }
            _estado.value = EstadoReservas(error = resultado.exceptionOrNull()?.message)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ReservaListViewModel(ReservaRepository.getInstance(checkNotNull(this[APPLICATION_KEY]))) }
        }
    }
}
