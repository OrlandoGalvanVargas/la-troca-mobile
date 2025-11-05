package com.example.latroca.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.troca.latroca.ui.screens.BulletPoint
import com.troca.latroca.ui.screens.ContactSection
import com.troca.latroca.ui.screens.HelpSection
import com.troca.latroca.ui.screens.HelpSectionContent
import com.troca.latroca.ui.screens.InfoBox
import com.troca.latroca.ui.screens.SectionHeader
import com.troca.latroca.ui.screens.SectionText
import com.troca.latroca.ui.screens.WarningBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndPoliciesScreen(
    navController: NavController
) {
    var expandedSection by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Términos y Políticas",
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
                        imageVector = Icons.Default.Gavel,
                        contentDescription = "Términos",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Términos y Políticas",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Última actualización: 09 de octubre de 2025",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Sección 1: Términos de Servicio
            HelpSection(
                icon = Icons.Default.Description,
                title = "Términos de Servicio",
                isExpanded = expandedSection == "terms",
                onToggle = {
                    expandedSection = if (expandedSection == "terms") null else "terms"
                }
            ) {
                HelpSectionContent {
                    SectionText(
                        "Al utilizar la aplicación La Troca, usted acepta los siguientes términos:"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    BulletPoint("El usuario se compromete a utilizar la app únicamente para realizar trueques o intercambios legítimos")
                    BulletPoint("Está prohibido publicar contenido ofensivo, ilegal o fraudulento")
                    BulletPoint("La Troca no se hace responsable por los acuerdos entre usuarios fuera de la plataforma")
                    BulletPoint("Nos reservamos el derecho de suspender o eliminar cuentas que incumplan las normas de uso o ética")
                    BulletPoint("El contenido publicado por los usuarios puede ser moderado mediante IA para mantener un entorno seguro")
                    BulletPoint("La app puede mostrar anuncios o notificaciones relacionadas con su uso")

                    Spacer(modifier = Modifier.height(12.dp))

                    InfoBox(
                        "Estos términos pueden actualizarse ocasionalmente. El uso continuo de la app implica aceptación de las modificaciones."
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Sección 2: Política de Privacidad
            HelpSection(
                icon = Icons.Default.Lock,
                title = "Política de Privacidad",
                isExpanded = expandedSection == "privacy",
                onToggle = {
                    expandedSection = if (expandedSection == "privacy") null else "privacy"
                }
            ) {
                HelpSectionContent {
                    SectionText(
                        "La Troca recopila y trata datos personales conforme a la LFPDPPP (México), GDPR (UE) y CCPA (EE. UU.)."
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("Datos Recopilados")
                    BulletPoint("Nombre, correo, foto de perfil")
                    BulletPoint("Ubicación (previo consentimiento)")
                    BulletPoint("Datos de uso y dispositivo")
                    BulletPoint("Contenido publicado y mensajes")

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("Finalidades")
                    BulletPoint("Creación de cuenta y gestión de perfil")
                    BulletPoint("Publicaciones y chat interno")
                    BulletPoint("Seguridad y prevención de fraude")
                    BulletPoint("Estadísticas y mejoras del servicio")

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("Compartición de Datos")
                    BulletPoint("Firebase (infraestructura y almacenamiento)")
                    BulletPoint("DeepAI (moderación de contenido)")
                    BulletPoint("Autoridades competentes cuando sea necesario")

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("Seguridad")
                    BulletPoint("Cifrado TLS/HTTPS en todas las comunicaciones")
                    BulletPoint("Control de accesos y autenticación")
                    BulletPoint("Auditorías periódicas de seguridad")

                    Spacer(modifier = Modifier.height(16.dp))

                    WarningBox(
                        "⚠ Transferencias internacionales: Los datos pueden alojarse en servidores en EE. UU. bajo cláusulas contractuales estándar."
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("Conservación de Datos")
                    BulletPoint("Datos de cuenta: mientras la cuenta esté activa")
                    BulletPoint("Cuenta desactivada: eliminación permanente tras 30 días")
                    BulletPoint("Chats: hasta 12 meses para investigación de incidencias")
                    BulletPoint("Datos anonimizados: pueden conservarse indefinidamente")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Sección 3: Derechos del Usuario
            HelpSection(
                icon = Icons.Default.VerifiedUser,
                title = "Derechos del Usuario (ARCO)",
                isExpanded = expandedSection == "rights",
                onToggle = {
                    expandedSection = if (expandedSection == "rights") null else "rights"
                }
            ) {
                HelpSectionContent {
                    SectionText(
                        "Usted puede ejercer los siguientes derechos sobre sus datos personales:"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    BulletPoint("Acceso: Conocer qué datos personales tenemos sobre usted")
                    BulletPoint("Rectificación: Corregir datos incorrectos o desactualizados")
                    BulletPoint("Cancelación: Solicitar la eliminación de sus datos")
                    BulletPoint("Oposición: Limitar el uso de sus datos para ciertos fines")
                    BulletPoint("Portabilidad: Recibir sus datos en formato estructurado")
                    BulletPoint("Retiro del Consentimiento: Revocar autorizaciones otorgadas")

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("Cómo Ejercer sus Derechos")
                    BulletPoint("Envíe un correo a: privacidad.latroca.app@gmail.com")
                    BulletPoint("Incluya: nombre completo y solicitud detallada")
                    BulletPoint("Tiempo de respuesta: máximo 30 días hábiles")

                    Spacer(modifier = Modifier.height(12.dp))

                    InfoBox(
                        "Garantizamos la protección de sus derechos conforme a la legislación aplicable."
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Sección 4: Menores de Edad
            HelpSection(
                icon = Icons.Default.ChildCare,
                title = "Protección de Menores",
                isExpanded = expandedSection == "minors",
                onToggle = {
                    expandedSection = if (expandedSection == "minors") null else "minors"
                }
            ) {
                HelpSectionContent {
                    SectionText(
                        "La Troca no está dirigida a menores de 16 años. No recopilamos datos personales de menores de manera intencionada."
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    WarningBox(
                        "⚠ Si usted es padre o tutor y cree que su hijo menor de 16 años ha proporcionado información personal, contáctenos de inmediato para proceder con su eliminación."
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