package com.troca.latroca.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Modal de carga reutilizable que bloquea la interacción con la pantalla
 * @param isVisible Controla si el modal es visible
 * @param message Mensaje a mostrar (opcional)
 */
@Composable
fun LoadingModal(
    isVisible: Boolean,
    message: String = "Cargando..."
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = { /* No permite cerrar tocando fuera */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            // Fondo semi-transparente que cubre toda la pantalla
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                // Tarjeta con el contenido del loading
                Surface(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Animación del CircularProgressIndicator
                        val infiniteTransition = rememberInfiniteTransition(label = "loading")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )

                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = Color(0xFFE53E3E),
                            strokeWidth = 4.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = message,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2D3748),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}