package com.pawsup.billing

import com.android.billingclient.api.Purchase
import com.pawsup.data.UserPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(private val billingClient: BillingClientWrapper, private val prefs: UserPreferences) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val message = billingClient.message
    private val clock = flow { while (true) { emit(System.currentTimeMillis()); delay(60_000) } }
    val entitlements: Flow<Entitlements> = combine(prefs.ownedCatIds, prefs.hasCafeBundle, prefs.entitlementsLastSynced, clock) { owned, bundle, synced, now ->
        if (EntitlementPolicy.canUseCache(synced, now)) Entitlements(owned, bundle, synced) else Entitlements.DEFAULT
    }
    init {
        scope.launch {
            billingClient.purchases.collect { result ->
                if (result is PurchaseQuery.Success) {
                    val owned = mutableSetOf("miso", "mocha")
                    var bundle = false
                    result.purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { p ->
                        p.products.forEach { id ->
                            if (id == ProductIds.CAFE_PRODUCT) bundle = true
                            ProductIds.PER_CAT.entries.firstOrNull { it.value.productId == id }?.let { owned.add(it.key) }
                        }
                    }
                    prefs.syncEntitlements(owned, bundle, result.checkedAt)
                }
            }
        }
    }
    fun refresh() = billingClient.refresh()
    suspend fun refreshPurchases() { billingClient.queryPurchases() }
}
