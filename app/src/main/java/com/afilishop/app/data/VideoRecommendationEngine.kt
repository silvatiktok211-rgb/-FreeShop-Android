package com.afilishop.app.data

import android.content.Context
import com.afilishop.app.model.SocialVideo
import java.text.Normalizer
import java.util.Locale
import kotlin.math.absoluteValue

class VideoRecommendationEngine(context: Context, viewerId: String?) {
    private val preferences = context.applicationContext.getSharedPreferences("afilishop_video_interests", Context.MODE_PRIVATE)
    private val viewerKey = viewerId?.takeIf { it.isNotBlank() } ?: "guest"
    private val sessionSalt = System.nanoTime()

    fun recordWatch(video: SocialVideo) {
        preferences.edit().putLong(recentWatchKey(video.id), System.currentTimeMillis()).apply()
        if (!video.isPromoted) addSignals(video, 1f)
    }

    fun recordLike(video: SocialVideo) { addSignals(video, 2.5f) }

    fun rank(videos: List<SocialVideo>): List<SocialVideo> {
        if (videos.size < 2) return videos
        val promoted = videos.filter(SocialVideo::isPromoted).distinctBy(SocialVideo::id)
        val organic = videos.filterNot(SocialVideo::isPromoted).distinctBy(SocialVideo::id)
        val rankedOrganic = rankOrganic(organic)
        if (promoted.isEmpty()) return rankedOrganic
        if (rankedOrganic.isEmpty()) return promoted
        return injectPromotions(rankedOrganic, promoted)
    }

    private fun rankOrganic(videos: List<SocialVideo>): List<SocialVideo> {
        if (videos.size < 2) return videos
        return videos.mapIndexed { index, video ->
            val interest = topics(video).sumOf { signalScore(it).toDouble() }.toFloat()
            val recentPenalty = recentWatchPenalty(video.id)
            val exploration = explorationScore(video.id)
            val freshness = ((videos.size - index).coerceAtLeast(0) / videos.size.toFloat()) * 2.5f
            RankedVideo(index, video, interest + exploration + freshness - recentPenalty)
        }.sortedWith(compareByDescending<RankedVideo> { it.score }.thenBy { it.originalIndex }).map(RankedVideo::video)
    }

    private fun injectPromotions(organic: List<SocialVideo>, promoted: List<SocialVideo>): List<SocialVideo> {
        val result = organic.toMutableList()
        val ordered = promoted.sortedWith(compareBy<SocialVideo> { recentWatchPenalty(it.id) }.thenByDescending { explorationScore("boost:${it.boostCampaignId}:${it.id}") })
        val business = ordered.filter { it.promotionType == "business" }
        val creator = ordered.filter { it.promotionType == "creator" }
        business.firstOrNull()?.let { ad ->
            val earlyWindow = minOf(2, result.size)
            val position = 1 + (sessionHash(ad.id) % earlyWindow.coerceAtLeast(1))
            insertPromotion(result, ad, position)
        }
        business.drop(1).take(2).forEachIndexed { index, ad -> insertPromotion(result, ad, 10 + index * 11 + (sessionHash(ad.id) % 3)) }
        creator.take(4).forEachIndexed { index, ad -> insertPromotion(result, ad, 5 + (sessionHash(ad.id) % 3) + index * 7) }
        return result
    }

    private fun insertPromotion(result: MutableList<SocialVideo>, video: SocialVideo, requestedIndex: Int) {
        if (result.any { it.id == video.id }) return
        var index = requestedIndex.coerceIn(0, result.size)
        if (index > 0 && result.getOrNull(index - 1)?.isPromoted == true) index++
        if (result.getOrNull(index)?.isPromoted == true) index++
        result.add(index.coerceAtMost(result.size), video)
    }

    private fun addSignals(video: SocialVideo, multiplier: Float) {
        val editor = preferences.edit()
        topics(video).forEach { topic ->
            val weight = when {
                topic.startsWith("category:") -> 6f
                topic.startsWith("theme:") -> 4f
                else -> 1f
            } * multiplier
            editor.putFloat(preferenceKey(topic), (signalScore(topic) * 0.98f + weight).coerceAtMost(120f))
        }
        editor.apply()
    }

    private fun recentWatchPenalty(videoId: String): Float {
        val watchedAt = preferences.getLong(recentWatchKey(videoId), 0L)
        if (watchedAt <= 0L) return 0f
        val ageMillis = (System.currentTimeMillis() - watchedAt).coerceAtLeast(0L)
        return when {
            ageMillis < TWO_HOURS_MS -> 140f
            ageMillis < ONE_DAY_MS -> 70f
            ageMillis < THREE_DAYS_MS -> 22f
            ageMillis < SEVEN_DAYS_MS -> 7f
            else -> 0f
        }
    }

    private fun explorationScore(videoId: String): Float = ("$viewerKey:$videoId:$sessionSalt".hashCode().absoluteValue % 1_000) / 1_000f * 8f
    private fun sessionHash(value: String): Int = "$viewerKey:$value:$sessionSalt".hashCode().absoluteValue

    private fun topics(video: SocialVideo): Set<String> {
        val result = linkedSetOf<String>()
        video.product?.categoryId?.takeIf { it.isNotBlank() }?.let { result += "category:$it" }
        val normalizedText = normalize(listOfNotNull(video.caption, video.productTitle, video.product?.title).joinToString(" "))
        THEME_KEYWORDS.forEach { (theme, keywords) -> if (keywords.any(normalizedText::contains)) result += "theme:$theme" }
        normalizedText.split(Regex("[^a-z0-9]+"))
            .asSequence().filter { it.length >= 4 && it !in STOP_WORDS }.distinct().take(6)
            .forEach { result += "word:$it" }
        return result
    }

    private fun signalScore(topic: String) = preferences.getFloat(preferenceKey(topic), 0f)
    private fun preferenceKey(topic: String) = "$viewerKey:$topic"
    private fun recentWatchKey(videoId: String) = "$viewerKey:recent_video:$videoId"
    private fun normalize(value: String) = Normalizer.normalize(value.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
    private data class RankedVideo(val originalIndex: Int, val video: SocialVideo, val score: Float)

    private companion object {
        const val TWO_HOURS_MS = 2L * 60L * 60L * 1_000L
        const val ONE_DAY_MS = 24L * 60L * 60L * 1_000L
        const val THREE_DAYS_MS = 3L * ONE_DAY_MS
        const val SEVEN_DAYS_MS = 7L * ONE_DAY_MS
        val STOP_WORDS = setOf("para", "com", "uma", "esse", "essa", "este", "esta", "mais", "muito", "video", "produto", "oferta", "afili", "shop", "aqui", "voce", "sobre")
        val THEME_KEYWORDS = mapOf(
            "moda" to setOf("roupa", "moda", "camisa", "camiseta", "vestido", "calca", "tenis", "look"),
            "veiculos" to setOf("carro", "automovel", "veiculo", "moto", "motor", "pneu", "capacete"),
            "tecnologia" to setOf("celular", "smartphone", "notebook", "computador", "fone", "gamer", "eletronico"),
            "casa" to setOf("casa", "cozinha", "moveis", "decoracao", "quarto", "banheiro", "ferramenta"),
            "beleza" to setOf("beleza", "maquiagem", "perfume", "cabelo", "skincare", "cosmetico"),
            "fitness" to setOf("fitness", "academia", "treino", "esporte", "corrida", "suplemento"),
            "pets" to setOf("pet", "cachorro", "gato", "animal", "racao"),
            "comida" to setOf("comida", "receita", "cozinha", "lanche", "bebida"),
        )
    }
}
