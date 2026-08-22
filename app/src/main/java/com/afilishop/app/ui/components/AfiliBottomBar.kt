package com.afilishop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afilishop.app.ui.theme.AfiliColors
import com.afilishop.app.ui.theme.InterFamily

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val featured: Boolean = false,
)

@Composable
fun AfiliBottomBar(selectedRoute: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        BottomDestination("home", "Início", Icons.Default.Home),
        BottomDestination("videos", "Vídeos", Icons.Default.VideoLibrary),
        BottomDestination("upload", "Postar", Icons.Default.Add, featured = true),
        BottomDestination("vip", "VIP", Icons.Outlined.WorkspacePremium),
        BottomDestination("account", "Conta", Icons.Outlined.AccountCircle),
    )

    Surface(
        color = Color.White.copy(alpha = 0.98f),
        shadowElevation = 14.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            items.forEach { item ->
                BottomItem(
                    item = item,
                    selected = selectedRoute == item.route,
                    onClick = { onNavigate(item.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomItem(
    item: BottomDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
    ) {
        if (item.featured) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
                    .size(58.dp),
                shape = CircleShape,
                color = AfiliColors.Orange,
                shadowElevation = 9.dp,
                border = androidx.compose.foundation.BorderStroke(4.dp, Color.White),
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = Color.White,
                    modifier = Modifier.padding(14.dp),
                )
            }
            BottomLabel(
                text = item.label,
                color = if (selected) AfiliColors.Orange else AfiliColors.Muted,
                selected = selected,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 7.dp),
            )
        } else {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 31.dp)
                        .clip(CircleShape)
                        .background(if (selected) AfiliColors.OrangeSoft else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) AfiliColors.Orange else AfiliColors.Muted,
                        modifier = Modifier.size(23.dp),
                    )
                }
                BottomLabel(
                    text = item.label,
                    color = if (selected) AfiliColors.Orange else AfiliColors.Muted,
                    selected = selected,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(AfiliColors.Orange),
                )
            }
        }
    }
}

@Composable
private fun BottomLabel(text: String, color: Color, selected: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontFamily = InterFamily,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
        modifier = modifier,
    )
}
