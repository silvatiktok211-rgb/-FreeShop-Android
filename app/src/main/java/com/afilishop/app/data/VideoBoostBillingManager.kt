package com.afilishop.app.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams

class VideoBoostBillingManager(
    context: Context,
    private val onPurchase: (sku: String, purchaseToken: String, consumeAfterServerValidation: () -> Unit) -> Unit,
) {
    private val productCache = mutableMapOf<String, ProductDetails>()
    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) return@setListener
            purchases.orEmpty().filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { purchase ->
                purchase.products.forEach { sku ->
                    onPurchase(sku, purchase.purchaseToken) { consume(purchase.purchaseToken) }
                }
            }
        }
        .enablePendingPurchases()
        .build()

    fun connect(onReady: (Boolean) -> Unit = {}) {
        if (client.isReady) { onReady(true); return }
        client.startConnection(object : com.android.billingclient.api.BillingClientStateListener {
            override fun onBillingSetupFinished(result: com.android.billingclient.api.BillingResult) {
                onReady(result.responseCode == BillingResponseCode.OK)
            }
            override fun onBillingServiceDisconnected() { onReady(false) }
        })
    }

    fun queryProducts(skus: List<String>, onComplete: (List<ProductDetails>) -> Unit) {
        val requested = skus.distinct().filter(String::isNotBlank)
        if (requested.isEmpty()) { onComplete(emptyList()); return }
        connect { ready ->
            if (!ready) { onComplete(emptyList()); return@connect }
            val products = requested.map { sku ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(sku)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }
            client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()) { result, details ->
                if (result.responseCode == BillingResponseCode.OK) {
                    details.forEach { productCache[it.productId] = it }
                    onComplete(details)
                } else onComplete(emptyList())
            }
        }
    }

    fun formattedPrice(sku: String): String? = productCache[sku]?.oneTimePurchaseOfferDetails?.formattedPrice

    fun launch(activity: Activity, sku: String): Int {
        val product = productCache[sku] ?: return BillingResponseCode.ITEM_UNAVAILABLE
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()
        return client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build(),
        ).responseCode
    }

    private fun consume(purchaseToken: String) {
        if (!client.isReady) return
        client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build()) { _, _ -> }
    }

    fun close() { client.endConnection() }
}
