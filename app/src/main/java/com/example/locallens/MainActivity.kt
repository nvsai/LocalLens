package com.example.locallens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.locallens.ui.navigation.Screen
import com.example.locallens.ui.screens.AuthScreen
import com.example.locallens.ui.screens.DetailScreen
import com.example.locallens.ui.screens.ItineraryScreen
import com.example.locallens.ui.screens.MapScreen
import com.example.locallens.ui.screens.PreferencesScreen
import com.example.locallens.ui.theme.LocalLensTheme
import com.example.locallens.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalLensTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocalLensAppContent()
                }
            }
        }
    }
}

@Composable
fun LocalLensAppContent() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    val startDestination = if (authViewModel.currentUser != null) Screen.Home.route else Screen.Auth.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Auth.route) {
            AuthScreen(navController = navController)
        }
        composable(Screen.Preferences.route) {
            PreferencesScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            ItineraryScreen(navController = navController)
        }
        composable(Screen.Map.route + "/{latitude}/{longitude}") { backStackEntry ->
            val latitude = backStackEntry.arguments?.getString("latitude")?.toDoubleOrNull() ?: 0.0
            val longitude = backStackEntry.arguments?.getString("longitude")?.toDoubleOrNull() ?: 0.0
            MapScreen(navController = navController, initialLat = latitude, initialLng = longitude)
        }
    }
}