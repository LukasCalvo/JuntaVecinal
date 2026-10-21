package com.example.juntavecinal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.juntavecinal.data.AppDatabase
import com.example.juntavecinal.model.entity.Reserva
import com.example.juntavecinal.model.repository.ReservaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReservaListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReservaRepository(
        AppDatabase.getInstance(application).reservaDao()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val reservas: StateFlow<List<Reserva>> = SessionManager.usuarioId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.delUsuario(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun eliminar(reserva: Reserva) {
        viewModelScope.launch { repository.eliminar(reserva) }
    }
}