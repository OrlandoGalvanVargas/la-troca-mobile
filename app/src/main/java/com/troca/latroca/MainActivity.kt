package com.troca.latroca

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.troca.latroca.data.local.TokenManager
import com.troca.latroca.ui.screens.TermsAndPoliciesScreen
import com.google.firebase.messaging.FirebaseMessaging
import com.troca.latroca.data.repository.AuthRepository
import com.troca.latroca.data.repository.ChatRepository
import com.troca.latroca.data.repository.PostRepository
import com.troca.latroca.ui.screens.*
import com.troca.latroca.ui.theme.LaTrocaTheme
import com.troca.latroca.ui.viewmodels.AuthViewModel
import com.troca.latroca.ui.viewmodels.ChatViewModel
import com.troca.latroca.ui.viewmodels.PostViewModel
import com.troca.latroca.ui.viewmodels.RegistrationViewModel

class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel
    private var fcmTokenPending: String? = null

    private var pendingChatNavigation = mutableStateOf<Triple<String, String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate - Intent extras: ${intent?.extras?.keySet()?.joinToString()}")
        intent?.let { captureNotificationData(it) }

        setContent {
            LaTrocaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val authRepository = remember { AuthRepository() }
                    val postRepository = remember { PostRepository() }
                    val tokenManager = remember { TokenManager(applicationContext) }

                    authViewModel = viewModel(
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
                    val chatRepository = ChatRepository()
                    val chatViewModel = ChatViewModel(chatRepository)

                    val currentToken by authViewModel.currentToken.collectAsState()

                    LaunchedEffect(currentToken) {
                        if (!currentToken.isNullOrEmpty()) {
                            Log.d("MainActivity", "🔐 Usuario autenticado, registrando FCM token...")

                            fcmTokenPending?.let { fcmToken ->
                                authViewModel.updateFcmToken(fcmToken)
                                fcmTokenPending = null
                            }

                            subscribeToChatNotifications()
                        }
                    }

                    val startDestination = if (!currentToken.isNullOrEmpty()) "home" else "login"

                    val chatNavigationData by pendingChatNavigation

                    LaunchedEffect(navController, currentToken, chatNavigationData) {
                        Log.d("MainActivity", "🔄 LaunchedEffect ejecutado - Token: ${!currentToken.isNullOrEmpty()}, Data: $chatNavigationData")

                        if (!currentToken.isNullOrEmpty() && chatNavigationData != null) {
                            val (chatId, senderName, senderId) = chatNavigationData!!

                            Log.d("MainActivity", "📩 Navegando al chat desde notificación: $chatId")

                            kotlinx.coroutines.delay(300)

                            try {
                                navController.navigate(
                                    "chat_conversation/$chatId/$senderName/$senderId"
                                ) {
                                    launchSingleTop = true
                                }
                                Log.d("MainActivity", "✅ Navegación completada exitosamente")
                            } catch (e: Exception) {
                                Log.e("MainActivity", "❌ Error en navegación: ${e.message}")
                            }

                            pendingChatNavigation.value = null
                        }
                    }

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
                                authViewModel = authViewModel
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                navController = navController,
                                authViewModel = authViewModel,
                                postViewModel = postViewModel,
                                chatViewModel = chatViewModel,
                                onLogout = {
                                    authViewModel.logout()
                                    registrationViewModel.resetState()
                                    postViewModel.clearPosts()
                                }
                            )
                        }

                        composable("editProfile") {
                            EditProfileScreen(
                                navController = navController,
                                authViewModel = authViewModel,
                                postViewModel = postViewModel,
                                onLogout = {
                                    authViewModel.logout()
                                    registrationViewModel.resetState()
                                    postViewModel.clearPosts()
                                }
                            )
                        }

                        composable("admin_users") {
                            AdminUsersScreen(
                                navController = navController,
                                authViewModel = authViewModel
                            )
                        }

                        composable(
                            route = "admin_user_detail/{userId}",
                            arguments = listOf(
                                navArgument("userId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""
                            AdminUserDetailScreen(
                                navController = navController,
                                authViewModel = authViewModel,
                                userId = userId
                            )
                        }

                        composable("help") {
                            HelpScreen(navController = navController)
                        }

                        composable("termsAndPolicies") {
                            TermsAndPoliciesScreen(navController = navController)
                        }

                        composable("deleteAccount") {
                            DeleteAccountScreen(
                                navController = navController,
                                authViewModel = authViewModel
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
                                authViewModel = authViewModel,
                                chatViewModel = chatViewModel
                            )
                        }

                        composable(
                            route = "userProfile/{userId}",
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""
                            UserProfileScreen(
                                navController = navController,
                                userId = userId
                            )
                        }

                        composable("chat_list") {
                            ChatListScreen(
                                navController = navController,
                                chatViewModel = chatViewModel,
                                authViewModel = authViewModel
                            )
                        }

                        composable(
                            route = "chat_conversation/{chatId}/{otherUserName}/{otherUserId}",
                            arguments = listOf(
                                navArgument("chatId") { type = NavType.StringType },
                                navArgument("otherUserName") { type = NavType.StringType },
                                navArgument("otherUserId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            ChatConversationScreen(
                                navController = navController,
                                chatId = backStackEntry.arguments?.getString("chatId") ?: "",
                                otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: "",
                                otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: "",
                                chatViewModel = chatViewModel,
                                authViewModel = authViewModel
                            )
                        }
                    }
                }
            }
        }
    }

    private fun captureNotificationData(intent: Intent) {
        Log.d("MainActivity", "🔍 Verificando intent para datos de chat...")
        Log.d("MainActivity", "openChat: ${intent.getBooleanExtra("openChat", false)}")
        Log.d("MainActivity", "chatId: ${intent.getStringExtra("chatId")}")
        Log.d("MainActivity", "senderId: ${intent.getStringExtra("senderId")}")
        Log.d("MainActivity", "senderName: ${intent.getStringExtra("senderName")}")

        if (intent.getBooleanExtra("openChat", false)) {
            val chatId = intent.getStringExtra("chatId") ?: ""
            val senderId = intent.getStringExtra("senderId") ?: ""
            val senderName = intent.getStringExtra("senderName") ?: ""

            Log.d("MainActivity", "📋 Valores extraídos: chatId='$chatId', senderId='$senderId', senderName='$senderName'")

            if (chatId.isNotEmpty() && senderId.isNotEmpty() && senderName.isNotEmpty()) {
                Log.d("MainActivity", "✅ Datos de notificación capturados: chatId=$chatId, sender=$senderName")
                pendingChatNavigation.value = Triple(chatId, senderName, senderId)
            } else {
                Log.w("MainActivity", "⚠️ Algunos datos están vacíos, no se guardó navegación pendiente")
            }
        } else {
            Log.d("MainActivity", "ℹ️ No es una apertura desde notificación de chat")
        }
    }

    private fun subscribeToChatNotifications() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainActivity", "❌ Error al obtener FCM token", task.exception)
                return@addOnCompleteListener
            }

            val fcmToken = task.result
            Log.d("MainActivity", "✅ FCM Token obtenido: ${fcmToken.take(20)}...")

            val sharedPref = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
            sharedPref.edit().putString("fcm_token", fcmToken).apply()

            if (::authViewModel.isInitialized) {
                val currentToken = authViewModel.getToken()
                if (!currentToken.isNullOrEmpty()) {
                    Log.d("MainActivity", "🔔 Usuario autenticado, enviando FCM token al backend...")
                    authViewModel.updateFcmToken(fcmToken)
                } else {
                    Log.d("MainActivity", "⏳ Usuario no autenticado aún, guardando token para más tarde...")
                    fcmTokenPending = fcmToken
                }
            } else {
                Log.w("MainActivity", "⚠️ AuthViewModel no inicializado, guardando token...")
                fcmTokenPending = fcmToken

                Handler(Looper.getMainLooper()).postDelayed({
                    if (::authViewModel.isInitialized) {
                        val currentToken = authViewModel.getToken()
                        if (!currentToken.isNullOrEmpty()) {
                            authViewModel.updateFcmToken(fcmToken)
                            fcmTokenPending = null
                        }
                    }
                }, 2000)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        Log.d("MainActivity", "📨 onNewIntent llamado")

        intent.let {
            if (it.getBooleanExtra("openChat", false)) {
                Log.d("MainActivity", "📩 onNewIntent: Capturando datos de notificación")
                captureNotificationData(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "📱 onResume - Verificando intent")
        intent?.let { captureNotificationData(it) }
    }
}