package com.eggyswarehouse.betterglucodash.ui.navigation

/**
 * Type-safe navigation route definitions.
 *
 * Use [Screen.route] as the destination string in [NavHost] and [navigate] calls.
 * This replaces magic string literals ("login", "dashboard") throughout the app.
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")

    data object Dashboard : Screen("dashboard")
}
