package com.pawsup.paywall

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.pawsup.cats.CatRegistry
import com.pawsup.ui.theme.PawsUpTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class PaywallUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun missingPlayOfferDisablesPurchaseButAllowsRestore() {
        val cat = CatRegistry().find("yuki")
        var restores = 0
        compose.setContent { PawsUpTheme { PaywallScreen(PaywallUiState(cat = cat, message = "Offline"), {}, {}, {}, {}, { restores++ }) } }
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        compose.onNodeWithText(context.getString(com.pawsup.R.string.paywall_adopt_one, cat.displayName)).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(context.getString(com.pawsup.R.string.production_restore_purchases_retry)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1,restores) }
    }
}
