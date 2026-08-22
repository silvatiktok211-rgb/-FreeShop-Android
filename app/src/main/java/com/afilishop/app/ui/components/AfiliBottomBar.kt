package com.afilishop.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AfiliBottomBar(selectedRoute: String, onNavigate: (String) -> Unit) {
    val videoMode = selectedRoute == "videos"
    val items = listOf(
        Triple("home", "Início", Icons.Filled.Home),
        Triple("videos", "Vídeos", Icons.Filled.PlayCircle),
        Triple("upload", "Postar", Icons.Filled.AddCircle),
        Triple("vip", "VIP", Icons.Filled.LocalOffer),
        Triple("account", "Conta", Icons.Filled.AccountCircle),
    )

    NavigationBar(
        containerColor = if (videoMode) Color.Black else MaterialTheme.colorScheme.surface,
    ) {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = selectedRoute == route,
                onClick = { onNavigate(route) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = if (videoMode) {
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.58f),
                        unselectedTextColor = Color.White.copy(alpha = 0.58f),
                        indicatorColor = Color(0xFFFF6A00).copy(alpha = 0.28f),
                    )
                } else {
                    NavigationBarItemDefaults.colors()
                },
            )
        }
    }
}
