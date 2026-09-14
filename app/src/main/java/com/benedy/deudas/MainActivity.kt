package com.benedy.deudas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.benedy.deudas.ui.auth.AuthViewModel
import com.benedy.deudas.ui.auth.AuthViewModelFactory
import com.benedy.deudas.ui.navigation.DeudasNavGraph
import com.benedy.deudas.ui.theme.DeudasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DeudasApp

        setContent {
            DeudasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModelFactory(app.authRepository)
                    )
                    DeudasNavGraph(authViewModel = authViewModel)
                }
            }
        }
    }
}
