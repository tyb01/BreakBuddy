package com.pawsup.paywall;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.activity.ComponentActivity;
import androidx.compose.foundation.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.ui.layout.ContentScale;
import androidx.compose.ui.text.font.FontStyle;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import com.pawsup.R;
import com.pawsup.cats.Cat;
import com.pawsup.cats.CatAssetResolver;
import dagger.hilt.android.AndroidEntryPoint;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000(\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0007\u001aN\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00010\bH\u0003\u001a2\u0010\u000b\u001a\u00020\u00012\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\r2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\rH\u0003\u001a\u001e\u0010\u0011\u001a\u00020\u00012\u0006\u0010\u0012\u001a\u00020\r2\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00010\bH\u0003\u00a8\u0006\u0014"}, d2 = {"PaywallScreen", "", "state", "Lcom/pawsup/paywall/PaywallUiState;", "onPeriodChange", "Lkotlin/Function1;", "Lcom/pawsup/paywall/PricePeriod;", "onAdoptCat", "Lkotlin/Function0;", "onAdoptCafe", "onDismiss", "PurchaseCard", "title", "", "price", "onClick", "subtitle", "PurchaseSuccessScreen", "catName", "onDone", "app_debug"})
public final class PaywallActivityKt {
    
    @androidx.compose.runtime.Composable()
    private static final void PaywallScreen(com.pawsup.paywall.PaywallUiState state, kotlin.jvm.functions.Function1<? super com.pawsup.paywall.PricePeriod, kotlin.Unit> onPeriodChange, kotlin.jvm.functions.Function0<kotlin.Unit> onAdoptCat, kotlin.jvm.functions.Function0<kotlin.Unit> onAdoptCafe, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void PurchaseCard(java.lang.String title, java.lang.String price, kotlin.jvm.functions.Function0<kotlin.Unit> onClick, java.lang.String subtitle) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void PurchaseSuccessScreen(java.lang.String catName, kotlin.jvm.functions.Function0<kotlin.Unit> onDone) {
    }
}