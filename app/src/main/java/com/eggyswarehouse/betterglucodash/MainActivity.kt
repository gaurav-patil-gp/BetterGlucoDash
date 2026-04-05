package com.eggyswarehouse.betterglucodash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eggyswarehouse.betterglucodash.ui.auth.LoginScreen
import com.eggyswarehouse.betterglucodash.ui.dashboard.DashboardScreen
import com.eggyswarehouse.betterglucodash.ui.navigation.Screen
import com.eggyswarehouse.betterglucodash.ui.theme.BetterGlucoDashTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authManager = (applicationContext as GlucoDashApplication).container.authManager

        enableEdgeToEdge()
        setContent {
            BetterGlucoDashTheme {
                // Determine the start destination asynchronously by checking for a
                // persisted JWT. Null means we're still resolving — nothing is rendered yet.
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val token = authManager.getToken()
                    startDestination = if (token.isNullOrEmpty()) Screen.Login.route else Screen.Dashboard.route
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (startDestination != null) {
                        val navController = rememberNavController()

                        NavHost(
                            navController = navController,
                            startDestination = startDestination!!
                        ) {
                            composable(Screen.Login.route) {
                                LoginScreen(
                                    onLoginSuccess = {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(Screen.Dashboard.route) {
                                DashboardScreen(
                                    onLogout = {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}