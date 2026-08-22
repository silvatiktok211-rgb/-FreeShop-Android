package com.afilishop.app

import com.afilishop.app.data.parseOAuthCallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OAuthCallbackTest {
    @Test
    fun parsesPkceCodeFromNativeCallback() {
        val callback = parseOAuthCallback("afilishop://auth-callback?code=pkce-code-123")

        assertEquals("pkce-code-123", callback?.code)
        assertNull(callback?.error)
    }

    @Test
    fun decodesProviderCancellationError() {
        val callback = parseOAuthCallback(
            "afilishop://auth-callback?error=access_denied&error_description=Usu%C3%A1rio%20cancelou",
        )

        assertEquals("Usuário cancelou", callback?.error)
    }

    @Test
    fun rejectsCallbacksFromAnotherApp() {
        assertNull(parseOAuthCallback("https://evil.example/auth-callback?code=stolen"))
        assertNull(parseOAuthCallback("afilishop://outro-host?code=stolen"))
    }
}
