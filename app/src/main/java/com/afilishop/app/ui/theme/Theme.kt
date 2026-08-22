package com.afilishop.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Orange = Color(0xFFF37335)
private val Cream = Color(0xFFFDFCFB)
private val Ink = Color(0xFF20212B)
private val Muted = Color(0xFFF5F4F2)
private val MutedInk = Color(0xFF72727E)
private val Border = Color(0xFFECEAE7)

private val LightColors = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE7D8),
    onPrimaryContainer = Color(0xFF642400),
    secondary = Muted,
    onSecondary = Ink,
    background = Cream,
    surface = Color.White,
    surfaceVariant = Muted,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
    outline = Border,
    outlineVariant = Border
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF58A58),
    onPrimary = Color(0xFF26110A),
    primaryContainer = Color(0xFF713015),
    onPrimaryContainer = Color(0xFFFFE0CF),
    background = Color(0xFF202127),
    surface = Color(0xFF2B2C33),
    surfaceVariant = Color(0xFF383941),
    onBackground = Color(0xFFF6F5F3),
    onSurface = Color(0xFFF6F5F3),
    onSurfaceVariant = Color(0xFFB7B6BE),
    outline = Color(0xFF464750),
    outlineVariant = Color(0xFF464750)
)

@Composable
fun AfiliShopTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography().run {
            copy(
                headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
                titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
                titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        shapes = Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        ),
        content = content
    )
}
