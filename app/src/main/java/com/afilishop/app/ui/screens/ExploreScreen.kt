package com.afilishop.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afilishop.app.data.ProductSearchRepository
import com.afilishop.app.model.Product
import com.afilishop.app.ui.AfiliShopViewModel
import com.afilishop.app.ui.components.ProductCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: AfiliShopViewModel,
    padding: PaddingValues,
    onProduct: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val repository = remember { ProductSearchRepository() }

    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    DisposableEffect(repository) {
        onDispose { repository.close() }
    }

    LaunchedEffect(query, selectedCategoryId) {
        loading = true
        if (query.isNotBlank()) delay(280)
        results = repository.search(query = query, categoryId = selectedCategoryId)
        loading = false
    }

    fun openProductOffer(product: Product) {
        val link = product.originalAffiliateUrl ?: product.affiliateUrl
        if (link.isNullOrBlank()) {
            onProduct(product.id)
            return
        }
        viewModel.recordProductClick(product.id)
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        }.onFailure {
            onProduct(product.id)
        }
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Explorar", fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Produtos e ofertas da AfiliShop",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                    .padding(horizontal = 12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, "Limpar busca")
                        }
                    }
                },
                placeholder = { Text("Buscar produtos e ofertas") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )

            if (state.home.categories.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(top = 10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("Todos") },
                        )
                    }
                    items(state.home.categories.take(12), key = { it.id }) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = {
                                selectedCategoryId = if (selectedCategoryId == category.id) null else category.id
                            },
                            label = { Text(category.name) },
                        )
                    }
                }
            }

            Text(
                text = if (query.isNotBlank()) {
                    "Resultados para “$query”"
                } else if (selectedCategoryId != null) {
                    state.home.categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Resultados"
                } else {
                    "Explorar produtos"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            )

            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        CircularProgressIndicator(Modifier.padding(top = 32.dp))
                    }
                }

                results.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text("Nenhum produto encontrado", fontWeight = FontWeight.Bold)
                                Text(
                                    "Tente buscar por termos diferentes ou escolha outra categoria.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(165.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(results, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                onClick = { onProduct(product.id) },
                                favorite = state.favoriteIds.contains(product.id),
                                onFavorite = if (state.user != null) {
                                    { viewModel.toggleFavorite(product.id) }
                                } else null,
                                onBuy = { openProductOffer(product) },
                            )
                        }
                    }
                }
            }
        }
    }
}
