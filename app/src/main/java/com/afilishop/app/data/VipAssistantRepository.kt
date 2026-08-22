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
data class VipAiActionRequest(
    val action: String,
    val productId: String? = null,
    val permalink: String? = null,
    val title: String? = null,
    val thumbnail: String? = null,
    val price: Double? = null,
    val pendingId: String? = null,
    val affiliateUrl: String? = null,
    val slotNumber: Int? = null,
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
data class VipProductAttribute(
    val name: String = "",
    val value: String = "",
)

@Serializable
data class VipProductReview(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val rating: Int = 0,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val date: String = "",
    val reviewer: String? = null,
)

@Serializable
data class VipAiActionResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val pendingId: String? = null,
    val usage: VipAiUsage? = null,
    val verified: Boolean? = null,
    val productId: String? = null,
    val title: String? = null,
    val price: Double = 0.0,
    @SerialName("original_price") val originalPrice: Double? = null,
    val discount: Int = 0,
    @SerialName("free_shipping") val freeShipping: Boolean = false,
    @SerialName("available_quantity") val availableQuantity: Int = 0,
    val thumbnail: String? = null,
    val images: List<String> = emptyList(),
    val description: String? = null,
    val attributes: List<VipProductAttribute> = emptyList(),
    val reviews: List<VipProductReview> = emptyList(),
    @SerialName("rating_average") val ratingAverage: Double? = null,
    @SerialName("rating_count") val ratingCount: Int? = null,
    val slot: Int? = null,
    val homeProductId: String? = null,
    val homePublished: Boolean? = null,
    val affiliateVerified: Boolean? = null,
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

    private suspend fun sessionToken(): String =
        sessionStore.read()?.accessToken ?: error("Faça login para usar a IA VIP.")

    private fun endpoint(): String {
        val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
        require(baseUrl.isNotBlank()) { "Backend da AfiliShop não configurado." }
        return "$baseUrl/api/mobile/vip/search"
    }

    suspend fun search(query: String, expand: Boolean = false): Result<VipAiSearchResponse> = runCatching {
        val response = client.post(endpoint()) {
            bearerAuth(sessionToken())
            header("Content-Type", "application/json")
            setBody(VipAiSearchRequest(query.trim().take(2048), expand))
        }
        val body = response.body<VipAiSearchResponse>()
        if (response.status.value !in 200..299 || !body.ok) {
            error(body.message ?: body.error ?: "Não foi possível pesquisar agora.")
        }
        body
    }

    private suspend fun action(request: VipAiActionRequest): VipAiActionResponse {
        val response = client.post(endpoint()) {
            bearerAuth(sessionToken())
            header("Content-Type", "application/json")
            setBody(request)
        }
        val body = response.body<VipAiActionResponse>()
        if (response.status.value !in 200..299 || !body.ok) {
            error(body.message ?: body.error ?: "A IA VIP não conseguiu concluir esta etapa.")
        }
        return body
    }

    suspend fun usage(): Result<VipAiActionResponse> =
        runCatching { action(VipAiActionRequest(action = "usage")) }

    suspend fun select(product: VipAiProduct): Result<VipAiActionResponse> =
        runCatching {
            action(
                VipAiActionRequest(
                    action = "select",
                    productId = product.id,
                    permalink = product.permalink,
                    title = product.title,
                    thumbnail = product.thumbnail,
                    price = product.price,
                ),
            )
        }

    suspend fun detail(productId: String, pendingId: String): Result<VipAiActionResponse> =
        runCatching {
            action(
                VipAiActionRequest(
                    action = "detail",
                    productId = productId,
                    pendingId = pendingId,
                ),
            )
        }

    suspend fun validate(pendingId: String, affiliateUrl: String): Result<VipAiActionResponse> =
        runCatching {
            action(
                VipAiActionRequest(
                    action = "validate",
                    pendingId = pendingId,
                    affiliateUrl = affiliateUrl.trim(),
                ),
            )
        }

    suspend fun publish(pendingId: String, affiliateUrl: String): Result<VipAiActionResponse> =
        runCatching {
            action(
                VipAiActionRequest(
                    action = "publish",
                    pendingId = pendingId,
                    affiliateUrl = affiliateUrl.trim(),
                ),
            )
        }

    suspend fun delete(slotNumber: Int): Result<VipAiActionResponse> =
        runCatching {
            action(
                VipAiActionRequest(
                    action = "delete",
                    slotNumber = slotNumber,
                ),
            )
        }

    fun close() {
        client.close()
    }
}
