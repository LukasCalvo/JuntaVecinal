package com.example.juntavecinal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.juntavecinal.ui.components.CampoFormateado
import com.example.juntavecinal.ui.components.iconoCampo
import com.example.juntavecinal.util.Rut
import com.example.juntavecinal.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(
    onRegistroExitoso: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = viewModel()
) {
    val state = viewModel.uiState
    var verPassword by remember { mutableStateOf(false) }
    var verConfirmacion by remember { mutableStateOf(false) }
    var regionAbierta by remember { mutableStateOf(false) }
    var comunaAbierta by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Crear cuenta",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(4.dp))

        OutlinedTextField(
            value = state.nombre,
            onValueChange = viewModel::onNombreChange,
            label = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = state.errores.nombre != null,
            supportingText = { state.errores.nombre?.let { Text(it) } },
            leadingIcon = iconoCampo(
                error = state.errores.nombre,
                valido = state.nombre.isNotBlank()
            )
        )

        CampoFormateado(
            valor = state.rut,
            onValorChange = viewModel::onRutChange,
            formatear = Rut::formatear,
            label = "RUT",
            placeholder = "12.345.678-5",
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Text,
            isError = state.errores.rut != null,
            supportingText = state.errores.rut ?: "Puedes escribirlo con o sin puntos",
            leadingIcon = iconoCampo(
                error = state.errores.rut,
                valido = state.rut.isNotBlank() && state.errores.rut == null
            )
        )

        CampoFormateado(
            valor = state.telefono,
            onValorChange = viewModel::onTelefonoChange,
            formatear = viewModel::formatearTelefono,
            label = "Teléfono",
            placeholder = "9 1234 1234",
            modifier = Modifier.fillMaxWidth(),
            prefix = "+56",
            isError = state.errores.telefono != null,
            supportingText = state.errores.telefono ?: "Solo los 8 dígitos, sin el +56",
            leadingIcon = iconoCampo(
                error = state.errores.telefono,
                valido = state.errores.telefono == null &&
                        state.telefono.filter { it.isDigit() }.isNotEmpty()
            )
        )

        Column {
            OutlinedTextField(
                value = state.region,
                onValueChange = {},
                label = { Text("Región") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                isError = state.errores.region != null,
                supportingText = { state.errores.region?.let { Text(it) } },
                leadingIcon = iconoCampo(
                    error = state.errores.region,
                    valido = state.region.isNotBlank() && state.errores.region == null
                ),
                trailingIcon = {
                    TextButton(onClick = { regionAbierta = true }) { Text("Elegir") }
                }
            )
            DropdownMenu(
                expanded = regionAbierta,
                onDismissRequest = { regionAbierta = false }
            ) {
                viewModel.regionesDisponibles().forEach { region ->
                    DropdownMenuItem(
                        text = { Text(region) },
                        onClick = {
                            viewModel.onRegionChange(region)
                            regionAbierta = false
                        }
                    )
                }
            }
        }

        Column {
            OutlinedTextField(
                value = state.comuna,
                onValueChange = {},
                label = { Text("Comuna") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = state.region.isNotBlank(),
                isError = state.errores.comuna != null,
                supportingText = {
                    when {
                        state.region.isBlank() -> Text("Primero elige una región")
                        state.errores.comuna != null -> Text(state.errores.comuna)
                    }
                },
                leadingIcon = iconoCampo(
                    error = state.errores.comuna,
                    valido = state.comuna.isNotBlank() && state.errores.comuna == null
                ),
                trailingIcon = {
                    TextButton(onClick = { comunaAbierta = true }) { Text("Elegir") }
                }
            )
            DropdownMenu(
                expanded = comunaAbierta,
                onDismissRequest = { comunaAbierta = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                OutlinedTextField(
                    value = state.filtroComuna,
                    onValueChange = viewModel::onFiltroComunaChange,
                    label = { Text("Buscar comuna") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    singleLine = true
                )
                val comunas = viewModel.comunasDisponibles()
                if (comunas.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Sin resultados") },
                        onClick = { }
                    )
                } else {
                    comunas.forEach { comuna ->
                        DropdownMenuItem(
                            text = { Text(comuna) },
                            onClick = {
                                viewModel.onComunaChange(comuna)
                                comunaAbierta = false
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = state.errores.password != null,
            supportingText = {
                Text(
                    state.errores.password
                        ?: "Mínimo ${RegisterViewModel.PASSWORD_MINIMA} caracteres"
                )
            },
            leadingIcon = iconoCampo(
                error = state.errores.password,
                valido = state.errores.password == null && state.password.isNotEmpty()
            ),
            visualTransformation = if (verPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { verPassword = !verPassword }) {
                    Icon(
                        imageVector = if (verPassword) {
                            Icons.Filled.Visibility
                        } else {
                            Icons.Filled.VisibilityOff
                        },
                        contentDescription = if (verPassword) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        }
                    )
                }
            }
        )

        OutlinedTextField(
            value = state.confirmacion,
            onValueChange = viewModel::onConfirmacionChange,
            label = { Text("Confirmar contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = state.errores.confirmacion != null,
            supportingText = { state.errores.confirmacion?.let { Text(it) } },
            leadingIcon = iconoCampo(
                error = state.errores.confirmacion,
                valido = state.confirmacion.isNotEmpty() &&
                        state.confirmacion == state.password
            ),
            visualTransformation = if (verConfirmacion) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { verConfirmacion = !verConfirmacion }) {
                    Icon(
                        imageVector = if (verConfirmacion) {
                            Icons.Filled.Visibility
                        } else {
                            Icons.Filled.VisibilityOff
                        },
                        contentDescription = if (verConfirmacion) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        }
                    )
                }
            }
        )

        if (state.errorGeneral != null) {
            Text(
                text = state.errorGeneral,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { viewModel.registrar(onRegistroExitoso) },
            enabled = !state.enviando,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.enviando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("Creando cuenta...")
            } else {
                Text("Crear cuenta")
            }
        }

        TextButton(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}
