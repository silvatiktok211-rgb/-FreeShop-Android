package com.afilishop.app

import com.afilishop.app.model.Product
import com.afilishop.app.model.SignUpRequest
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
    }
}
