package com.afilishop.app.data

import com.afilishop.app.BuildConfig
import com.afilishop.app.model.ActiveVideoBoost
import com.afilishop.app.model.Product
import com.afilishop.app.model.Profile
import com.afilishop.app.model.SocialVideo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class FeedBoostRequest(val _limit: Int = 12)

class VideoFeedRepository {
    private val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val configured = supabaseUrl.isNotBlank() && anonKey.isNotBlank()
    private val client = HttpClient(Android) {
        install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000; socketTimeoutMillis = 15_000 }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }) }
        expectSuccess = false
    }

    suspend fun loadVideoById(videoId: String, creatorUserId: String? = null): SocialVideo? =
        if (videoId.isBlank()) null else loadVideos(1, creatorUserId, videoId).firstOrNull()

    suspend fun loadVideos(limit: Int = 40, creatorUserId: String? = null, videoId: String? = null): List<SocialVideo> {
        if (!configured) return emptyList()
        return runCatching {
            val organic = loadVideoRows(limit, creatorUserId, videoId)
            if (creatorUserId != null || videoId != null) return@runCatching attachProducts(attachProfiles(organic))
            val boosts = loadActiveBoosts()
            if (boosts.isEmpty()) return@runCatching attachProducts(attachProfiles(organic))
            val organicIds = organic.mapTo(mutableSetOf()) { it.id }
            val missingBoostIds = boosts.map { it.videoId }.distinct().filterNot(organicIds::contains)
            val extraBoosted = loadVideosByIds(missingBoostIds)
            val hydrated = attachProducts(attachProfiles((organic + extraBoosted).distinctBy { it.id }))
            val boostByVideo = boosts.groupBy(ActiveVideoBoost::videoId).mapValues { (_, rows) -> rows.first() }
            hydrated.map { video ->
                val boost = boostByVideo[video.id]
                if (boost == null) video else video.copy(boostCampaignId = boost.campaignId, promotionType = boost.campaignType)
            }
        }.getOrElse { emptyList() }
    }

    private suspend fun loadVideoRows(limit: Int, creatorUserId: String?, videoId: String?): List<SocialVideo> {
        val response = client.get("$supabaseUrl/rest/v1/social_videos") {
            header("apikey", anonKey); bearerAuth(anonKey)
            url.parameters.append("select", VIDEO_SELECT); url.parameters.append("is_active", "eq.true")
            videoId?.let { url.parameters.append("id", "eq.$it") }
            creatorUserId?.takeIf { it.isNotBlank() }?.let { url.parameters.append("user_id", "eq.$it") }
            url.parameters.append("order", "created_at.desc"); url.parameters.append("limit", limit.coerceIn(1, 100).toString())
        }
        if (response.status.value !in 200..299) return emptyList()
        return response.body()
    }

    private suspend fun loadVideosByIds(ids: List<String>): List<SocialVideo> {
        if (ids.isEmpty()) return emptyList()
        return runCatching {
            val response = client.get("$supabaseUrl/rest/v1/social_videos") {
                header("apikey", anonKey); bearerAuth(anonKey)
                url.parameters.append("select", VIDEO_SELECT); url.parameters.append("is_active", "eq.true")
                url.parameters.append("id", "in.(${ids.joinToString(",")})"); url.parameters.append("limit", ids.size.coerceIn(1, 30).toString())
            }
            if (response.status.value !in 200..299) emptyList() else response.body<List<SocialVideo>>()
        }.getOrElse { emptyList() }
    }

    private suspend fun loadActiveBoosts(): List<ActiveVideoBoost> = runCatching {
        val response = client.post("$supabaseUrl/rest/v1/rpc/video_boost_active_for_feed") {
            header("apikey", anonKey); bearerAuth(anonKey); header("Content-Type", ContentType.Application.Json.toString()); setBody(FeedBoostRequest())
        }
        if (response.status.value !in 200..299) emptyList() else response.body<List<ActiveVideoBoost>>()
    }.getOrElse { emptyList() }

    private suspend fun attachProfiles(videos: List<SocialVideo>): List<SocialVideo> {
        val ids = videos.mapNotNull { it.userId }.distinct(); if (ids.isEmpty()) return videos
        val profiles = runCatching {
            val response = client.get("$supabaseUrl/rest/v1/profiles_public") {
                header("apikey", anonKey); bearerAuth(anonKey)
                url.parameters.append("select", "id,display_name,avatar_url,bio,verification_type,is_admin,followers_count,following_count")
                url.parameters.append("id", "in.(${ids.joinToString(",")})")
            }
            if (response.status.value !in 200..299) emptyList() else response.body<List<Profile>>()
        }.getOrElse { emptyList() }
        if (profiles.isEmpty()) return videos
        val byId = profiles.associateBy { it.id }
        return videos.map { video -> video.copy(profile = video.userId?.let(byId::get)) }
    }

    private suspend fun attachProducts(videos: List<SocialVideo>): List<SocialVideo> {
        val ids = videos.mapNotNull { it.productId }.distinct(); if (ids.isEmpty()) return videos
        val products = runCatching {
            val response = client.get("$supabaseUrl/rest/v1/products") {
                header("apikey", anonKey); bearerAuth(anonKey)
                url.parameters.append("select", "id,title,price,currency,image_url,old_price,discount_percentage,affiliate_url,original_affiliate_url")
                url.parameters.append("id", "in.(${ids.joinToString(",")})")
            }
            if (response.status.value !in 200..299) emptyList() else response.body<List<Product>>()
        }.getOrElse { emptyList() }
        if (products.isEmpty()) return videos
        val byId = products.associateBy(Product::id)
        return videos.map { video -> video.copy(product = video.productId?.let(byId::get)) }
    }

    fun close() { client.close() }
    private companion object {
        const val VIDEO_SELECT = "id,video_url,thumbnail_url,description,is_active,is_featured,product_id,product_external_url,product_title,product_price,product_image,shares_count,views_count,user_id,likes_count,comments_count,saves_count,created_at"
    }
}
