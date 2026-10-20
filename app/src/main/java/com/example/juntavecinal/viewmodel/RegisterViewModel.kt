package com.example.juntavecinal.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.juntavecinal.data.AppDatabase
import com.example.juntavecinal.model.repository.UsuarioRepository
import com.example.juntavecinal.model.Ubicaciones
import com.example.juntavecinal.util.Rut
import kotlinx.coroutines.launch

data class RegistroErrores(
    val nombre: String? = null,
    val rut: String? = null,
    val telefono: String? = null,
    val region: String? = null,
    val comuna: String? = null,
    val password: String? = null,
    val confirmacion: String? = null
)

data class RegistroUiState(
    val nombre: String = "",
    val rut: String = "",
    val telefono: String = "",
    val region: String = "",
    val comuna: String = "",
    val filtroComuna: String = "",
    val password: String = "",
    val confirmacion: String = "",
    val errores: RegistroErrores = RegistroErrores(),
    val errorGeneral: String? = null,
    val enviando: Boolean = false
)

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val PASSWORD_MINIMA = 6
        const val LARGO_TELEFONO = 9
    }

    private val repository = UsuarioRepository(
        AppDatabase.getInstance(application).usuarioDao()
    )

    private val ubicaciones = Ubicaciones()

    var uiState by mutableStateOf(RegistroUiState())
        private set

    fun onNombreChange(valor: String) {
        uiState = uiState.copy(
            nombre = valor,
            errores = uiState.errores.copy(nombre = errorDeNombre(valor))
        )
    }

    fun onRutChange(valor: String) {
        val formateado = Rut.formatear(valor)
        uiState = uiState.copy(
            rut = formateado,
            errores = uiState.errores.copy(rut = errorDeRut(formateado))
        )
    }

    fun onTelefonoChange(valor: String) {
        val formateado = formatearTelefono(valor)
        uiState = uiState.copy(
            telefono = formateado,
            errores = uiState.errores.copy(telefono = errorDeTelefono(formateado))
        )
    }

    fun onRegionChange(region: String) {
        uiState = uiState.copy(
            region = region,
            comuna = "",
            filtroComuna = "",
            errores = uiState.errores.copy(region = errorDeRegion(region), comuna = null)
        )
    }

    fun onFiltroComunaChange(valor: String) {
        uiState = uiState.copy(filtroComuna = valor)
    }

    fun onComunaChange(comuna: String) {
        uiState = uiState.copy(
            comuna = comuna,
            filtroComuna = comuna,
            errores = uiState.errores.copy(comuna = errorDeComuna(comuna))
        )
    }

    fun onPasswordChange(valor: String) {
        val errorConfirmacion =
            if (uiState.confirmacion.isNotEmpty() && uiState.confirmacion != valor) {
                "Las contraseñas no coinciden"
            } else {
                null
            }
        uiState = uiState.copy(
            password = valor,
            errores = uiState.errores.copy(
                password = errorDePassword(valor),
                confirmacion = errorConfirmacion
            )
        )
    }

    fun onConfirmacionChange(valor: String) {
        uiState = uiState.copy(
            confirmacion = valor,
            errores = uiState.errores.copy(
                confirmacion = errorDeConfirmacion(valor, uiState.password)
            )
        )
    }

    fun comunasDisponibles(): List<String> =
        ubicaciones.obtenerComunas(uiState.region, uiState.filtroComuna)

    fun regionesDisponibles(): List<String> = ubicaciones.obtenerRegiones()

    fun formatearTelefono(valor: String): String {
        val digitos = valor.filter { it.isDigit() }.take(LARGO_TELEFONO)
        val sb = StringBuilder()
        digitos.forEachIndexed { i, c ->
            if (i == 1 || i == 5) sb.append(' ')
            sb.append(c)
        }
        return sb.toString()
    }

    fun errorDeNombre(nombre: String): String? {
        return when {
            nombre.isBlank() -> "Ingresa tu nombre completo"
            nombre.trim().length < 3 -> "Debe tener al menos 3 caracteres"
            else -> null
        }
    }

    fun errorDeRut(rut: String): String? {
        return when {
            rut.isBlank() -> "Ingresa tu RUT"
            !Rut.esValido(rut) -> "RUT inválido (ej: 12.345.678-5)"
            else -> null
        }
    }

    fun errorDeTelefono(telefono: String): String? {
        val digitos = telefono.filter { it.isDigit() }
        return when {
            digitos.isEmpty() -> "Ingresa tu teléfono"
            digitos.length < LARGO_TELEFONO -> "Debe tener $LARGO_TELEFONO dígitos"
            else -> null
        }
    }

    fun errorDeRegion(region: String): String? {
        return if (region.isBlank()) "Selecciona tu región" else null
    }

    fun errorDeComuna(comuna: String): String? {
        return if (comuna.isBlank()) "Selecciona tu comuna" else null
    }

    fun errorDePassword(password: String): String? {
        return when {
            password.isBlank() -> "Crea una contraseña"
            password.length < PASSWORD_MINIMA -> "Debe tener al menos $PASSWORD_MINIMA caracteres"
            else -> null
        }
    }

    fun errorDeConfirmacion(confirmacion: String, password: String): String? {
        return when {
            confirmacion.isBlank() -> "Confirma tu contraseña"
            confirmacion != password -> "Las contraseñas no coinciden"
            else -> null
        }
    }

    private fun calcularErrores(): RegistroErrores {
        return RegistroErrores(
            nombre = errorDeNombre(uiState.nombre),
            rut = errorDeRut(uiState.rut),
            telefono = errorDeTelefono(uiState.telefono),
            region = errorDeRegion(uiState.region),
            comuna = errorDeComuna(uiState.comuna),
            password = errorDePassword(uiState.password),
            confirmacion = errorDeConfirmacion(uiState.confirmacion, uiState.password)
        )
    }

    fun registrar(onExito: () -> Unit) {
        val errores = calcularErrores()
        if (errores != RegistroErrores()) {
            uiState = uiState.copy(errores = errores, errorGeneral = null)
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(enviando = true, errorGeneral = null)

            val creado = repository.crearUsuario(
                rut = uiState.rut,
                nombre = uiState.nombre.trim(),
                telefono = uiState.telefono.trim(),
                region = uiState.region,
                comuna = uiState.comuna,
                password = uiState.password
            )

            if (!creado) {
                uiState = uiState.copy(
                    enviando = false,
                    errores = errores.copy(rut = "Ese RUT ya está registrado")
                )
                return@launch
            }

            val usuario = repository.login(uiState.rut, uiState.password)
            if (usuario == null) {
                uiState = uiState.copy(
                    enviando = false,
                    errorGeneral = "No se pudo iniciar sesión con la cuenta creada"
                )
                return@launch
            }

            uiState = RegistroUiState()
            onExito()
        }
    }
}
