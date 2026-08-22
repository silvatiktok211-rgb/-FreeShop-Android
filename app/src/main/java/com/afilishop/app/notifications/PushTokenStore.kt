package com.afilishop.app.notifications

import android.content.Context

object PushTokenStore {
    private const val PREFERENCES = "afilishop_push"
    private const val PENDING_TOKEN = "pending_token"
    private const val CURRENT_TOKEN = "current_token"
    private const val PERMISSION_REQUESTED = "permission_requested"

    fun pending(context: Context): String? = preferences(context).getString(PENDING_TOKEN, null)

    fun current(context: Context): String? = preferences(context).getString(CURRENT_TOKEN, null)

    fun savePending(context: Context, token: String) {
        if (token.isBlank()) return
        preferences(context).edit().putString(PENDING_TOKEN, token).apply()
    }

    fun markSynced(context: Context, token: String) {
        preferences(context).edit()
            .putString(CURRENT_TOKEN, token)
            .remove(PENDING_TOKEN)
            .apply()
    }

    fun permissionWasRequested(context: Context): Boolean =
        preferences(context).getBoolean(PERMISSION_REQUESTED, false)

    fun markPermissionRequested(context: Context) {
        preferences(context).edit().putBoolean(PERMISSION_REQUESTED, true).apply()
    }

    fun clear(context: Context) {
        preferences(context).edit()
            .remove(PENDING_TOKEN)
            .remove(CURRENT_TOKEN)
            .apply()
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
