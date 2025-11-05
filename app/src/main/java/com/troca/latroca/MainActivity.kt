package com.troca.latroca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.troca.latroca.data.repository.AuthRepository
import com.troca.latroca.data.repository.PostRepository
import com.troca.latroca.ui.screens.*
import com.troca.latroca.ui.theme.LaTrocaTheme
import com.troca.latroca.ui.viewmodels.AuthViewModel
import com.troca.latroca.ui.viewmodels.PostViewModel
import com.troca.latroca.ui.viewmodels.RegistrationViewModel

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

                    // Repositorios y ViewModels
                    val authRepository = remember { AuthRepository() }
                    val postRepository = remember { PostRepository() }

                    val authViewModel = remember { AuthViewModel(authRepository) }
                    // 👇 CAMBIO: Pasar authViewModel al RegistrationViewModel
                    val registrationViewModel = remember {
                        RegistrationViewModel(authRepository, authViewModel)
                    }
                    val postViewModel = remember { PostViewModel(postRepository) }

                    NavHost(
                        navController = navController,
                        startDestination = "login"
                    ) {
                        composable("login") {
                            LoginScreen(
                                navController = navController,
                                authViewModel = authViewModel,
                                registrationViewModel = registrationViewModel
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

                        composable("settings") {
                            SettingsScreen(
                                navController = navController,
                                authViewModel = authViewModel,  // 👈 PASAR authViewModel
                                onLogout = {
                                    authViewModel.logout()
                                    registrationViewModel.resetState()
                                    postViewModel.clearPosts()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("help") {
                            HelpScreen(navController = navController)
                        }
                        composable("home") {
                            HomeScreen(
                                navController = navController,
                                onLogout = {
                                    authViewModel.logout()  // 👈 Usar nueva función
                                    registrationViewModel.resetState()
                                    postViewModel.clearPosts()  // 👈 Si tienes esta función

                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                authViewModel = authViewModel,
                                postViewModel = postViewModel
                            )
                        }

                        composable("deleteAccount") {
                            DeleteAccountScreen(
                                navController = navController,
                                authViewModel = authViewModel  // 👈 PASAR authViewModel
                            )
                        }

                        composable("newPublication") {
                            NewPublicationScreen(
                                navController = navController,
                                authViewModel = authViewModel,
                                postViewModel = postViewModel
                            )
                        }

                        composable(
                            route = "publicationDetail/{postId}",
                            arguments = listOf(navArgument("postId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val postId = backStackEntry.arguments?.getString("postId") ?: ""
                            PublicationDetailScreen(
                                navController = navController,
                                postId = postId,
                                postViewModel = postViewModel,
                                authViewModel = authViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}