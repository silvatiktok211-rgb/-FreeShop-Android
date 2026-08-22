package com.afilishop.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
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
import com.afilishop.app.model.Product
import com.afilishop.app.model.SocialVideo
import com.afilishop.app.ui.AfiliShopViewModel
import com.afilishop.app.ui.components.ProductCard

private val Orange = Color(0xFFFF641F)
private val Page = Color(0xFFFFFDF9)
private val Ink = Color(0xFF171717)
private val artwork = mapOf(
    "automotivo" to "automotivo.webp", "bebes" to "bebes-infantil.webp",
    "beleza-e-cuidados" to "beleza-cuidados.webp", "brinquedos" to "brinquedos.webp",
    "calcados" to "moda.webp", "casa-e-cozinha" to "casa-cozinha.webp",
    "casa-e-decoracao" to "casa-decoracao.webp", "eletronicos" to "eletronicos.webp",
    "esportes" to "esportes.webp", "games" to "games.webp", "informatica" to "informatica.webp",
    "pet-shop" to "pet-shop.webp", "smartphones" to "smartphones.webp",
)

@Composable
fun HomeScreen(
    viewModel: AfiliShopViewModel, padding: PaddingValues, onProduct: (String) -> Unit,
    onSearch: () -> Unit, onNotifications: () -> Unit, onVideos: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val premium = state.home.products.filter { (it.priorityTier ?: 0) > 0 }.ifEmpty { state.home.products }.take(8)
    Column(Modifier.fillMaxSize().padding(padding).background(Page)) {
        HomeHeader(onSearch, onNotifications, state.notifications.size)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(3) }) { val b = state.home.banners.firstOrNull(); Banner(b?.mediaUrl, b?.title ?: "Achados AfiliShop") }
            item(span = { GridItemSpan(3) }) {
                SectionHeader("Categorias", "Ver todas")
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(state.home.categories.take(12), key = { it.id }) { c -> CategoryTile(c) { viewModel.filterCategory(c.id) } }
                }
            }
            item(span = { GridItemSpan(3) }) { QuickLinks() }
            item(span = { GridItemSpan(3) }) {
                SectionHeader("♛  Produtos Premium", "Ver todos", "Produtos publicados por afiliados VIP")
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(premium, key = { it.id }) { p -> Box(Modifier.width(148.dp)) {
                        ProductCard(p, { onProduct(p.id) }, favorite = p.id in state.favoriteIds,
                            onFavorite = if (state.user != null) ({ viewModel.toggleFavorite(p.id) }) else null,
                            onBuy = { openProduct(p, viewModel, context, onProduct) })
                    } }
                }
            }
            if (state.videos.isNotEmpty()) item(span = { GridItemSpan(3) }) {
                SectionHeader("▶  Vídeos dos Premium", "Ver todos", "Assista aos melhores achados dos nossos afiliados VIP", onVideos)
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.videos.take(8), key = { it.id }) { VideoTile(it, onVideos) }
                }
            }
            item(span = { GridItemSpan(3) }) { Text("Para você", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 4.dp)) }
            items(state.home.products, key = { it.id }) { p -> ProductCard(
                p, { onProduct(p.id) }, Modifier.padding(horizontal = 2.dp), p.id in state.favoriteIds,
                if (state.user != null) ({ viewModel.toggleFavorite(p.id) }) else null,
                { openProduct(p, viewModel, context, onProduct) },
            ) }
        }
    }
}

@Composable private fun HomeHeader(onSearch: () -> Unit, onNotifications: () -> Unit, count: Int) {
    Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 9.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(BuildConfig.API_BASE_URL.trimEnd('/') + "/logo.png", "AfiliShop", Modifier.size(42.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        Surface(Modifier.weight(1f).height(48.dp).padding(start = 7.dp).clickable(onClick = onSearch), CircleShape, Color(0xFFF7F7F7), shadowElevation = 2.dp) {
            Row(Modifier.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Search, null, tint = Color(0xFF62656E)); Text("Buscar produtos, categorias ou usuários", maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF656872), modifier = Modifier.padding(start = 8.dp)) }
        }
        IconButton(onClick = {}) { Icon(Icons.Default.DarkMode, "Tema") }
        Box { IconButton(onClick = onNotifications) { Icon(Icons.Outlined.Notifications, "Notificações") }
            if (count > 0) Surface(Modifier.align(Alignment.TopEnd), CircleShape, Orange) { Text(count.coerceAtMost(9).toString(), color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)) }
        }
    }
}

@Composable private fun Banner(url: String?, title: String) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(148.dp), shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE9E3))) {
        if (!url.isNullOrBlank()) AsyncImage(url, title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize().background(Orange), contentAlignment = Alignment.Center) { Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable private fun SectionHeader(title: String, action: String, subtitle: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 9.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink); if (subtitle != null) Text(subtitle, color = Color(0xFF686A73), style = MaterialTheme.typography.bodySmall) }
        Text(action, color = Orange, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onAction))
    }
}

@Composable private fun CategoryTile(category: Category, onClick: () -> Unit) {
    Column(Modifier.width(68.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(Modifier.size(60.dp), CircleShape, Color(0xFFFFF8F1), border = BorderStroke(1.dp, Color(0xFFEAE5DF))) { AsyncImage(categoryUrl(category), category.name, Modifier.padding(4.dp).fillMaxSize(), contentScale = ContentScale.Fit) }
        Text(category.name, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable private fun QuickLinks() {
    val links = listOf(Triple("Mais vendidos", "pelos Premium", Icons.Default.LocalFireDepartment), Triple("Destaques da", "comunidade", Icons.Default.StarBorder), Triple("Frete grátis", "em produtos selecionados", Icons.Default.LocalShipping), Triple("Afiliados", "Verificados", Icons.Default.VerifiedUser))
    val colors = listOf(Color(0xFFFFEED7), Color(0xFFFFF8C9), Color(0xFFD8F8EC), Color(0xFFEDE4FF))
    Column(Modifier.padding(horizontal = 10.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { links.chunked(2).forEach { pair -> Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { pair.forEach { link ->
        val i = links.indexOf(link); Surface(Modifier.weight(1f).height(76.dp), RoundedCornerShape(22.dp), Color.White, shadowElevation = 2.dp) { Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(42.dp), CircleShape, colors[i]) { Box(contentAlignment = Alignment.Center) { Icon(link.third, null, tint = if (i == 2) Color(0xFF00A979) else if (i == 3) Color(0xFF8B2BE2) else Orange) } }
            Column(Modifier.padding(start = 8.dp)) { Text(link.first, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(link.second, color = Color(0xFF686A73), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } }
    } } } }
}

@Composable private fun VideoTile(video: SocialVideo, onClick: () -> Unit) {
    Card(Modifier.width(126.dp).height(202.dp).clickable(onClick = onClick), RoundedCornerShape(20.dp)) { Box(Modifier.fillMaxSize()) {
        AsyncImage(video.thumbnailUrl ?: video.productImage, video.caption, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Surface(Modifier.align(Alignment.TopStart).padding(7.dp), CircleShape, Color.Black.copy(alpha = .68f)) { Text("▶ ${views(video.viewsCount)}", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)) }
        Surface(Modifier.align(Alignment.Center), CircleShape, Color.White.copy(alpha = .9f)) { Icon(Icons.Default.PlayArrow, "Reproduzir", Modifier.padding(11.dp).size(24.dp), tint = Orange) }
        Text("@${video.profile?.displayName ?: "Afilishopp"}  ✓", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.align(Alignment.BottomStart).padding(9.dp))
    } }
}

private fun views(v: Int) = if (v >= 1000) "${v / 1000}.${(v % 1000) / 100}K" else v.toString()
private fun categoryUrl(c: Category): String? { val raw = c.slug?.let(artwork::get)?.let { "/categories/$it" } ?: c.icon ?: return null; return if (raw.startsWith("http")) raw else BuildConfig.API_BASE_URL.trimEnd('/') + "/" + raw.trimStart('/') }
private fun openProduct(p: Product, vm: AfiliShopViewModel, c: Context, detail: (String) -> Unit) { val href = p.originalAffiliateUrl ?: p.affiliateUrl; if (href.isNullOrBlank()) detail(p.id) else { vm.recordProductClick(p.id); runCatching { c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(href))) }.onFailure { detail(p.id) } } }
