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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
fun PointsScreen(viewModel: AfiliShopViewModel, padding: PaddingValues, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.user?.id) { viewModel.loadPoints() }
    Scaffold(modifier = Modifier.padding(padding), topBar = { TopAppBar(title = { Text("Pontos") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } }) }) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Icon(Icons.Default.Stars, null, tint = MaterialTheme.colorScheme.primary)
                        Text("${state.points?.balance ?: 0} pontos", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))
                        Text("Acompanhe seu saldo e as atividades que geraram pontos.")
                    }
                }
            }
            item { Text("Histórico", style = MaterialTheme.typography.titleLarge) }
            if (state.pointTransactions.isEmpty()) item { Text("Nenhuma transação encontrada.") }
            items(state.pointTransactions, key = { it.id }) { tx -> ListItem(headlineContent = { Text(if (tx.delta >= 0) "+${tx.delta} pontos" else "${tx.delta} pontos") }, supportingContent = { Text(tx.reason) }) }
        }
    }
}
