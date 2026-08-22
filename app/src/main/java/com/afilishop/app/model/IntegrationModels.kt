package com.afilishop.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationItem(
    val id: String,
    val title: String,
    val body: String? = null,
    val type: String? = null,
    val link: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class NotificationPreferences(
    @SerialName("push_enabled") val pushEnabled: Boolean = true,
    @SerialName("email_enabled") val emailEnabled: Boolean = true
)

@Serializable
data class Conversation(
    val id: String,
    @SerialName("user_a") val userA: String? = null,
    @SerialName("user_b") val userB: String? = null,
    val status: String? = null,
    @SerialName("last_message_preview") val lastMessagePreview: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("unread_count") val unreadCount: Int? = 0,
    val otherProfile: Profile? = null
)

@Serializable
data class ChatMessage(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String? = null,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_flagged") val isFlagged: Boolean = false
)

@Serializable
data class LiveRoom(
    val id: String,
    val title: String? = null,
    @SerialName("host_id") val hostId: String? = null,
    @SerialName("is_live") val isLive: Boolean = false,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    val channel: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("viewers_count") val viewerCount: Int? = 0,
    @SerialName("peak_viewers") val peakViewers: Int? = 0,
    val host: Profile? = null
)

@Serializable
data class LiveGift(
    val id: String,
    val name: String,
    val emoji: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("cost_points") val costPoints: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    val benefits: List<String> = emptyList()
)

@Serializable
data class UserSubscription(
    val tier: Int? = 0,
    val status: String? = null,
    @SerialName("plan_id") val planId: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("slots_total") val slotsTotal: Int? = 0,
    @SerialName("slots_used") val slotsUsed: Int? = 0
)

@Serializable
data class PublishVideoRequest(
    @SerialName("video_url") val videoUrl: String,
    val caption: String? = null,
    @SerialName("is_published") val isPublished: Boolean = true
)

@Serializable
data class PushTokenPayload(
    @SerialName("user_id") val userId: String,
    val token: String,
    val platform: String = "android"
)

@Serializable
data class ChatMessageInsert(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String
)

@Serializable
data class NotificationReadUpdate(@SerialName("is_read") val isRead: Boolean = true)

@Serializable
data class NotificationPreferencesPayload(
    @SerialName("user_id") val userId: String,
    @SerialName("push_enabled") val pushEnabled: Boolean,
    @SerialName("email_enabled") val emailEnabled: Boolean
)

@Serializable
data class PlayBillingActivationRequest(
    val sku: String,
    val purchaseToken: String
)

@Serializable
data class UserRole(
    @SerialName("user_id") val userId: String,
    val role: String
)

@Serializable
data class RefreshTokenRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class AuthUserUpdateRequest(
    val data: Map<String, String> = emptyMap(),
    val password: String? = null
)

@Serializable
data class ProfileUpdatePayload(
    @SerialName("display_name") val displayName: String? = null,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class FavoriteRow(@SerialName("product_id") val productId: String)

@Serializable
data class PointBalance(
    val balance: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class PointTransaction(
    val id: String,
    val delta: Int,
    val reason: String,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class Store(
    val id: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("banner_url") val bannerUrl: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("rating_avg") val ratingAvg: Double = 0.0,
    @SerialName("rating_count") val ratingCount: Int = 0
)

@Serializable
data class ProductOffer(
    val id: String,
    val store: String? = null,
    val url: String,
    val price: Double? = null,
    val currency: String? = "BRL"
)

@Serializable
data class PricePoint(val price: Double, @SerialName("recorded_at") val recordedAt: String? = null)

@Serializable
data class ProductComment(
    val id: String,
    @SerialName("product_id") val productId: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("is_promoted") val isPromoted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    val profile: Profile? = null
)

@Serializable
data class VideoComment(
    val id: String,
    @SerialName("video_id") val videoId: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class RankingEntry(
    @SerialName("user_id") val userId: String,
    val points: Int = 0,
    val rank: Int = 0,
    val profile: Profile? = null
)

@Serializable
data class SubscriptionPlan(
    val id: String,
    val code: String,
    val name: String,
    @SerialName("price_brl") val priceBrl: Double = 0.0,
    val slots: Int = 0,
    val tier: Int = 0,
    @SerialName("duration_days") val durationDays: Int? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    val benefits: List<String> = emptyList()
)

@Serializable
data class PlanFeature(
    val key: String,
    val name: String,
    @SerialName("min_tier") val minTier: Int = 0,
    @SerialName("limit_value") val limitValue: Int? = null,
    @SerialName("limit_period") val limitPeriod: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class FavoriteTogglePayload(
    @SerialName("user_id") val userId: String,
    @SerialName("product_id") val productId: String
)

@Serializable
data class ProductCommentInsert(
    @SerialName("product_id") val productId: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("parent_id") val parentId: String? = null
)

@Serializable
data class VideoCommentInsert(
    @SerialName("video_id") val videoId: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("parent_id") val parentId: String? = null
)

@Serializable
data class SocialVideoInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("video_url") val videoUrl: String,
    val description: String? = null,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("product_external_url") val productExternalUrl: String? = null,
    @SerialName("product_title") val productTitle: String? = null,
    @SerialName("product_price") val productPrice: Double? = null,
    @SerialName("product_image") val productImage: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class VideoLikeRow(@SerialName("video_id") val videoId: String)

@Serializable
data class LiveActionResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val live: LiveRoom? = null,
    @SerialName("appId") val appId: String? = null,
    val token: String? = null,
    val uid: Int? = null,
    val channel: String? = null
)

@Serializable
data class LiveActionRequest(
    val action: String,
    @SerialName("liveId") val liveId: String? = null,
    val title: String? = null,
    val coverUrl: String? = null,
    @SerialName("giftId") val giftId: String? = null,
    val quantity: Int? = null
)

@Serializable
data class AdminStats(
    val products: Int = 0,
    val videos: Int = 0,
    val users: Int = 0,
    val unreadNotifications: Int = 0,
    val activeLives: Int = 0
)

@Serializable
data class AdminUser(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AdminBanner(
    val id: String,
    val title: String? = null,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0
)

@Serializable
data class AdminResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val stats: AdminStats? = null,
    val users: List<AdminUser> = emptyList(),
    val banners: List<AdminBanner> = emptyList()
)

@Serializable
data class AdminActionRequest(
    val action: String,
    val id: String? = null,
    val userId: String? = null,
    val role: String? = null,
    val title: String? = null,
    val mediaUrl: String? = null,
    val isActive: Boolean? = null,
    val sortOrder: Int? = null
)

@Serializable
data class FollowRow(
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String
)

@Serializable
data class FollowPayload(
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String
)
