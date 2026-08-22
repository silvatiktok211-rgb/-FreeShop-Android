package com.afilishop.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.afilishop.app.BuildConfig
import com.afilishop.app.model.Category
import com.afilishop.app.ui.AfiliShopViewModel
import com.afilishop.app.ui.components.ProductCard

private val CATEGORY_ARTWORK_BY_SLUG = mapOf(
    "automotivo" to "/categories/automotivo.webp",
    "bebes" to "/categories/bebes-infantil.webp",
    "beleza-e-cuidados" to "/categories/beleza-cuidados.webp",
    "brinquedos" to "/categories/brinquedos.webp",
    "calcados" to "/categories/moda.webp",
    "casa-e-cozinha" to "/categories/casa-cozinha.webp",
    "casa-e-decoracao" to "/categories/casa-decoracao.webp",
    "eletronicos" to "/categories/eletronicos.webp",
    "esportes" to "/categories/esportes.webp",
    "ferramentas" to "/categories/ferramentas.webp",
    "games" to "/categories/games.webp",
    "informatica" to "/categories/informatica.webp",
    "livros" to "/categories/livros.webp",
    "malas-e-bolsas" to "/categories/moda.webp",
    "material-escolar" to "/categories/material-escolar.webp",
    "moda-bebes-e-infantil" to "/categories/bebes-infantil.webp",
    "moda-feminina" to "/categories/moda.webp",
    "moda-masculina" to "/categories/moda.webp",
    "ofertas-do-dia" to "/categories/promocoes-ofertas.webp",
    "outros" to "/categories/outros.webp",
    "perfumaria" to "/categories/beleza-cuidados.webp",
    "pet-shop" to "/categories/pet-shop.webp",
    "promocoes" to "/categories/promocoes-ofertas.webp",
    "saude" to "/categories/saude.webp",
    "smartphones" to "/categories/smartphones.webp",
    "video-gamer" to "/categories/games.webp",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: AfiliShopViewModel, padding: PaddingValues, onProduct: (String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AfiliShop", fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Vitrine dos Afiliados Premium",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, "Atualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { inner ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(156.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            contentPadding = PaddingValues(
                top = inner.calculateTopPadding() + 6.dp,
                bottom = 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.home.banners.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    BannerCarouselPreview(
                        imageUrl = state.home.banners.first().mediaUrl,
                        title = state.home.banners.first().title ?: "Achados em destaque",
                        subtitle = state.home.banners.first().subtitle,
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) { WelcomeHero() }
            }

            if (state.home.categories.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Text("Categorias", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                CategoryTile(
                                    name = "Todas",
                                    imageUrl = null,
                                    fallback = "A",
                                    onClick = viewModel::clearCategoryFilter,
                                )
                            }
                            items(state.home.categories.take(12), key = { it.id }) { category ->
                                CategoryTile(
                                    name = category.name,
                                    imageUrl = categoryArtworkUrl(category),
                                    fallback = category.icon ?: category.name.take(1),
                                    onClick = { viewModel.filterCategory(category.id) },
                                )
                            }
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Ofertas para você", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Produtos publicados pelos criadores da AfiliShop",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(state.home.products, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onClick = { onProduct(product.id) },
                    favorite = product.id in state.favoriteIds,
                    onFavorite = if (state.user != null) {
                        { viewModel.toggleFavorite(product.id) }
                    } else null,
                    onBuy = {
                        val href = product.originalAffiliateUrl ?: product.affiliateUrl
                        if (href.isNullOrBlank()) {
                            onProduct(product.id)
                        } else {
                            viewModel.recordProductClick(product.id)
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(href)))
                            }.onFailure {
                                onProduct(product.id)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryTile(
    name: String,
    imageUrl: String?,
    fallback: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.size(width = 78.dp, height = 104.dp),
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (name == "Todas") {
                            Icon(Icons.Default.LocalOffer, null, tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Text(
                                fallback.take(2),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

private fun categoryArtworkUrl(category: Category): String? {
    val configured = category.slug?.let { CATEGORY_ARTWORK_BY_SLUG[it] }
    val raw = configured ?: category.icon ?: return null
    return when {
        raw.startsWith("http://") || raw.startsWith("https://") -> raw
        raw.startsWith("/") -> BuildConfig.API_BASE_URL.trimEnd('/') + raw
        else -> null
    }
}

@Composable
private fun WelcomeHero() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Descubra os melhores achados", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Ofertas, cupons e produtos publicados por criadores verificados.")
            }
            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BannerCarouselPreview(imageUrl: String?, title: String, subtitle: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(22.dp),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp),
                contentScale = ContentScale.Crop,
            )
        }
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}
