# Kotlin serialization and Compose reflection-free models are kept by generated serializers.
-keep class com.afilishop.app.model.** { *; }
-keep class androidx.media3.** { *; }
-dontwarn kotlinx.serialization.**

# Agora RTC usa JNI/reflexão em partes do SDK nativo.
-keep class io.agora.** { *; }
-keep class io.agora.rtc.** { *; }
-dontwarn io.agora.**

# Google Play Billing pode acessar callbacks e Parcelable por reflexão.
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**
