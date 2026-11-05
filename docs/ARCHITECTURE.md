# Production architecture decisions

Decision date: 4 November 2026. These are implementation choices, not guarantees of Play approval.

## User-started monitoring

A `specialUse` foreground service observes selected app usage while the user interacts with their phone. `dataSync` was not appropriate for this function and has been removed. The service declares its subtype explanation and supplies Pause, Stop and Open controls. The service is not sticky, does not wake the screen and does not use boot/alarm/worker restart loops. Removing its task stops it. Opening PawsUp can resume a previously enabled session from a visible activity. Required usage access is checked before and during operation.

Android documents the [specialUse declaration](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use) and [background-start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start). The owner must supply the actual [Play declaration and demonstration](https://support.google.com/googleplay/android-developer/answer/13392821) and obtain review. This repository cannot establish approval.

Automatic break presentation uses the documented display-over-apps exception to [background activity launch restrictions](https://developer.android.com/guide/components/activities/background-starts). Access is optional. Without it, a PendingIntent lets the user open the break from the notification. An activity's session acknowledgement, not merely a call to startActivity, consumes the due budget. Unacknowledged presentation waits before retrying. Actual target-36 behavior requires device tests. No full-screen intent is used.

## Time and recovery

Usage events are queried incrementally and translated from wall timestamps into elapsed time before accumulation. Only the most recently resumed selected package counts: split-screen/PiP overlap is counted once, not as parallel app time. Screen/keyguard events and current PowerManager/Keyguard state exclude rest periods. API 24/25 lack newer usage screen events; current state provides a conservative fallback with polling uncertainty.

Combined mode preserves a budget across app switches; per-visit mode resets on leaving the current app. A changed mode resets its budget. Checkpoints can recover a same-boot gap of at most five minutes. Larger/unreconstructable gaps reset observation rather than fabricate duration. Clock changes invalidate the wall-time event cursor; durations remain elapsed-time based. Poll cadence is two seconds while runnable, not an Android delivery guarantee. The service holds no wake lock.

Break IDs and DataStore edits deduplicate outcomes and atomically update history/counters. A break deadline is independent of media. A live screen-off break can complete, but switching to another app interrupts it. Active sessions persist their elapsed deadline and [BOOT_COUNT](https://developer.android.com/reference/android/provider/Settings.Global#BOOT_COUNT), available at the minimum API 24 without requesting another permission. A reboot, unknown boot, or expired orphan cannot be asserted to be completed rest. A bounded finished-ID set prevents repeated callbacks from counting again.

Remaining limitations: this is a reminder, not an enforcement boundary. Event delivery, multitasking, process death, manufacturer behavior and time spent between samples need device qualification. CPU/video/battery budgets have not been measured.

## Play-only subscriptions: explicit user constraint

The user declined a backend. The implementation queries Play on connection, app-screen resume, purchase callback and Restore. Only purchased-state known products grant access; pending purchases do not. Successful-empty and failure are distinct. Entitlements are saved atomically, and a successful check of unchanged purchases refreshes the cache timestamp. Paid access falls back to free cats when cache age exceeds 72 hours or the clock rolls backward.

The 72-hour limit is a product policy. A client purchase query does not expose authoritative paid-through expiry, and cached rights can outlive a refund while offline. There is no RTDN service, server validation, server-side fraud resistance, or background entitlement guarantee. Google recommends a secure backend for [purchase security](https://developer.android.com/google/play/billing/security); that recommendation is not claimed to be satisfied. Client processing/acknowledgement follows the supported [Billing integration APIs](https://developer.android.com/google/play/billing/integrate).

Eligible unacknowledged purchases retry acknowledgement three times in-process and are rediscovered from Play on subsequent connection/resume/restore. If the app stays closed through a failure, acknowledgement cannot be guaranteed before Play's refund deadline. Validate this with license testers; do not describe it as server-durable fulfilment.

Offers and all pricing phases shown come from the same Play offer selected for purchase. Missing products cannot be purchased. Cat-to-bundle overlap is refused with a management link; no automatic replacement or proration is promised. Billing 8.0.0 is a supported pinned major, not the newest release; the Java callback API avoids the newer Kotlin metadata required by billing-ktx. See [release notes](https://developer.android.com/google/play/billing/release-notes) and [support deadlines](https://developer.android.com/google/play/billing/deprecation-faq).

## Privacy and permissions

No app server, advertising SDK or extra analytics SDK was introduced. Google Billing's transport adds INTERNET and ACCESS_NETWORK_STATE; Media3 also declares connectivity observation. Those normal permissions are retained for shipped dependencies. AndroidX's private receiver signature permission is generated per application ID. Exact merger owners are checked in [permissions.json](../scripts/permissions.json). Usage special access is essential only to monitoring; overlay and notifications are optional setup steps.

The unused FileProvider/share generator was removed. Auto Backup and device-transfer rules exclude app data. No purchase tokens or selected package names are written to app diagnostic logs. [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) provides atomic updates; corrupt preferences recover to safe defaults with a visible recovery notice. No account deletion backend exists. The owner must review the SDK inventory and publish accurate [Data safety](https://support.google.com/googleplay/android-developer/answer/10787469) and privacy disclosures before release.
