package com.afilishop.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Crown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AfiliBottomBar(selectedRoute: String, onNavigate: (String) -> Unit) {
    val orange = Color(0xFFFF641F)
    val items = listOf(
        Triple("home", "Início", Icons.Default.Home), Triple("videos", "Vídeos", Icons.Default.VideoLibrary),
        Triple("upload", "Postar", Icons.Default.Add), Triple("vip", "VIP", Icons.Default.Crown),
        Triple("account", "Conta", Icons.Default.AccountCircle),
    )
    NavigationBar(containerColor = Color.White, tonalElevation = 5.dp) {
        items.forEach { (route, label, icon) ->
            val post = route == "upload"
            NavigationBarItem(
                selected = selectedRoute == route, onClick = { onNavigate(route) },
                icon = { if (post) Surface(shape = CircleShape, color = orange, shadowElevation = 5.dp) { Icon(icon, label, Modifier.size(58.dp).padding(13.dp), tint = Color.White) } else Icon(icon, label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = orange, selectedTextColor = orange, indicatorColor = Color(0xFFFFE9DE), unselectedIconColor = Color(0xFF63666D), unselectedTextColor = Color(0xFF63666D)),
            )
        }
    }
}
