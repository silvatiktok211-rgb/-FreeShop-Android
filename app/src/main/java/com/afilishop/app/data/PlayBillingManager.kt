package com.afilishop.app.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams

class PlayBillingManager(
    context: Context,
    private val onPurchaseToken: (sku: String, purchaseToken: String, acknowledge: () -> Unit) -> Unit
) {
    private val client = BillingClient.newBuilder(context)
        .setListener { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) {
                purchases.orEmpty().filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { purchase ->
                    purchase.products.forEach { sku -> onPurchaseToken(sku, purchase.purchaseToken) { acknowledge(purchase.purchaseToken) } }
                }
            }
        }
        .enablePendingPurchases()
        .build()

    private val productCache = mutableMapOf<String, ProductDetails>()

    fun connect(onReady: (Boolean) -> Unit = {}) {
        if (client.isReady) { onReady(true); return }
        client.startConnection(object : com.android.billingclient.api.BillingClientStateListener {
            override fun onBillingSetupFinished(result: com.android.billingclient.api.BillingResult) {
                onReady(result.responseCode == BillingResponseCode.OK)
            }
            override fun onBillingServiceDisconnected() { onReady(false) }
        })
    }

    fun querySubscriptions(skus: List<String>, onComplete: (List<ProductDetails>) -> Unit) {
        connect { ready ->
            if (!ready) { onComplete(emptyList()); return@connect }
            val products = skus.map { sku -> QueryProductDetailsParams.Product.newBuilder().setProductId(sku).setProductType(BillingClient.ProductType.SUBS).build() }
            client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()) { result, details ->
                if (result.responseCode == BillingResponseCode.OK) {
                    details.forEach { productCache[it.productId] = it }
                    onComplete(details)
                } else onComplete(emptyList())
            }
        }
    }

    fun launchSubscription(activity: Activity, sku: String): Int {
        val product = productCache[sku] ?: return BillingResponseCode.ITEM_UNAVAILABLE
        val offer = product.subscriptionOfferDetails?.firstOrNull() ?: return BillingResponseCode.ITEM_UNAVAILABLE
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .setOfferToken(offer.offerToken)
            .build()
        return client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build()).responseCode
    }

    private fun acknowledge(purchaseToken: String) {
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build()) { }
    }

    fun close() { client.endConnection() }
}
