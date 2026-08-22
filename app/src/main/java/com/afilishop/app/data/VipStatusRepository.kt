package com.afilishop.app.data

import android.content.Context
import com.afilishop.app.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Serializable
private data class VipUsageRow(
    val count: Int = 0,
)

@Serializable
private data class VipExtraCreditsRow(
    val balance: Int = 0,
    @SerialName("total_purchased") val totalPurchased: Int = 0,
)

@Serializable
private data class ExtraSearchRequestPayload(
    @SerialName("_quantity") val quantity: Int,
    @SerialName("_provider") val provider: String = "manual",
)

data class VipStatusSnapshot(
    val usedToday: Int = 0,
    val extraCredits: Int = 0,
    val totalExtraPurchased: Int = 0,
)

class VipStatusRepository(context: Context) {
    private val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val configured = supabaseUrl.isNotBlank() && anonKey.isNotBlank()
    private val sessionStore = SessionStore(context.applicationContext)
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; explicitNulls = false })
        }
        expectSuccess = false
    }

    suspend fun load(): VipStatusSnapshot {
        if (!configured) return VipStatusSnapshot()
        val session = sessionStore.read() ?: return VipStatusSnapshot()
        val userId = session.user?.id ?: return VipStatusSnapshot()
        val utcDay = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate().toString()

        val usageResponse = client.get("$supabaseUrl/rest/v1/vip_ai_search_usage") {
            commonHeaders(session.accessToken)
            url.parameters.append("select", "count")
            url.parameters.append("user_id", "eq.$userId")
            url.parameters.append("day", "eq.$utcDay")
            url.parameters.append("limit", "1")
        }
        val usage = if (usageResponse.status.value in 200..299) {
            usageResponse.body<List<VipUsageRow>>().firstOrNull()
        } else null

        val creditsResponse = client.get("$supabaseUrl/rest/v1/vip_extra_search_credits") {
            commonHeaders(session.accessToken)
            url.parameters.append("select", "balance,total_purchased")
            url.parameters.append("user_id", "eq.$userId")
            url.parameters.append("limit", "1")
        }
        val credits = if (creditsResponse.status.value in 200..299) {
            creditsResponse.body<List<VipExtraCreditsRow>>().firstOrNull()
        } else null

        return VipStatusSnapshot(
            usedToday = usage?.count ?: 0,
            extraCredits = credits?.balance ?: 0,
            totalExtraPurchased = credits?.totalPurchased ?: 0,
        )
    }

    suspend fun createExtraSearchRequest(quantity: Int): Result<Unit> = runCatching {
        require(configured) { "AfiliShop não configurada." }
        require(quantity in 1..100) { "Escolha entre 1 e 100 buscas extras." }
        val session = sessionStore.read() ?: error("Faça login para solicitar buscas extras.")

        val response = client.post("$supabaseUrl/rest/v1/rpc/create_extra_search_request") {
            commonHeaders(session.accessToken)
            header("Content-Type", "application/json")
            setBody(ExtraSearchRequestPayload(quantity))
        }
        if (response.status.value !in 200..299) {
            error(response.bodyAsText().ifBlank { "Não foi possível criar a solicitação." })
        }
    }

    fun close() {
        client.close()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.commonHeaders(token: String) {
        header("apikey", anonKey)
        bearerAuth(token)
    }
}
