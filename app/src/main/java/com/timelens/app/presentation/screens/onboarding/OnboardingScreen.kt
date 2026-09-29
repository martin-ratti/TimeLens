package com.timelens.app.presentation.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.timelens.app.presentation.theme.NeonBlue
import com.timelens.app.presentation.theme.TimeLensTheme

@Composable
fun OnboardingScreen(
    onOpenSettings: () -> Unit,
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Bloquea el gesto/botón "Atrás" para que el usuario no salga de la app
    // sin completar el flujo de permisos requerido.
    BackHandler(enabled = true) { /* consumido intencionalmente */ }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.RemoveRedEye,
            contentDescription = null,
            tint = NeonBlue,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Bienvenido a TimeLens",
            style = MaterialTheme.typography.headlineLarge,
            color = NeonBlue,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Para monitorear tu tiempo de pantalla, TimeLens requiere acceso a las estadísticas de uso de tu dispositivo. Esto nos permite ayudarte a construir mejores hábitos digitales.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onOpenSettings,
            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(text = "Abrir Configuración")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onPermissionGranted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Ya otorgué el permiso (Continuar)",
                color = NeonBlue
            )
        }
    }
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
