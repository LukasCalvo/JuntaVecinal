package com.example.juntavecinal.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun CampoFormateado(
    valor: String,
    onValorChange: (String) -> Unit,
    formatear: (String) -> String,
    label: String,
    placeholder: String,
    isError: Boolean,
    supportingText: String?,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Number,
    prefix: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    var campo by remember { mutableStateOf(TextFieldValue(valor)) }

    LaunchedEffect(valor) {
        if (campo.text != valor) {
            campo = TextFieldValue(valor, TextRange(valor.length))
        }
    }

    OutlinedTextField(
        value = campo,
        onValueChange = { nuevo ->
            val texto = formatear(nuevo.text)
            campo = TextFieldValue(texto, TextRange(texto.length))
            onValorChange(texto)
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        prefix = prefix?.let { texto -> { Text(texto) } },
        isError = isError,
        supportingText = supportingText?.let { mensaje ->
            { Text(mensaje) }
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}

@Composable
fun iconoCampo(
    error: String?,
    valido: Boolean
): (@Composable () -> Unit)? {
    return when {
        error != null -> {
            {
                Icon(
                    imageVector = Icons.Outlined.Error,
                    contentDescription = "Campo con error",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        valido -> {
            {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Campo válido",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        else -> null
    }
}