package com.troca.latroca.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun LoadingModal(
    isVisible: Boolean,
    message: String = "Cargando...",
    timeoutSeconds: Int = 10
) {
    var showTimeoutMessage by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            showTimeoutMessage = false
            // Esperar el tiempo especificado
            delay(timeoutSeconds * 1000L)
            showTimeoutMessage = true
        } else {
            showTimeoutMessage = false
        }
    }

    if (isVisible) {
        Dialog(
            onDismissRequest = { /* No permite cerrar tocando fuera */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(horizontal = 40.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 36.dp, vertical = 32.dp)
                            .widthIn(min = 200.dp, max = 280.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = Color(0xFFE53E3E),
                            strokeWidth = 5.dp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = message,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2D3748),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        if (showTimeoutMessage) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Esto puede tomar algo de tiempo...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF718096),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}