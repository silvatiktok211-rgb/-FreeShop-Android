package com.afilishop.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.afilishop.app.model.AuthSession
import com.afilishop.app.model.AuthUser
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first

private val Context.afiliShopSessionStore by preferencesDataStore(name = "afilishop_session")

private class SessionCrypto {
    private companion object {
        const val KEY_ALIAS = "afilishop_session_aes_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFIX = "v1:"
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    fun isEncrypted(value: String?): Boolean = value?.startsWith(PREFIX) == true

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encrypted = Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        return "$PREFIX$iv.$encrypted"
    }

    fun decrypt(value: String?): String? {
        if (value.isNullOrBlank()) return null
        if (!isEncrypted(value)) return value

        return runCatching {
            val parts = value.removePrefix(PREFIX).split('.', limit = 2)
            require(parts.size == 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrNull()
    }
}

class SessionStore(private val context: Context) {
    private object Keys {
        val accessToken = stringPreferencesKey("access_token")
        val refreshToken = stringPreferencesKey("refresh_token")
        val userId = stringPreferencesKey("user_id")
        val email = stringPreferencesKey("email")
    }

    private val crypto = SessionCrypto()

    suspend fun read(): AuthSession? {
        val preferences = context.afiliShopSessionStore.data.first()
        val rawAccessToken = preferences[Keys.accessToken] ?: return null
        val accessToken = crypto.decrypt(rawAccessToken)
        if (accessToken.isNullOrBlank()) {
            clear()
            return null
        }

        val rawRefresh = preferences[Keys.refreshToken]
        val rawEmail = preferences[Keys.email]
        val userId = preferences[Keys.userId]
        val session = AuthSession(
            accessToken = accessToken,
            refreshToken = crypto.decrypt(rawRefresh),
            user = userId?.let { AuthUser(it, crypto.decrypt(rawEmail)) },
        )

        if (!crypto.isEncrypted(rawAccessToken) ||
            (rawRefresh != null && !crypto.isEncrypted(rawRefresh)) ||
            (rawEmail != null && !crypto.isEncrypted(rawEmail))
        ) {
            write(session)
        }

        return session
    }

    suspend fun write(session: AuthSession) {
        context.afiliShopSessionStore.edit { preferences ->
            preferences[Keys.accessToken] = crypto.encrypt(session.accessToken)
            if (session.refreshToken.isNullOrBlank()) {
                preferences.remove(Keys.refreshToken)
            } else {
                preferences[Keys.refreshToken] = crypto.encrypt(session.refreshToken)
            }

            if (session.user == null) {
                preferences.remove(Keys.userId)
                preferences.remove(Keys.email)
            } else {
                preferences[Keys.userId] = session.user.id
                if (session.user.email.isNullOrBlank()) {
                    preferences.remove(Keys.email)
                } else {
                    preferences[Keys.email] = crypto.encrypt(session.user.email)
                }
            }
        }
    }

    suspend fun clear() {
        context.afiliShopSessionStore.edit { it.clear() }
    }
}
