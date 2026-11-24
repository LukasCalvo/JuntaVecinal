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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.juntavecinal.data.remote.Perfil
import com.example.juntavecinal.model.entity.Reserva
import com.example.juntavecinal.util.Fechas
import com.example.juntavecinal.viewmodel.HomeViewModel
import com.example.juntavecinal.viewmodel.ReservaListViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(
    perfil: Perfil,
    reservas: ReservaListViewModel,
    onNuevaReserva: () -> Unit,
    onVerReservas: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val lista by reservas.reservas.collectAsState()
    val estadoReservas by reservas.estado.collectAsState()
    val feriados by viewModel.feriados.collectAsState()
    val proximas = viewModel.proximas(lista, perfil.id)

    LaunchedEffect(perfil.id) {
        reservas.actualizar()
        viewModel.consultarFeriados()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Hola, ${perfil.nombre}",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Junta de Vecinos Población Zaror",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        estadoReservas.error?.let { error ->
            item {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = reservas::actualizar) { Text("Actualizar reservas") }
            }
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
                        text = proximas.count { it.estado == "PENDIENTE" }.toString(),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Próximo feriado nacional", style = MaterialTheme.typography.titleMedium)
                    val fecha = feriados.fecha
                    Text(
                        when {
                            feriados.cargando -> "Consultando…"
                            fecha != null -> "${Fechas.formatearFecha(fecha)} · ${feriados.nombre}"
                            else -> "Calendario de feriados no disponible"
                        }
                    )
                    Text("Datos de Roison", style = MaterialTheme.typography.bodySmall)
                    if (!feriados.cargando && fecha == null) {
                        TextButton(onClick = viewModel::consultarFeriados) { Text("Reintentar") }
                    }
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
                    "${Fechas.formatearHora(reserva.horaInicio)} - " +
                            "${Fechas.formatearHora(reserva.horaFin)}  •  ${reserva.asistentes} asistentes"
                )
                Text(reserva.estadoVisible)
                if (reserva.requiereProyector) Text("Requiere proyector")
                TextButton(onClick = onVerReservas) { Text("Ver todas mis reservas") }
            }
        }
    }
}

private fun etiquetaDia(fecha: String): String {
    val dias = ChronoUnit.DAYS.between(Fechas.hoy(), LocalDate.parse(fecha)).toInt()
    return when (dias) {
        0 -> "Hoy"
        1 -> "Mañana"
        else -> "En $dias días"
    }
}
