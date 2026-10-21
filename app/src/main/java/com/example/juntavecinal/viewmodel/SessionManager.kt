package com.example.juntavecinal.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {
    private val _usuarioId = MutableStateFlow<Int?>(null)
    val usuarioId: StateFlow<Int?> = _usuarioId.asStateFlow()

    private val _nombre = MutableStateFlow<String?>(null)
    val nombre: StateFlow<String?> = _nombre.asStateFlow()

    fun iniciarSesion(id: Int, nombre: String) {
        _usuarioId.value = id
        _nombre.value = nombre
    }

    fun cerrarSesion() {
        _usuarioId.value = null
        _nombre.value = null
    }
}