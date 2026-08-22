package com.afilishop.app.util

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed interface MercadoLivreCandidate {
    val value: String
    val reason: String
    data class Item(override val value: String, override val reason: String) : MercadoLivreCandidate
    data class Catalog(override val value: String, override val reason: String) : MercadoLivreCandidate
    data class UserProduct(override val value: String, override val reason: String) : MercadoLivreCandidate
}

data class ResolvedMercadoLivreLink(
    val originalInput: String,
    val candidates: List<MercadoLivreCandidate>,
    val isMercadoLivre: Boolean
) {
    val itemIds: List<String> get() = candidates.filterIsInstance<MercadoLivreCandidate.Item>().map { it.value }.distinct()
    val catalogIds: List<String> get() = candidates.filterIsInstance<MercadoLivreCandidate.Catalog>().map { it.value }.distinct()
    val userProductIds: List<String> get() = candidates.filterIsInstance<MercadoLivreCandidate.UserProduct>().map { it.value }.distinct()
}

object MercadoLivreLinkParser {
    private val itemRegex = Regex("\\bMLB[0-9]{6,14}\\b", RegexOption.IGNORE_CASE)
    private val catalogRegex = Regex("(?:^|/)p/(MLB[0-9]{6,14})(?:[/?#]|$)", RegexOption.IGNORE_CASE)
    private val userProductRegex = Regex("(?:^|/)up/(MLBU[0-9]{6,18})(?:[/?#]|$)", RegexOption.IGNORE_CASE)

    fun resolve(input: String): ResolvedMercadoLivreLink {
        val normalized = input.trim()
        if (normalized.isBlank()) return ResolvedMercadoLivreLink(input, emptyList(), false)
        val candidates = mutableListOf<MercadoLivreCandidate>()
        val decoded = runCatching { URLDecoder.decode(normalized, StandardCharsets.UTF_8.name()) }.getOrDefault(normalized)
        val uri = runCatching { URI(decoded) }.getOrNull()
        val host = uri?.host?.lowercase().orEmpty()
        val isMercadoLivre = host.contains("mercadolivre") || host.contains("mercadolibre") || decoded.contains("MLB", ignoreCase = true)

        fun add(candidate: MercadoLivreCandidate) {
            if (candidates.none { it.value.equals(candidate.value, ignoreCase = true) && it::class == candidate::class }) candidates += candidate
        }

        catalogRegex.find(decoded)?.groupValues?.getOrNull(1)?.uppercase()?.let { add(MercadoLivreCandidate.Catalog(it, "catalog_product_path")) }
        userProductRegex.find(decoded)?.groupValues?.getOrNull(1)?.uppercase()?.let { add(MercadoLivreCandidate.UserProduct(it, "user_product_path")) }

        uri?.rawQuery.orEmpty().split('&').forEach { pair ->
            val parts = pair.split('=', limit = 2)
            if (parts.size != 2) return@forEach
            val key = parts[0].lowercase()
            val value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
            when (key) {
                "wid", "item_id", "itemid" -> itemRegex.find(value)?.value?.uppercase()?.let { add(MercadoLivreCandidate.Item(it, "share_parameter_$key")) }
                "pdp_filters" -> itemRegex.find(value)?.value?.uppercase()?.let { add(MercadoLivreCandidate.Item(it, "pdp_filters")) }
            }
        }

        itemRegex.findAll(decoded).forEach { match ->
            val id = match.value.uppercase()
            if (catalogRegex.find(decoded)?.groupValues?.getOrNull(1)?.equals(id, ignoreCase = true) != true) {
                add(MercadoLivreCandidate.Item(id, "standard_or_legacy_item_id"))
            }
        }
        return ResolvedMercadoLivreLink(input, candidates, isMercadoLivre)
    }
}
