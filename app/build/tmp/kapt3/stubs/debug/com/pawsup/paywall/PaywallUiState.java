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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0015\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001BC\u0012\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\u0007\u0012\b\b\u0002\u0010\t\u001a\u00020\n\u0012\b\b\u0002\u0010\u000b\u001a\u00020\n\u00a2\u0006\u0002\u0010\fJ\u000b\u0010\u0016\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010\u0017\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0018\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u0019\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u001a\u001a\u00020\nH\u00c6\u0003J\t\u0010\u001b\u001a\u00020\nH\u00c6\u0003JG\u0010\u001c\u001a\u00020\u00002\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00072\b\b\u0002\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\nH\u00c6\u0001J\u0013\u0010\u001d\u001a\u00020\n2\b\u0010\u001e\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001f\u001a\u00020 H\u00d6\u0001J\t\u0010!\u001a\u00020\u0007H\u00d6\u0001R\u0011\u0010\b\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0013\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u000eR\u0011\u0010\u000b\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\u0012R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0012\u00a8\u0006\""}, d2 = {"Lcom/pawsup/paywall/PaywallUiState;", "", "cat", "Lcom/pawsup/cats/Cat;", "period", "Lcom/pawsup/paywall/PricePeriod;", "catPrice", "", "cafePrice", "purchaseSuccess", "", "isLoading", "(Lcom/pawsup/cats/Cat;Lcom/pawsup/paywall/PricePeriod;Ljava/lang/String;Ljava/lang/String;ZZ)V", "getCafePrice", "()Ljava/lang/String;", "getCat", "()Lcom/pawsup/cats/Cat;", "getCatPrice", "()Z", "getPeriod", "()Lcom/pawsup/paywall/PricePeriod;", "getPurchaseSuccess", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "other", "hashCode", "", "toString", "app_debug"})
public final class PaywallUiState {
    @org.jetbrains.annotations.Nullable()
    private final com.pawsup.cats.Cat cat = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pawsup.paywall.PricePeriod period = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String catPrice = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String cafePrice = null;
    private final boolean purchaseSuccess = false;
    private final boolean isLoading = false;
    
    public PaywallUiState(@org.jetbrains.annotations.Nullable()
    com.pawsup.cats.Cat cat, @org.jetbrains.annotations.NotNull()
    com.pawsup.paywall.PricePeriod period, @org.jetbrains.annotations.NotNull()
    java.lang.String catPrice, @org.jetbrains.annotations.NotNull()
    java.lang.String cafePrice, boolean purchaseSuccess, boolean isLoading) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.pawsup.cats.Cat getCat() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pawsup.paywall.PricePeriod getPeriod() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getCatPrice() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getCafePrice() {
        return null;
    }
    
    public final boolean getPurchaseSuccess() {
        return false;
    }
    
    public final boolean isLoading() {
        return false;
    }
    
    public PaywallUiState() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.pawsup.cats.Cat component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pawsup.paywall.PricePeriod component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component4() {
        return null;
    }
    
    public final boolean component5() {
        return false;
    }
    
    public final boolean component6() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pawsup.paywall.PaywallUiState copy(@org.jetbrains.annotations.Nullable()
    com.pawsup.cats.Cat cat, @org.jetbrains.annotations.NotNull()
    com.pawsup.paywall.PricePeriod period, @org.jetbrains.annotations.NotNull()
    java.lang.String catPrice, @org.jetbrains.annotations.NotNull()
    java.lang.String cafePrice, boolean purchaseSuccess, boolean isLoading) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}