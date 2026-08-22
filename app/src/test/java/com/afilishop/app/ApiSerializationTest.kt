package com.afilishop.app

import com.afilishop.app.model.Product
import com.afilishop.app.model.PremiumProductRow
import com.afilishop.app.model.SignUpRequest
import com.afilishop.app.model.SocialVideo
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun signUpRequestHasGeneratedSerializerAndMatchesSupabasePayload() {
        val encoded = json.encodeToString(
            SignUpRequest(
                email = "cliente@afilishop.com",
                password = "senha-segura",
                data = mapOf("display_name" to "Cliente"),
            ),
        )
        val payload = json.parseToJsonElement(encoded).jsonObject

        assertEquals("cliente@afilishop.com", payload.getValue("email").jsonPrimitive.content)
        assertEquals("senha-segura", payload.getValue("password").jsonPrimitive.content)
        assertEquals(
            "Cliente",
            payload.getValue("data").jsonObject.getValue("display_name").jsonPrimitive.content,
        )
    }

    @Test
    fun publicCatalogPayloadDecodesWithoutAuthenticatedUserData() {
        val payload = """
            [{
              "id":"produto-1",
              "title":"Produto público",
              "price":49.9,
              "currency":"BRL",
              "image_url":"https://cdn.example.com/produto.webp",
              "images":["https://cdn.example.com/produto-2.webp"],
              "old_price":69.9,
              "discount_percentage":29,
              "free_shipping":true,
              "affiliate_url":"https://example.com/comprar",
              "original_affiliate_url":null,
              "priority_tier":3,
              "category_id":"categoria-1"
            }]
        """.trimIndent()

        val products = json.decodeFromString<List<Product>>(payload)

        assertEquals(1, products.size)
        assertEquals("Produto público", products.single().title)
        assertTrue(products.single().freeShipping)
        assertEquals(3, products.single().priorityTier)
        assertEquals(1, products.single().images.size)
    }

    @Test
    fun premiumCatalogMapsThumbnailGalleryAndOffer() {
        val payload = """
            [{
              "id":"premium-1",
              "ml_item_id":"MLB123",
              "affiliate_link":"https://example.com/afiliado",
              "original_affiliate_url":"https://example.com/oferta",
              "title":"Oferta Premium",
              "current_price":199.9,
              "original_price":249.9,
              "discount_percentage":20,
              "thumbnail":"https://cdn.example.com/capa.webp",
              "images":["https://cdn.example.com/1.webp","https://cdn.example.com/2.webp"],
              "user_id":"criador-1",
              "availability":true
            }]
        """.trimIndent()

        val premium = json.decodeFromString<List<PremiumProductRow>>(payload).single().toProduct()

        assertEquals("Oferta Premium", premium.title)
        assertEquals("https://cdn.example.com/capa.webp", premium.imageUrl)
        assertEquals(2, premium.images.size)
        assertEquals("https://example.com/oferta", premium.originalAffiliateUrl)
        assertEquals(3, premium.priorityTier)
    }

    @Test
    fun activeVideoPayloadDecodesAllMediaFields() {
        val payload = """
            [{
              "id":"video-1",
              "video_url":"https://cdn.example.com/video.mp4",
              "thumbnail_url":null,
              "description":"Achado do dia",
              "is_active":true,
              "is_featured":false,
              "product_id":null,
              "product_external_url":"https://example.com/oferta",
              "product_title":"Produto do vídeo",
              "product_price":89.9,
              "product_image":null,
              "shares_count":2,
              "views_count":1200,
              "user_id":"criador-1",
              "likes_count":10,
              "comments_count":3,
              "created_at":"2026-08-22T00:00:00Z"
            }]
        """.trimIndent()

        val video = json.decodeFromString<List<SocialVideo>>(payload).single()

        assertTrue(video.isActive)
        assertEquals("https://cdn.example.com/video.mp4", video.videoUrl)
        assertEquals("Produto do vídeo", video.productTitle)
        assertEquals(1200, video.viewsCount)
    }
}
