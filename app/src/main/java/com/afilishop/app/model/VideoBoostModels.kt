package com.afilishop.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoBoostPlan(
    val id: String? = null,
    val code: String,
    val name: String,
    @SerialName("audience_type") val audienceType: String,
    @SerialName("duration_hours") val durationHours: Int,
    @SerialName("price_brl") val priceBrl: Double,
    @SerialName("google_play_sku") val googlePlaySku: String,
    @SerialName("estimated_reach_min") val estimatedReachMin: Int = 0,
    @SerialName("estimated_reach_max") val estimatedReachMax: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class VideoBoostCampaign(
    val id: String,
    @SerialName("video_id") val videoId: String,
    @SerialName("plan_code") val planCode: String,
    @SerialName("campaign_type") val campaignType: String,
    @SerialName("amount_brl") val amountBrl: Double,
    @SerialName("duration_hours") val durationHours: Int,
    val status: String,
    @SerialName("payment_status") val paymentStatus: String,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    val impressions: Long = 0,
    @SerialName("qualified_views") val qualifiedViews: Long = 0,
    @SerialName("profile_visits") val profileVisits: Long = 0,
    @SerialName("product_clicks") val productClicks: Long = 0,
)

@Serializable
data class ActiveVideoBoost(
    @SerialName("campaign_id") val campaignId: String,
    @SerialName("video_id") val videoId: String,
    @SerialName("campaign_type") val campaignType: String,
    @SerialName("ends_at") val endsAt: String? = null,
)
