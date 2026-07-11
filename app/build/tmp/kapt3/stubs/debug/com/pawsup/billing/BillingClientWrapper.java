package com.pawsup.billing;

import android.app.Activity;
import android.content.Context;
import com.android.billingclient.api.*;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.*;
import kotlinx.coroutines.flow.SharedFlow;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000x\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010$\n\u0002\b\u0003\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\b\u0005\b\u0007\u0018\u00002\u00020\u0001B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u001e\u001a\u00020\u001fH\u0082@\u00a2\u0006\u0002\u0010 J\b\u0010!\u001a\u00020\u001dH\u0002J\u001e\u0010\"\u001a\u00020#2\u0006\u0010$\u001a\u00020%2\u0006\u0010&\u001a\u00020\n2\u0006\u0010\'\u001a\u00020\nJ \u0010(\u001a\u00020\u001d2\u0006\u0010)\u001a\u00020*2\u000e\u0010+\u001a\n\u0012\u0004\u0012\u00020\u001f\u0018\u00010,H\u0016J\u000e\u0010-\u001a\u00020\u001dH\u0082@\u00a2\u0006\u0002\u0010.J\u0014\u0010/\u001a\b\u0012\u0004\u0012\u00020\u001f0,H\u0086@\u00a2\u0006\u0002\u0010.J\b\u00100\u001a\u00020\u001dH\u0002R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u000b0\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\f\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00070\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u001d\u0010\u0014\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u000b0\u00158F\u00a2\u0006\u0006\u001a\u0004\b\u0016\u0010\u0017R\u000e\u0010\u0018\u001a\u00020\u0019X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001a\u001a\u00020\u001bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00061"}, d2 = {"Lcom/pawsup/billing/BillingClientWrapper;", "Lcom/android/billingclient/api/PurchasesUpdatedListener;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "_events", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "Lcom/pawsup/billing/BillingEvent;", "_productDetails", "", "", "Lcom/android/billingclient/api/ProductDetails;", "client", "Lcom/android/billingclient/api/BillingClient;", "getClient", "()Lcom/android/billingclient/api/BillingClient;", "events", "Lkotlinx/coroutines/flow/SharedFlow;", "getEvents", "()Lkotlinx/coroutines/flow/SharedFlow;", "productDetails", "", "getProductDetails", "()Ljava/util/Map;", "retryDelay", "", "scope", "Lkotlinx/coroutines/CoroutineScope;", "acknowledgePurchaseIfNeeded", "", "purchase", "Lcom/android/billingclient/api/Purchase;", "(Lcom/android/billingclient/api/Purchase;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "connect", "launchBillingFlow", "", "activity", "Landroid/app/Activity;", "productId", "basePlanId", "onPurchasesUpdated", "result", "Lcom/android/billingclient/api/BillingResult;", "purchases", "", "queryProductDetails", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "queryPurchases", "scheduleReconnect", "app_debug"})
public final class BillingClientWrapper implements com.android.billingclient.api.PurchasesUpdatedListener {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableSharedFlow<com.pawsup.billing.BillingEvent> _events = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.SharedFlow<com.pawsup.billing.BillingEvent> events = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, com.android.billingclient.api.ProductDetails> _productDetails = null;
    private long retryDelay = 1000L;
    @org.jetbrains.annotations.NotNull()
    private final com.android.billingclient.api.BillingClient client = null;
    
    @javax.inject.Inject()
    public BillingClientWrapper(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.SharedFlow<com.pawsup.billing.BillingEvent> getEvents() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, com.android.billingclient.api.ProductDetails> getProductDetails() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.android.billingclient.api.BillingClient getClient() {
        return null;
    }
    
    private final void connect() {
    }
    
    private final void scheduleReconnect() {
    }
    
    private final java.lang.Object queryProductDetails(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object queryPurchases(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<? extends com.android.billingclient.api.Purchase>> $completion) {
        return null;
    }
    
    public final boolean launchBillingFlow(@org.jetbrains.annotations.NotNull()
    android.app.Activity activity, @org.jetbrains.annotations.NotNull()
    java.lang.String productId, @org.jetbrains.annotations.NotNull()
    java.lang.String basePlanId) {
        return false;
    }
    
    private final java.lang.Object acknowledgePurchaseIfNeeded(com.android.billingclient.api.Purchase purchase, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    public void onPurchasesUpdated(@org.jetbrains.annotations.NotNull()
    com.android.billingclient.api.BillingResult result, @org.jetbrains.annotations.Nullable()
    java.util.List<? extends com.android.billingclient.api.Purchase> purchases) {
    }
}