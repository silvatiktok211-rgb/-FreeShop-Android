package com.afilishop.app.data

import android.content.Context
import com.afilishop.app.BuildConfig
import com.afilishop.app.model.Product
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class FavoriteProductId(
    @SerialName("product_id") val productId: String,
)

class FavoriteProductsRepository(context: Context) {
    private val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val configured = supabaseUrl.isNotBlank() && anonKey.isNotBlank()
    private val sessionStore = SessionStore(context.applicationContext)
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false })
        }
        expectSuccess = false
    }

    suspend fun load(): List<Product> {
        if (!configured) return emptyList()
        val session = sessionStore.read() ?: return emptyList()
        val userId = session.user?.id ?: return emptyList()

        val favoritesResponse = client.get("$supabaseUrl/rest/v1/favorites") {
            commonHeaders(session.accessToken)
            url.parameters.append("select", "product_id")
            url.parameters.append("user_id", "eq.$userId")
            url.parameters.append("limit", "500")
        }
        if (favoritesResponse.status.value !in 200..299) return emptyList()

        val ids = favoritesResponse.body<List<FavoriteProductId>>()
            .map { it.productId }
            .distinct()
        if (ids.isEmpty()) return emptyList()

        val productsResponse = client.get("$supabaseUrl/rest/v1/products") {
            commonHeaders(session.accessToken)
            url.parameters.append(
                "select",
                "id,title,price,currency,image_url,images,old_price,discount_percentage,free_shipping,affiliate_url,original_affiliate_url,priority_tier,category_id,created_at",
            )
            url.parameters.append("id", "in.(${ids.joinToString(",")})")
            url.parameters.append("order", "created_at.desc")
            url.parameters.append("limit", ids.size.coerceAtMost(500).toString())
        }
        if (productsResponse.status.value !in 200..299) return emptyList()

        val byId = productsResponse.body<List<Product>>().associateBy { it.id }
        return ids.mapNotNull(byId::get)
    }

    fun close() {
        client.close()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.commonHeaders(token: String) {
        header("apikey", anonKey)
        bearerAuth(token)
    }
}
