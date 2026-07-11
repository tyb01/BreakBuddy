package com.pawsup.paywall;

import android.app.Activity;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.android.billingclient.api.ProductDetails;
import com.pawsup.billing.BillingClientWrapper;
import com.pawsup.billing.BillingRepository;
import com.pawsup.billing.ProductIds;
import com.pawsup.cats.Cat;
import com.pawsup.cats.CatRegistry;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.*;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\'\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\u000e\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017J\u000e\u0010\u0018\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017J\u000e\u0010\u0019\u001a\u00020\u00152\u0006\u0010\u001a\u001a\u00020\u001bJ\u0010\u0010\u001c\u001a\u00020\u00152\u0006\u0010\u001a\u001a\u00020\u001bH\u0002J\u0018\u0010\u001d\u001a\u0004\u0018\u00010\u000f*\u00020\u001e2\b\u0010\u001f\u001a\u0004\u0018\u00010\u000fH\u0002R\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\r0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013\u00a8\u0006 "}, d2 = {"Lcom/pawsup/paywall/PaywallViewModel;", "Landroidx/lifecycle/ViewModel;", "catRegistry", "Lcom/pawsup/cats/CatRegistry;", "billingClient", "Lcom/pawsup/billing/BillingClientWrapper;", "billingRepo", "Lcom/pawsup/billing/BillingRepository;", "savedState", "Landroidx/lifecycle/SavedStateHandle;", "(Lcom/pawsup/cats/CatRegistry;Lcom/pawsup/billing/BillingClientWrapper;Lcom/pawsup/billing/BillingRepository;Landroidx/lifecycle/SavedStateHandle;)V", "_state", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pawsup/paywall/PaywallUiState;", "catId", "", "state", "Lkotlinx/coroutines/flow/StateFlow;", "getState", "()Lkotlinx/coroutines/flow/StateFlow;", "adoptCafe", "", "activity", "Landroid/app/Activity;", "adoptCat", "setPeriod", "period", "Lcom/pawsup/paywall/PricePeriod;", "updatePrices", "priceForPlan", "Lcom/android/billingclient/api/ProductDetails;", "planId", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class PaywallViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.pawsup.cats.CatRegistry catRegistry = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pawsup.billing.BillingClientWrapper billingClient = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pawsup.billing.BillingRepository billingRepo = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String catId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pawsup.paywall.PaywallUiState> _state = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pawsup.paywall.PaywallUiState> state = null;
    
    @javax.inject.Inject()
    public PaywallViewModel(@org.jetbrains.annotations.NotNull()
    com.pawsup.cats.CatRegistry catRegistry, @org.jetbrains.annotations.NotNull()
    com.pawsup.billing.BillingClientWrapper billingClient, @org.jetbrains.annotations.NotNull()
    com.pawsup.billing.BillingRepository billingRepo, @org.jetbrains.annotations.NotNull()
    androidx.lifecycle.SavedStateHandle savedState) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pawsup.paywall.PaywallUiState> getState() {
        return null;
    }
    
    public final void setPeriod(@org.jetbrains.annotations.NotNull()
    com.pawsup.paywall.PricePeriod period) {
    }
    
    private final void updatePrices(com.pawsup.paywall.PricePeriod period) {
    }
    
    public final void adoptCat(@org.jetbrains.annotations.NotNull()
    android.app.Activity activity) {
    }
    
    public final void adoptCafe(@org.jetbrains.annotations.NotNull()
    android.app.Activity activity) {
    }
    
    private final java.lang.String priceForPlan(com.android.billingclient.api.ProductDetails $this$priceForPlan, java.lang.String planId) {
        return null;
    }
}