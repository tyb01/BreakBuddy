package com.pawsup.billing
import org.junit.Assert.*
import org.junit.Test
class EntitlementPolicyTest {
    @Test fun neverCheckedCannotGrantPaidCache() { assertFalse(EntitlementPolicy.canUseCache(0, 1000)) }
    @Test fun cacheExpiresAfterPolicyWindow() {
        assertTrue(EntitlementPolicy.canUseCache(1000, 1000 + EntitlementPolicy.CACHE_WINDOW_MS))
        assertFalse(EntitlementPolicy.canUseCache(1000, 1001 + EntitlementPolicy.CACHE_WINDOW_MS))
    }
    @Test fun clockRollbackDoesNotExtendPaidCache() { assertFalse(EntitlementPolicy.canUseCache(2000, 1000)) }
    @Test fun freeCatsRemainAvailableWithoutPurchases() {
        assertTrue(Entitlements.DEFAULT.owns("miso")); assertTrue(Entitlements.DEFAULT.owns("mocha"))
        assertFalse(Entitlements.DEFAULT.owns("yuki"))
    }
}
