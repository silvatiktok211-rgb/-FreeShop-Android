package com.afilishop.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class AfiliShopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.FIREBASE_PROJECT_ID.isBlank() || BuildConfig.FIREBASE_APP_ID.isBlank() || BuildConfig.FIREBASE_API_KEY.isBlank()) return
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this, FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                .build())
        }
    }
}
