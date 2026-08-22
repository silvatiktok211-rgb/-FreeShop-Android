package com.afilishop.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Orange = Color(0xFFFF6A00)
private val OrangeDark = Color(0xFFE55D00)
private val Cream = Color(0xFFFCFBF8)
private val Ink = Color(0xFF1B1B1B)

private val LightColors = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4D3),
    onPrimaryContainer = Color(0xFF5B2200),
    secondary = Color(0xFF765848),
    background = Cream,
    surface = Color.White,
    onBackground = Ink,
    onSurface = Ink
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFA26E),
    onPrimary = Color(0xFF4A1700),
    primaryContainer = Color(0xFF7E3000),
    onPrimaryContainer = Color(0xFFFFDBCA),
    background = Color(0xFF17110E),
    surface = Color(0xFF241A16),
    onBackground = Color(0xFFF4DED2),
    onSurface = Color(0xFFF4DED2)
)

@Composable
fun AfiliShopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        // O site usa a identidade clara em todas as telas. Manter a mesma
        // paleta evita textos claros sobre cards brancos quando o celular
        // estiver configurado no modo escuro.
        colorScheme = LightColors,
        typography = Typography().run {
            copy(
                headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
                titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
                titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        shapes = Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
        ),
        content = content
    )
}
