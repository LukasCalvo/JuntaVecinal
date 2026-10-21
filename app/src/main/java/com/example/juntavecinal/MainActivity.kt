package com.example.juntavecinal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.juntavecinal.ui.screens.HomeScreen
import com.example.juntavecinal.ui.screens.LoginScreen
import com.example.juntavecinal.ui.screens.ProfileScreen
import com.example.juntavecinal.ui.screens.RegisterScreen
import com.example.juntavecinal.ui.screens.ReservaFormScreen
import com.example.juntavecinal.ui.screens.ReservaListScreen
import com.example.juntavecinal.ui.theme.JuntaVecinalTheme
import com.example.juntavecinal.viewmodel.SessionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JuntaVecinalTheme {
                JuntaVecinalApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun JuntaVecinalApp() {
    val usuarioId by SessionManager.usuarioId.collectAsState()
    val isLoggedIn = usuarioId != null
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var pantallaAuth by rememberSaveable { mutableStateOf(PantallaAuth.LOGIN) }

    AnimatedContent(
        targetState = isLoggedIn,
        transitionSpec = {
            fadeIn(tween(400)) togetherWith fadeOut(tween(250))
        },
        label = "sesion"
    ) { logueado ->
        if (!logueado) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                AnimatedContent(
                    targetState = pantallaAuth,
                    transitionSpec = {
                        val adelante = targetState.ordinal > initialState.ordinal
                        if (adelante) {
                            slideInHorizontally(tween(300)) { it } togetherWith
                                    slideOutHorizontally(tween(300)) { -it }
                        } else {
                            slideInHorizontally(tween(300)) { -it } togetherWith
                                    slideOutHorizontally(tween(300)) { it }
                        }
                    },
                    label = "auth"
                ) { pantalla ->
                    when (pantalla) {
                        PantallaAuth.LOGIN -> LoginScreen(
                            onLoginSuccess = { },
                            onIrARegistro = { pantallaAuth = PantallaAuth.REGISTRO },
                            modifier = Modifier.padding(innerPadding)
                        )

                        PantallaAuth.REGISTRO -> RegisterScreen(
                            onRegistroExitoso = { },
                            onVolver = { pantallaAuth = PantallaAuth.LOGIN },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        } else {
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    AppDestinations.entries.filter { it.enBarra }.forEach {
                        item(
                            icon = {
                                Icon(
                                    imageVector = it.icon,
                                    contentDescription = it.label
                                )
                            },
                            label = { Text(it.label) },
                            selected = it == currentDestination,
                            onClick = { currentDestination = it }
                        )
                    }
                }
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val screenModifier = Modifier.padding(innerPadding)

                    AnimatedContent(
                        targetState = currentDestination,
                        transitionSpec = {
                            val adelante = targetState.ordinal > initialState.ordinal
                            if (adelante) {
                                slideInHorizontally(tween(300)) { it } togetherWith
                                        slideOutHorizontally(tween(300)) { -it }
                            } else {
                                slideInHorizontally(tween(300)) { -it } togetherWith
                                        slideOutHorizontally(tween(300)) { it }
                            }
                        },
                        label = "navegacion"
                    ) { destino ->
                        when (destino) {
                            AppDestinations.HOME -> HomeScreen(modifier = screenModifier)
                            AppDestinations.RESERVAS -> ReservaListScreen(
                                onNuevaReserva = { currentDestination = AppDestinations.FORM_RESERVA },
                                modifier = screenModifier
                            )

                            AppDestinations.FORM_RESERVA -> ReservaFormScreen(
                                onReservaCreada = { currentDestination = AppDestinations.RESERVAS },
                                modifier = screenModifier
                            )

                            AppDestinations.PERFIL -> ProfileScreen(
                                onCerrarSesion = {
                                    SessionManager.cerrarSesion()
                                    currentDestination = AppDestinations.HOME
                                    pantallaAuth = PantallaAuth.LOGIN
                                },
                                modifier = screenModifier
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val enBarra: Boolean = true,
) {
    HOME("Página Principal", Icons.Filled.Home),
    RESERVAS("Reservas", Icons.Filled.CalendarMonth),
    FORM_RESERVA("Nueva reserva", Icons.Filled.Add, enBarra = false),
    PERFIL("Perfil", Icons.Filled.AccountCircle),
}

private enum class PantallaAuth { LOGIN, REGISTRO }