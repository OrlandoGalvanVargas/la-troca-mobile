package com.example.latroca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.latroca.data.repository.AuthRepository
import com.example.latroca.ui.screens.*
import com.example.latroca.ui.theme.LaTrocaTheme
import com.example.latroca.ui.viewmodels.AuthViewModel
import com.example.latroca.ui.viewmodels.RegistrationViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LaTrocaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authRepository = remember { AuthRepository() }

                    val authViewModel = remember { AuthViewModel(authRepository) }
                    val registrationViewModel = remember { RegistrationViewModel(authRepository) }

                    NavHost(
                        navController = navController,
                        startDestination = "login"
                    ) {
                        composable("login") {
                            LoginScreen(
                                navController = navController,
                                authViewModel = authViewModel
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                navController = navController,
                                registrationViewModel = registrationViewModel
                            )
                        }

                        composable("completeProfile") {
                            CompleteProfileScreen(
                                navController = navController,
                                registrationViewModel = registrationViewModel
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                onLogout = {
                                    // Resetear estados al hacer logout
                                    authViewModel.resetLoginState()
                                    registrationViewModel.resetState()
                                    navController.navigate("login") {
                                        popUpTo(0)
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