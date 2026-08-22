package com.afilishop.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afilishop.app.data.AccountSettingsRepository
import com.afilishop.app.data.AccountSettingsSnapshot
import com.afilishop.app.ui.AfiliShopViewModel
import com.afilishop.app.ui.components.PushRegistrationCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AfiliShopViewModel,
    padding: PaddingValues,
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onLives: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val repository = remember { AccountSettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(AccountSettingsSnapshot()) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf(false) }

    suspend fun reloadSettings() {
        loading = true
        settings = repository.load()
        loading = false
    }

    LaunchedEffect(state.user?.id) {
        if (state.user != null) reloadSettings() else loading = false
    }
    DisposableEffect(repository) { onDispose { repository.close() } }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sair da conta?") },
            text = { Text("Você poderá entrar novamente a qualquer momento.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.signOut()
                    onSignedOut()
                }) { Text("Sair") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") } },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!deleting) showDeleteDialog = false },
            title = { Text("Excluir conta definitivamente?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Esta ação remove sua conta e não pode ser desfeita. Digite EXCLUIR para confirmar.")
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        label = { Text("Digite EXCLUIR") },
                        singleLine = true,
                        enabled = !deleting,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = deleteConfirmText.trim().equals("EXCLUIR", ignoreCase = true) && !deleting,
                    onClick = {
                        scope.launch {
                            deleting = true
                            repository.deleteAccount()
                                .onSuccess {
                                    showDeleteDialog = false
                                    viewModel.signOut()
                                    onSignedOut()
                                }
                                .onFailure { message = it.message ?: "Não foi possível excluir a conta." }
                            deleting = false
                        }
                    },
                ) { Text(if (deleting) "Excluindo…" else "Excluir conta") }
            },
            dismissButton = {
                TextButton(enabled = !deleting, onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            },
        )
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            TopAppBar(
                title = { Text("Configurações e privacidade") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } },
            )
        },
    ) { inner ->
        if (loading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(inner),
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator(modifier = Modifier.padding(24.dp)) }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { SectionTitle("Conta") }
            item {
                SettingsCard {
                    SettingsNavigationRow(Icons.Default.Edit, "Editar perfil", "Foto, nome e biografia", onProfile)
                    SettingsNavigationRow(
                        Icons.Default.Security,
                        "Conta e segurança",
                        "Senha, acesso e exclusão da conta",
                        onClick = {
                            state.user?.email?.let(viewModel::requestPasswordReset)
                            message = "Enviamos a solicitação de redefinição para seu e-mail."
                        },
                    )
                }
            }

            item { SectionTitle("Privacidade") }
            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text("Status de atividade") },
                        supportingContent = { Text("Permitir que outras pessoas vejam quando você está ativo") },
                        leadingContent = { Icon(Icons.Default.Visibility, null) },
                        trailingContent = {
                            Switch(
                                checked = settings.activityStatusEnabled,
                                onCheckedChange = { enabled ->
                                    val previous = settings
                                    settings = settings.copy(activityStatusEnabled = enabled)
                                    scope.launch {
                                        if (!repository.setActivityStatus(enabled)) {
                                            settings = previous
                                            message = "Não foi possível atualizar o status de atividade."
                                        }
                                    }
                                },
                            )
                        },
                    )
                    if (settings.blockedAccounts.isEmpty()) {
                        ListItem(
                            headlineContent = { Text("Contas bloqueadas") },
                            supportingContent = { Text("Nenhuma conta bloqueada") },
                            leadingContent = { Icon(Icons.Default.Block, null) },
                        )
                    }
                }
            }
            if (settings.blockedAccounts.isNotEmpty()) {
                item { Text("Contas bloqueadas", style = MaterialTheme.typography.titleSmall) }
                items(settings.blockedAccounts, key = { it.id }) { blocked ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(blocked.displayName) },
                            supportingContent = { Text("Usuário bloqueado") },
                            leadingContent = { Icon(Icons.Default.Block, null) },
                            trailingContent = {
                                TextButton(onClick = {
                                    scope.launch {
                                        if (repository.unblockUser(blocked.id)) {
                                            settings = settings.copy(
                                                blockedAccounts = settings.blockedAccounts.filterNot { it.id == blocked.id },
                                            )
                                            message = "Usuário desbloqueado."
                                        } else message = "Não foi possível desbloquear o usuário."
                                    }
                                }) { Text("Desbloquear") }
                            },
                        )
                    }
                }
            }

            item { SectionTitle("Preferências") }
            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text("Promoções") },
                        supportingContent = { Text("Receber novidades e campanhas da AfiliShop") },
                        leadingContent = { Icon(Icons.Default.Notifications, null) },
                        trailingContent = {
                            Switch(
                                checked = settings.allowPromo,
                                onCheckedChange = { enabled ->
                                    val previous = settings
                                    val next = settings.copy(allowPromo = enabled)
                                    settings = next
                                    scope.launch {
                                        if (!repository.setNotificationPreferences(next.allowPromo, next.allowPriceAlerts)) {
                                            settings = previous
                                            message = "Não foi possível atualizar as notificações."
                                        }
                                    }
                                },
                            )
                        },
                    )
                    ListItem(
                        headlineContent = { Text("Alertas de preço") },
                        supportingContent = { Text("Avisar quando produtos acompanhados mudarem de preço") },
                        leadingContent = { Icon(Icons.Default.Notifications, null) },
                        trailingContent = {
                            Switch(
                                checked = settings.allowPriceAlerts,
                                onCheckedChange = { enabled ->
                                    val previous = settings
                                    val next = settings.copy(allowPriceAlerts = enabled)
                                    settings = next
                                    scope.launch {
                                        if (!repository.setNotificationPreferences(next.allowPromo, next.allowPriceAlerts)) {
                                            settings = previous
                                            message = "Não foi possível atualizar os alertas de preço."
                                        }
                                    }
                                },
                            )
                        },
                    )
                    SettingsNavigationRow(
                        Icons.Default.Palette,
                        "Aparência",
                        "O app acompanha o tema claro ou escuro do aparelho",
                        onClick = {},
                        showChevron = false,
                    )
                }
            }

            item { PushRegistrationCard(viewModel) }

            item { SectionTitle("Recursos") }
            item {
                SettingsCard {
                    SettingsNavigationRow(Icons.Default.LiveTv, "Lives", "Entrar, transmitir e enviar presentes", onLives)
                }
            }

            message?.let { value ->
                item {
                    Text(
                        value,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    )
                }
            }
            state.authMessage?.let { value ->
                item {
                    Text(
                        value,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    )
                }
            }

            if (state.user != null) {
                item {
                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Logout, null)
                        Text("Sair da conta", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                item {
                    TextButton(
                        onClick = {
                            deleteConfirmText = ""
                            showDeleteDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.DeleteForever, null)
                        Text("Excluir conta", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        content = { Column { content() } },
    )
}

@Composable
private fun SettingsNavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    showChevron: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = { Icon(icon, null) },
        trailingContent = { if (showChevron) Icon(Icons.Default.ChevronRight, null) },
        modifier = if (showChevron) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth(),
    )
}
