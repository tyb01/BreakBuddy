package com.pawsup.paywall

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.pawsup.billing.*
import com.pawsup.cats.Cat
import com.pawsup.cats.CatRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PricePeriod { MONTHLY, YEARLY }
data class PaywallUiState(
    val cat: Cat? = null,
    val period: PricePeriod = PricePeriod.YEARLY,
    val catPrice: String = "",
    val cafePrice: String = "",
    val catAvailable: Boolean = false,
    val cafeAvailable: Boolean = false,
    val purchaseSuccess: Boolean = false,
    val bundleSuccess: Boolean = false,
    val message: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    catRegistry: CatRegistry,
    private val billingClient: BillingClientWrapper,
    private val billingRepo: BillingRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    savedState: SavedStateHandle
) : ViewModel() {
    private val catId: String = savedState[PaywallActivity.EXTRA_CAT_ID] ?: "yuki"
    private val _state = MutableStateFlow(PaywallUiState(cat = catRegistry.find(catId)))
    val state = _state.asStateFlow()
    private var requestedProduct: String? = null

    init {
        viewModelScope.launch {
            combine(billingClient.products, billingClient.purchases, billingClient.message) { _, purchases, message ->
                purchases to message
            }.collect { (purchases, message) ->
                val confirmed = (purchases as? PurchaseQuery.Success)?.purchases.orEmpty().any {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED && requestedProduct in it.products
                }
                _state.update { it.copy(message = message,
                    purchaseSuccess = it.purchaseSuccess || confirmed,
                    bundleSuccess = confirmed && requestedProduct == ProductIds.CAFE_PRODUCT,
                    isLoading = if (confirmed || purchases is PurchaseQuery.Failure) false else it.isLoading) }
                updatePrices()
            }
        }
        viewModelScope.launch {
            billingClient.events.collect { event ->
                _state.update { it.copy(isLoading = false, message = when (event) {
                    is BillingEvent.Pending -> context.getString(com.pawsup.R.string.ui_payment_pending_google_play_must_confirm_payment_a48ef4)
                    is BillingEvent.UserCancelled -> context.getString(com.pawsup.R.string.ui_purchase_cancelled_you_have_not_unlocked_this_su_92f149)
                    is BillingEvent.Error -> event.message
                    else -> it.message
                }) }
            }
        }
    }
    fun refresh() = billingRepo.refresh()
    fun setPeriod(period: PricePeriod) { _state.update { it.copy(period = period) }; updatePrices() }
    private fun updatePrices() {
        val perCat = ProductIds.PER_CAT[catId]
        val monthly = state.value.period == PricePeriod.MONTHLY
        val catOffer = perCat?.let { billingClient.offer(it.productId, if (monthly) it.monthlyPlan else it.yearlyPlan) }
        val cafeOffer = billingClient.offer(ProductIds.CAFE_PRODUCT, if (monthly) ProductIds.CAFE_MONTHLY else ProductIds.CAFE_YEARLY)
        val ready = billingClient.purchases.value is PurchaseQuery.Success
        _state.update { it.copy(catPrice = catOffer?.description() ?: context.getString(com.pawsup.R.string.ui_unavailable_2c9c1f),
            cafePrice = cafeOffer?.description() ?: context.getString(com.pawsup.R.string.ui_unavailable_2c9c1f),
            catAvailable = ready && catOffer != null, cafeAvailable = ready && cafeOffer != null) }
    }
    fun adoptCat(activity: Activity) {
        val product = ProductIds.PER_CAT[catId] ?: return
        buy(activity, product.productId, if (state.value.period == PricePeriod.MONTHLY) product.monthlyPlan else product.yearlyPlan)
    }
    fun adoptCafe(activity: Activity) = buy(activity, ProductIds.CAFE_PRODUCT,
        if (state.value.period == PricePeriod.MONTHLY) ProductIds.CAFE_MONTHLY else ProductIds.CAFE_YEARLY)
    private fun buy(activity: Activity, product: String, plan: String) {
        if (state.value.isLoading) return
        requestedProduct = product
        val launched = billingClient.launchBillingFlow(activity, product, plan)
        _state.update { it.copy(isLoading = launched, message = billingClient.message.value) }
    }
    private fun ProductDetails.SubscriptionOfferDetails.description(): String =
        pricingPhases.pricingPhaseList.joinToString(context.getString(com.pawsup.R.string.price_then)) { phase ->
            val period = when (phase.billingPeriod) {
                "P1M" -> context.getString(com.pawsup.R.string.period_month)
                "P1Y" -> context.getString(com.pawsup.R.string.period_year)
                "P1W" -> context.getString(com.pawsup.R.string.period_week)
                "P1D" -> context.getString(com.pawsup.R.string.period_day)
                else -> phase.billingPeriod
            }
            val duration = if (phase.billingCycleCount > 0) context.resources.getQuantityString(com.pawsup.R.plurals.price_cycles, phase.billingCycleCount, phase.billingCycleCount)
                else context.getString(com.pawsup.R.string.price_renews)
            context.getString(com.pawsup.R.string.price_phase, phase.formattedPrice, period, duration)
        }
}
