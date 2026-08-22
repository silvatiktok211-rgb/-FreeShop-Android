package com.afilishop.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afilishop.app.data.VipAiProduct
import com.afilishop.app.data.VipAiSearchResponse
import com.afilishop.app.data.VipAssistantRepository
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipAssistantScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { VipAssistantRepository(context) }
    val scope = rememberCoroutineScope()
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    var query by remember { mutableStateOf("") }
    var response by remember { mutableStateOf<VipAiSearchResponse?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(repository) {
        onDispose { repository.close() }
    }

    fun runSearch(expand: Boolean = false) {
        val term = query.trim()
        if (term.length < 2 || loading) return
        loading = true
        error = null
        scope.launch {
            repository.search(term, expand)
                .onSuccess { response = it }
                .onFailure { error = it.message ?: "Não foi possível pesquisar agora." }
            loading = false
        }
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Text("IA VIP", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("O que você quer encontrar?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Descreva o produto com marca, modelo, tamanho ou faixa de preço. A mesma IA da AfiliShop consulta o catálogo real do Mercado Livre.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(250) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ex.: TV Samsung 55 4K até R$ 3.000") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    minLines = 2,
                    maxLines = 4,
                )
            }

            item {
                Button(
                    onClick = { runSearch(false) },
                    enabled = query.trim().length >= 2 && !loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(Icons.Default.AutoAwesome, null)
                        Text("Buscar com IA", modifier = Modifier.padding(start = 7.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }

            error?.let { message ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }

            response?.usage?.let { usage ->
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Buscas hoje: ${usage.used}/${usage.limit}")
                            Text(
                                "${usage.remaining} restantes",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            response?.message?.takeIf { it.isNotBlank() }?.let { message ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(message)
                            if (response?.ok == true && response?.results?.size.orZero() < 5) {
                                OutlinedButton(
                                    onClick = { runSearch(true) },
                                    enabled = !loading,
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                ) {
                                    Text("Ampliar busca")
                                }
                            }
                        }
                    }
                }
            }

            val results = response?.results.orEmpty()
            if (response?.ok == true && results.isEmpty() && response?.message.isNullOrBlank()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhum produto exato encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(results, key = { it.id }) { product ->
                VipResultCard(
                    product = product,
                    currency = currency,
                    onOpen = {
                        product.permalink?.takeIf { it.isNotBlank() }?.let { url ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun VipResultCard(
    product: VipAiProduct,
    currency: NumberFormat,
    onOpen: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(14.dp)) {
            product.thumbnail?.takeIf { it.isNotBlank() }?.let { image ->
                AsyncImage(
                    model = image,
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxWidth().height(210.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            Text(
                product.title,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )

            product.originalPrice?.takeIf { it > product.price && product.price > 0 }?.let {
                Text(
                    currency.format(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Text(
                if (product.price > 0) currency.format(product.price) else "Preço a confirmar",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (product.freeShipping) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalShipping, null, tint = Color(0xFF15803D))
                        Text("Frete grátis", color = Color(0xFF15803D), modifier = Modifier.padding(start = 4.dp))
                    }
                }
                product.rating?.let { rating ->
                    Text("★ ${String.format(Locale("pt", "BR"), "%.1f", rating)}")
                }
            }

            product.seller?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "Vendedor: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Button(
                onClick = onOpen,
                enabled = !product.permalink.isNullOrBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Icon(Icons.Default.OpenInNew, null)
                Text("Ver anúncio", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

private fun Int?.orZero(): Int = this ?: 0
