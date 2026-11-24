package com.example.juntavecinal.viewmodel

import com.example.juntavecinal.data.remote.ErrorApi
import com.example.juntavecinal.data.remote.Sesion
import com.example.juntavecinal.data.remote.peticion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {
    private val _sesion = MutableStateFlow<Sesion?>(null)
    val sesion = _sesion.asStateFlow()

    fun iniciarSesion(sesion: Sesion) {
        _sesion.value = sesion
    }

    fun cerrarSesion() {
        _sesion.value = null
    }

    internal suspend fun <T> solicitar(sesion: Sesion, accion: suspend () -> T): Result<T> =
        peticion(accion).onFailure { error ->
            if (error is ErrorApi && error.codigo == 401) _sesion.compareAndSet(sesion, null)
        }
}
