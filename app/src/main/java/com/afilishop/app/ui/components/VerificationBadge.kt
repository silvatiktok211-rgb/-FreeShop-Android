package com.afilishop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class VerificationKind(
    val backendValue: String,
    val label: String,
    val tint: Color,
    val softBackground: Color,
) {
    VERIFIED("verified", "Conta verificada", Color(0xFF1D9BF0), Color(0x1A1D9BF0)),
    PARTNER("partner", "Empresa verificada", Color(0xFFFFB000), Color(0x1AFFB000)),
    VIP("vip", "Criador destaque", Color(0xFF8B5CF6), Color(0x1A8B5CF6));

    companion object {
        fun fromBackend(value: String?): VerificationKind? = when (value?.trim()?.lowercase()) {
            VERIFIED.backendValue -> VERIFIED
            PARTNER.backendValue -> PARTNER
            VIP.backendValue -> VIP
            else -> null
        }
    }
}

@Composable
fun VerificationBadge(
    verificationType: String?,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    showText: Boolean = false,
) {
    val kind = VerificationKind.fromBackend(verificationType) ?: return
    if (showText) {
        Row(
            modifier = modifier
                .semantics { contentDescription = kind.label }
                .background(kind.softBackground, RoundedCornerShape(999.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Verified, null, tint = kind.tint, modifier = Modifier.size(size))
            Text(kind.label, color = kind.tint, fontWeight = FontWeight.Bold)
        }
    } else {
        Icon(Icons.Filled.Verified, kind.label, tint = kind.tint, modifier = modifier.size(size))
    }
}
