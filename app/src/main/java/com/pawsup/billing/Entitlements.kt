package com.pawsup.billing

data class Entitlements(val ownedCatIds: Set<String>, val hasCafeBundle: Boolean, val lastSyncedAt: Long) {
    fun owns(catId: String) = catId in setOf("miso", "mocha") || hasCafeBundle || catId in ownedCatIds
    companion object { val DEFAULT = Entitlements(setOf("miso", "mocha"), false, 0L) }
}
object EntitlementPolicy {
    // Client-only Play Billing does not expose authoritative paid-through expiry.
    // This is a bounded cached-access policy, not an assertion that a subscription is active.
    const val CACHE_WINDOW_MS = 72 * 60 * 60 * 1000L
    fun canUseCache(lastSuccess: Long, now: Long) = lastSuccess > 0 && now - lastSuccess in 0..CACHE_WINDOW_MS
}
