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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.afilishop.app.ui.AfiliShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AfiliShopViewModel,
    padding: PaddingValues,
    onLogin: () -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onCommunity: () -> Unit,
    onLives: () -> Unit,
    onAdmin: () -> Unit,
    onFavorites: () -> Unit,
    onPoints: () -> Unit,
    onSettings: () -> Unit,
    onTerms: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.user?.id) {
        if (state.user != null) viewModel.loadAccountData()
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            TopAppBar(
                title = { Text("Conta", fontWeight = FontWeight.Bold) },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.user == null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Sua AfiliShop fica melhor com você dentro",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Salve produtos, siga criadores, participe da comunidade e use seus benefícios VIP.",
                            modifier = Modifier.padding(top = 7.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onLogin,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        ) {
                            Text("Entrar ou criar conta", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!state.profile?.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = state.profile?.avatarUrl,
                                        contentDescription = state.profile?.displayName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Text(
                                        (state.profile?.displayName ?: state.user?.email ?: "A")
                                            .take(1)
                                            .uppercase(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f).padding(start = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    state.profile?.displayName ?: "Usuário AfiliShop",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                state.user?.email?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                val subscription = state.subscription
                                if (subscription?.status == "active") {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFFFF1E6),
                                        modifier = Modifier.padding(top = 4.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.WorkspacePremium,
                                                null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(15.dp),
                                            )
                                            Text(
                                                "VIP ativo • ${subscription.slotsTotal ?: 0} buscas/dia",
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(start = 4.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            AccountMetric("Favoritos", state.favoriteIds.size.toString())
                            AccountMetric("Pontos", (state.points?.balance ?: 0).toString())
                            AccountMetric(
                                "Plano",
                                if (state.subscription?.status == "active") "VIP" else "Free",
                            )
                        }

                        OutlinedButton(
                            onClick = onProfile,
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                        ) {
                            Text("Ver meu perfil", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Text(
                "Sua AfiliShop",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column {
                    AccountMenuItem("Favoritos", Icons.Default.FavoriteBorder, onFavorites)
                    AccountMenuItem("Pontos e ranking", Icons.Default.StarOutline, onPoints)
                    AccountMenuItem("Notificações", Icons.Default.NotificationsNone, onNotifications)
                    AccountMenuItem("Comunidade e mensagens", Icons.Default.ChatBubbleOutline, onCommunity)
                    AccountMenuItem("Lives", Icons.Default.LiveTv, onLives)
                    AccountMenuItem("Termos de Uso", Icons.Default.Gavel, onTerms)
                    AccountMenuItem(
                        title = "Configurações e privacidade",
                        icon = Icons.Default.Settings,
                        onClick = onSettings,
                        subtitle = "Conta, privacidade, notificações e segurança",
                    )
                    if (state.isAdmin) {
                        AccountMenuItem(
                            "Painel administrativo",
                            Icons.Default.AdminPanelSettings,
                            onAdmin,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AccountMenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { text -> { Text(text) } },
        leadingContent = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
