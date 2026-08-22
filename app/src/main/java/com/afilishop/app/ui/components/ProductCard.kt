package com.afilishop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Crown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afilishop.app.model.Product
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    favorite: Boolean = false,
    onFavorite: (() -> Unit)? = null,
    onBuy: (() -> Unit)? = null,
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val tier = product.priorityTier ?: 0
    val badgeLabel = when {
        tier >= 3 -> "VIP"
        tier == 2 -> "PRO"
        tier == 1 -> "Destaque"
        else -> null
    }
    val badgeIcon = if (tier >= 3) Icons.Default.Crown else Icons.Default.AutoAwesome

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.White),
        ) {
            AsyncImage(
                model = product.imageUrl ?: product.images.firstOrNull(),
                contentDescription = product.title,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                contentScale = ContentScale.Fit,
            )

            if ((product.discountPercentage ?: 0.0) > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error,
                ) {
                    Text(
                        text = "-${product.discountPercentage!!.toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            if (badgeLabel != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    shape = CircleShape,
                    color = if (tier >= 3) Color(0xFFFFC83D) else MaterialTheme.colorScheme.primary,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (tier >= 3) Color(0xFF332200) else Color.White,
                        )
                        Text(
                            text = badgeLabel,
                            color = if (tier >= 3) Color(0xFF332200) else Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }

            if (onFavorite != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.94f),
                    shadowElevation = 2.dp,
                ) {
                    IconButton(onClick = onFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (favorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (favorite) "Remover dos favoritos" else "Favoritar",
                            tint = if (favorite) MaterialTheme.colorScheme.primary else Color(0xFF666666),
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(7.dp))

            product.oldPrice?.takeIf { it > 0 && it > (product.price ?: 0.0) }?.let {
                Text(
                    text = formatter.format(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                )
            }

            Text(
                text = product.price?.takeIf { it > 0 }?.let(formatter::format) ?: "Consultar preço",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )

            if (product.freeShipping) {
                Text(
                    text = "Frete grátis",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF138A4A),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Button(
                onClick = onBuy ?: onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 7.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Comprar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
