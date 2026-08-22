package com.afilishop.app.data

import android.content.Context
import com.afilishop.app.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

@Serializable
data class NativeNotification(
    val id: String,
    val title: String,
    val body: String? = null,
    val type: String? = null,
    val link: String? = null,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    val isRead: Boolean get() = !readAt.isNullOrBlank()
}

class NotificationRepository(context: Context) {
    private val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val configured = supabaseUrl.isNotBlank() && anonKey.isNotBlank()
    private val sessionStore = SessionStore(context.applicationContext)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
        expectSuccess = false
    }

    suspend fun load(): List<NativeNotification> {
        if (!configured) return emptyList()
        val session = sessionStore.read() ?: return emptyList()
        val userId = session.user?.id ?: return emptyList()
        val response = client.get("$supabaseUrl/rest/v1/notifications") {
            commonHeaders(session.accessToken)
            url.parameters.append("select", "id,title,body,type,link,product_id,read_at,created_at")
            url.parameters.append("or", "(user_id.eq.$userId,user_id.is.null)")
            url.parameters.append("order", "created_at.desc")
            url.parameters.append("limit", "80")
        }
        if (response.status.value !in 200..299) return emptyList()
        return response.body()
    }

    suspend fun markRead(id: String): Boolean {
        if (id.isBlank()) return false
        val session = sessionStore.read() ?: return false
        val response = client.patch("$supabaseUrl/rest/v1/notifications") {
            commonHeaders(session.accessToken)
            header("Content-Type", "application/json")
            header("Prefer", "return=minimal")
            url.parameters.append("id", "eq.$id")
            setBody(json.encodeToString(mapOf("read_at" to Instant.now().toString())))
        }
        return response.status.value in 200..299
    }

    suspend fun markAllRead(): Boolean {
        val session = sessionStore.read() ?: return false
        val userId = session.user?.id ?: return false
        val response = client.patch("$supabaseUrl/rest/v1/notifications") {
            commonHeaders(session.accessToken)
            header("Content-Type", "application/json")
            header("Prefer", "return=minimal")
            url.parameters.append("user_id", "eq.$userId")
            url.parameters.append("read_at", "is.null")
            setBody(json.encodeToString(mapOf("read_at" to Instant.now().toString())))
        }
        return response.status.value in 200..299
    }

    suspend fun delete(id: String): Boolean {
        if (id.isBlank()) return false
        val session = sessionStore.read() ?: return false
        val response = client.delete("$supabaseUrl/rest/v1/notifications") {
            commonHeaders(session.accessToken)
            header("Prefer", "return=minimal")
            url.parameters.append("id", "eq.$id")
        }
        return response.status.value in 200..299
    }

    fun close() {
        client.close()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.commonHeaders(token: String) {
        header("apikey", anonKey)
        bearerAuth(token)
    }
}
