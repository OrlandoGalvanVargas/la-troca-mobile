package com.troca.latroca.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    navController: NavController
) {
    var expandedSection by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Centro de Ayuda",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7FAFC))
                .verticalScroll(rememberScrollState())
        ) {
            // 📌 Header informativo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE53E3E))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = "Ayuda",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "¿Necesitas ayuda?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aquí encontrarás información sobre tu cuenta y privacidad",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Sección 1: Eliminación de Cuenta
            HelpSection(
                icon = Icons.Default.AccountCircle,
                title = "Eliminación de Cuenta y Datos",
                isExpanded = expandedSection == "account",
                onToggle = {
                    expandedSection = if (expandedSection == "account") null else "account"
                }
            ) {
                HelpSectionContent {
                    SectionHeader("Descripción General")
                    SectionText(
                        "En La Troca respetamos tu derecho a eliminar tu cuenta y todos los datos " +
                                "personales asociados de forma permanente. Esta acción borra tu información " +
                                "de nuestros sistemas principales y de Firebase (autenticación, publicaciones y mensajes)."
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("Cómo Eliminar tu Cuenta")

                    SubHeader(" Opción A — Desde la app")
                    BulletPoint("Abre la app La Troca")
                    BulletPoint("Ve a Configuración → Eliminar Cuenta → Eliminar")
                    BulletPoint("Confirma tu decisión siguiendo las instrucciones")
                    BulletPoint("Tu cuenta se desactivará inmediatamente")

                    Spacer(modifier = Modifier.height(12.dp))

                    SubHeader("✉️ Opción B — Por correo electrónico")
                    BulletPoint("Envía un correo a: privacidad.latroca@gmail.com")
                    BulletPoint("Asunto: Solicitud de eliminación de cuenta — La Troca")
                    BulletPoint("Incluye el correo asociado a tu cuenta")
                    BulletPoint("Recibirás confirmación en máximo 30 días hábiles")

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("Qué Datos se Eliminan")
                    BulletPoint("Información de cuenta (nombre, correo, foto de perfil)")
                    BulletPoint("Publicaciones creadas por ti")
                    BulletPoint("Chats y mensajes")
                    BulletPoint("Valoraciones y reportes realizados")
                    BulletPoint("Datos de uso vinculados a tu ID")

                    Spacer(modifier = Modifier.height(16.dp))

                    WarningBox(
                        "⚠️ Importante: Una vez eliminada tu cuenta, no será posible recuperarla " +
                                "ni los datos asociados. Esta acción es permanente."
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("Período de Retención Temporal")
                    SectionText(
                        "Por motivos legales o de seguridad, algunos datos pueden conservarse " +
                                "hasta 90 días antes de su eliminación definitiva:"
                    )
                    BulletPoint("Registros de actividad para detección de fraude")
                    BulletPoint("Logs técnicos para integridad del sistema")

                    Spacer(modifier = Modifier.height(8.dp))

                    InfoBox(
                        "ℹ️ Todos estos datos se eliminan automáticamente una vez vencido el plazo."
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Sección 2: Eliminación Parcial de Datos
            HelpSection(
                icon = Icons.Default.Delete,
                title = "Eliminación Parcial de Datos",
                isExpanded = expandedSection == "partial",
                onToggle = {
                    expandedSection = if (expandedSection == "partial") null else "partial"
                }
            ) {
                HelpSectionContent {
                    SectionHeader("Descripción General")
                    SectionText(
                        "Si no deseas eliminar tu cuenta completa, puedes solicitar la eliminación " +
                                "de datos específicos sin cerrar tu cuenta. Esto te permite mantener tu " +
                                "perfil activo mientras eliminas información que ya no deseas conservar."
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("Cómo Solicitar Eliminación Parcial")
                    BulletPoint("Envía un correo a: privacidad.latroca@gmail.com")
                    BulletPoint("Asunto: Solicitud de eliminación de datos — La Troca")
                    BulletPoint("Incluye tu correo electrónico registrado")
                    BulletPoint("Describe qué información deseas eliminar")
                    BulletPoint("Proceso completado en máximo 30 días hábiles")

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("Tipos de Datos que Puedes Eliminar")
                    BulletPoint("Publicaciones o imágenes específicas")
                    BulletPoint("Mensajes o chats con otros usuarios")
                    BulletPoint("Comentarios o reportes realizados")
                    BulletPoint("Datos de ubicación compartidos")
                    BulletPoint("Información opcional de tu perfil (bio, intereses)")
                    BulletPoint("Datos técnicos asociados a tu dispositivo")

                    Spacer(modifier = Modifier.height(16.dp))

                    InfoBox(
                        "✅ Ventaja: Tu cuenta permanecerá activa y podrás seguir usando La Troca " +
                                "con normalidad después de eliminar los datos específicos."
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 📞 Sección de contacto
            ContactSection()

            Spacer(modifier = Modifier.height(16.dp))

            // Footer
            Text(
                text = "© 2025 La Troca — Todos los derechos reservados",
                fontSize = 12.sp,
                color = Color(0xFF718096),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun HelpSection(
    icon: ImageVector,
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Header expandible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFFE53E3E),
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3748),
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Contraer" else "Expandir",
                    tint = Color(0xFF718096)
                )
            }

            // Contenido expandible
            if (isExpanded) {
                Divider(color = Color(0xFFE2E8F0))
                content()
            }
        }
    }
}

@Composable
fun HelpSectionContent(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2D3748)
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun SubHeader(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF4A5568)
    )
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
fun SectionText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color(0xFF4A5568),
        lineHeight = 20.sp
    )
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = "• ",
            fontSize = 14.sp,
            color = Color(0xFFE53E3E),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFF4A5568),
            lineHeight = 20.sp
        )
    }
}

@Composable
fun WarningBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFFB91C1C),
            lineHeight = 18.sp
        )
    }
}

@Composable
fun InfoBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF1E40AF),
            lineHeight = 18.sp
        )
    }
}

@Composable
fun ContactSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE53E3E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth() // 👈 asegura que ocupe todo el ancho
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Contacto",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "¿Necesitas más ayuda?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center, // 👈 centrado visual del texto
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Contáctanos en:",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "privacidad.latroca.app@gmail.com",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Última actualización: 15 de octubre de 2025",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
