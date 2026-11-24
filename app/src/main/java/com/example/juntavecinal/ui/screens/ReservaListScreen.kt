package com.example.juntavecinal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.juntavecinal.data.remote.Perfil
import com.example.juntavecinal.model.entity.Reserva
import com.example.juntavecinal.util.Fechas
import com.example.juntavecinal.viewmodel.ReservaListViewModel

@Composable
fun ReservaListScreen(
    perfil: Perfil,
    viewModel: ReservaListViewModel,
    onNuevaReserva: () -> Unit,
    onEditar: (Reserva) -> Unit,
    modifier: Modifier = Modifier
) {
    val reservas by viewModel.reservas.collectAsState()
    val estado by viewModel.estado.collectAsState()
    var porCancelar by remember { mutableStateOf<Reserva?>(null) }

    LaunchedEffect(perfil.id) { viewModel.actualizar() }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Mis reservas",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    TextButton(onClick = viewModel::actualizar, enabled = !estado.cargando) { Text("Actualizar") }
                }
                if (estado.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
                estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (reservas.isEmpty() && !estado.cargando && estado.error == null) {
                    Text("Aún no hay reservas.")
                }
            }
            items(reservas, key = { it.id }) { reserva ->
                TarjetaReserva(
                    reserva, perfil, !estado.cargando,
                    onEditar = { onEditar(reserva) },
                    onCancelar = { porCancelar = reserva },
                    modifier = Modifier.animateItem()
                )
            }
        }
        FloatingActionButton(
            onClick = onNuevaReserva,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Nueva reserva")
        }
    }

    porCancelar?.let { reserva ->
        AlertDialog(
            onDismissRequest = { porCancelar = null },
            title = { Text("Cancelar reserva") },
            text = { Text("¿Quieres cancelar la reserva de ${reserva.nombre}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelar(reserva)
                    porCancelar = null
                }) { Text("Cancelar reserva") }
            },
            dismissButton = { TextButton(onClick = { porCancelar = null }) { Text("Volver") } }
        )
    }
}

@Composable
private fun TarjetaReserva(
    reserva: Reserva,
    perfil: Perfil,
    disponible: Boolean,
    onEditar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(reserva.nombre, style = MaterialTheme.typography.titleMedium)
            Text("Reserva Nº ${reserva.id} · ${reserva.estadoVisible}", style = MaterialTheme.typography.labelLarge)
            Text(
                "${Fechas.formatearFecha(reserva.fecha)} · ${Fechas.formatearHora(reserva.horaInicio)}–${Fechas.formatearHora(reserva.horaFin)}"
            )
            Text("${reserva.asistentes} asistentes")
            if (reserva.requiereProyector) Text("Requiere proyector")
            reserva.observaciones?.takeIf { it.isNotBlank() }?.let { Text(it) }
            if (reserva.modificablePor(perfil)) {
                Row {
                    TextButton(onClick = onEditar, enabled = disponible) { Text("Editar") }
                    TextButton(onClick = onCancelar, enabled = disponible) { Text("Cancelar") }
                }
            }
        }
    }
}
