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
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class ProfilePrivacyRow(
    @SerialName("hide_last_seen") val hideLastSeen: Boolean = false,
)

@Serializable
private data class NotificationPreferenceRow(
    @SerialName("allow_promo") val allowPromo: Boolean = true,
    @SerialName("allow_price_alerts") val allowPriceAlerts: Boolean = true,
)

@Serializable
private data class NotificationPreferenceUpsert(
    @SerialName("user_id") val userId: String,
    @SerialName("allow_promo") val allowPromo: Boolean,
    @SerialName("allow_price_alerts") val allowPriceAlerts: Boolean,
)

@Serializable
private data class BlockedRow(
    @SerialName("blocked_id") val blockedId: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
private data class BlockedProfileRow(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
private data class UnblockRequest(
    @SerialName("target_user_id") val targetUserId: String,
)

data class BlockedAccount(
    val id: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val blockedAt: String? = null,
)

data class AccountSettingsSnapshot(
    val activityStatusEnabled: Boolean = true,
    val allowPromo: Boolean = true,
    val allowPriceAlerts: Boolean = true,
    val blockedAccounts: List<BlockedAccount> = emptyList(),
)

class AccountSettingsRepository(context: Context) {
    private val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val configured = supabaseUrl.isNotBlank() && anonKey.isNotBlank()
    private val sessionStore = SessionStore(context.applicationContext)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
        expectSuccess = false
    }

    suspend fun load(): AccountSettingsSnapshot {
        if (!configured) return AccountSettingsSnapshot()
        val session = sessionStore.read() ?: return AccountSettingsSnapshot()
        val userId = session.user?.id ?: return AccountSettingsSnapshot()

        val profile = getTable<ProfilePrivacyRow>(
            table = "profiles",
            token = session.accessToken,
            params = mapOf("select" to "hide_last_seen", "id" to "eq.$userId", "limit" to "1"),
        ).firstOrNull()

        val notification = getTable<NotificationPreferenceRow>(
            table = "notification_preferences",
            token = session.accessToken,
            params = mapOf(
                "select" to "allow_promo,allow_price_alerts",
                "user_id" to "eq.$userId",
                "limit" to "1",
            ),
        ).firstOrNull()

        val blocks = getTable<BlockedRow>(
            table = "blocked_users",
            token = session.accessToken,
            params = mapOf(
                "select" to "blocked_id,created_at",
                "blocker_id" to "eq.$userId",
                "order" to "created_at.desc",
                "limit" to "100",
            ),
        )

        val blockedAccounts = blocks.map { block ->
            val blockedProfile = getTable<BlockedProfileRow>(
                table = "profiles",
                token = session.accessToken,
                params = mapOf(
                    "select" to "id,display_name,avatar_url",
                    "id" to "eq.${block.blockedId}",
                    "limit" to "1",
                ),
            ).firstOrNull()
            BlockedAccount(
                id = block.blockedId,
                displayName = blockedProfile?.displayName?.trim().takeUnless { it.isNullOrBlank() }
                    ?: "Usuário da AfiliShop",
                avatarUrl = blockedProfile?.avatarUrl,
                blockedAt = block.createdAt,
            )
        }

        return AccountSettingsSnapshot(
            activityStatusEnabled = profile?.hideLastSeen != true,
            allowPromo = notification?.allowPromo ?: true,
            allowPriceAlerts = notification?.allowPriceAlerts ?: true,
            blockedAccounts = blockedAccounts,
        )
    }

    suspend fun setActivityStatus(enabled: Boolean): Boolean {
        val session = sessionStore.read() ?: return false
        val userId = session.user?.id ?: return false
        val response = client.patch("$supabaseUrl/rest/v1/profiles") {
            commonHeaders(session.accessToken)
            header("Prefer", "return=minimal")
            url.parameters.append("id", "eq.$userId")
            setBody(json.encodeToString(mapOf("hide_last_seen" to !enabled)))
        }
        return response.status.value in 200..299
    }

    suspend fun setNotificationPreferences(allowPromo: Boolean, allowPriceAlerts: Boolean): Boolean {
        val session = sessionStore.read() ?: return false
        val userId = session.user?.id ?: return false
        val response = client.post("$supabaseUrl/rest/v1/notification_preferences") {
            commonHeaders(session.accessToken)
            header("Prefer", "resolution=merge-duplicates,return=minimal")
            url.parameters.append("on_conflict", "user_id")
            setBody(NotificationPreferenceUpsert(userId, allowPromo, allowPriceAlerts))
        }
        return response.status.value in 200..299
    }

    suspend fun unblockUser(targetUserId: String): Boolean {
        if (targetUserId.isBlank()) return false
        val session = sessionStore.read() ?: return false
        val response = client.post("$supabaseUrl/rest/v1/rpc/unblock_user") {
            commonHeaders(session.accessToken)
            setBody(UnblockRequest(targetUserId))
        }
        return response.status.value in 200..299
    }

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        val session = sessionStore.read() ?: error("Sessão não encontrada.")
        val response = client.post("$supabaseUrl/rest/v1/rpc/delete_user_account") {
            commonHeaders(session.accessToken)
            setBody("{}")
        }
        if (response.status.value !in 200..299) {
            error(response.bodyAsText().ifBlank { "Não foi possível excluir a conta." })
        }
        sessionStore.clear()
    }

    fun close() {
        client.close()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.commonHeaders(token: String) {
        header("apikey", anonKey)
        bearerAuth(token)
        header("Content-Type", "application/json")
    }

    private suspend inline fun <reified T> getTable(
        table: String,
        token: String,
        params: Map<String, String>,
    ): List<T> {
        val response = client.get("$supabaseUrl/rest/v1/$table") {
            commonHeaders(token)
            params.forEach { (key, value) -> url.parameters.append(key, value) }
        }
        if (response.status.value !in 200..299) return emptyList()
        return response.body()
    }
}
