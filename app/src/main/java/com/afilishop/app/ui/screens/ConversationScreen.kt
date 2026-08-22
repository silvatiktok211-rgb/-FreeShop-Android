package com.afilishop.app.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afilishop.app.data.CommunityConversation
import com.afilishop.app.data.CommunityMessage
import com.afilishop.app.data.CommunityRepository
import com.afilishop.app.ui.AfiliShopViewModel
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: AfiliShopViewModel,
    conversationId: String,
    padding: PaddingValues,
    onBack: () -> Unit,
    onProfile: (String) -> Unit = {},
    onProduct: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { CommunityRepository(context) }
    val listState = rememberLazyListState()
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var conversation by remember { mutableStateOf<CommunityConversation?>(null) }
    var messages by remember { mutableStateOf<List<CommunityMessage>>(emptyList()) }
    var content by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var accepting by remember { mutableStateOf(false) }

    DisposableEffect(repository) {
        onDispose { repository.close() }
    }

    LaunchedEffect(conversationId) {
        currentUserId = repository.currentUserId()
        repository.touchLastSeen()
        while (true) {
            conversation = repository.loadConversation(conversationId)
            messages = repository.loadMessages(conversationId)
            repository.markRead(conversationId)
            loading = false
            delay(3_000)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val userId = currentUserId
    val otherId = userId?.let { conversation?.otherUserId(it) }
    val other = conversation?.otherProfile
    val online = isChatOnline(other?.lastSeenAt)
    val incomingPending = conversation?.status == "pending" && conversation?.requestedBy != userId
    val canSend = conversation?.status == "accepted"

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(enabled = otherId != null) {
                            otherId?.let(onProfile)
                        },
                    ) {
                        Box(modifier = Modifier.size(38.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!other?.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = other?.avatarUrl,
                                        contentDescription = other?.displayName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Text(
                                        other?.displayName?.take(1)?.uppercase() ?: "A",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                            }
                            if (online) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E)),
                                )
                            }
                        }
                        Column(Modifier.padding(start = 9.dp)) {
                            Text(
                                other?.displayName ?: "Conversa AfiliShop",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (online) "online" else chatLastSeen(other?.lastSeenAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (online) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            if (incomingPending) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Este usuário quer conversar com você.",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Button(
                            onClick = {
                                accepting = true
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    if (repository.acceptConversation(conversationId)) {
                                        conversation = repository.loadConversation(conversationId)
                                    }
                                    accepting = false
                                }
                            },
                            enabled = !accepting,
                        ) { Text(if (accepting) "…" else "Aceitar") }
                    }
                }
            }

            when {
                loading -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                messages.isEmpty() -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Comece a conversa por aqui.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(messages.size, key = { messages[it].id }) { index ->
                        val message = messages[index]
                        MessageBubble(
                            message = message,
                            own = message.senderId == userId,
                            onProduct = onProduct,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it.take(2_000) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (canSend) "Escreva uma mensagem" else "Aceite a conversa para responder") },
                    maxLines = 4,
                    enabled = canSend && !sending,
                    shape = RoundedCornerShape(22.dp),
                )
                IconButton(
                    onClick = {
                        val text = content.trim()
                        if (text.isBlank() || !canSend || sending) return@IconButton
                        sending = true
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            if (repository.sendMessage(conversationId, text)) {
                                content = ""
                                messages = repository.loadMessages(conversationId)
                            }
                            sending = false
                        }
                    },
                    enabled = canSend && content.isNotBlank() && !sending,
                ) {
                    Icon(
                        Icons.Default.Send,
                        "Enviar",
                        tint = if (canSend && content.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: CommunityMessage,
    own: Boolean,
    onProduct: (String) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (own) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.82f),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (own) 18.dp else 5.dp,
                bottomEnd = if (own) 5.dp else 18.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (own) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                if (!message.attachmentType.isNullOrBlank()) {
                    AttachmentPreview(
                        type = message.attachmentType,
                        value = message.attachmentUrl,
                        own = own,
                        onProduct = onProduct,
                    )
                }

                if (!message.content.isNullOrBlank()) {
                    Text(
                        message.content,
                        color = if (own) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        chatTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (own) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (own) {
                        Text(
                            when {
                                message.readAt != null -> "  ✓✓"
                                message.deliveredAt != null -> "  ✓"
                                else -> "  •"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreview(
    type: String?,
    value: String?,
    own: Boolean,
    onProduct: (String) -> Unit,
) {
    val icon = when (type) {
        "image" -> Icons.Default.Image
        "video" -> Icons.Default.Videocam
        "product" -> Icons.Default.ShoppingBag
        else -> Icons.Default.AttachFile
    }
    val label = when (type) {
        "image" -> "Imagem anexada"
        "video" -> "Vídeo anexado"
        "product" -> "Produto compartilhado"
        else -> "Anexo"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = type == "product" && !value.isNullOrBlank()) {
                if (type == "product") value?.let(onProduct)
            }
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            null,
            tint = if (own) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            label,
            color = if (own) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

private fun chatTime(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return runCatching {
        val instant = OffsetDateTime.parse(value).toInstant()
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(instant)
    }.getOrDefault("")
}

private fun isChatOnline(lastSeen: String?): Boolean {
    if (lastSeen.isNullOrBlank()) return false
    return runCatching {
        val instant = OffsetDateTime.parse(lastSeen).toInstant()
        ChronoUnit.SECONDS.between(instant, Instant.now()) in 0..90
    }.getOrDefault(false)
}

private fun chatLastSeen(lastSeen: String?): String {
    if (lastSeen.isNullOrBlank()) return ""
    return runCatching {
        val instant = OffsetDateTime.parse(lastSeen).toInstant()
        val minutes = ChronoUnit.MINUTES.between(instant, Instant.now()).coerceAtLeast(0)
        when {
            minutes < 1 -> "visto agora"
            minutes < 60 -> "visto há ${minutes}m"
            minutes < 1_440 -> "visto há ${minutes / 60}h"
            else -> "visto há ${minutes / 1_440}d"
        }
    }.getOrDefault("")
}
