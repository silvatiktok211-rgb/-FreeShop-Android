package com.afilishop.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afilishop.app.data.NativeNotification
import com.afilishop.app.data.NotificationRepository
import com.afilishop.app.ui.AfiliShopViewModel
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: AfiliShopViewModel,
    padding: PaddingValues,
    onBack: () -> Unit,
    onProduct: (String) -> Unit = {},
    onConversation: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { NotificationRepository(context) }
    val scope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf<List<NativeNotification>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    suspend fun reload() {
        notifications = repository.load()
        loading = false
    }

    LaunchedEffect(Unit) { reload() }
    DisposableEffect(repository) { onDispose { repository.close() } }

    val unreadCount = notifications.count { !it.isRead }

    fun openNotification(notification: NativeNotification) {
        notification.productId?.takeIf { it.isNotBlank() }?.let {
            onProduct(it)
            return
        }

        val link = notification.link.orEmpty()
        if (notification.type == "new_message" && link.startsWith("/mensagens/")) {
            val conversationId = link.removePrefix("/mensagens/").substringBefore('?').trim('/')
            if (conversationId.isNotBlank()) onConversation(conversationId)
            return
        }

        if (link.startsWith("http://") || link.startsWith("https://")) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        }
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Notificações", fontWeight = FontWeight.Bold)
                        if (unreadCount > 0) {
                            Text(
                                "$unreadCount não ${if (unreadCount == 1) "lida" else "lidas"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    if (repository.markAllRead()) reload()
                                }
                            },
                        ) {
                            Icon(Icons.Default.DoneAll, null, modifier = Modifier.size(18.dp))
                            Text("Todas", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                },
            )
        },
    ) { inner ->
        when {
            loading -> Box(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.TopCenter,
            ) {
                CircularProgressIndicator(Modifier.padding(top = 44.dp))
            }

            notifications.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 64.dp),
                ) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        modifier = Modifier.size(46.dp),
                    )
                    Text("Nenhuma notificação por aqui ainda.")
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onOpen = {
                            scope.launch {
                                if (!notification.isRead) repository.markRead(notification.id)
                                openNotification(notification)
                                if (!notification.isRead) reload()
                            }
                        },
                        onDelete = {
                            scope.launch {
                                if (repository.delete(notification.id)) {
                                    notifications = notifications.filterNot { it.id == notification.id }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NativeNotification,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val unread = !notification.isRead
    val icon = notificationIcon(notification.type)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unread) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (unread) 1.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (unread) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    null,
                    tint = if (unread) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp),
                )
            }

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        notification.title,
                        fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        notificationTimeAgo(notification.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                notification.body?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                if (unread) {
                    Text(
                        "Nova",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.Delete,
                    "Apagar notificação",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun notificationIcon(type: String?): ImageVector = when (type) {
    "promo", "favorite_discount" -> Icons.Default.LocalOffer
    "price_drop", "price_reduction" -> Icons.Default.TrendingDown
    "follow" -> Icons.Default.PersonAdd
    "video_like" -> Icons.Default.Favorite
    "video_comment", "video_reply", "new_message" -> Icons.Default.ChatBubbleOutline
    "live_started" -> Icons.Default.Radio
    else -> Icons.Default.NotificationsNone
}

private fun notificationTimeAgo(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return runCatching {
        val instant = OffsetDateTime.parse(value).toInstant()
        val minutes = ChronoUnit.MINUTES.between(instant, Instant.now()).coerceAtLeast(0)
        when {
            minutes < 1 -> "agora"
            minutes < 60 -> "${minutes} min"
            minutes < 1_440 -> "${minutes / 60} h"
            else -> "${minutes / 1_440} d"
        }
    }.getOrDefault("")
}
