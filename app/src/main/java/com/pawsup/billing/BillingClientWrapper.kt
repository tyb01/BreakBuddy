package com.pawsup.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

sealed class BillingEvent {
    data class Purchased(val productId: String, val purchaseToken: String) : BillingEvent()
    data class Error(val code: Int, val message: String) : BillingEvent()
    object Pending : BillingEvent()
    object UserCancelled : BillingEvent()
}
sealed class PurchaseQuery {
    data class Success(val purchases: List<Purchase>, val checkedAt: Long = System.currentTimeMillis()) : PurchaseQuery()
    data class Failure(val message: String) : PurchaseQuery()
    object Loading : PurchaseQuery()
}

@Singleton
class BillingClientWrapper @Inject constructor(@ApplicationContext private val context: Context) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private var connecting = false
    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()
    val products = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val purchases = MutableStateFlow<PurchaseQuery>(PurchaseQuery.Loading)
    val message = MutableStateFlow("Connecting to Google Play...")
    val productDetails: Map<String, ProductDetails> get() = products.value
    val client = BillingClient.newBuilder(context).setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().enablePrepaidPlans().build())
        .enableAutoServiceReconnection().build()

    init { refresh() }
    fun refresh() {
        if (client.isReady) { scope.launch { refreshReady() }; return }
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) scope.launch { refreshReady() }
                else fail("Google Play unavailable. Check your connection and retry.")
            }
            override fun onBillingServiceDisconnected() {
                connecting = false
                fail("Disconnected from Google Play. Your last purchase check is retained.")
            }
        })
    }
    private fun fail(text: String) { message.value = text; purchases.value = PurchaseQuery.Failure(text) }
    private suspend fun refreshReady() = mutex.withLock {
        try {
            val ids = ProductIds.PER_CAT.values.map { it.productId } + ProductIds.CAFE_PRODUCT
            val params = QueryProductDetailsParams.newBuilder().setProductList(ids.map {
                QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.SUBS).build()
            }).build()
            val result = suspendCancellableCoroutine<Pair<BillingResult, QueryProductDetailsResult>> { continuation ->
                client.queryProductDetailsAsync(params) { response, details ->
                    if (continuation.isActive) continuation.resume(response to details)
                }
            }
            if (result.first.responseCode == BillingClient.BillingResponseCode.OK)
                products.value = result.second.productDetailsList.associateBy { it.productId }
            else products.value = emptyMap()
            queryPurchases()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { fail("Unable to check Google Play. Try again.") }
    }
    suspend fun queryPurchases(): PurchaseQuery {
        if (!client.isReady) return PurchaseQuery.Failure("Google Play is not connected.").also { purchases.value = it }
        val result = suspendCancellableCoroutine<Pair<BillingResult, List<Purchase>>> { continuation ->
            client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { response, list ->
                if (continuation.isActive) continuation.resume(response to list)
            }
        }
        if (result.first.responseCode != BillingClient.BillingResponseCode.OK) {
            val failure = PurchaseQuery.Failure("Purchase check failed. Cached access is retained.")
            purchases.value = failure; message.value = failure.message; return failure
        }
        val valid = result.second.filter { it.packageName == context.packageName }
        val success = PurchaseQuery.Success(valid)
        purchases.value = success
        message.value = if (valid.any { it.purchaseState == Purchase.PurchaseState.PENDING })
            "Payment pending. Access unlocks when Google Play confirms payment." else "Purchases checked with Google Play."
        valid.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }.forEach { purchase ->
            scope.launch {
                repeat(3) { attempt ->
                    val response = suspendCancellableCoroutine<BillingResult> { continuation ->
                        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) {
                            if (continuation.isActive) continuation.resume(it)
                        }
                    }
                    if (response.responseCode == BillingClient.BillingResponseCode.OK) return@launch
                    delay((attempt + 1) * 2_000L)
                }
                message.value = "Purchase confirmation needs a retry. Reopen PawsUp while online."
            }
        }
        return success
    }
    fun offer(productId: String, planId: String?): ProductDetails.SubscriptionOfferDetails? =
        products.value[productId]?.subscriptionOfferDetails?.filter { it.basePlanId == planId }
            ?.sortedBy { it.offerId != null }?.firstOrNull()

    fun launchBillingFlow(activity: Activity, productId: String, basePlanId: String): Boolean {
        val details = products.value[productId] ?: return false
        val offer = offer(productId, basePlanId) ?: return false
        // Do not allow overlapping subscriptions without an explicit migration flow.
        val existing = (purchases.value as? PurchaseQuery.Success)?.purchases ?: return false
        if (existing.any { it.purchaseState != Purchase.PurchaseState.UNSPECIFIED_STATE &&
                (productId in it.products || ProductIds.CAFE_PRODUCT in it.products || productId == ProductIds.CAFE_PRODUCT) }) {
            message.value = "An existing subscription covers or overlaps this purchase. Manage it in Google Play first."
            return false
        }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).setOfferToken(offer.offerToken).build())).build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) message.value = "Unable to open Google Play purchase screen. Retry when connected."
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }
    override fun onPurchasesUpdated(result: BillingResult, updated: List<Purchase>?) {
        scope.launch {
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    queryPurchases()
                    updated.orEmpty().forEach { p ->
                        if (p.purchaseState == Purchase.PurchaseState.PURCHASED) p.products.forEach {
                            _events.emit(BillingEvent.Purchased(it, p.purchaseToken))
                        } else if (p.purchaseState == Purchase.PurchaseState.PENDING) _events.emit(BillingEvent.Pending)
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> _events.emit(BillingEvent.UserCancelled)
                else -> _events.emit(BillingEvent.Error(result.responseCode, "Purchase unavailable. Please retry in Google Play."))
            }
        }
    }
}
