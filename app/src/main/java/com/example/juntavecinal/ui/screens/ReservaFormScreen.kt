package com.example.juntavecinal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.juntavecinal.viewmodel.ReservaFormViewModel

/** Cuál de los dos selectores de hora está abierto. */
private enum class CampoHora { INICIO, FIN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservaFormScreen(
    onReservaCreada: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReservaFormViewModel = viewModel()
) {
    val formulario by viewModel.formulario.collectAsState()
    val errores by viewModel.errores.collectAsState()
    val guardando by viewModel.guardando.collectAsState()
    val guardado by viewModel.guardado.collectAsState()
    val mensajeGeneral by viewModel.mensajeGeneral.collectAsState()

    var mostrarDatePicker by remember { mutableStateOf(false) }
    var horaEnEdicion by remember { mutableStateOf<CampoHora?>(null) }

    LaunchedEffect(guardado) {
        if (guardado) {
            onReservaCreada()
            viewModel.consumirGuardado()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Nueva reserva", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))

        // ----- Nombre (String) -----
        OutlinedTextField(
            value = formulario.nombre,
            onValueChange = viewModel::actualizarNombre,
            label = { Text("Nombre del solicitante") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errores.nombre != null,
            supportingText = { errores.nombre?.let { Text(it) } },
            leadingIcon = iconoCampo(error = errores.nombre, valido = formulario.nombre.isNotBlank())
        )

        // ----- Fecha (Date): se escribe o se elige del calendario -----
        OutlinedTextField(
            value = formulario.fechaTexto,
            onValueChange = viewModel::actualizarFechaTexto,
            label = { Text("Fecha (DD/MM/AAAA)") },
            placeholder = { Text("25/12/2026") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = errores.fecha != null,
            supportingText = { errores.fecha?.let { Text(it) } },
            leadingIcon = iconoCampo(
                error = errores.fecha,
                valido = formulario.fechaTexto.isNotBlank()
            ),
            trailingIcon = {
                IconButton(onClick = { mostrarDatePicker = true }) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Elegir del calendario")
                }
            }
        )

        // ----- Horas (HH:MM): se escriben o se eligen del reloj -----
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = formulario.horaInicioTexto,
                onValueChange = viewModel::actualizarHoraInicioTexto,
                label = { Text("Hora inicio") },
                placeholder = { Text("HH:MM") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = errores.horaInicio != null,
                supportingText = { errores.horaInicio?.let { Text(it) } },
                trailingIcon = {
                    IconButton(onClick = { horaEnEdicion = CampoHora.INICIO }) {
                        Icon(Icons.Filled.Schedule, contentDescription = "Elegir hora de inicio")
                    }
                }
            )
            OutlinedTextField(
                value = formulario.horaFinTexto,
                onValueChange = viewModel::actualizarHoraFinTexto,
                label = { Text("Hora término") },
                placeholder = { Text("HH:MM") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = errores.horaFin != null,
                supportingText = { errores.horaFin?.let { Text(it) } },
                trailingIcon = {
                    IconButton(onClick = { horaEnEdicion = CampoHora.FIN }) {
                        Icon(Icons.Filled.Schedule, contentDescription = "Elegir hora de término")
                    }
                }
            )
        }

        // ----- Asistentes (Int) -----
        OutlinedTextField(
            value = formulario.asistentes,
            onValueChange = viewModel::actualizarAsistentes,
            label = { Text("Cantidad de asistentes") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = errores.asistentes != null,
            supportingText = {
                Text(
                    errores.asistentes
                        ?: "Entre 1 y ${ReservaFormViewModel.MAX_ASISTENTES} personas"
                )
            },
            leadingIcon = iconoCampo(error = errores.asistentes, valido = false)
        )

        // ----- Observaciones (String) -----
        OutlinedTextField(
            value = formulario.observaciones,
            onValueChange = viewModel::actualizarObservaciones,
            label = { Text("Observaciones (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            isError = errores.observaciones != null,
            supportingText = { errores.observaciones?.let { Text(it) } }
        )

        // ----- Switch: montaje -----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Requiere un proyector")
            Switch(
                checked = formulario.requiereProyector,
                onCheckedChange = viewModel::actualizarRequiereProyector
            )
        }

        // ----- Checkbox: reglamento -----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = formulario.aceptaReglamento,
                onCheckedChange = viewModel::actualizarAceptaReglamento
            )
            Text("Acepto el reglamento de uso")
        }
        AnimatedVisibility(
            visible = errores.aceptaReglamento != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = errores.aceptaReglamento.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        // ----- Mensaje general (choque de horarios) -----
        AnimatedVisibility(
            visible = mensajeGeneral != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = mensajeGeneral.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // ----- Botón con estado de carga -----
        Button(
            onClick = { viewModel.guardar() },
            enabled = !guardando,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (guardando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("Guardando...")
            } else {
                Text("Guardar reserva")
            }
        }
    }

    // ----- Calendario -----
    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.textoAFecha(formulario.fechaTexto)
        )
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.actualizarFechaTexto(
                        viewModel.fechaATexto(datePickerState.selectedDateMillis)
                    )
                    mostrarDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ----- Reloj (hora inicio / término) -----
    horaEnEdicion?.let { campo ->
        val textoActual =
            if (campo == CampoHora.INICIO) formulario.horaInicioTexto
            else formulario.horaFinTexto
        val minutosActuales = viewModel.textoAMinutos(textoActual) ?: (8 * 60)
        // rememberTimePickerState(initialHour, initialMinute, is24Hour)
        val timePickerState = rememberTimePickerState(
            minutosActuales / 60,
            minutosActuales % 60,
            true
        )
        AlertDialog(
            onDismissRequest = { horaEnEdicion = null },
            title = { Text(if (campo == CampoHora.INICIO) "Hora de inicio" else "Hora de término") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val texto = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                    if (campo == CampoHora.INICIO) viewModel.actualizarHoraInicioTexto(texto)
                    else viewModel.actualizarHoraFinTexto(texto)
                    horaEnEdicion = null
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { horaEnEdicion = null }) { Text("Cancelar") }
            }
        )
    }
}

/** Ícono de estado del campo: error o válido (requisito de íconos). */
@Composable
private fun iconoCampo(
    error: String?,
    valido: Boolean
): (@Composable () -> Unit)? = when {
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