package com.benedy.deudas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.benedy.deudas.ui.auth.AuthViewModel
import com.benedy.deudas.ui.auth.LoginScreen
import com.benedy.deudas.ui.home.HomeScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
}

/**
 * Navegación con auth gate: si hay sesión → Home; si no → Login.
 */
@Composable
fun DeudasNavGraph(authViewModel: AuthViewModel) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current

    val startDestination = if (uiState.isSignedIn) Routes.HOME else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                uiState = uiState,
                onGoogleSignIn = { authViewModel.signInWithGoogle(context) },
                onClearError = { authViewModel.clearError() }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                uiState = uiState,
                onLogout = { authViewModel.signOut() }
            )
        }
    }

    LaunchedEffect(uiState.isSignedIn) {
        val target = if (uiState.isSignedIn) Routes.HOME else Routes.LOGIN
        val current = navController.currentDestination?.route
        if (current != target) {
            navController.navigate(target) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
