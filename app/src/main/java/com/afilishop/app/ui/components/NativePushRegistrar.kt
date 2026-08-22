package com.afilishop.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.afilishop.app.notifications.PushTokenStore
import com.afilishop.app.notifications.PushTokenSyncWorker
import com.afilishop.app.ui.AfiliShopViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun NativePushRegistrar(
    userId: String?,
    viewModel: AfiliShopViewModel,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        PushTokenStore.markPermissionRequested(context)
        if (granted) syncNativePushToken(context, viewModel)
    }

    LaunchedEffect(userId) {
        if (userId.isNullOrBlank()) return@LaunchedEffect
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        when {
            hasPermission -> syncNativePushToken(context, viewModel)
            !PushTokenStore.permissionWasRequested(context) -> {
                PushTokenStore.markPermissionRequested(context)
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

fun syncNativePushToken(context: Context, viewModel: AfiliShopViewModel? = null) {
    if (FirebaseApp.getApps(context).isEmpty()) return
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful) return@addOnCompleteListener
        val token = task.result?.takeIf { it.isNotBlank() } ?: return@addOnCompleteListener
        PushTokenStore.savePending(context, token)
        PushTokenSyncWorker.enqueue(context, token)
        viewModel?.registerPushToken(token)
    }
}
