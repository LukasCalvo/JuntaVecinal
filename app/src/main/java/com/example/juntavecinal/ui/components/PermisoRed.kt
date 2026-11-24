package com.example.juntavecinal.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberAccesoBackend(accion: () -> Unit, denegado: (String) -> Unit): () -> Unit {
    val contexto = LocalContext.current
    val alContinuar by rememberUpdatedState(accion)
    val alDenegar by rememberUpdatedState(denegado)
    val permiso = Manifest.permission.ACCESS_LOCAL_NETWORK
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) alContinuar()
        else alDenegar("Permite el acceso a la red local para conectarte al servidor.")
    }
    return {
        if (Build.VERSION.SDK_INT >= 37 &&
            ContextCompat.checkSelfPermission(contexto, permiso) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(permiso)
        } else {
            alContinuar()
        }
    }
}
