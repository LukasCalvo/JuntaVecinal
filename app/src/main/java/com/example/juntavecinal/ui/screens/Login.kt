package com.example.juntavecinal.ui.screens

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.example.juntavecinal.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onIrARegistro: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val state = viewModel.uiState
    var verPassword by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Iniciar sesión",
            style = MaterialTheme.typography.headlineMedium
        )

        CampoFormateado(
            valor = state.rut,
            onValorChange = viewModel::onRutChange,
            formatear = Rut::formatear,
            label = "RUT / Identificación",
            placeholder = "12.345.678-5",
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Text,
            isError = state.rutTocado && state.errores.rut != null,
            supportingText = if (state.rutTocado) state.errores.rut else null,
            leadingIcon = iconoCampo(
                error = if (state.rutTocado) state.errores.rut else null,
                valido = state.rutTocado && state.errores.rut == null && state.rut.isNotBlank()
            )
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = state.passwordTocado && state.errores.password != null,
            supportingText = {
                if (state.passwordTocado && state.errores.password != null) {
                    Text(state.errores.password)
                }
            },
            leadingIcon = iconoCampo(
                error = if (state.passwordTocado) state.errores.password else null,
                valido = state.passwordTocado && state.errores.password == null &&
                        state.password.isNotEmpty()
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

        state.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { viewModel.login(onLoginSuccess) },
            enabled = !state.cargando,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("Verificando...")
            } else {
                Text("Ingresar")
            }
        }

        TextButton(
            onClick = onIrARegistro,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("¿Todavía no tienes cuenta? Regístrate")
        }
    }
}