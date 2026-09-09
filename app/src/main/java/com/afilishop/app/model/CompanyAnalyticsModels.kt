package com.afilishop.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CompanyTopProduct(
    val id: String,
    val title: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val views: Long = 0,
    val clicks: Long = 0,
    @SerialName("ctr_pct") val ctrPercent: Double = 0.0,
)

@Serializable
data class CompanyAnalytics(
    @SerialName("product_views") val productViews: Long = 0,
    @SerialName("product_clicks") val productClicks: Long = 0,
    @SerialName("product_ctr_pct") val productCtrPercent: Double = 0.0,
    @SerialName("video_views") val videoViews: Long = 0,
    @SerialName("lifetime_video_views") val lifetimeVideoViews: Long = 0,
    @SerialName("lifetime_video_likes") val lifetimeVideoLikes: Long = 0,
    @SerialName("product_count") val productCount: Long = 0,
    @SerialName("video_count") val videoCount: Long = 0,
    @SerialName("top_products") val topProducts: List<CompanyTopProduct> = emptyList(),
    @SerialName("generated_at") val generatedAt: String? = null,
)
