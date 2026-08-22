package com.afilishop.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afilishop.app.data.FavoriteProductsRepository
import com.afilishop.app.model.Product
import com.afilishop.app.ui.AfiliShopViewModel
import com.afilishop.app.ui.components.ProductCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: AfiliShopViewModel,
    padding: PaddingValues,
    onBack: () -> Unit,
    onProduct: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val repository = remember { FavoriteProductsRepository(context) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    DisposableEffect(repository) {
        onDispose { repository.close() }
    }

    LaunchedEffect(state.user?.id) {
        loading = true
        if (state.user != null) {
            viewModel.loadFavorites()
            products = repository.load()
        } else {
            products = emptyList()
        }
        loading = false
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Favoritos", fontWeight = FontWeight.Bold)
                        if (!loading && products.isNotEmpty()) {
                            Text(
                                "${products.size} ${if (products.size == 1) "produto salvo" else "produtos salvos"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
        when {
            loading -> Box(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.TopCenter,
            ) {
                CircularProgressIndicator(Modifier.padding(top = 44.dp))
            }

            state.user == null -> FavoritesEmpty(
                modifier = Modifier.fillMaxSize().padding(inner),
                title = "Entre para ver seus favoritos",
                subtitle = "Os produtos salvos ficam vinculados à sua conta AfiliShop.",
            )

            products.isEmpty() -> FavoritesEmpty(
                modifier = Modifier.fillMaxSize().padding(inner),
                title = "Nenhum favorito ainda",
                subtitle = "Toque no coração de um produto para encontrá-lo aqui depois.",
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(165.dp),
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onClick = { onProduct(product.id) },
                        favorite = true,
                        onFavorite = {
                            viewModel.toggleFavorite(product.id)
                            products = products.filterNot { it.id == product.id }
                        },
                        onBuy = { onProduct(product.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesEmpty(
    modifier: Modifier,
    title: String,
    subtitle: String,
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 64.dp),
        ) {
            Icon(
                Icons.Default.FavoriteBorder,
                null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            )
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
