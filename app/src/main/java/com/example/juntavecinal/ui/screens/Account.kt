package com.example.juntavecinal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.juntavecinal.data.remote.Perfil


@Composable
fun ProfileScreen(perfil: Perfil, onCerrarSesion: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Mi perfil", style = MaterialTheme.typography.headlineMedium)
        Text(perfil.nombre, style = MaterialTheme.typography.titleMedium)
        Text(perfil.rol.name.lowercase().replaceFirstChar { it.uppercase() })
        Button(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }
    }
}
