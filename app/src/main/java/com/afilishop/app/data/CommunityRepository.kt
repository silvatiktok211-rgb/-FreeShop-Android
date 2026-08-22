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
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

@Serializable
data class CommunityProfile(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
)

@Serializable
private data class CommunityConversationRow(
    val id: String,
    @SerialName("user_a") val userA: String,
    @SerialName("user_b") val userB: String,
    val status: String = "accepted",
    @SerialName("requested_by") val requestedBy: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("last_message_preview") val lastMessagePreview: String? = null,
    @SerialName("last_message_sender") val lastMessageSender: String? = null,
    @SerialName("pinned_by_a") val pinnedByA: Boolean = false,
    @SerialName("pinned_by_b") val pinnedByB: Boolean = false,
    @SerialName("unread_a") val unreadA: Int = 0,
    @SerialName("unread_b") val unreadB: Int = 0,
)

data class CommunityConversation(
    val id: String,
    val userA: String,
    val userB: String,
    val status: String,
    val requestedBy: String?,
    val lastMessageAt: String?,
    val lastMessagePreview: String?,
    val lastMessageSender: String?,
    val pinned: Boolean,
    val unread: Int,
    val otherProfile: CommunityProfile?,
) {
    fun otherUserId(currentUserId: String): String = if (userA == currentUserId) userB else userA
}

@Serializable
data class CommunityMessage(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("attachment_type") val attachmentType: String? = null,
    @SerialName("attachment_url") val attachmentUrl: String? = null,
    @SerialName("is_flagged") val isFlagged: Boolean = false,
)

@Serializable
private data class CommunityMessageInsert(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
)

class CommunityRepository(context: Context) {
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

    suspend fun currentUserId(): String? = sessionStore.read()?.user?.id

    suspend fun touchLastSeen() {
        if (!configured) return
        val session = sessionStore.read() ?: return
        runCatching {
            client.post("$supabaseUrl/rest/v1/rpc/touch_last_seen") {
                commonHeaders(session.accessToken)
                header("Content-Type", "application/json")
                setBody(emptyMap<String, String>())
            }
        }
    }

    suspend fun loadConversations(): List<CommunityConversation> {
        if (!configured) return emptyList()
        val session = sessionStore.read() ?: return emptyList()
        val userId = session.user?.id ?: return emptyList()
        val select = "id,user_a,user_b,status,requested_by,last_message_at,last_message_preview,last_message_sender,pinned_by_a,pinned_by_b,unread_a,unread_b"

        val rows = runCatching {
            val asA = getConversations(select, "user_a", userId, session.accessToken)
            val asB = getConversations(select, "user_b", userId, session.accessToken)
            (asA + asB).distinctBy { it.id }
        }.getOrElse { emptyList() }

        if (rows.isEmpty()) return emptyList()
        val otherIds = rows.map { if (it.userA == userId) it.userB else it.userA }.distinct()
        val profiles = loadProfiles(otherIds, session.accessToken).associateBy { it.id }

        return rows.map { row ->
            val isA = row.userA == userId
            val otherId = if (isA) row.userB else row.userA
            CommunityConversation(
                id = row.id,
                userA = row.userA,
                userB = row.userB,
                status = row.status,
                requestedBy = row.requestedBy,
                lastMessageAt = row.lastMessageAt,
                lastMessagePreview = row.lastMessagePreview,
                lastMessageSender = row.lastMessageSender,
                pinned = if (isA) row.pinnedByA else row.pinnedByB,
                unread = if (isA) row.unreadA else row.unreadB,
                otherProfile = profiles[otherId],
            )
        }.sortedWith(
            compareByDescending<CommunityConversation> { it.pinned }
                .thenByDescending { it.lastMessageAt.orEmpty() },
        )
    }

    suspend fun loadConversation(conversationId: String): CommunityConversation? {
        val userId = currentUserId() ?: return null
        return loadConversations().firstOrNull { it.id == conversationId && (it.userA == userId || it.userB == userId) }
    }

    suspend fun acceptConversation(conversationId: String): Boolean {
        if (!configured || conversationId.isBlank()) return false
        val session = sessionStore.read() ?: return false
        val response = client.patch("$supabaseUrl/rest/v1/conversations") {
            commonHeaders(session.accessToken)
            header("Content-Type", "application/json")
            header("Prefer", "return=minimal")
            url.parameters.append("id", "eq.$conversationId")
            setBody(mapOf("status" to "accepted"))
        }
        return response.status.value in 200..299
    }

    suspend fun loadMessages(conversationId: String): List<CommunityMessage> {
        if (!configured || conversationId.isBlank()) return emptyList()
        val session = sessionStore.read() ?: return emptyList()
        return runCatching {
            val response = client.get("$supabaseUrl/rest/v1/messages") {
                commonHeaders(session.accessToken)
                url.parameters.append(
                    "select",
                    "id,conversation_id,sender_id,content,created_at,read_at,delivered_at,reply_to_id,attachment_type,attachment_url,is_flagged",
                )
                url.parameters.append("conversation_id", "eq.$conversationId")
                url.parameters.append("order", "created_at.asc")
                url.parameters.append("limit", "100")
            }
            if (response.status.value !in 200..299) emptyList() else response.body<List<CommunityMessage>>()
        }.getOrElse { emptyList() }
    }

    suspend fun sendMessage(conversationId: String, content: String): Boolean {
        if (!configured || conversationId.isBlank() || content.isBlank()) return false
        val session = sessionStore.read() ?: return false
        val userId = session.user?.id ?: return false
        val response = client.post("$supabaseUrl/rest/v1/messages") {
            commonHeaders(session.accessToken)
            header("Content-Type", "application/json")
            header("Prefer", "return=minimal")
            setBody(CommunityMessageInsert(conversationId, userId, content.trim()))
        }
        return response.status.value in 200..299
    }

    suspend fun markRead(conversationId: String) {
        if (!configured || conversationId.isBlank()) return
        val session = sessionStore.read() ?: return
        val userId = session.user?.id ?: return
        runCatching {
            client.patch("$supabaseUrl/rest/v1/messages") {
                commonHeaders(session.accessToken)
                header("Content-Type", "application/json")
                header("Prefer", "return=minimal")
                url.parameters.append("conversation_id", "eq.$conversationId")
                url.parameters.append("sender_id", "neq.$userId")
                url.parameters.append("read_at", "is.null")
                setBody(mapOf("read_at" to Instant.now().toString()))
            }
        }
    }

    fun close() {
        client.close()
    }

    private suspend fun getConversations(
        select: String,
        field: String,
        userId: String,
        token: String,
    ): List<CommunityConversationRow> {
        val response = client.get("$supabaseUrl/rest/v1/conversations") {
            commonHeaders(token)
            url.parameters.append("select", select)
            url.parameters.append(field, "eq.$userId")
            url.parameters.append("order", "last_message_at.desc")
            url.parameters.append("limit", "100")
        }
        return if (response.status.value in 200..299) response.body() else emptyList()
    }

    private suspend fun loadProfiles(ids: List<String>, token: String): List<CommunityProfile> {
        if (ids.isEmpty()) return emptyList()
        val response = client.get("$supabaseUrl/rest/v1/profiles") {
            commonHeaders(token)
            url.parameters.append("select", "id,display_name,avatar_url,last_seen_at")
            url.parameters.append("id", "in.(${ids.joinToString(",")})")
        }
        return if (response.status.value in 200..299) response.body() else emptyList()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.commonHeaders(token: String) {
        header("apikey", anonKey)
        bearerAuth(token)
    }
}
