package com.afilishop.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afilishop.app.ui.AfiliShopViewModel
import com.afilishop.app.ui.components.AgoraLiveView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(viewModel: AfiliShopViewModel, padding: PaddingValues, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var selectedLiveId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { viewModel.loadLives() }
    Scaffold(modifier = Modifier.padding(padding), topBar = { TopAppBar(title = { Text("Lives") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } }) }) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            if (state.user != null) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Começar uma live", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Título") }, singleLine = true)
                        Button(onClick = { viewModel.startLive(title); title = "" }, modifier = Modifier.fillMaxWidth()) { Text("Iniciar live") }
                    }
                }
            }
            state.activeLive?.let { active -> item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sessão ativa", style = MaterialTheme.typography.titleLarge)
                        Text("Canal: ${active.channel ?: "—"}")
                        if (!active.appId.isNullOrBlank() && !active.channel.isNullOrBlank() && active.uid != null) AgoraLiveView(appId = active.appId, token = active.token, channel = active.channel, uid = active.uid, isBroadcaster = active.live?.hostId == state.user?.id, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                        Text(if (active.token.isNullOrBlank()) "Token Agora pendente de configuração server-side." else "Token Agora emitido com segurança.")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            active.live?.id?.let { id -> Button(onClick = { viewModel.leaveLive(id) }, modifier = Modifier.weight(1f)) { Text("Sair") } }
                            active.live?.hostId?.let { host -> if (host == state.user?.id) Button(onClick = { active.live.id.let(viewModel::endLive) }, modifier = Modifier.weight(1f)) { Text("Encerrar") } }
                        }
                    }
                }
            } }
            if (state.liveRooms.isEmpty()) item { Text("Nenhuma live acontecendo agora.") }
            items(state.liveRooms, key = { it.id }) { room ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        Icon(Icons.Default.LiveTv, null, tint = MaterialTheme.colorScheme.primary)
                        Text(room.title ?: "Live AfiliShop", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                        Text("${room.viewerCount ?: 0} espectadores", style = MaterialTheme.typography.labelMedium)
                        Button(onClick = { selectedLiveId = room.id; viewModel.joinLive(room.id) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), enabled = state.activeLive == null) { Text("Entrar na live") }
                    }
                }
            }
            if (state.liveGifts.isNotEmpty()) {
                item { Text("Presentes disponíveis", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
                items(state.liveGifts, key = { it.id }) { gift ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${gift.emoji ?: "Presente"}  ${gift.name} — ${gift.costPoints} pontos")
                        if (selectedLiveId != null) Button(onClick = { viewModel.sendLiveGift(selectedLiveId!!, gift.id) }) { Text("Enviar") }
                    }
                }
            }
            state.liveMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        }
    }
}
