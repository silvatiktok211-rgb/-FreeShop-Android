package com.afilishop.app.notifications

import java.net.URI

fun notificationDestination(rawLink: String?): String? {
    val raw = rawLink?.trim().orEmpty()
    if (raw.isBlank()) return null
    val parsed = runCatching { URI(raw) }.getOrNull()
    val path = when {
        parsed?.scheme == "http" || parsed?.scheme == "https" -> parsed.path.orEmpty()
        raw.startsWith('/') -> raw.substringBefore('?').substringBefore('#')
        else -> parsed?.path.orEmpty()
    }
    val segments = path.trim('/').split('/').filter { it.isNotBlank() }
    if (segments.isEmpty()) return "home"

    val first = segments.first().lowercase()
    val id = segments.getOrNull(1)?.takeIf { it.isNotBlank() }
    return when (first) {
        "produto", "produtos", "product" -> id?.let { "product/$it" } ?: "explore"
        "mensagens", "mensagem", "conversation" -> id?.let { "conversation/$it" } ?: "community"
        "notificacoes", "notificações", "notifications" -> "notifications"
        "videos", "vídeos" -> "videos"
        "perfil", "profile" -> id?.let { "profile/$it" } ?: "profile"
        "loja", "store" -> id?.let { "store/$it" } ?: "explore"
        "busca", "buscar", "explorar", "explore", "search" -> "explore"
        "conta", "account" -> "account"
        "comunidade", "community" -> "community"
        "lives", "live", "ao-vivo" -> "lives"
        "planos", "vip", "dashboard-vip" -> "vip"
        "assistente-vip", "vip-assistant" -> "vip-assistant"
        "postar", "publicar", "upload" -> "upload"
        "configuracoes", "configurações", "settings" -> "settings"
        "termos", "terms" -> "terms"
        "admin" -> "admin"
        else -> null
    }
}
