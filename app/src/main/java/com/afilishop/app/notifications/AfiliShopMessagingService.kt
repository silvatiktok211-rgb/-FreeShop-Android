package com.afilishop.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.afilishop.app.MainActivity
import com.afilishop.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AfiliShopMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushTokenStore.savePending(this, token)
        PushTokenSyncWorker.enqueue(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: message.notification?.title ?: "AfiliShop"
        val body = message.data["body"] ?: message.notification?.body ?: "Você tem uma novidade."
        val link = message.data["url"] ?: message.data["link"]
        val type = message.data["type"] ?: "admin"
        val tag = message.data["tag"] ?: message.messageId ?: System.currentTimeMillis().toString()
        showNotification(title, body, link, type, tag)
        sendBroadcast(Intent(ACTION_NOTIFICATION_RECEIVED).setPackage(packageName))
    }

    private fun showNotification(title: String, body: String, link: String?, type: String, tag: String) {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sound = Settings.System.DEFAULT_NOTIFICATION_URI
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notificações da AfiliShop",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Ofertas, mensagens, avisos da conta e novidades da AfiliShop"
                enableLights(true)
                lightColor = Color.rgb(255, 105, 48)
                enableVibration(true)
                setSound(sound, audioAttributes)
            }
            manager.createNotificationChannel(channel)
        }
        val requestCode = tag.hashCode()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = Uri.parse("afilishop://notification/${Uri.encode(tag)}")
            putExtra(EXTRA_NOTIFICATION_LINK, link)
            putExtra(EXTRA_NOTIFICATION_TYPE, type)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFFF6A00.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (type == "message" || type == "new_message") NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setGroup(NOTIFICATION_GROUP)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(tag, requestCode, notification)
        }
    }

    companion object {
        const val ACTION_NOTIFICATION_RECEIVED = "com.afilishop.app.NOTIFICATION_RECEIVED"
        const val EXTRA_NOTIFICATION_LINK = "notification_link"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val CHANNEL_ID = "afilishop_updates"
        private const val NOTIFICATION_GROUP = "afilishop_notifications"
    }
}
