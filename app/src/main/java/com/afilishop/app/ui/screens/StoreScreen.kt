package com.afilishop.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.afilishop.app.ui.AfiliShopViewModel
import com.afilishop.app.ui.components.ProductCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(viewModel: AfiliShopViewModel, slug: String, padding: PaddingValues, onBack: () -> Unit, onProduct: (String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(slug) { viewModel.loadStore(slug) }
    Scaffold(modifier = Modifier.padding(padding), topBar = { TopAppBar(title = { Text(state.store?.name ?: "Loja") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } }) }) { inner ->
        if (state.store == null) {
            Text("Carregando loja…", Modifier.padding(inner).padding(20.dp))
        } else {
            Column(Modifier.fillMaxSize().padding(inner)) {
                state.store?.bannerUrl?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.fillMaxWidth().padding(12.dp)) }
                Text(state.store?.description.orEmpty(), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.storeProducts, key = { it.id }) { ProductCard(it, onClick = { onProduct(it.id) }) }
                }
            }
        }
    }
}
