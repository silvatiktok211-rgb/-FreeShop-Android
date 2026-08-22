package com.afilishop.app

import com.afilishop.app.data.PlayBillingSkus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayBillingSkusTest {
    @Test
    fun mapsEveryPaidPlanToBackendSku() {
        assertEquals("empreendedor_monthly", PlayBillingSkus.forPlanCode("empreendedor"))
        assertEquals("profissional_monthly", PlayBillingSkus.forPlanCode("professional"))
        assertEquals("profissional_monthly", PlayBillingSkus.forPlanCode("profissional"))
        assertEquals("dominante_monthly", PlayBillingSkus.forPlanCode("dominante"))
    }

    @Test
    fun rejectsFreeAndUnknownPlans() {
        assertNull(PlayBillingSkus.forPlanCode("free"))
        assertNull(PlayBillingSkus.forPlanCode("desconhecido"))
    }
}
