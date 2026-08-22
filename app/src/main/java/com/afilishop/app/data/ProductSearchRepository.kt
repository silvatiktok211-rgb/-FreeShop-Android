package com.afilishop.app.data

import com.afilishop.app.BuildConfig
import com.afilishop.app.model.Product
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ProductSearchRepository {
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

    suspend fun search(query: String, categoryId: String? = null, limit: Int = 60): List<Product> {
        if (!configured) return emptyList()

        return runCatching {
            val response = client.get("$supabaseUrl/rest/v1/products") {
                header("apikey", anonKey)
                url.parameters.append(
                    "select",
                    "id,title,price,currency,image_url,old_price,discount_percentage,free_shipping,affiliate_url,original_affiliate_url,priority_tier,category_id,created_at",
                )
                query.trim().takeIf { it.isNotBlank() }?.let {
                    url.parameters.append("title", "ilike.*$it*")
                }
                categoryId?.takeIf { it.isNotBlank() }?.let {
                    url.parameters.append("category_id", "eq.$it")
                }
                url.parameters.append("order", "priority_tier.desc,created_at.desc")
                url.parameters.append("limit", limit.coerceIn(1, 100).toString())
            }

            if (response.status.value !in 200..299) return@runCatching emptyList()
            response.body<List<Product>>()
        }.getOrElse { emptyList() }
    }

    fun close() {
        client.close()
    }
}
