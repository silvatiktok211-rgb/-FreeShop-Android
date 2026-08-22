package com.afilishop.app.data

import com.afilishop.app.BuildConfig
import com.afilishop.app.model.Profile
import com.afilishop.app.model.SocialVideo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class VideoFeedRepository {
    private val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val configured = supabaseUrl.isNotBlank() && anonKey.isNotBlank()

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                },
            )
        }
        expectSuccess = false
    }

    suspend fun loadVideos(): List<SocialVideo> {
        if (!configured) return emptyList()

        return runCatching {
            val videos = mutableListOf<SocialVideo>()
            var offset = 0
            val pageSize = 100
            while (true) {
                val response = client.get("$supabaseUrl/rest/v1/social_videos") {
                    header("apikey", anonKey)
                    url.parameters.append(
                        "select",
                        "id,video_url,thumbnail_url,description,is_active,is_featured,product_id,product_external_url,product_title,product_price,product_image,shares_count,views_count,user_id,likes_count,comments_count,created_at",
                    )
                    url.parameters.append("is_active", "eq.true")
                    url.parameters.append("order", "created_at.desc")
                    url.parameters.append("limit", pageSize.toString())
                    url.parameters.append("offset", offset.toString())
                }
                if (response.status.value !in 200..299) error("Vídeos indisponíveis (${response.status.value}).")
                val page = response.body<List<SocialVideo>>()
                videos += page
                if (page.size < pageSize) break
                offset += page.size
            }
            attachProfiles(videos)
        }.getOrElse { emptyList() }
    }

    private suspend fun attachProfiles(videos: List<SocialVideo>): List<SocialVideo> {
        val ids = videos.mapNotNull { it.userId }.distinct()
        if (ids.isEmpty()) return videos

        val profiles = ids.chunked(50).flatMap { batch ->
            runCatching {
                val response = client.get("$supabaseUrl/rest/v1/profiles") {
                    header("apikey", anonKey)
                    url.parameters.append("select", "id,display_name,avatar_url,bio,followers_count,following_count")
                    url.parameters.append("id", "in.(${batch.joinToString(",")})")
                }
                if (response.status.value !in 200..299) emptyList() else response.body<List<Profile>>()
            }.getOrElse { emptyList() }
        }

        if (profiles.isEmpty()) return videos
        val byId = profiles.associateBy { it.id }
        return videos.map { video ->
            video.copy(profile = video.userId?.let(byId::get))
        }
    }

    fun close() {
        client.close()
    }
}
