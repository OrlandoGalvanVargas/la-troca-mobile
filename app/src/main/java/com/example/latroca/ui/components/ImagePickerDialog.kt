package com.example.latroca.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ImagePickerDialog(
    onTakePhoto: () -> Unit,
    onSelectFromGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Título
                Text(
                    text = "Seleccionar foto de perfil",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF2D3748)
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Elige cómo quieres agregar tu foto",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF718096)
                    ),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Opción Tomar foto
                Surface(
                    onClick = onTakePhoto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color(0xFFFEF2F2)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📷",
                            modifier = Modifier.size(24.dp),
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Tomar una foto",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFEF4444)
                            )
                        )
                    }
                }

                // Opción Seleccionar imagen
                Surface(
                    onClick = onSelectFromGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color(0xFFFEF2F2)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🖼️",
                            modifier = Modifier.size(24.dp),
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Seleccionar de galería",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFEF4444)
                            )
                        )
                    }
                }

                // Boton
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Cancelar",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFEF4444)
                        )
                    )
                }
            }
        }
    }
}