package com.example.juntavecinal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
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
import com.example.juntavecinal.ui.components.CampoFormateado
import com.example.juntavecinal.ui.components.SelectableDiasDesde
import com.example.juntavecinal.ui.components.iconoCampo
import com.example.juntavecinal.viewmodel.ReservaFormViewModel
import kotlinx.coroutines.delay

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
    val tocados by viewModel.tocados.collectAsState()
    val guardando by viewModel.guardando.collectAsState()
    val guardado by viewModel.guardado.collectAsState()
    val mostrarExito by viewModel.mostrarExito.collectAsState()
    val mensajeGeneral by viewModel.mensajeGeneral.collectAsState()

    var mostrarDatePicker by remember { mutableStateOf(false) }
    var horaEnEdicion by remember { mutableStateOf<CampoHora?>(null) }

    LaunchedEffect(guardado) {
        if (!guardado) return@LaunchedEffect
        viewModel.mostrarAvisoExito()
        try {
            delay(1400)
            viewModel.ocultarAvisoExito()
            onReservaCreada()
        } finally {
            viewModel.ocultarAvisoExito()
            viewModel.consumirGuardado()
        }

    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = !mostrarExito,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Column(
                modifier = Modifier
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
                    leadingIcon = iconoCampo(
                        error = errores.nombre,
                        valido = tocados.nombre && errores.nombre == null && formulario.nombre.isNotBlank()
                    )
                )

                // ----- Fecha (Date): se escribe o se elige del calendario -----
                CampoFormateado(
                    valor = formulario.fechaTexto,
                    onValorChange = viewModel::actualizarFechaTexto,
                    formatear = viewModel::formatearFechaEntrada,
                    label = "Fecha (DD/MM/AAAA)",
                    placeholder = "25/12/2026",
                    modifier = Modifier.fillMaxWidth(),
                    isError = errores.fecha != null,
                    supportingText = errores.fecha,
                    leadingIcon = iconoCampo(
                        error = errores.fecha,
                        valido = tocados.fecha && errores.fecha == null &&
                                viewModel.textoAFecha(formulario.fechaTexto) != null
                    ),
                    trailingIcon = {
                        IconButton(onClick = { mostrarDatePicker = true }) {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = "Elegir del calendario"
                            )
                        }
                    }
                )

                // ----- Horas (HH:MM): se escriben o se eligen del reloj -----
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoFormateado(
                        valor = formulario.horaInicioTexto,
                        onValorChange = viewModel::actualizarHoraInicioTexto,
                        formatear = viewModel::formatearHoraEntrada,
                        label = "Hora inicio",
                        placeholder = "HH:MM",
                        modifier = Modifier.weight(1f),
                        isError = errores.horaInicio != null,
                        supportingText = errores.horaInicio,
                        leadingIcon = iconoCampo(
                            error = errores.horaInicio,
                            valido = tocados.horaInicio && errores.horaInicio == null &&
                                    viewModel.textoAMinutos(formulario.horaInicioTexto) != null
                        ),
                        trailingIcon = {
                            IconButton(onClick = { horaEnEdicion = CampoHora.INICIO }) {
                                Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = "Elegir hora de inicio"
                                )
                            }
                        }
                    )
                    CampoFormateado(
                        valor = formulario.horaFinTexto,
                        onValorChange = viewModel::actualizarHoraFinTexto,
                        formatear = viewModel::formatearHoraEntrada,
                        label = "Hora término",
                        placeholder = "HH:MM",
                        modifier = Modifier.weight(1f),
                        isError = errores.horaFin != null,
                        supportingText = errores.horaFin,
                        leadingIcon = iconoCampo(
                            error = errores.horaFin,
                            valido = tocados.horaFin && errores.horaFin == null &&
                                    viewModel.textoAMinutos(formulario.horaFinTexto) != null
                        ),
                        trailingIcon = {
                            IconButton(onClick = { horaEnEdicion = CampoHora.FIN }) {
                                Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = "Elegir hora de término"
                                )
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
                    leadingIcon = iconoCampo(
                        error = errores.asistentes,
                        valido = tocados.asistentes && errores.asistentes == null &&
                                formulario.asistentes.isNotBlank()
                    )
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

                // ----- Switch: proyector -----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Requiere proyector")
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
        }

        // ----- Confirmación animada, centrada en la pantalla -----
        AnimatedVisibility(
            visible = mostrarExito,
            enter = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.8f),
            exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    text = "¡Reserva creada con éxito!",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }

    // ----- Calendario -----
    if (mostrarDatePicker) {
        val hoy = viewModel.inicioDeHoy()
        if (formulario.fechaTexto.isBlank()) {
            viewModel.actualizarFechaTexto(viewModel.fechaATexto(hoy))
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.textoAFecha(formulario.fechaTexto) ?: hoy,
            selectableDates = SelectableDiasDesde(hoy)
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