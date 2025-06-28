package com.example.locallens.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth_screen")
    object Preferences : Screen("preferences_screen")
    object Home : Screen("home_screen")
    object Map : Screen("map_screen")
    object Detail : Screen("detail_screen")
}