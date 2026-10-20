package com.example.juntavecinal.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.juntavecinal.data.AppDatabase
import com.example.juntavecinal.model.repository.UsuarioRepository
import com.example.juntavecinal.util.Rut
import kotlinx.coroutines.launch

data class LoginErrores(
    val rut: String? = null,
    val password: String? = null
)

data class LoginUiState(
    val rut: String = "",
    val rutTocado: Boolean = false,
    val password: String = "",
    val passwordTocado: Boolean = false,
    val errores: LoginErrores = LoginErrores(),
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
        viewModelScope.launch { repository.sembrarUsuariosDePrueba() }
    }

    fun onRutChange(valor: String) {
        val formateado = Rut.formatear(valor)
        uiState = uiState.copy(
            rut = formateado,
            rutTocado = true,
            errores = uiState.errores.copy(rut = errorDeRut(formateado)),
            error = null
        )
    }

    fun onPasswordChange(valor: String) {
        uiState = uiState.copy(
            password = valor,
            passwordTocado = true,
            errores = uiState.errores.copy(password = errorDePassword(valor)),
            error = null
        )
    }

    fun errorDeRut(rut: String): String? {
        if (rut.isBlank()) return "Ingresa tu RUT"

        val limpio = Rut.normalizar(rut)
        return if (limpio.length < 8) "RUT inválido (ej: 12.345.678-5)" else null
    }

    fun errorDePassword(password: String): String? =
        if (password.isBlank()) "La contraseña no puede estar vacía" else null

    fun login(onSuccess: () -> Unit) {
        val errores = LoginErrores(
            rut = errorDeRut(uiState.rut),
            password = errorDePassword(uiState.password)
        )

        uiState = uiState.copy(
            errores = errores,
            rutTocado = true,
            passwordTocado = true
        )
        if (errores != LoginErrores()) return

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