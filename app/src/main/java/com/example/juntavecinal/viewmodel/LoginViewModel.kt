package com.example.juntavecinal.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.juntavecinal.data.AppDatabase
import com.example.juntavecinal.model.repository.UsuarioRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
data class LoginUiState(
    val rut: String = "",
    val password: String = "",
    val error: String? = null,
    val cargando: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UsuarioRepository(
        AppDatabase.getInstance(application).usuarioDao()
    )

    var uiState by mutableStateOf(LoginUiState())
        private set

    init {
        // Crea los usuarios de prueba la primera vez
        viewModelScope.launch { repository.sembrarUsuariosDePrueba() }
    }

    fun onRutChange(valor: String) {
        uiState = uiState.copy(rut = valor, error = null)
    }

    fun onPasswordChange(valor: String) {
        uiState = uiState.copy(password = valor, error = null)
    }

    fun login(onSuccess: () -> Unit) {
        if (uiState.rut.isBlank() || uiState.password.isBlank()) {
            uiState = uiState.copy(error = "Completa RUT y contraseña")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true)
            val usuario = repository.login(uiState.rut, uiState.password)
            if (usuario != null) {
                uiState = uiState.copy(cargando = false)
                onSuccess()
            } else {
                uiState = uiState.copy(
                    cargando = false,
                    error = "RUT o contraseña incorrectos"
                )
            }
        }
    }
}