package com.afilishop.app.data

import android.content.Context
import com.afilishop.app.BuildConfig
import com.afilishop.app.model.CompanyAnalytics
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
private data class CompanyAnalyticsRequest(val _since: String)

class CompanyAnalyticsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val sessionStore = SessionStore(appContext)
    private val wireJson = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        install(ContentNegotiation) { json(wireJson) }
        expectSuccess = false
    }

    suspend fun load(days: Int): Result<CompanyAnalytics> = runCatching {
        require(days == 7 || days == 30) { "Período inválido" }
        check(supabaseUrl.isNotBlank() && anonKey.isNotBlank()) { "Backend do AfiliShop não configurado." }
        val session = sessionStore.read() ?: error("Entre na sua conta para abrir o painel da empresa.")
        val since = Instant.now().minus(days.toLong(), ChronoUnit.DAYS).toString()
        val response = client.post("$supabaseUrl/rest/v1/rpc/get_creator_vip_analytics") {
            header("apikey", anonKey)
            bearerAuth(session.accessToken)
            header("Content-Type", ContentType.Application.Json.toString())
            setBody(CompanyAnalyticsRequest(since))
        }
        val body = response.bodyAsText()
        if (response.status.value !in 200..299) {
            val friendly = when (response.status.value) {
                401 -> "Sua sessão expirou. Entre novamente para ver as métricas."
                403 -> "As métricas avançadas ainda não estão liberadas para este plano."
                else -> "Não foi possível carregar as métricas comerciais agora."
            }
            error(friendly)
        }
        wireJson.decodeFromString<CompanyAnalytics>(body)
    }
}
