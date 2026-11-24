package com.example.juntavecinal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.juntavecinal.data.remote.CuotaPendiente
import com.example.juntavecinal.data.remote.Perfil
import com.example.juntavecinal.data.remote.TipoMovimiento
import com.example.juntavecinal.util.Fechas
import com.example.juntavecinal.viewmodel.FinanzasViewModel
import com.example.juntavecinal.viewmodel.FormularioMovimiento
import com.example.juntavecinal.viewmodel.FormularioPago
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.format.TextStyle
import java.util.Locale

private val CHILE = Locale.forLanguageTag("es-CL")

@Composable
fun FinanzasScreen(perfil: Perfil, modifier: Modifier = Modifier) {
    if (!perfil.rol.consultaFinanzas) return
    val viewModel: FinanzasViewModel = viewModel(key = "finanzas-${perfil.id}")
    val estado by viewModel.estado.collectAsState()
    val datos = estado.datos
    var formulario by remember { mutableStateOf<FormularioMovimiento?>(null) }
    var pago by remember { mutableStateOf<FormularioPago?>(null) }
    LaunchedEffect(perfil.id) { viewModel.actualizar() }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Finanzas", style = MaterialTheme.typography.headlineMedium)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.cambiarMes(-1) }, enabled = !estado.cargando) { Text("Anterior") }
                Text("${estado.mes.month.getDisplayName(TextStyle.FULL, CHILE)} ${estado.mes.year}")
                TextButton(onClick = { viewModel.cambiarMes(1) }, enabled = !estado.cargando) { Text("Siguiente") }
            }
            TextButton(onClick = viewModel::actualizar, enabled = !estado.cargando) { Text("Actualizar") }
            if (estado.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (formulario == null && pago == null) ErrorFinanzas(estado.error)
        }
        if (datos != null) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Saldo ${pesos(datos.resumen.saldo)}", style = MaterialTheme.typography.titleLarge)
                        Text("Ingresos ${pesos(datos.resumen.ingresos)}")
                        Text("Egresos ${pesos(datos.resumen.egresos)}")
                    }
                }
            }
            if (perfil.rol.gestionaFinanzas) {
                item {
                    OutlinedButton(
                        onClick = {
                            viewModel.limpiarError()
                            formulario = FormularioMovimiento()
                        },
                        enabled = !estado.cargando,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Nuevo movimiento") }

                    if (datos.cuotasPendientes.isEmpty()) {
                        Text("No hay cuotas asignadas pendientes de pago.")
                    } else {
                        OutlinedButton(
                            onClick = {
                                viewModel.limpiarError()
                                pago = FormularioPago()
                            },
                            enabled = !estado.cargando,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Registrar pago de cuota") }
                    }
                }
            }
            if (datos.movimientos.isEmpty()) item { Text("Sin movimientos en este período.") }
            items(datos.movimientos, key = { it.id }) { movimiento ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(movimiento.descripcion, style = MaterialTheme.typography.titleMedium)
                        Text("${movimiento.tipo.etiqueta} · ${pesos(movimiento.monto)} · ${movimiento.categoria}")
                        Text(Fechas.formatearFecha(movimiento.fecha))
                    }
                }
            }
        }
    }

    formulario?.let { form ->
        FormularioFinanzas(
            "Nuevo movimiento", estado.cargando, estado.error, { formulario = null },
            { viewModel.guardarMovimiento(form) { formulario = null } }
        ) {
            OpcionesFinanzas(
                form.tipo.etiqueta, TipoMovimiento.entries, TipoMovimiento::etiqueta, !estado.cargando
            ) { formulario = form.copy(tipo = it) }
            CampoFinanzas("Categoría", form.categoria, { formulario = form.copy(categoria = it) })
            CampoFinanzas("Descripción", form.descripcion, { formulario = form.copy(descripcion = it) })
            CampoFinanzas("Monto", form.monto, { formulario = form.copy(monto = it) }, KeyboardType.Decimal)
            CampoFinanzas("Fecha (AAAA-MM-DD)", form.fecha, { formulario = form.copy(fecha = it) })
        }
    }
    pago?.let { form ->
        val cuotas = datos?.cuotasPendientes.orEmpty()
        val cuota = cuotas.find { it.id == form.cuotaId }
        FormularioFinanzas(
            "Registrar pago de cuota", estado.cargando, estado.error, { pago = null },
            { viewModel.registrarPago(form) { pago = null } }
        ) {
            OpcionesFinanzas(cuota?.cuotaNombre ?: "Seleccionar cuota", cuotas, ::etiquetaCuota, !estado.cargando) {
                pago = form.copy(cuotaId = it.id, monto = it.saldoPendiente.toPlainString())
            }
            cuota?.let { Text("Vecino Nº ${it.vecinoId} · Pendiente ${pesos(it.saldoPendiente)}") }
            CampoFinanzas("Monto", form.monto, { pago = form.copy(monto = it) }, KeyboardType.Decimal)
            CampoFinanzas("Medio de pago", form.medio, { pago = form.copy(medio = it) })
        }
    }
}

private fun etiquetaCuota(cuota: CuotaPendiente): String = "${cuota.cuotaNombre} · Vecino Nº ${cuota.vecinoId}"

@Composable
private fun <T> OpcionesFinanzas(
    titulo: String, opciones: List<T>, etiqueta: (T) -> String, habilitado: Boolean, seleccionar: (T) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { abierto = true }, enabled = habilitado, modifier = Modifier.fillMaxWidth()) {
            Text(titulo)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(text = { Text(etiqueta(opcion)) }, onClick = {
                    abierto = false
                    seleccionar(opcion)
                })
            }
        }
    }
}

@Composable
private fun FormularioFinanzas(
    titulo: String,
    ocupado: Boolean,
    error: String?,
    onCerrar: () -> Unit,
    onGuardar: () -> Unit,
    campos: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!ocupado) onCerrar() },
        title = { Text(titulo) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                campos()
                ErrorFinanzas(error)
            }
        },
        confirmButton = {
            Button(onClick = onGuardar, enabled = !ocupado) { Text(if (ocupado) "Guardando…" else "Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar, enabled = !ocupado) { Text("Volver") } }
    )
}

@Composable
private fun CampoFinanzas(etiqueta: String, valor: String, cambiar: (String) -> Unit, tipo: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = valor, onValueChange = cambiar, label = { Text(etiqueta) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = tipo), modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ErrorFinanzas(error: String?) {
    if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
}

private fun pesos(monto: BigDecimal): String = NumberFormat.getCurrencyInstance(CHILE).apply {
    maximumFractionDigits = 2
}.format(monto)
