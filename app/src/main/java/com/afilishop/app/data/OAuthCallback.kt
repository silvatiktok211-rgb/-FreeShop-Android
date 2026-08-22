package com.afilishop.app.data

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal const val AFILISHOP_AUTH_SCHEME = "afilishop"
internal const val AFILISHOP_AUTH_HOST = "auth-callback"

internal data class OAuthCallback(
    val code: String? = null,
    val error: String? = null,
)

internal fun parseOAuthCallback(rawUrl: String): OAuthCallback? {
    val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return null
    if (!uri.scheme.equals(AFILISHOP_AUTH_SCHEME, ignoreCase = true) ||
        !uri.host.equals(AFILISHOP_AUTH_HOST, ignoreCase = true)
    ) return null

    val parameters = buildMap {
        sequenceOf(uri.rawQuery, uri.rawFragment)
            .filterNotNull()
            .flatMap { it.split('&').asSequence() }
            .forEach { entry ->
                val pieces = entry.split('=', limit = 2)
                if (pieces.isNotEmpty() && pieces[0].isNotBlank()) {
                    val key = decode(pieces[0])
                    val value = decode(pieces.getOrElse(1) { "" })
                    put(key, value)
                }
            }
    }

    return OAuthCallback(
        code = parameters["code"]?.takeIf(String::isNotBlank),
        error = (parameters["error_description"] ?: parameters["error"])
            ?.takeIf(String::isNotBlank),
    )
}

private fun decode(value: String): String =
    runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }
        .getOrDefault(value)
