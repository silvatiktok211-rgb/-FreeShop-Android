package com.afilishop.app

import com.afilishop.app.notifications.notificationDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationRoutesTest {
    @Test
    fun mapsRelativeProductAndMessageLinks() {
        assertEquals("product/abc-123", notificationDestination("/produto/abc-123"))
        assertEquals("conversation/conv-9", notificationDestination("/mensagens/conv-9"))
    }

    @Test
    fun mapsAbsoluteSiteLinksAndIgnoresQuery() {
        assertEquals(
            "videos",
            notificationDestination("https://afilishop.lovable.app/videos?v=video-42"),
        )
        assertEquals("vip", notificationDestination("https://afilishop.lovable.app/planos"))
    }

    @Test
    fun mapsNotificationCenterAndRoot() {
        assertEquals("notifications", notificationDestination("/notificacoes"))
        assertEquals("home", notificationDestination("/"))
    }

    @Test
    fun keepsOwnProfileAndSettingsInsideAccount() {
        assertEquals("account/profile", notificationDestination("/perfil"))
        assertEquals("account/settings", notificationDestination("/configuracoes"))
        assertEquals("account/settings", notificationDestination("/conta/configuracoes"))
        assertEquals("profile/criador-1", notificationDestination("/perfil/criador-1"))
    }

    @Test
    fun leavesUnknownExternalDestinationForTheBrowser() {
        assertNull(notificationDestination("https://example.com/oferta"))
    }
}
