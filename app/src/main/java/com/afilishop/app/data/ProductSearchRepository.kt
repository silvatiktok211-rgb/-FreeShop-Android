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

    suspend fun search(query: String, categoryId: String? = null): List<Product> {
        if (!configured) return emptyList()

        return runCatching {
            val products = mutableListOf<Product>()
            var offset = 0
            val pageSize = 200
            while (true) {
                val response = client.get("$supabaseUrl/rest/v1/products") {
                    header("apikey", anonKey)
                    url.parameters.append(
                        "select",
                        "id,title,price,currency,image_url,images,old_price,discount_percentage,free_shipping,affiliate_url,original_affiliate_url,priority_tier,category_id,created_at",
                    )
                    query.trim().takeIf { it.isNotBlank() }?.let {
                        url.parameters.append("title", "ilike.*$it*")
                    }
                    categoryId?.takeIf { it.isNotBlank() }?.let {
                        url.parameters.append("category_id", "eq.$it")
                    }
                    url.parameters.append("order", "priority_tier.desc,created_at.desc")
                    url.parameters.append("limit", pageSize.toString())
                    url.parameters.append("offset", offset.toString())
                }
                if (response.status.value !in 200..299) error("Catálogo indisponível (${response.status.value}).")
                val page = response.body<List<Product>>()
                products += page
                if (page.size < pageSize) break
                offset += page.size
            }
            products
        }.getOrElse { emptyList() }
    }

    fun close() {
        client.close()
    }
}
