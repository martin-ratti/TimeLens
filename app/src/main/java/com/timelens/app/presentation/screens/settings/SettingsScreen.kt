package com.timelens.app.presentation.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timelens.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToSummary: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val dailyGoal by viewModel.dailyGoalHours.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val darkThemeEnabled by viewModel.darkThemeEnabled.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var showGoalDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.toggleNotifications(true)
            val sent = viewModel.sendTestNotification()
            if (sent) {
                Toast.makeText(context, "¡Permiso concedido! Alerta enviada", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Se necesita permiso para mostrar notificaciones", Toast.LENGTH_LONG).show()
        }
    }

    val onToggleNotifications: (Boolean) -> Unit = { enable ->
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.toggleNotifications(true)
            }
        } else {
            viewModel.toggleNotifications(false)
        }
    }

    // Si algún dialog está abierto, interceptamos el gesto "Atrás" para cerrarlo
    // en lugar de navegar a la pantalla anterior.
    BackHandler(enabled = showGoalDialog || showAboutDialog) {
        showGoalDialog = false
        showAboutDialog = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Ajustes",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Objetivo diario
            SettingItem(
                icon = Icons.Outlined.Flag,
                title = "Objetivo diario",
                subtitle = "Límite: $dailyGoal horas al día",
                iconTint = NeonOrange,
                onClick = { showGoalDialog = true },
                action = {
                    Text(
                        text = "${dailyGoal}h",
                        color = NeonOrange,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            )

            // Notificaciones
            SettingItem(
                icon = Icons.Outlined.Notifications,
                title = "Notificaciones",
                subtitle = if (notificationsEnabled) "Alertas de bienestar activadas" else "Alertas desactivadas",
                iconTint = NeonBlue,
                onClick = { onToggleNotifications(!notificationsEnabled) },
                action = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { onToggleNotifications(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonBlue,
                            checkedTrackColor = NeonBlue.copy(alpha = 0.5f)
                        )
                    )
                }
            )

            // Tarjeta de pruebas de notificación cuando están activadas
            if (notificationsEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = NeonBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Probar Notificaciones",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!hasPermission) {
                            Surface(
                                color = NeonRed.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.NotificationsOff,
                                        contentDescription = null,
                                        tint = NeonRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Permiso de Android requerido. Tocá 'Conceder permiso' para activar las alertas.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeonRed,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Permitir", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Presioná para recibir una alerta inmediata o simular el resumen nocturno de las 22:00 hs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        val sent = viewModel.sendTestNotification()
                                        if (sent) {
                                            Toast.makeText(context, "¡Notificación de alerta enviada!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No se pudo mostrar. Verificá los permisos del sistema.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Probar alerta", color = NeonBlue, fontSize = 12.sp, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.triggerDailySummaryTest()
                                        Toast.makeText(context, "Generando resumen diario...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Probar resumen", color = NeonPurple, fontSize = 12.sp, maxLines = 1)
                            }
                        }

                        if (onNavigateToSummary != null) {
                            TextButton(
                                onClick = onNavigateToSummary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Bedtime,
                                    contentDescription = null,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Ver cierre del día de hoy",
                                    color = NeonPurple,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Tema (Oscuro / Claro)
            SettingItem(
                icon = if (darkThemeEnabled) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                title = if (darkThemeEnabled) "Modo Oscuro Neón" else "Modo Claro",
                subtitle = if (darkThemeEnabled) "Diseño oscuro con acentos neón" else "Diseño claro y luminoso",
                iconTint = NeonGreen,
                onClick = { viewModel.toggleDarkTheme(!darkThemeEnabled) },
                action = {
                    Switch(
                        checked = darkThemeEnabled,
                        onCheckedChange = { viewModel.toggleDarkTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGreen,
                            checkedTrackColor = NeonGreen.copy(alpha = 0.5f)
                        )
                    )
                }
            )

            // Compartir TimeLens
            SettingItem(
                icon = Icons.Outlined.Share,
                title = "Compartir TimeLens",
                subtitle = "Recomendar la app a un amigo",
                iconTint = NeonCyan,
                onClick = {
                    viewModel.shareApp { chooserIntent ->
                        context.startActivity(chooserIntent)
                    }
                },
                action = {
                    Icon(
                        imageVector = Icons.Outlined.ArrowOutward,
                        contentDescription = "Compartir",
                        tint = NeonCyan
                    )
                }
            )

            // Acerca de
            SettingItem(
                icon = Icons.Outlined.Info,
                title = "Acerca de TimeLens",
                subtitle = "Misión, privacidad y desarrollador",
                iconTint = MaterialTheme.colorScheme.onSurface,
                onClick = { showAboutDialog = true },
                action = {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "TimeLens v0.2.0 • 100% Local & Privado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    // Dialog: Seleccionar Objetivo Diario
    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Definir objetivo diario", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Selecciona la cantidad de horas máxima recomendada por día:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(4, 5, 6, 7, 8, 9).forEach { hours ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDailyGoal(hours)
                                    showGoalDialog = false
                                    Toast.makeText(context, "Objetivo actualizado a ${hours}h", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (hours == dailyGoal) NeonOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$hours horas",
                                    fontWeight = if (hours == dailyGoal) FontWeight.Bold else FontWeight.Normal,
                                    color = if (hours == dailyGoal) NeonOrange else MaterialTheme.colorScheme.onSurface
                                )
                                if (hours == dailyGoal) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = NeonOrange
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cerrar", color = NeonBlue)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // Dialog: Acerca de (Completamente Mejorado y Visual)
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = NeonBlue.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.RemoveRedEye,
                                contentDescription = null,
                                tint = NeonBlue,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "TimeLens",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Consciencia & Bienestar Digital",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonBlue
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Card Misión
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🎯 Nuestra Misión",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonPurple
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ayudarte a comprender tus hábitos de uso del teléfono y construir una relación consciente y saludable con la tecnología.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Card Privacidad
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🛡️ 100% Local & Privado",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tus estadísticas nunca salen de este teléfono. No existen servidores externos, telemetría ni rastreadores de publicidad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Card Desarrollador
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "👨‍💻 Creador",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonOrange
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Desarrollado por Martín Ratti en colaboración con Antigravity.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Versión 0.2.0 • Código Abierto",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/martin-ratti/TimeLens"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Code,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GitHub")
                    }
                    Button(
                        onClick = { showAboutDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                    ) {
                        Text("Cerrar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit = {},
    action: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            action()
        }
    }
}
