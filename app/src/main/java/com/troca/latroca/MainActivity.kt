package com.troca.latroca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.latroca.data.local.TokenManager
import com.example.latroca.ui.screens.TermsAndPoliciesScreen
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

                    // 🆕 Crear TokenManager
                    val tokenManager = remember { TokenManager(applicationContext) }

                    // ✅ DESPUÉS (usa viewModel para caché)
                    val authViewModel: AuthViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return AuthViewModel(authRepository, tokenManager) as T
                            }
                        }
                    )

                    val registrationViewModel: RegistrationViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return RegistrationViewModel(authRepository, authViewModel) as T
                            }
                        }
                    )

                    val postViewModel = remember { PostViewModel(postRepository) }

                    // 🆕 Determinar pantalla inicial basada en si hay token
                    val startDestination = if (tokenManager.hasToken()) "home" else "login"

                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300)
                            )
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300)
                            )
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300)
                            )
                        }
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
                        composable("termsAndPolicies") {
                            TermsAndPoliciesScreen(navController = navController)
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