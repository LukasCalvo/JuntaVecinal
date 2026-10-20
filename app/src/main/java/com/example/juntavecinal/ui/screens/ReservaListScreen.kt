package com.example.juntavecinal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.juntavecinal.model.entity.Reserva
import com.example.juntavecinal.viewmodel.ReservaListViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

@Composable
fun ReservaListScreen(
    onNuevaReserva: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReservaListViewModel = viewModel()
) {
    val reservas by viewModel.reservas.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        if (reservas.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Aún no tienes reservas",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Toca + para crear la primera",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Mis reservas",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(reservas, key = { it.id }) { reserva ->
                    TarjetaReserva(
                        reserva = reserva,
                        onEliminar = { viewModel.eliminar(reserva) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onNuevaReserva,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Nueva reserva")
        }
    }
}

@Composable
private fun TarjetaReserva(
    reserva: Reserva,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formato = SimpleDateFormat("dd/MM/yyyy", LocalLocale.current.platformLocale)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reserva.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Confirmada",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "${formato.format(Date(reserva.fecha))}  •  " +
                        "${horaTexto(reserva.horaInicioMin)} - ${horaTexto(reserva.horaFinMin)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${reserva.asistentes} asistentes",
                style = MaterialTheme.typography.bodySmall
            )
            if (reserva.requiereProyector) {
                Text(
                    text = "Requiere proyector",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (reserva.observaciones.isNotBlank()) {
                Text(
                    text = reserva.observaciones,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEliminar) { Text("Eliminar") }
            }
        }
    }
}

private fun horaTexto(minutos: Int): String =
    "%02d:%02d".format(minutos / 60, minutos % 60)