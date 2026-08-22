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
import com.afilishop.app.ui.theme.AfiliColors

private val Orange = AfiliColors.Orange
private val Page = AfiliColors.Page
private val Ink = AfiliColors.Ink
private val artwork = mapOf(
    "automotivo" to "automotivo.webp",
    "bebes" to "bebes-infantil.webp", "moda-bebes-e-infantil" to "bebes-infantil.webp",
    "beleza" to "beleza-cuidados.webp", "beleza-e-cuidados" to "beleza-cuidados.webp",
    "perfumaria" to "beleza-cuidados.webp", "saude" to "saude.webp",
    "brinquedos" to "brinquedos.webp",
    "calcados" to "moda.webp", "moda" to "moda.webp", "moda-feminina" to "moda.webp",
    "moda-masculina" to "moda.webp", "malas-e-bolsas" to "moda.webp",
    "casa" to "casa-cozinha.webp", "casa-e-cozinha" to "casa-cozinha.webp",
    "casa-e-decoracao" to "casa-decoracao.webp",
    "eletronicos" to "eletronicos.webp", "smartphones" to "smartphones.webp",
    "esportes" to "esportes.webp", "ferramentas" to "ferramentas.webp",
    "games" to "games.webp", "video-gamer" to "games.webp",
    "informatica" to "informatica.webp", "livros" to "livros.webp",
    "material-escolar" to "material-escolar.webp", "pet-shop" to "pet-shop.webp",
    "ofertas" to "promocoes-ofertas.webp", "ofertas-do-dia" to "promocoes-ofertas.webp",
    "promocoes" to "promocoes-ofertas.webp", "outros" to "outros.webp",
)

@Composable
fun HomeScreen(
    viewModel: AfiliShopViewModel, padding: PaddingValues, onProduct: (String) -> Unit,
    onSearch: () -> Unit, onNotifications: () -> Unit, onVideos: () -> Unit,
    onSignUp: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val premium = state.home.products.filter { (it.priorityTier ?: 0) > 0 }.ifEmpty { state.home.products }.take(8)
    Column(Modifier.fillMaxSize().padding(padding).background(Page)) {
        HomeHeader(onSearch, onNotifications, state.notifications.count { !it.isRead })
        LazyVerticalGrid(
            columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 112.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(3) }) { val b = state.home.banners.firstOrNull(); Banner(b?.mediaUrl, b?.title ?: "Achados AfiliShop") }
            item(span = { GridItemSpan(3) }) {
                Column {
                    SectionHeader("Categorias", "Ver todas", onAction = onSearch)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.home.categories.take(12), key = { it.id }) { c -> CategoryTile(c) { viewModel.filterCategory(c.id) } }
                    }
                }
            }
            item(span = { GridItemSpan(3) }) { QuickLinks() }
            if (state.home.products.isEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    CatalogStatus(
                        loading = state.isLoading,
                        message = state.error,
                        onRetry = viewModel::refresh,
                    )
                }
            } else {
                item(span = { GridItemSpan(3) }) {
                    Column {
                        SectionHeader("♛  Produtos Premium", "Ver todos", "Produtos publicados por afiliados VIP")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(premium, key = { it.id }) { p -> Box(Modifier.width(168.dp)) {
                                ProductCard(p, { onProduct(p.id) }, favorite = p.id in state.favoriteIds,
                                    onFavorite = if (state.user != null) ({ viewModel.toggleFavorite(p.id) }) else null,
                                    onBuy = { openProduct(p, viewModel, context, onProduct) })
                            } }
                        }
                    }
                }
            }
            if (state.videos.isNotEmpty()) item(span = { GridItemSpan(3) }) {
                Column {
                    SectionHeader("▶  Vídeos dos Premium", "Ver todos", "Assista aos melhores achados dos nossos afiliados VIP", onVideos)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.videos.take(8), key = { it.id }) { VideoTile(it, onVideos) }
                    }
                }
            }
            if (state.user == null) item(span = { GridItemSpan(3) }) {
                WelcomeCard(onSignUp)
            }
            if (state.home.products.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) { Text("Para você", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)) }
                items(state.home.products, key = { it.id }) { p -> ProductCard(
                    p, { onProduct(p.id) }, Modifier.padding(horizontal = 2.dp), p.id in state.favoriteIds,
                    if (state.user != null) ({ viewModel.toggleFavorite(p.id) }) else null,
                    { openProduct(p, viewModel, context, onProduct) },
                    compact = true,
                ) }
            }
        }
    }
}

@Composable
private fun CatalogStatus(loading: Boolean, message: String?, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(116.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            loading -> CircularProgressIndicator(color = Orange)
            message != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(message, color = Color(0xFF686A73), textAlign = TextAlign.Center)
                TextButton(onClick = onRetry) { Text("Tentar novamente", color = Orange, fontWeight = FontWeight.Bold) }
            }
            else -> Text("Nenhum produto disponível agora.", color = Color(0xFF686A73))
        }
    }
}

@Composable private fun HomeHeader(onSearch: () -> Unit, onNotifications: () -> Unit, count: Int) {
    Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(BuildConfig.API_BASE_URL.trimEnd('/') + "/icon-192.png", "AfiliShop", Modifier.size(36.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        Surface(Modifier.weight(1f).height(40.dp).padding(start = 8.dp).clickable(onClick = onSearch), CircleShape, Color(0xFFF7F5F2), shadowElevation = 1.dp) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = Color(0xFF62656E)); Text("Buscar produtos, categorias ou usuários...", maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF656872), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp)) }
        }
        IconButton(onClick = {}, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.DarkMode, "Tema", Modifier.size(21.dp)) }
        Box { IconButton(onClick = onNotifications, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.Notifications, "Notificações", Modifier.size(22.dp)) }
            if (count > 0) Surface(Modifier.align(Alignment.TopEnd), CircleShape, Orange) { Text(count.coerceAtMost(9).toString(), color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)) }
        }
    }
}

@Composable private fun Banner(url: String?, title: String) {
    Card(Modifier.fillMaxWidth().height(148.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE9E3)), elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)) {
        if (!url.isNullOrBlank()) AsyncImage(url, title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize().background(Orange), contentAlignment = Alignment.Center) { Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable private fun SectionHeader(title: String, action: String, subtitle: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 9.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink); if (subtitle != null) Text(subtitle, color = Color(0xFF686A73), style = MaterialTheme.typography.bodySmall) }
        Text(action, color = Orange, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onAction))
    }
}

@Composable private fun CategoryTile(category: Category, onClick: () -> Unit) {
    Column(Modifier.width(68.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(Modifier.size(56.dp), CircleShape, Color(0xFFFFF8F1), border = BorderStroke(1.dp, Color(0xFFEAE5DF))) { AsyncImage(categoryUrl(category), category.name, Modifier.padding(4.dp).fillMaxSize(), contentScale = ContentScale.Fit) }
        Text(category.name, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable private fun QuickLinks() {
    val links = listOf(Triple("Mais vendidos", "pelos Premium", Icons.Default.LocalFireDepartment), Triple("Destaques da", "comunidade", Icons.Default.StarBorder), Triple("Frete grátis", "em produtos selecionados", Icons.Default.LocalShipping), Triple("Afiliados", "Verificados", Icons.Default.VerifiedUser))
    val colors = listOf(Color(0xFFFFEED7), Color(0xFFFFF8C9), Color(0xFFD8F8EC), Color(0xFFEDE4FF))
    Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { links.chunked(2).forEach { pair -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { pair.forEach { link ->
        val i = links.indexOf(link); Surface(Modifier.weight(1f).height(64.dp), RoundedCornerShape(16.dp), Color.White, shadowElevation = 2.dp, border = BorderStroke(1.dp, AfiliColors.Border)) { Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), RoundedCornerShape(12.dp), colors[i]) { Box(contentAlignment = Alignment.Center) { Icon(link.third, null, Modifier.size(20.dp), tint = if (i == 2) Color(0xFF00A979) else if (i == 3) Color(0xFF8B2BE2) else Orange) } }
            Column(Modifier.padding(start = 8.dp)) { Text(link.first, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(link.second, color = Color(0xFF686A73), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } }
    } } } }
}

@Composable private fun WelcomeCard(onSignUp: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF09090B)),
    ) {
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 28.dp, y = (-34).dp)
                    .size(136.dp)
                    .clip(CircleShape)
                    .background(Orange.copy(alpha = 0.20f)),
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Bem-vindo à AfiliShop!", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Crie sua conta para salvar favoritos e ganhar pontos.",
                    color = Color(0xFFA1A1AA),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(240.dp).padding(top = 4.dp),
                )
                Button(
                    onClick = onSignUp,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 7.dp),
                    modifier = Modifier.padding(top = 12.dp).height(36.dp),
                ) { Text("Crie sua conta", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable private fun VideoTile(video: SocialVideo, onClick: () -> Unit) {
    Card(Modifier.width(130.dp).height(202.dp).clickable(onClick = onClick), RoundedCornerShape(20.dp)) { Box(Modifier.fillMaxSize()) {
        AsyncImage(video.thumbnailUrl ?: video.productImage, video.caption, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Surface(Modifier.align(Alignment.TopStart).padding(7.dp), CircleShape, Color.Black.copy(alpha = .68f)) { Text("▶ ${views(video.viewsCount)}", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)) }
        Surface(Modifier.align(Alignment.Center), CircleShape, Color.White.copy(alpha = .9f)) { Icon(Icons.Default.PlayArrow, "Reproduzir", Modifier.padding(11.dp).size(24.dp), tint = Orange) }
        Text("@${video.profile?.displayName ?: "Afilishopp"}  ✓", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.align(Alignment.BottomStart).padding(9.dp))
    } }
}

private fun views(v: Int) = if (v >= 1000) "${v / 1000}.${(v % 1000) / 100}K" else v.toString()
private fun categoryUrl(c: Category): String? { val raw = c.slug?.let(artwork::get)?.let { "/categories/$it" } ?: c.icon ?: return null; return if (raw.startsWith("http")) raw else BuildConfig.API_BASE_URL.trimEnd('/') + "/" + raw.trimStart('/') }
private fun openProduct(p: Product, vm: AfiliShopViewModel, c: Context, detail: (String) -> Unit) { val href = p.originalAffiliateUrl ?: p.affiliateUrl; if (href.isNullOrBlank()) detail(p.id) else { vm.recordProductClick(p.id); runCatching { c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(href))) }.onFailure { detail(p.id) } } }
