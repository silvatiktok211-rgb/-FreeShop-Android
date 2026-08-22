package com.afilishop.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Crown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.afilishop.app.BuildConfig
import com.afilishop.app.ui.AfiliShopViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AfiliShopViewModel,
    padding: PaddingValues,
    onUpload: () -> Unit,
    onSettings: (() -> Unit)? = null,
    onNotifications: (() -> Unit)? = null,
    publicUserId: String? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isOwn = publicUserId == null || publicUserId == state.user?.id
    val profileUserId = publicUserId ?: state.user?.id
    var editing by remember { mutableStateOf(false) }
    var displayName by remember(state.profile?.displayName) { mutableStateOf(state.profile?.displayName.orEmpty()) }
    var bio by remember(state.profile?.bio) { mutableStateOf(state.profile?.bio.orEmpty()) }

    LaunchedEffect(publicUserId, state.user?.id) {
        if (isOwn) viewModel.loadAccountData() else publicUserId?.let(viewModel::loadPublicProfile)
    }

    val profile = state.profile
    val shownName = profile?.displayName?.takeIf { it.isNotBlank() }
        ?: state.user?.email?.substringBefore('@')
        ?: "Criador AfiliShop"
    val handle = shownName
        .lowercase()
        .replace(" ", "")
        .replace(Regex("[^a-z0-9._]"), "")
        .ifBlank { "afilishop" }
    val ownVideos = state.videos.filter { it.userId == profileUserId }
    val totalLikes = ownVideos.sumOf { it.likesCount ?: 0 }
    val tier = state.subscription?.tier ?: 0
    val tierLabel = when {
        tier >= 3 -> "VIP"
        tier >= 2 -> "PRO"
        tier >= 1 -> "VERIFICADO"
        else -> "FREE"
    }

    fun shareProfile() {
        val url = "${BuildConfig.API_BASE_URL.trimEnd('/')}/perfil/${profileUserId.orEmpty()}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Perfil no AfiliShop")
            putExtra(Intent.EXTRA_TEXT, "Confira o perfil de $shownName no AfiliShop: $url")
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar perfil"))
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isOwn) "@$handle" else shownName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (isOwn && onNotifications != null) {
                        IconButton(onClick = onNotifications) {
                            Icon(Icons.Default.NotificationsNone, "Notificações")
                        }
                    }
                    if (isOwn && onSettings != null) {
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, "Configurações")
                        }
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF6A00)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!profile?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profile?.avatarUrl,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                Icons.Default.AccountCircle,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(92.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(shownName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "@$handle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = tierLabel,
                            color = if (tier >= 2) Color.White else Color(0xFFFF6A00),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (tier >= 2) Color(0xFF111827) else Color(0xFFFFF1E6))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                        if (tier >= 3) {
                            Icon(
                                Icons.Default.Crown,
                                contentDescription = "VIP",
                                tint = Color(0xFFFFB000),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        if (state.isAdmin) {
                            Text(
                                "Administrador",
                                color = Color(0xFFFF6A00),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    Text(
                        profile?.bio?.takeIf { it.isNotBlank() }
                            ?: if (isOwn) "Adicione uma bio para contar quem você é." else "",
                        textAlign = TextAlign.Center,
                        color = if (profile?.bio.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ProfileMetric("Seguindo", formatMetric(profile?.followingCount ?: 0))
                    ProfileMetric("Seguidores", formatMetric(profile?.followersCount ?: 0))
                    ProfileMetric("Curtidas", formatMetric(totalLikes))
                }
            }

            item {
                if (isOwn) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { editing = !editing },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Edit, null)
                            Text("Editar perfil", modifier = Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(
                            onClick = ::shareProfile,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Share, null)
                            Text("Compartilhar", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                } else if (state.user != null && publicUserId != null) {
                    Button(
                        onClick = { viewModel.toggleFollow(publicUserId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.isFollowingPublicProfile) "Deixar de seguir" else "Seguir")
                    }
                }
            }

            if (isOwn && editing) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("Editar perfil", style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it.take(50) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Nome de exibição") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = bio,
                                onValueChange = { bio = it.take(160) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Bio") },
                                supportingText = { Text("${bio.length}/160") },
                                maxLines = 4,
                            )
                            Button(
                                onClick = {
                                    viewModel.updateProfile(displayName.trim(), bio.trim())
                                    editing = false
                                },
                                enabled = displayName.trim().length >= 2,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Salvar alterações") }
                            state.accountMessage?.let {
                                Text(it, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Vídeos", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${ownVideos.size} publicações",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (ownVideos.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                if (isOwn) "Seu perfil ainda não tem vídeos publicados." else "Este perfil ainda não publicou vídeos.",
                                textAlign = TextAlign.Center,
                            )
                            if (isOwn) {
                                Button(onClick = onUpload, modifier = Modifier.padding(top = 12.dp)) {
                                    Text("Publicar primeiro vídeo")
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(ownVideos, key = { it.id }) { video ->
                            Card(
                                modifier = Modifier.size(width = 128.dp, height = 190.dp),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Box(Modifier.fillMaxSize()) {
                                    if (!video.thumbnailUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = video.thumbnailUrl,
                                            contentDescription = video.caption,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Box(
                                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center,
                                        ) { Text("Vídeo") }
                                    }
                                    Text(
                                        "♥ ${formatMetric(video.likesCount ?: 0)}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatMetric(value: Int): String = when {
    value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0).replace(".0", "")
    value >= 1_000 -> String.format(Locale.US, "%.1fk", value / 1_000.0).replace(".0", "")
    else -> value.toString()
}
