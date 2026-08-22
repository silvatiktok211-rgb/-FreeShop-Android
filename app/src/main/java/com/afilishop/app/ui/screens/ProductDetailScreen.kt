package com.afilishop.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.afilishop.app.model.Product
import com.afilishop.app.ui.AfiliShopViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    viewModel: AfiliShopViewModel,
    id: String,
    padding: PaddingValues,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val product = state.selectedProduct
    val context = LocalContext.current
    val formatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    var comment by remember { mutableStateOf("") }

    LaunchedEffect(id) {
        viewModel.selectProduct(id)
        viewModel.loadProductExtras(id)
        viewModel.loadFavorites()
    }

    BackHandler { onBack() }

    val purchaseLink = product?.originalAffiliateUrl ?: product?.affiliateUrl

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    fun buy() {
        val url = purchaseLink ?: return
        viewModel.recordProductClick(id)
        openUrl(url)
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            TopAppBar(
                title = { Text("Produto", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(id) },
                        enabled = state.user != null,
                    ) {
                        Icon(
                            if (state.favoriteIds.contains(id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favoritar produto",
                            tint = if (state.favoriteIds.contains(id)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    purchaseLink?.let { link ->
                        IconButton(
                            onClick = {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, product?.title ?: "Produto AfiliShop")
                                            putExtra(Intent.EXTRA_TEXT, link)
                                        },
                                        "Compartilhar oferta",
                                    ),
                                )
                            },
                        ) { Icon(Icons.Default.Share, "Compartilhar") }
                    }
                },
            )
        },
        bottomBar = {
            if (product != null && !purchaseLink.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Preço final",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                product.price?.let(formatter::format) ?: "Consultar",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                            )
                        }
                        Button(onClick = ::buy, modifier = Modifier.weight(1.25f)) {
                            Text("Comprar agora")
                        }
                    }
                }
            }
        },
    ) { inner ->
        if (product == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center,
            ) {
                Text("Carregando produto…")
            }
            return@Scaffold
        }

        val gallery = remember(product.id, product.imageUrl, product.images) {
            buildList {
                product.imageUrl?.takeIf { it.isNotBlank() }?.let(::add)
                product.images.filter { it.isNotBlank() }.forEach(::add)
            }.distinct()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(bottom = if (purchaseLink.isNullOrBlank()) 24.dp else 96.dp),
        ) {
            item {
                ProductGallery(product = product, gallery = gallery)
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        product.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        product.price?.let { currentPrice ->
                            product.oldPrice?.takeIf { it > currentPrice }?.let { old ->
                                Text(
                                    formatter.format(old),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textDecoration = TextDecoration.LineThrough,
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                product.price?.let(formatter::format) ?: "Consultar preço",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            product.discountPercentage?.takeIf { it > 0 }?.let { discount ->
                                Text(
                                    "${discount.toInt()}% OFF",
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFDCFCE7))
                                        .padding(horizontal = 9.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }

                    if (product.freeShipping) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalShipping,
                                null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                "Frete grátis",
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 7.dp),
                            )
                        }
                    }

                    TrustRow()
                }
            }

            if (state.productOffers.isNotEmpty()) {
                item {
                    Text(
                        "Comparador de preços",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(
                    state.productOffers.sortedBy { it.price ?: Double.POSITIVE_INFINITY },
                    key = { it.id },
                ) { offer ->
                    val bestPrice = state.productOffers.mapNotNull { it.price }.minOrNull()
                    val isBest = offer.price != null && offer.price == bestPrice
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    (offer.store ?: "Loja").take(2).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(offer.store ?: "Oferta", fontWeight = FontWeight.SemiBold)
                                    if (isBest) {
                                        Text(
                                            "Melhor",
                                            color = Color(0xFF15803D),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(start = 8.dp),
                                        )
                                    }
                                }
                                Text(
                                    offer.price?.let(formatter::format) ?: "Consultar preço",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            IconButton(onClick = {
                                viewModel.recordProductClick(id)
                                openUrl(offer.url)
                            }) {
                                Icon(Icons.Default.OpenInNew, "Abrir oferta")
                            }
                        }
                    }
                }
            }

            if (!product.description.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Descrição do produto",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                product.description.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 9.dp),
                            )
                        }
                    }
                }
            }

            if (state.priceHistory.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Histórico recente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                state.priceHistory.take(5).joinToString("  •  ") { formatter.format(it.price) },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Comentários",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }

            items(state.productComments, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(item.profile?.displayName ?: "Usuário", fontWeight = FontWeight.SemiBold)
                        Text(item.content, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            if (state.user != null) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it.take(500) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Escreva um comentário") },
                            maxLines = 4,
                        )
                        Button(
                            onClick = {
                                viewModel.addProductComment(id, comment.trim())
                                comment = ""
                            },
                            enabled = comment.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) { Text("Comentar") }
                    }
                }
            }

            if (purchaseLink.isNullOrBlank()) {
                item {
                    Text(
                        "O link de compra deste produto ainda não está disponível.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductGallery(product: Product, gallery: List<String>) {
    if (gallery.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(330.dp).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text("Imagem indisponível", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { gallery.size })
    Box(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(340.dp),
        ) { page ->
            AsyncImage(
                model = gallery[page],
                contentDescription = "${product.title} — imagem ${page + 1}",
                modifier = Modifier.fillMaxSize().padding(14.dp),
                contentScale = ContentScale.Fit,
            )
        }

        if (gallery.size > 1) {
            Text(
                "${pagerState.currentPage + 1}/${gallery.size}",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun TrustRow() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrustItem(Icons.Default.Security, "Compra segura")
            TrustItem(Icons.Default.CheckCircle, "Oferta verificada")
        }
    }
}

@Composable
private fun TrustItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
