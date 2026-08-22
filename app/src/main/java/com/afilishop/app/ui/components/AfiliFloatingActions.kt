package com.afilishop.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afilishop.app.ui.theme.AfiliColors

private data class FloatingDestination(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun AfiliFloatingActions(
    bottomPadding: Dp,
    showAdmin: Boolean,
    onAdmin: () -> Unit,
    onPostVideo: () -> Unit,
    onLive: () -> Unit,
    onCommunity: () -> Unit,
    onSupport: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (open) 135f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 520f),
        label = "fab-rotation",
    )
    val destinations = listOf(
        FloatingDestination("Postar vídeo", Icons.Default.Videocam, onPostVideo),
        FloatingDestination("Iniciar live", Icons.Default.LiveTv, onLive),
        FloatingDestination("Comunidade", Icons.Default.People, onCommunity),
        FloatingDestination("Suporte", Icons.Default.HeadsetMic, onSupport),
    )

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { open = false },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = bottomPadding),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(
                visible = open,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 }),
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    destinations.forEach { destination ->
                        FloatingMenuItem(destination) {
                            open = false
                            destination.onClick()
                        }
                    }
                }
            }

            if (!open && showAdmin) {
                RoundAction(
                    icon = Icons.Default.Shield,
                    description = "Admin",
                    background = Color(0xFF18181B),
                    size = 48.dp,
                    onClick = onAdmin,
                )
            }

            if (!open) {
                RoundAction(
                    icon = Icons.Default.ChatBubble,
                    description = "Falar com o suporte",
                    background = Color(0xFF25D366),
                    size = 56.dp,
                    onClick = onSupport,
                )
            }

            Surface(
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer { rotationZ = rotation }
                    .clickable { open = !open },
                shape = CircleShape,
                color = AfiliColors.Orange,
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(4.dp, Color.White),
            ) {
                Icon(
                    imageVector = if (open) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (open) "Fechar menu" else "Abrir menu",
                    tint = Color.White,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
    }
}

@Composable
private fun FloatingMenuItem(destination: FloatingDestination, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.96f),
            shadowElevation = 5.dp,
        ) {
            Text(
                text = destination.label,
                color = Color(0xFF27272A),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            )
        }
        RoundAction(
            icon = destination.icon,
            description = destination.label,
            background = Color.White,
            foreground = AfiliColors.Orange,
            size = 48.dp,
            onClick = onClick,
        )
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    description: String,
    background: Color,
    size: Dp,
    onClick: () -> Unit,
    foreground: Color = Color.White,
) {
    Surface(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = background,
        shadowElevation = 9.dp,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = foreground,
            modifier = Modifier.padding(size * 0.28f),
        )
    }
}
