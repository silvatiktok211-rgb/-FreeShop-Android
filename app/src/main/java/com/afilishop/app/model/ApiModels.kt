package com.afilishop.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Product(
    val id: String,
    val title: String,
    val price: Double? = null,
    val currency: String? = "BRL",
    @SerialName("image_url") val imageUrl: String? = null,
    val images: List<String> = emptyList(),
    @SerialName("old_price") val oldPrice: Double? = null,
    @SerialName("discount_percentage") val discountPercentage: Double? = null,
    @SerialName("free_shipping") val freeShipping: Boolean = false,
    @SerialName("affiliate_url") val affiliateUrl: String? = null,
    @SerialName("original_affiliate_url") val originalAffiliateUrl: String? = null,
    val description: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("priority_tier") val priorityTier: Int? = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @Transient val sellerProfile: Profile? = null,
)

@Serializable
data class Category(val id: String, val name: String, val slug: String? = null, val icon: String? = null)

@Serializable
data class Banner(
    val id: String,
    @SerialName("media_url") val mediaUrl: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    @SerialName("link_url") val linkUrl: String? = null,
    val type: String? = null,
)

@Serializable
data class Profile(
    val id: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("verification_type") val verificationType: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("followers_count") val followersCount: Int? = 0,
    @SerialName("following_count") val followingCount: Int? = 0,
)

@Serializable
data class SocialVideo(
    val id: String,
    @SerialName("video_url") val videoUrl: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("description") val caption: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("product_external_url") val productExternalUrl: String? = null,
    @SerialName("product_title") val productTitle: String? = null,
    @SerialName("product_price") val productPrice: Double? = null,
    @SerialName("product_image") val productImage: String? = null,
    @SerialName("shares_count") val sharesCount: Int = 0,
    @SerialName("views_count") val viewsCount: Int = 0,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("likes_count") val likesCount: Int? = 0,
    @SerialName("comments_count") val commentsCount: Int? = 0,
    @SerialName("saves_count") val savesCount: Int? = 0,
    @SerialName("created_at") val createdAt: String? = null,
    val profile: Profile? = null,
    val product: Product? = null,
    @Transient val boostCampaignId: String? = null,
    @Transient val promotionType: String? = null,
) {
    val isPromoted: Boolean get() = !boostCampaignId.isNullOrBlank()
    val promotionLabel: String?
        get() = when (promotionType) {
            "business" -> "Patrocinado"
            "creator" -> "Impulsionado"
            else -> null
        }
}

@Serializable
data class AuthSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    val user: AuthUser? = null,
)

@Serializable
data class AuthUser(val id: String, val email: String? = null)
@Serializable data class SignInRequest(val email: String, val password: String)
@Serializable data class SignUpRequest(val email: String, val password: String, val data: Map<String, String> = emptyMap())

@Serializable
data class HomePayload(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val banners: List<Banner> = emptyList(),
    val premiumProducts: List<Product> = emptyList(),
    val presentation: SitePresentation = SitePresentation(),
)

@Serializable
data class SitePresentation(
    val brandName: String = "AfiliShop",
    val logoUrl: String? = null,
    val appIconUrl: String? = null,
    val loginBackgroundUrl: String? = null,
    val loginTitle: String? = null,
    val loginSubtitle: String? = null,
    val whatsappEnabled: Boolean = false,
    val whatsappNumber: String? = null,
    val whatsappMessage: String = "Olá! Vim pelo aplicativo AfiliShop.",
    val systemAlertEnabled: Boolean = false,
    val systemAlertTitle: String? = null,
    val systemAlertMessage: String? = null,
    val welcomeTitle: String? = null,
    val welcomeSubtitle: String? = null,
)

data class ProductBoost(val productId: String, val weight: Int = 0)

@Serializable
data class PremiumProductRow(
    val id: String,
    @SerialName("ml_item_id") val mlItemId: String? = null,
    @SerialName("affiliate_link") val affiliateLink: String? = null,
    @SerialName("original_affiliate_url") val originalAffiliateUrl: String? = null,
    val title: String,
    @SerialName("current_price") val currentPrice: Double? = null,
    @SerialName("original_price") val originalPrice: Double? = null,
    @SerialName("discount_percentage") val discountPercentage: Double? = null,
    val thumbnail: String? = null,
    val images: List<String> = emptyList(),
    @SerialName("user_id") val userId: String? = null,
    val availability: Boolean = true,
) {
    fun toProduct(): Product = Product(
        id = id,
        title = title,
        price = currentPrice,
        currency = "BRL",
        imageUrl = thumbnail ?: images.firstOrNull(),
        images = images,
        oldPrice = originalPrice,
        discountPercentage = discountPercentage,
        affiliateUrl = affiliateLink,
        originalAffiliateUrl = originalAffiliateUrl,
        userId = userId,
        priorityTier = 3,
    )
}
