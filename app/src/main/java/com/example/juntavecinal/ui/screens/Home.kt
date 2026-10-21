package com.example.juntavecinal.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.juntavecinal.model.entity.Reserva
import com.example.juntavecinal.util.Fechas
import com.example.juntavecinal.viewmodel.HomeViewModel
import com.example.juntavecinal.viewmodel.SessionManager

@Composable
fun HomeScreen(
    onNuevaReserva: () -> Unit,
    onVerReservas: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val nombre by SessionManager.nombre.collectAsState()
    val proximas by viewModel.proximas.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Hola, ${nombre.orEmpty()}",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Junta de Vecinos Población Zaror",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            ProximaReservaCard(
                reserva = proximas.firstOrNull(),
                onNuevaReserva = onNuevaReserva,
                onVerReservas = onVerReservas
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Reservas pendientes", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = proximas.size.toString(),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ProximaReservaCard(
    reserva: Reserva?,
    onNuevaReserva: () -> Unit,
    onVerReservas: () -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .animateContentSize()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Tu próxima reserva", style = MaterialTheme.typography.titleMedium)

            if (reserva == null) {
                Text("No tienes reservas próximas.")
                Button(onClick = onNuevaReserva) { Text("Reservar ahora") }
            } else {
                Text(
                    text = reserva.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text("${etiquetaDia(reserva.fecha)}  •  ${Fechas.formatearFecha(reserva.fecha)}")
                Text(
                    "${Fechas.formatearHora(reserva.horaInicioMin)} - " +
                            "${Fechas.formatearHora(reserva.horaFinMin)}  •  ${reserva.asistentes} asistentes"
                )
                if (reserva.requiereProyector) Text("Requiere proyector")
                TextButton(onClick = onVerReservas) { Text("Ver todas mis reservas") }
            }
        }
    }
}

private fun etiquetaDia(fecha: Long): String {
    val dias = ((fecha - Fechas.inicioDeHoy()) / Fechas.DIA_MS).toInt()
    return when (dias) {
        0 -> "Hoy"
        1 -> "Mañana"
        else -> "En $dias días"
    }
}