package com.afilishop.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afilishop.app.ui.AfiliShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: AfiliShopViewModel, padding: PaddingValues, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isAdmin) { if (state.isAdmin) viewModel.loadAdminData() }
    Scaffold(modifier = Modifier.padding(padding), topBar = { TopAppBar(title = { Text("Administração") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } }) }) { inner ->
        if (!state.isAdmin) {
            Text("Acesso restrito a administradores autorizados.", Modifier.padding(inner).padding(20.dp))
            return@Scaffold
        }
        val admin = state.adminData
        LazyColumn(Modifier.fillMaxSize().padding(inner).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Painel protegido", style = MaterialTheme.typography.headlineSmall)
                        Text("A autorização foi confirmada pelo backend; operações sensíveis continuam server-side.")
                    }
                }
            }
            item { Button(onClick = viewModel::loadAdminData, modifier = Modifier.fillMaxWidth()) { Text("Atualizar painel") } }
            item { Text("Produtos: ${admin?.stats?.products ?: state.home.products.size}  •  Vídeos ativos: ${admin?.stats?.videos ?: state.videos.size}") }
            item { Text("Usuários consultados: ${admin?.stats?.users ?: 0}  •  Lives ativas: ${admin?.stats?.activeLives ?: 0}") }
            item { Text("Notificações não lidas: ${admin?.stats?.unreadNotifications ?: state.notifications.count { !it.isRead }}") }
            item { Text("Vídeos recentes", style = MaterialTheme.typography.titleLarge) }
            items(state.videos.take(20), key = { it.id }) { video ->
                Card(modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(video.caption ?: video.id); Text("Curtidas: ${video.likesCount ?: 0} • Visualizações: ${video.viewsCount}"); Button(onClick = { viewModel.adminDeactivateVideo(video.id) }) { Text("Desativar publicação") } } }
            }
        }
    }
}
