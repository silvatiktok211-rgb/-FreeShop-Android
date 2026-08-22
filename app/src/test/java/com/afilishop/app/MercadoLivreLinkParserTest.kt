package com.afilishop.app

import com.afilishop.app.util.MercadoLivreCandidate
import com.afilishop.app.util.MercadoLivreLinkParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MercadoLivreLinkParserTest {
    @Test
    fun catalogPathIsNotTreatedAsListingItem() {
        val result = MercadoLivreLinkParser.resolve("https://www.mercadolivre.com.br/p/MLB12345678")
        assertEquals(listOf("MLB12345678"), result.catalogIds)
        assertTrue(result.itemIds.isEmpty())
    }

    @Test
    fun userProductPathIsKeptSeparate() {
        val result = MercadoLivreLinkParser.resolve("https://www.mercadolivre.com.br/up/MLBU99887766")
        assertEquals(listOf("MLBU99887766"), result.userProductIds)
        assertTrue(result.candidates.first() is MercadoLivreCandidate.UserProduct)
    }

    @Test
    fun shareParameterWinsAsItemCandidate() {
        val result = MercadoLivreLinkParser.resolve("https://mercadolivre.com.br/item?wid=MLB987654321")
        assertEquals(listOf("MLB987654321"), result.itemIds)
        assertEquals("share_parameter_wid", result.candidates.first().reason)
    }
}
