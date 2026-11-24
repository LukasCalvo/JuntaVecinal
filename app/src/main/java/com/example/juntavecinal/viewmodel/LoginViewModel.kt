package com.example.juntavecinal.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juntavecinal.data.remote.Api
import com.example.juntavecinal.data.remote.ErrorApi
import com.example.juntavecinal.data.remote.peticion
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

class LoginViewModel(
    private val repository: UsuarioRepository = UsuarioRepository(Api.auth)
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

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

        return if (!Rut.esValido(rut)) "RUT inválido (ej: 12.345.678-5)" else null
    }

    fun errorDePassword(password: String): String? = when {
        password.isBlank() -> "La contraseña no puede estar vacía"
        password.length > 128 -> "Máximo 128 caracteres"
        else -> null
    }

    fun login(onSuccess: () -> Unit) {
        if (uiState.cargando) return
        val datos = uiState
        val errores = LoginErrores(
            rut = errorDeRut(datos.rut),
            password = errorDePassword(datos.password)
        )

        uiState = uiState.copy(
            errores = errores,
            rutTocado = true,
            passwordTocado = true
        )
        if (errores != LoginErrores()) return

        uiState = uiState.copy(cargando = true, error = null)
        viewModelScope.launch {
            peticion { repository.login(datos.rut, datos.password) }.onSuccess { sesion ->
                SessionManager.iniciarSesion(sesion)
                uiState = LoginUiState()
                onSuccess()
            }.onFailure { error ->
                uiState = uiState.copy(
                    cargando = false,
                    error = if ((error as? ErrorApi)?.codigo == 401) "RUT o contraseña incorrectos"
                    else error.message
                )
            }
        }
    }

    fun informarError(mensaje: String) {
        uiState = uiState.copy(error = mensaje)
    }
}
