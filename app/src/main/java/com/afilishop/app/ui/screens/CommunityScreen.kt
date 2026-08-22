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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Groups
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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
import com.afilishop.app.data.CommunityRepository
import com.afilishop.app.ui.AfiliShopViewModel
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class CommunityTab { CONVERSATIONS, REQUESTS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: AfiliShopViewModel,
    padding: PaddingValues,
    onBack: () -> Unit,
    onConversation: (String) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { CommunityRepository(context) }
    var loading by remember { mutableStateOf(true) }
    var conversations by remember { mutableStateOf<List<CommunityConversation>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(CommunityTab.CONVERSATIONS) }
    var acceptingId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(repository) {
        onDispose { repository.close() }
    }

    LaunchedEffect(Unit) {
        repository.touchLastSeen()
        while (true) {
            conversations = repository.loadConversations()
            loading = false
            delay(5_000)
        }
    }

    val userId = viewModel.uiState.value.user?.id
    val accepted = conversations.filter { it.status == "accepted" }
    val requests = conversations.filter {
        it.status == "pending" && it.requestedBy != null && it.requestedBy != userId
    }
    val source = if (selectedTab == CommunityTab.CONVERSATIONS) accepted else requests
    val filtered = source.filter { conversation ->
        val needle = query.trim().lowercase()
        needle.isBlank() ||
            conversation.otherProfile?.displayName.orEmpty().lowercase().contains(needle) ||
            conversation.lastMessagePreview.orEmpty().lowercase().contains(needle)
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Groups,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(21.dp),
                        )
                        Text(
                            "Comunidade",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
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
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(80) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Buscar conversas e mensagens") },
                shape = RoundedCornerShape(24.dp),
            )

            TabRow(selectedTabIndex = if (selectedTab == CommunityTab.CONVERSATIONS) 0 else 1) {
                Tab(
                    selected = selectedTab == CommunityTab.CONVERSATIONS,
                    onClick = { selectedTab = CommunityTab.CONVERSATIONS },
                    text = { Text("Conversas (${accepted.size})") },
                )
                Tab(
                    selected = selectedTab == CommunityTab.REQUESTS,
                    onClick = { selectedTab = CommunityTab.REQUESTS },
                    text = { Text("Solicitações (${requests.size})") },
                )
            }

            when {
                loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    CircularProgressIndicator(Modifier.padding(top = 44.dp))
                }

                filtered.isEmpty() -> CommunityEmptyState(
                    message = when {
                        query.isNotBlank() -> "Nada encontrado"
                        selectedTab == CommunityTab.REQUESTS -> "Nenhuma solicitação pendente"
                        else -> "Nenhuma conversa ainda"
                    },
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.id }) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            currentUserId = userId,
                            requestMode = selectedTab == CommunityTab.REQUESTS,
                            accepting = acceptingId == conversation.id,
                            onOpen = { onConversation(conversation.id) },
                            onAccept = {
                                acceptingId = conversation.id
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    if (repository.acceptConversation(conversation.id)) {
                                        conversations = repository.loadConversations()
                                        selectedTab = CommunityTab.CONVERSATIONS
                                    }
                                    acceptingId = null
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: CommunityConversation,
    currentUserId: String?,
    requestMode: Boolean,
    accepting: Boolean,
    onOpen: () -> Unit,
    onAccept: () -> Unit,
) {
    val profile = conversation.otherProfile
    val online = isCommunityOnline(profile?.lastSeenAt)
    val meSentLast = conversation.lastMessageSender == currentUserId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !requestMode) { onOpen() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.size(52.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!profile?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile?.avatarUrl,
                            contentDescription = profile?.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (online) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E)),
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (conversation.pinned) {
                        Icon(
                            Icons.Default.PushPin,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        profile?.displayName ?: "Usuário AfiliShop",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = if (conversation.pinned) 5.dp else 0.dp),
                    )
                    Text(
                        communityTimeAgo(conversation.lastMessageAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 3.dp),
                ) {
                    Text(
                        text = if (requestMode) {
                            "Quer conversar com você"
                        } else if (meSentLast) {
                            "Você: ${conversation.lastMessagePreview ?: "…"}"
                        } else {
                            conversation.lastMessagePreview ?: "Toque para abrir"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (conversation.unread > 0 && !meSentLast) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (conversation.unread > 0 && !meSentLast) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (conversation.unread > 0 && !meSentLast) {
                        Text(
                            if (conversation.unread > 99) "99+" else conversation.unread.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                if (requestMode) {
                    Button(
                        onClick = onAccept,
                        enabled = !accepting,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(if (accepting) "Aceitando…" else "Aceitar conversa")
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityEmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 64.dp),
        ) {
            Icon(
                Icons.Default.Inbox,
                null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(42.dp),
            )
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun isCommunityOnline(lastSeen: String?): Boolean {
    if (lastSeen.isNullOrBlank()) return false
    return runCatching {
        val instant = OffsetDateTime.parse(lastSeen).toInstant()
        ChronoUnit.SECONDS.between(instant, Instant.now()) in 0..90
    }.getOrDefault(false)
}

private fun communityTimeAgo(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return runCatching {
        val instant = OffsetDateTime.parse(value).toInstant()
        val minutes = ChronoUnit.MINUTES.between(instant, Instant.now()).coerceAtLeast(0)
        when {
            minutes < 1 -> "agora"
            minutes < 60 -> "${minutes}m"
            minutes < 1_440 -> "${minutes / 60}h"
            minutes < 10_080 -> "${minutes / 1_440}d"
            else -> DateTimeFormatter.ofPattern("dd/MM").withZone(ZoneId.systemDefault()).format(instant)
        }
    }.getOrDefault("")
}
