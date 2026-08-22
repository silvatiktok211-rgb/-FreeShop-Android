package com.afilishop.app.data

import android.content.Context
import com.afilishop.app.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class VipAiSearchRequest(
    val query: String,
    val expand: Boolean = false,
)

@Serializable
data class VipAiUsage(
    val used: Int = 0,
    val limit: Int = 0,
    val remaining: Int = 0,
    val tier: Int = 0,
)

@Serializable
data class VipAiProduct(
    val id: String,
    val title: String,
    val description: String? = null,
    val price: Double = 0.0,
    @SerialName("original_price") val originalPrice: Double? = null,
    val thumbnail: String? = null,
    val permalink: String? = null,
    @SerialName("free_shipping") val freeShipping: Boolean = false,
    @SerialName("sold_quantity") val soldQuantity: Int = 0,
    val rating: Double? = null,
    @SerialName("rating_count") val ratingCount: Int? = null,
    val seller: String? = null,
)

@Serializable
data class VipAiSearchResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val results: List<VipAiProduct> = emptyList(),
    val usage: VipAiUsage? = null,
    @SerialName("searchRequestId") val searchRequestId: String? = null,
)

class VipAssistantRepository(context: Context) {
    private val sessionStore = SessionStore(context.applicationContext)
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false })
        }
        expectSuccess = false
    }

    suspend fun search(query: String, expand: Boolean = false): Result<VipAiSearchResponse> = runCatching {
        val session = sessionStore.read() ?: error("Faça login para usar a IA VIP.")
        val token = session.accessToken
        val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
        require(baseUrl.isNotBlank()) { "Backend da AfiliShop não configurado." }

        val response = client.post("$baseUrl/api/mobile/vip/search") {
            bearerAuth(token)
            header("Content-Type", "application/json")
            setBody(VipAiSearchRequest(query.trim().take(250), expand))
        }
        val body = response.body<VipAiSearchResponse>()
        if (response.status.value !in 200..299 && !body.ok) {
            error(body.message ?: body.error ?: "Não foi possível pesquisar agora.")
        }
        body
    }

    fun close() {
        client.close()
    }
}
