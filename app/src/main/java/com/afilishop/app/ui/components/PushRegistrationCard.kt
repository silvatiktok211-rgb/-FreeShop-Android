package com.afilishop.app.ui.components

import android.Manifest
import android.os.Build
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
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun PushRegistrationCard(viewModel: AfiliShopViewModel) {
    fun registerToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) viewModel.registerPushToken(task.result)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted || Build.VERSION.SDK_INT < 33) registerToken()
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Notificações push", style = MaterialTheme.typography.titleMedium)
            Text("Receba novidades, avisos da conta e ofertas mesmo com o app fechado.", modifier = Modifier.padding(top = 4.dp))
            Button(onClick = { if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else registerToken() }, modifier = Modifier.padding(top = 10.dp)) { Text("Ativar notificações") }
        }
    }
}
