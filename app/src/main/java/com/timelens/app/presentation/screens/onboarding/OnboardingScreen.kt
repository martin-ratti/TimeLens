package com.timelens.app.presentation.screens.onboarding

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.timelens.app.presentation.theme.*

@Composable
fun OnboardingScreen(
    onOpenSettings: () -> Unit,
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier,
    checkPermissionOverride: (() -> Boolean)? = null
) {
    val context = LocalContext.current

    // Bloquea el botón "Atrás" para que el usuario no quede atrapado fuera del flujo
    BackHandler(enabled = true) { /* Consumido intencionalmente */ }

    // Auto-detección cuando el usuario vuelve de Configuración
    LifecycleResumeEffect(Unit) {
        val granted = checkPermissionOverride?.invoke() ?: hasUsageStatsPermission(context)
        if (granted) {
            onPermissionGranted()
        }
        onPauseOrDispose { }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícono distintivo con aura
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(NeonBlue.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.RemoveRedEye,
                contentDescription = null,
                tint = NeonBlue,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Bienvenido a TimeLens",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonBlue,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Conocé tus hábitos digitales con claridad y recuperá el control de tu tiempo de pantalla.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Tarjetas de propuesta de valor y privacidad
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ValuePropCard(
                icon = Icons.Outlined.Shield,
                iconTint = NeonGreen,
                title = "100% Privado y Offline",
                description = "Tus estadísticas jamás salen de tu teléfono. Sin servidores ni nube."
            )

            ValuePropCard(
                icon = Icons.Outlined.Lock,
                iconTint = NeonPurple,
                title = "Cero Publicidad y Rastreo",
                description = "Experiencia limpia sin anuncios ni SDKs invasivos de recolección de datos."
            )

            ValuePropCard(
                icon = Icons.Outlined.Bolt,
                iconTint = NeonCyan,
                title = "Eficiente con tu Batería",
                description = "Análisis liviano en memoria sin procesos de fondo que agoten tu batería."
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Tarjeta informativa de permiso
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Para calcular tu tiempo real, Android requiere que concedas el permiso de Acceso de Uso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onOpenSettings,
            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Habilitar Acceso de Uso",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onPermissionGranted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Ya otorgué el permiso (Continuar)",
                color = NeonBlue.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ValuePropCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

@Preview
@Composable
fun OnboardingScreenPreview() {
    TimeLensTheme {
        OnboardingScreen(
            onOpenSettings = {},
            onPermissionGranted = {}
        )
    }
}
