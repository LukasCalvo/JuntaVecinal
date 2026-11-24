package com.example.juntavecinal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.juntavecinal.ui.screens.FinanzasScreen
import com.example.juntavecinal.ui.screens.HomeScreen
import com.example.juntavecinal.ui.screens.LoginScreen
import com.example.juntavecinal.ui.screens.ProfileScreen
import com.example.juntavecinal.ui.screens.RegisterScreen
import com.example.juntavecinal.ui.screens.ReservaFormScreen
import com.example.juntavecinal.ui.screens.ReservaListScreen
import com.example.juntavecinal.ui.theme.JuntaVecinalTheme
import com.example.juntavecinal.viewmodel.SessionManager
import com.example.juntavecinal.viewmodel.ReservaFormViewModel
import com.example.juntavecinal.viewmodel.ReservaListViewModel

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
    val sesion by SessionManager.sesion.collectAsState()
    val perfil = sesion?.usuario
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var pantallaAuth by rememberSaveable { mutableStateOf(PantallaAuth.LOGIN) }
    var editando by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(perfil?.id) {
        if (perfil == null) {
            currentDestination = AppDestinations.HOME
            pantallaAuth = PantallaAuth.LOGIN
        }
    }

    AnimatedContent(
        targetState = perfil != null,
        transitionSpec = {
            fadeIn(tween(400)) togetherWith fadeOut(tween(250))
        },
        label = "sesion"
    ) { logueado ->
        if (!logueado || perfil == null) {
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
            val reservas: ReservaListViewModel = viewModel(
                key = "reservas-${perfil.id}", factory = ReservaListViewModel.Factory
            )
            val formulario: ReservaFormViewModel = viewModel(
                key = "formulario-${perfil.id}", factory = ReservaFormViewModel.Factory
            )
            val nuevaReserva = {
                editando = false
                formulario.preparar(null)
                currentDestination = AppDestinations.FORM_RESERVA
            }
            val volverAReservas = { currentDestination = AppDestinations.RESERVAS }
            BackHandler(currentDestination == AppDestinations.FORM_RESERVA, volverAReservas)

            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    AppDestinations.entries.filter {
                        it.enBarra && (it != AppDestinations.FINANZAS || perfil.rol.consultaFinanzas)
                    }.forEach {
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
                            AppDestinations.HOME -> HomeScreen(
                                perfil = perfil,
                                reservas = reservas,
                                onNuevaReserva = nuevaReserva,
                                onVerReservas = volverAReservas,
                                modifier = screenModifier
                            )

                            AppDestinations.RESERVAS -> ReservaListScreen(
                                perfil = perfil,
                                viewModel = reservas,
                                onNuevaReserva = nuevaReserva,
                                onEditar = {
                                    editando = true
                                    formulario.preparar(it)
                                    currentDestination = AppDestinations.FORM_RESERVA
                                },
                                modifier = screenModifier
                            )

                            AppDestinations.FORM_RESERVA -> ReservaFormScreen(
                                onReservaCreada = volverAReservas,
                                onVolver = volverAReservas,
                                editando = editando,
                                viewModel = formulario,
                                modifier = screenModifier
                            )

                            AppDestinations.FINANZAS -> FinanzasScreen(perfil, modifier = screenModifier)

                            AppDestinations.PERFIL -> ProfileScreen(
                                perfil = perfil,
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
    FINANZAS("Finanzas", Icons.Filled.AccountBalanceWallet),
    PERFIL("Perfil", Icons.Filled.AccountCircle),
}

private enum class PantallaAuth { LOGIN, REGISTRO }
