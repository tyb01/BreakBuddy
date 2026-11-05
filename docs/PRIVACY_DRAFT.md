# Privacy disclosure draft for owner review

This is a repository draft, not a published policy or completed Play Data safety form. Verify the current Google Billing SDK disclosure and the actual final artifact before publication.

PawsUp stores selected app package identifiers, preferences, usage-budget checkpoints, active break records, bounded break outcome history, pet/break counters and a cached set of Play entitlements on the device. App usage observations are processed locally to offer break reminders. The app has no developer-operated backend or account system.

Google Play processes purchases and restores subscriptions. Its Billing library and transport dependencies can communicate with Google. PawsUp does not receive payment-card details. Purchase tokens are handled in memory for acknowledgement and are not included in app logs. Do not describe the entire application as zero-network or infer that no SDK data is collected from the absence of a developer backend. Data safety answers must include applicable SDK behavior under Google's current rules.

The last 200 break outcomes and a bounded set of finished session identifiers are retained locally. Settings remain until reset or app data removal. Delete history removes visible outcomes and counters; bounded session identifiers remain to prevent late callbacks recreating deleted entries. Reset clears local preferences and disables monitoring; Play can restore purchases on a subsequent successful query. Neither action cancels a Play subscription. Manage cancellation in Google Play.

Backup and device-transfer exclusions are configured for app data. The release owner must test these on supported Android/OEM versions. Usage access is used only for monitoring. Optional display-over-apps access supports automatic break presentation; optional notifications provide status/actions and tap-to-open reminders. Users may revoke access through Android settings.

No advertising or analytics SDK was added by this remediation. External privacy/subscription links open another application governed by its provider. The owner must add verified business identity, contact details, retention obligations and effective date to the public policy; these details must not be guessed.

Reference: [Google Play Data safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469), [Android data-use declarations](https://developer.android.com/privacy-and-security/declare-data-use).
