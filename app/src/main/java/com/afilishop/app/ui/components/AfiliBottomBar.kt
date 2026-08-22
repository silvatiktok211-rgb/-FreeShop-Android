package com.afilishop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val featured: Boolean = false,
)

@Composable
fun AfiliBottomBar(selectedRoute: String, onNavigate: (String) -> Unit) {
    val videoMode = selectedRoute == "videos"
    val active = if (videoMode) Color.White else MaterialTheme.colorScheme.primary
    val inactive = if (videoMode) Color.White.copy(alpha = 0.58f) else MaterialTheme.colorScheme.onSurfaceVariant
    val surface = if (videoMode) Color.Black else MaterialTheme.colorScheme.surface
    val items = listOf(
        BottomItem("home", "Início", Icons.Filled.Home),
        BottomItem("videos", "Vídeos", Icons.Filled.PlayCircle),
        BottomItem("upload", "Postar", Icons.Filled.Add, featured = true),
        BottomItem("vip", "VIP", Icons.Filled.Stars),
        BottomItem("account", "Conta", Icons.Filled.AccountCircle),
    )

    Surface(
        color = surface.copy(alpha = 0.97f),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            items.forEach { item ->
                val selected = selectedRoute == item.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clickable { onNavigate(item.route) }
                        .padding(bottom = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (item.featured) {
                        Box(
                            modifier = Modifier
                                .offset(y = (-7).dp)
                                .size(56.dp)
                                .shadow(12.dp, CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFF58A4B), Color(0xFFF05A2A)),
                                    ),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = Color.White,
                                modifier = Modifier.size(29.dp),
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 32.dp)
                                .background(
                                    if (selected) active.copy(alpha = 0.10f) else Color.Transparent,
                                    RoundedCornerShape(50),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = if (selected) active else inactive,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Text(
                        item.label,
                        color = if (selected || item.featured) active else inactive,
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                        modifier = if (item.featured) Modifier.offset(y = (-5).dp) else Modifier,
                    )
                }
            }
        }
    }
}
