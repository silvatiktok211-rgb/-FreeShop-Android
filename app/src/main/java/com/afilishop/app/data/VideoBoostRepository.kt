package com.afilishop.app.data

import android.content.Context
import com.afilishop.app.BuildConfig
import com.afilishop.app.model.ActiveVideoBoost
import com.afilishop.app.model.VideoBoostCampaign
import com.afilishop.app.model.VideoBoostPlan
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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class BoostCheckoutPayload(val _video_id: String, val _plan_code: String, val _idempotency_key: String)
@Serializable
private data class BoostFeedPayload(val _limit: Int)
@Serializable
private data class BoostEventPayload(val _campaign_id: String, val _event_type: String, val _dedupe_key: String)
@Serializable
private data class BoostActivationPayload(val campaignId: String, val sku: String, val purchaseToken: String)
@Serializable
private data class BoostActivationResponse(val ok: Boolean = false, val campaignId: String? = null, val error: String? = null)

class VideoBoostRepository(context: Context) {
    private val appContext = context.applicationContext
    private val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val apiBaseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
    private val sessionStore = SessionStore(appContext)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        install(ContentNegotiation) { json(json) }
        expectSuccess = false
    }

    suspend fun loadPlans(): List<VideoBoostPlan> {
        if (supabaseUrl.isBlank() || anonKey.isBlank()) return fallbackPlans()
        return runCatching {
            val response = client.get("$supabaseUrl/rest/v1/video_boost_plans") {
                header("apikey", anonKey)
                bearerAuth(anonKey)
                url.parameters.append("select", "id,code,name,audience_type,duration_hours,price_brl,google_play_sku,estimated_reach_min,estimated_reach_max,is_active,sort_order")
                url.parameters.append("is_active", "eq.true")
                url.parameters.append("order", "sort_order.asc")
            }
            if (response.status.value !in 200..299) return@runCatching fallbackPlans()
            response.body<List<VideoBoostPlan>>().ifEmpty(::fallbackPlans)
        }.getOrElse { fallbackPlans() }
    }

    suspend fun createCheckout(videoId: String, planCode: String): Result<String> = runCatching {
        val session = sessionStore.read() ?: error("Entre na sua conta para impulsionar um vídeo.")
        if (supabaseUrl.isBlank() || anonKey.isBlank()) error("Backend de impulsionamento indisponível.")
        val response = client.post("$supabaseUrl/rest/v1/rpc/video_boost_create_checkout") {
            header("apikey", anonKey)
            bearerAuth(session.accessToken)
            header("Content-Type", ContentType.Application.Json.toString())
            setBody(BoostCheckoutPayload(videoId, planCode, UUID.randomUUID().toString()))
        }
        val body = response.bodyAsText()
        if (response.status.value !in 200..299) {
            val friendly = when {
                body.contains("business_profile_required", true) -> "Para usar campanha empresarial, sua conta precisa ter uma loja ativa."
                body.contains("video_not_owned", true) -> "Você só pode impulsionar seus próprios vídeos."
                body.contains("invalid_plan", true) -> "Este pacote não está mais disponível."
                else -> "Não foi possível preparar o impulsionamento agora."
            }
            error(friendly)
        }
        body.trim().trim('"').takeIf { it.isNotBlank() } ?: error("O servidor não devolveu a campanha criada.")
    }

    suspend fun activatePurchase(campaignId: String, sku: String, purchaseToken: String): Result<Unit> = runCatching {
        val session = sessionStore.read() ?: error("Sua sessão expirou.")
        if (apiBaseUrl.isBlank()) error("Servidor de pagamentos indisponível.")
        val response = client.post("$apiBaseUrl/api/mobile/video-boost/activate") {
            bearerAuth(session.accessToken)
            header("Content-Type", ContentType.Application.Json.toString())
            setBody(BoostActivationPayload(campaignId, sku, purchaseToken))
        }
        val body = response.bodyAsText()
        if (response.status.value !in 200..299) error("A compra não pôde ser validada. Nenhuma campanha foi ativada.")
        val parsed = runCatching { json.decodeFromString<BoostActivationResponse>(body) }.getOrNull()
        if (parsed?.ok != true) error("A compra não pôde ser validada. Nenhuma campanha foi ativada.")
    }

    suspend fun loadMyCampaigns(): List<VideoBoostCampaign> {
        val session = sessionStore.read() ?: return emptyList()
        val userId = session.user?.id ?: return emptyList()
        if (supabaseUrl.isBlank() || anonKey.isBlank()) return emptyList()
        return runCatching {
            val response = client.get("$supabaseUrl/rest/v1/video_boost_campaigns") {
                header("apikey", anonKey)
                bearerAuth(session.accessToken)
                url.parameters.append("select", "id,video_id,plan_code,campaign_type,amount_brl,duration_hours,status,payment_status,starts_at,ends_at,impressions,qualified_views,profile_visits,product_clicks")
                url.parameters.append("advertiser_id", "eq.$userId")
                url.parameters.append("order", "created_at.desc")
                url.parameters.append("limit", "20")
            }
            if (response.status.value !in 200..299) emptyList() else response.body<List<VideoBoostCampaign>>()
        }.getOrElse { emptyList() }
    }

    suspend fun loadActiveForFeed(limit: Int = 12): List<ActiveVideoBoost> {
        if (supabaseUrl.isBlank() || anonKey.isBlank()) return emptyList()
        return runCatching {
            val response = client.post("$supabaseUrl/rest/v1/rpc/video_boost_active_for_feed") {
                header("apikey", anonKey)
                bearerAuth(anonKey)
                header("Content-Type", ContentType.Application.Json.toString())
                setBody(BoostFeedPayload(limit.coerceIn(1, 30)))
            }
            if (response.status.value !in 200..299) emptyList() else response.body<List<ActiveVideoBoost>>()
        }.getOrElse { emptyList() }
    }

    suspend fun recordEvent(campaignId: String, eventType: String, dedupeKey: String): Boolean {
        if (supabaseUrl.isBlank() || anonKey.isBlank()) return false
        return runCatching {
            val session = sessionStore.read()
            val response = client.post("$supabaseUrl/rest/v1/rpc/video_boost_record_event") {
                header("apikey", anonKey)
                bearerAuth(session?.accessToken ?: anonKey)
                header("Content-Type", ContentType.Application.Json.toString())
                setBody(BoostEventPayload(campaignId, eventType, dedupeKey))
            }
            response.status.value in 200..299 && response.bodyAsText().trim().equals("true", true)
        }.getOrDefault(false)
    }

    fun close() = client.close()

    companion object {
        fun fallbackPlans(): List<VideoBoostPlan> = listOf(
            VideoBoostPlan(null, "creator_24h", "Experimentar", "creator", 24, 4.90, "boost_creator_24h", 500, 1500, true, 10),
            VideoBoostPlan(null, "creator_3d", "Dar um gás", "creator", 72, 9.90, "boost_creator_3d", 1200, 3500, true, 20),
            VideoBoostPlan(null, "creator_7d", "Crescer", "creator", 168, 19.90, "boost_creator_7d", 3000, 8000, true, 30),
            VideoBoostPlan(null, "creator_14d", "Turbo", "creator", 336, 34.90, "boost_creator_14d", 6000, 15000, true, 40),
            VideoBoostPlan(null, "business_24h", "Teste comercial", "business", 24, 9.90, "boost_business_24h", 1000, 3000, true, 110),
            VideoBoostPlan(null, "business_3d", "Movimentar produto", "business", 72, 24.90, "boost_business_3d", 3000, 8000, true, 120),
            VideoBoostPlan(null, "business_7d", "Campanha", "business", 168, 49.90, "boost_business_7d", 8000, 20000, true, 130),
            VideoBoostPlan(null, "business_14d", "Campanha forte", "business", 336, 89.90, "boost_business_14d", 15000, 40000, true, 140),
        )
    }
}
