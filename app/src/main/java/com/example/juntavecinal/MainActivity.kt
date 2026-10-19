package com.example.juntavecinal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.example.juntavecinal.ui.screens.HomeScreen
import com.example.juntavecinal.ui.screens.ProfileScreen
import com.example.juntavecinal.ui.theme.JuntaVecinalTheme
import com.example.juntavecinal.ui.screens.LoginScreen

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
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    if (!isLoggedIn) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            LoginScreen(
                onLoginSuccess = { isLoggedIn = true },
                modifier = Modifier.padding(innerPadding)
            )
        }
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
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

                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(modifier = screenModifier)
                    AppDestinations.RESERVAS -> {
                        Text(
                            text = "Reservas",
                            modifier = screenModifier.padding(16.dp)
                        )
                    }
                    AppDestinations.PERFIL -> ProfileScreen(modifier = screenModifier)
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Página Principal", Icons.Filled.Home),
    RESERVAS("Reservas", Icons.Filled.CalendarMonth),
    PERFIL("Perfil", Icons.Filled.AccountCircle),
}