package com.troca.latroca.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.troca.latroca.R

@Composable
fun WelcomeModal(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(300)
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(200)
        )
    ) {
        Dialog(
            onDismissRequest = { /* No se puede cerrar tocando fuera */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.la_troca_logo_2),
                                contentDescription = "Logo La Troca",
                                modifier = Modifier.size(80.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "¡Bienvenido a La Troca!",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3748),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "La app de intercambio sin dinero",
                            fontSize = 16.sp,
                            color = Color(0xFF718096),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF0FDF4)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiObjects,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "¿Qué es La Troca?",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534)
                                    )
                                }
                                Text(
                                    "Una plataforma donde puedes intercambiar objetos que ya no uses por cosas que realmente necesites, " +
                                            "sin usar dinero. Promueve la reutilización y cuida el medio ambiente.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF166534),
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            "¿Cómo funciona?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3748),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        FeatureItem(
                            icon = Icons.Default.AddBox,
                            iconColor = Color(0xFFE53935),
                            title = "1. Publica lo que tienes",
                            description = "Sube fotos de objetos que ya no uses: electrónicos, muebles, ropa, libros... ¡lo que sea!"
                        )

                        FeatureItem(
                            icon = Icons.Default.SwapHoriz,
                            iconColor = Color(0xFFFF9800),
                            title = "2. Di qué necesitas",
                            description = "Indica qué te gustaría recibir a cambio. Sé flexible, ¡la negociación es parte del trueque!"
                        )

                        FeatureItem(
                            icon = Icons.Default.Chat,
                            iconColor = Color(0xFF2196F3),
                            title = "3. Conecta y negocia",
                            description = "Otros usuarios te contactarán si les interesa. Chatea, negocia y acuerden el intercambio."
                        )

                        FeatureItem(
                            icon = Icons.Default.CheckCircle,
                            iconColor = Color(0xFF4CAF50),
                            title = "4. Haz el intercambio",
                            description = "Acuerden un lugar seguro, verifiquen los objetos y completen el trueque. ¡Ambos ganan!"
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53935)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "¡Entendido, empecemos!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2D3748)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color(0xFF718096),
                lineHeight = 18.sp
            )
        }
    }
}