package com.afilishop.app.ui.components

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afilishop.app.ui.AfiliShopViewModel
import com.afilishop.app.notifications.PushTokenStore

@Composable
fun PushRegistrationCard(viewModel: AfiliShopViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val notificationsEnabled = notificationManager.areNotificationsEnabled()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        PushTokenStore.markPermissionRequested(context)
        if (granted || Build.VERSION.SDK_INT < 33) syncNativePushToken(context, viewModel)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Notificações push", style = MaterialTheme.typography.titleMedium)
            Text("Receba novidades, avisos da conta e ofertas mesmo com o app fechado.", modifier = Modifier.padding(top = 4.dp))
            Text(
                if (notificationsEnabled) "Ativadas neste aparelho" else "Desativadas neste aparelho",
                color = if (notificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
            Button(
                onClick = {
                    when {
                        notificationsEnabled -> syncNativePushToken(context, viewModel)
                        Build.VERSION.SDK_INT >= 33 && !PushTokenStore.permissionWasRequested(context) -> {
                            PushTokenStore.markPermissionRequested(context)
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        else -> context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    }
                },
                modifier = Modifier.padding(top = 10.dp),
            ) {
                Text(if (notificationsEnabled) "Sincronizar aparelho" else "Ativar notificações")
            }
        }
    }
}
