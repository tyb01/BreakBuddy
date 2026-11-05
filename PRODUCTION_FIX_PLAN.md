PawsUp production remediation plan — 2 November 2026

User decision (implementation): no backend. Google Play Billing on-device is the purchase source; no server verification service, account system, or RTDN deployment will be added. All backend-specific steps below are superseded by this decision. Reconcile ownership on successful Play connection/resume/restore and explain offline limitations. Never claim server verification or expiry data unavailable from the client.

Status: implementation plan; see IMPLEMENTATION_STATUS.md for delivered work and outstanding release evidence. Baseline: PRODUCTION_AUDIT.md, source version 1.0.4 (5), lint with 8 errors and 74 warnings, and no test source sets. Scope: address every audited defect, establish release evidence, then add the identified productivity improvements.

Platform requirements below come from Android/Google Play documentation. Architecture and security practices draw on Google recommendations and OWASP MASVS. Product defaults and acceptance thresholds are proposed engineering decisions, not official mandates or proven productivity outcomes. A plan cannot guarantee Play approval, uninterrupted execution on every manufacturer device, or improved productivity.

**Recommended product and architecture decisions**

- Position PawsUp as a user-controlled app-usage break companion. Do not claim detection of scrolling or an unbreakable phone lock.
- Preserve existing users' explicit preferences. Offer combined selected-app usage as the recommended mode for new users; explain the choice. Existing per-visit users retain their mode until they choose to change it.
- Retain API 24 support initially and fix compatibility guards. Raise the minimum only as an explicit support decision backed by device/user evidence.
- Keep usage history on-device. Use on-device Google Play queries for purchases under the explicit no-backend decision; disclose the client cache and SDK data flows. No sign-in is required for the free monitoring feature.
- Use repositories, lifecycle-aware UI state, and a testable Kotlin usage/break engine. Keep the current Hilt/Compose structure; avoid a large unrelated rewrite or unnecessary module split. This follows Google's architecture recommendations. [S1]
- Investigate a user-started `specialUse` foreground service, UsageStats event processing, and permission-gated break presentation. The `specialUse` use case must be justified in the manifest and Play Console and remains subject to review. It does not waive background-start restrictions. [S2–S4]
- Treat actual monitoring availability, permission state, and the user's requested monitoring setting as distinct values.

**Delivery order and dependencies**

Mandatory user constraint: request and ship only permissions actually needed by implemented production features. Prefer a permission-free alternative when it meets the same product requirement. Optional permissions must not block unrelated features. Do not add permissions speculatively for future features.

| Permission/capability | Production decision |
|---|---|
| PACKAGE_USAGE_STATS | Keep for selected-app usage monitoring; explain and request special access when the user enables that feature. |
| SYSTEM_ALERT_WINDOW | Conditional: keep only if the validated automatic break presentation relies on it, including a documented background-activity exemption. Remove if the chosen presentation does not require it; offer a clearly described limited mode when denied. |
| FOREGROUND_SERVICE | Keep only with the validated foreground monitoring service. |
| FOREGROUND_SERVICE_DATA_SYNC | Remove with replacement of the inappropriate dataSync monitoring implementation. |
| FOREGROUND_SERVICE_SPECIAL_USE | Add only if Phase 1 selects and justifies this actual service type; no unrelated service-type permissions. |
| RECEIVE_BOOT_COMPLETED | Retain only for an implemented, tested boot reconciliation/recovery feature; remove the app's declaration if that feature is dropped. Separately review any dependency-owned boot receiver and its requirement. |
| POST_NOTIFICATIONS | Request in context for visible status/reminders; optional for onboarding and monitoring where Android allows. Explain that a notification-based fallback needs notifications enabled. |
| com.android.vending.BILLING | Keep while Play purchases are implemented; it is not a user runtime prompt. |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | Remove by default. Retain only if device evidence establishes necessity for the shipped core feature and the official acceptable-use rules apply; never request as a blanket setup step. |
| INTERNET / ACCESS_NETWORK_STATE and dependency-added normal permissions | Audit the release merged manifest and merger report. Retain only demonstrated requirements of purchase verification, Billing, or another shipped component; do not describe normal manifest permissions as user consent prompts. |
| Exact alarms, full-screen intents, broad package visibility, contacts, location, camera, microphone, phone state, media/storage access, accessibility-service or device-admin capabilities | Do not add for the current feature set. Use launcher-scoped visibility, app-private assets, scoped sharing, and existing audio/lifecycle APIs. No additional sensitive permission solely to detect calls or keep the process alive. |

Permission release gate: maintain an allowlist recording each permission's declaring app/dependency, actual feature, official rationale, Android versions, request timing, and denial/revocation behavior. Compare both debug and release merged manifests against it in CI; fail unexplained additions. Recheck after dependency upgrades and inspect the final release artifact. Remove unused declarations, receivers, and request UI together; do not strip a dependency permission blindly if a retained feature requires it. Validate that users can decline optional access without losing unrelated functionality. This gate applies to every phase below.

| Phase | Deliverable | Dependency | Exit gate |
|---|---|---|---|
| 0 | Secure, reproducible development baseline | None | Clean build inputs; secrets externalized; baseline documented |
| 1 | Validated monitoring architecture | Phase 0 | Documented platform behavior and viable Play declaration |
| 2 | Correct usage and break engine | Phase 1 contract | Deterministic timing/recovery tests pass |
| 3 | Resilient cat experience and setup | Phase 2 | Lifecycle, permission, accessibility, and media-failure tests pass |
| 4 | Correct subscription system | Phase 0; coordinates with Phase 3 | Play license-test lifecycle matrix passes |
| 5 | Privacy, data migration, diagnostics, performance | Phases 2–4 | Upgrade/privacy/performance evidence complete |
| 6 | Production qualification and release | Phases 0–5 | Release checklist signed off with evidence |
| 7 | Productivity expansion | Stable core from Phase 6 | Each feature independently validated |

Phases 1 and 4 can be implemented independently after the baseline; this is a dependency observation, not a requirement to use parallel agents. Build tests alongside each fix rather than adding them at the end. No calendar estimate is asserted without team capacity, device access, and the Phase 1 result.

**Phase 0 — secure and stabilize the build**

Primary files: app/build.gradle.kts, build.gradle.kts, gradle wrapper/configuration, AndroidManifest.xml, .gitignore, CI configuration, TODO.md.

1. Preserve the current source state and record the initial lint report. Separate generated artifacts changed by the audit from authored changes.
2. Remove signing passwords and personal absolute paths from build scripts. Load release credentials from local untracked configuration or CI secret storage. Keep debug builds independent of release secrets. Inspect repository access/history and determine whether an upload-key reset or credential rotation is necessary. Do not replace the signing identity blindly. Do not rewrite shared Git history without coordination. [S5]
3. Stop tracking app/build, root build output, and .gradle in a dedicated cleanup commit; retain local files where useful and add ignore rules. Keep Gradle wrapper files tracked. Add secret scanning and dependency verification/locking as appropriate.
4. Upgrade target/compile SDK to meet the current phone-app requirement (API 36 as of this plan). Select stable compatible AGP, Gradle, Kotlin, Compose, and libraries based on their release compatibility documentation; pin versions and verify each upgrade group. Upgrade Billing to a supported stable major. Remove manually forged billing-version metadata and inspect the merged manifest. [S6, S7]
5. Fix the four API-26 notification-call lint errors with proper version guards. Fix Media3 unstable-API opt-ins deliberately. Review all 74 warnings: resolve defects and document narrowly justified exceptions; do not baseline errors merely to pass.
6. Add CI for compilation, lint, meaningful unit tests, and debug packaging. Add a protected release workflow for signed AAB generation, mapping/native-symbol retention, and artifact provenance. Build dependencies must resolve from declared repositories on a clean environment.

Acceptance: a fresh checkout builds without a personal absolute path; no signing secret is tracked; lint has zero errors and no unexplained high-severity warning; tests execute a nonzero relevant suite. Release artifacts and mapping files are archived without secrets. Check both debug and release variants.

**Phase 1 — prove a compliant monitoring design before broad refactoring**

Primary files: MonitoringService, BootReceiver, KeepAliveReceiver, WatchdogWorker, AlarmScheduler, MonitoringRepository, manifest.

1. Write a short architecture decision record explaining the user-visible monitoring function, why continuous timely work is needed, stop controls, expected battery impact, permissions, and failure states.
2. Prototype the documented `specialUse` foreground-service route with the required permission and `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` explanation. Start it through an explicit visible user action. Prepare the actual Play Console declaration and demonstration video from that build. Do not describe it as approved until Play review establishes that. [S2, S3]
3. Remove `dataSync` classification and its prohibited boot-launch path as part of the validated replacement. Remove alarm/worker restart loops whose purpose is to keep reviving the process. Use WorkManager only for eligible deferred work, such as housekeeping or durable retry—not as a real-time usage observer or restriction bypass. [S8]
4. Implement boot/process recovery with permission and eligibility checks. Resume only where documented and tested conditions allow; otherwise expose a needs-resume state and a user-triggered resume path. Handle foreground-service start exceptions. Respect user stop and force-stop; never promise automatic restart after them.
5. Choose break presentation from demonstrated platform behavior: existing activity launch with a documented exemption, or a genuine overlay window where permission permits. Validate target-36 behavior. Do not use full-screen intents, AccessibilityService, device-owner mode, or another service type as a shortcut around restrictions. Provide a notification/user-open fallback when automatic presentation is unavailable, and describe this degraded mode honestly. [S3, S4]
6. Confirm presentation with a session-ID acknowledgement from the visible break UI. Calling startActivity alone is not proof that Android displayed it. Retry only with a bounded policy and cooldown to avoid interruption storms.
7. Replace the service boolean with health state: requested mode, actual service lifecycle, permission availability, last successful observation, last confirmed presentation, and last failure reason. Show unknown/stale when current evidence is unavailable. Use notification Pause/Stop/Open actions.
8. Remove compulsory battery-exemption claims. Request exemptions only if the actual core function meets the documented acceptable use case; otherwise explain supported settings without claiming guaranteed survival. Manufacturer shortcuts must fail safely and show unverified status when their setting cannot be read.

Acceptance: test starts, stops, reboot, process kill, user stop, permission revocation, and background launch refusal on representative devices. Demonstrate an all-day active-use/idle/lock cycle with diagnostic evidence; no silent loss of monitoring, no prohibited restarts, no screen wake for an inactive phone. Obtain Play acceptance before advertising the architecture as production-supported.

Fallback decision: if the proposed service use is not acceptable or cannot meet the tested behavior, ship an explicitly limited user-started focus/reminder mode only after updating the product contract. Do not silently replace automatic monitoring with periodic WorkManager checks while retaining the same promise.

**Phase 2 — make time accounting deterministic and recoverable**

Primary files: MonitoringService, UserPreferences, DataStoreKeys, BreakOverlayViewModel; proposed UsageEventSource, UsageAccumulator, BreakCoordinator, Clock, SessionRepository.

1. Extract pure Kotlin logic from Android lifecycle classes. Inject event source, monotonic/wall clocks, preferences, and presentation result. Android components should translate platform events into domain inputs.
2. Define two supported rules explicitly: per-visit timing and cumulative selected-app timing. Count only eligible foreground interactive intervals; exclude PawsUp, locked/screen-off periods, configured pause/quiet time, and breaks. Document split-screen/PiP treatment and count overlapping time once. A usage budget measures app visibility/activity according to this rule, not human attention.
3. Process resume/pause/stop plus screen/keyguard transitions where available, with version-compatible fallbacks. Query incrementally with a cursor and bounded overlap; deduplicate replayed events. Never discard all prior time merely because the app switches or the service restarts.
4. Use elapsedRealtime for running durations; reserve wall time for calendar/day boundaries and persisted timestamps. Store enough checkpoint information to recognize reboot and recover from available usage events. When an interval cannot be reconstructed reliably, mark it unknown rather than inventing usage. [S9]
5. Implement trigger states: observing → threshold reached → presentation pending → break active → completed/skipped/interrupted. Use unique session IDs and idempotent transitions. A refused launch must not permanently consume the threshold.
6. Make the agreed break duration independent of video callbacks. Recommended behavior: countdown starts when the break is visibly acknowledged; entrance/outro are cosmetic and cannot extend a completed countdown. An explicit Put phone down action allows screen-off rest. Backgrounding into another app records an interruption rather than silently claiming success.
7. Persist active session and outcome sufficiently to survive process death. For a reboot during an active break, mark interrupted/needs reconciliation instead of assuming a completed break from a changed wall clock.
8. Increment counters and update selection sets inside DataStore transactions; validate stored limits and cat IDs; serialize conflicting operations. Use Room for transactional session/history records if introduced; keep preferences in DataStore. Migrate existing values without silently resetting user choices. [S10]

Acceptance examples (proposed product tests):

- Combined mode: 8 minutes in app A plus 7 in app B reaches a 15-minute threshold; switching does not erase time.
- Per-visit mode follows its documented reset rule and is labeled accordingly.
- Screen locked for 20 minutes adds zero eligible usage and causes no screen-waking break.
- Wall-clock/timezone changes cannot shorten or extend an active elapsed-time countdown.
- Replayed events, repeated callbacks, rotation, and rapid taps cause no duplicate break completion or lost counter update.
- Process recreation reconciles known time; unavailable history is explicit.
- Under a running healthy service, presentation occurs within the documented polling interval plus measured processing latency. Treat this as a tested operating bound, not an unconditional Android delivery guarantee.

**Phase 3 — repair setup and the complete break experience**

Primary files: OnboardingActivity, SettingsActivity/ViewModel, AppPickerActivity/ViewModel/Repository, BreakOverlayActivity/ViewModel, video components, RecapToast, cat and theme resources.

1. Restore onboarding progress after recreation. Let users understand essential permissions before system prompts. Handle denied/permanently-denied permissions with actionable settings links. Make notification permission optional where supported; still supply the required foreground notification. [S11]
2. Empty selection means not configured, not active. Add a test-break action so users can verify presentation. Recheck required capabilities when starting monitoring and when returning from settings.
3. Deduplicate launcher results by package; exclude the current applicationId; tolerate removed apps and unavailable icons; apply rapid toggles transactionally.
4. Present actual health on Home and Reliability. Replace the permanently-red OEM row with unknown/manual confirmation and a clear limitation; never treat a user's confirmation as an OS-verified setting.
5. Add visible pause, snooze, stop, and emergency-exit controls. Define automatic resume rules and time remaining. Back/Home/Recents behavior must match the reminder product contract and record outcomes honestly.
6. Remove unnecessary wake/lock-screen/keep-screen-on flags. Default to quiet operation with explicit sound settings. Respect audio focus and test interaction with media/calls without adding unnecessary sensitive permissions.
7. Add video error handling, bounded preparation timeout, static-poster fallback, and an independent timer. Release/pause players with lifecycle changes and restore visuals from session state. A decoder failure must never trap onboarding or a break.
8. Persist session UI inputs appropriately; derive player state and countdown from the coordinator. Make completion idempotent. Make recap timely and accessible; remove unused share/escape code or expose tested features intentionally.
9. Move premium guest promotions into the Cafe or a voluntary post-break area; they must not pause a break. Resolve the existing guest state safely on upgrades.
10. Add labeled accessible pet/cat actions, selected/locked state semantics, correct touch targets, adaptable layouts, large-text support, and reduced-motion/static presentation. Externalize hardcoded text and use plural resources. Support a tested language set; do not claim unverified translations. [S12]

Acceptance: an accessible, understandable path exists through setup, monitoring repair, pause/resume, normal break, skip, media failure, and return to the previous task. Test TalkBack, keyboard/switch interaction where available, 200% font scale, small screens, landscape, and system insets. No promotion extends a break; no video controls the ability to leave.

**Phase 4 — rebuild subscription handling around verified state**

Primary files: BillingClientWrapper, BillingRepository, Entitlements, PaywallViewModel/Activity, ProductIds, application lifecycle; no developer backend under the user decision.

1. Make product and purchase state observable. Represent loading, success, successful-empty, pending, unavailable, and error separately. Never translate network failure into authoritative ownership loss.
2. Funnel startup connection, foreground/resume refresh, explicit Restore, and purchase callbacks through one reconciliation path. Preserve the distinction between last attempted and last successful verification. [S13]
3. Reconcile eligible PURCHASED products through Google Play Billing on the device. Validate package/product and exclude pending states. Persist entitlement changes atomically. No server, Play API credentials, authenticated backend, or account association will be introduced. Document that this differs from Google's recommended secure-server verification and is less resistant to client tampering. [S13, S14]
4. Query on successful connection, resume, purchase callback and Restore. A successful empty result removes paid rights; a query failure does not invent an authoritative empty result. Without RTDN/backend, the app cannot know off-device lifecycle changes until another successful query. Validate cancellation, grace/hold, pause, expiry, refund/revocation and recovery with Play license tests. [S15, S16]
5. Acknowledge eligible Play purchases with immediate bounded retry and re-query unacknowledged purchases on reconnect/resume/Restore before Google's applicable deadline where the app can run; pending purchases receive no paid entitlement. Capture acknowledgement failures rather than ignoring them.
6. Replace the arbitrary 14-day UI fallback with a documented bounded offline policy derived from the last successful Play query. The client does not supply an authoritative subscription expiry. A proposed engineering starting point is at most 72 hours of offline tolerance, without claiming a paid-through expiry; validate the customer tradeoff before shipping. This is not a Google requirement. Avoid resetting grace on failed requests and handle clock rollback conservatively.
7. Display only current Play-provided eligible offers. Show local price, period, introductory/trial duration, subsequent renewal charge, and automatic renewal/cancellation terms. Calculate comparable savings from current data or omit them. Disable purchase until ready; display cancellation, pending, error, retry, and success distinctly.
8. Provide Restore purchases and Manage subscription links. Handle single-cat to bundle upgrade without accidentally leaving overlapping paid subscriptions; use supported replacement flows where applicable and explain existing subscriptions. Do not silently change billing arrangements.
9. Reconcile the selected cat on verified loss of access; explain any free-cat fallback. After purchase, offer Use this cat and use bundle-specific success copy. Clean stale product mappings and publishing instructions.

Acceptance: Google license testers/Play Billing Lab validate purchase, cancel, pending completion, network drop, reinstall restore, renewal, grace/hold, expiry, refund/revocation, failed acknowledgement, and bundle transition. An outage preserves only the explicitly permitted cached access and never invents successful verification. Test real locale/currency/offer variations. Free monitoring remains independent of billing availability. [S16]

**Phase 5 — data, privacy, support, and performance**

1. Define the data inventory: local app selections/settings, aggregate usage/session outcomes, cached on-device purchase entitlements, and any diagnostics; no server purchase records exist. Define retention, deletion, recipients, and backup behavior before writing public disclosures.
2. Prefer excluding usage history, transient sessions, permission snapshots, and entitlement caches from cloud backup/device transfer using appropriate version-specific rules. If selected settings are backed up, document exactly which. Restore always rechecks permissions and Play entitlement. Verify OEM transfer behavior rather than assuming allowBackup=false handles every route. [S17]
3. Add Delete local history and Reset app settings with clear consequences. Explain that these do not cancel Play subscriptions. Do not imply there is developer-server data to delete; explain Google Play subscription management separately.
4. Update the real hosted privacy page, in-app disclosures, Data safety answers, README, and TODO. Describe purchase verification traffic accurately. Replace unsupported zero-data, permanent-enforcement, and guaranteed-restart claims. Review these materials against the shipped dependency/permission inventory.
5. Add bounded local diagnostics for service lifecycle, permission changes, trigger outcomes, and billing error categories. Exclude app-usage details and purchase tokens from routine logs. User-shared diagnostics must be explicit and redacted. Use Android vitals for production crashes/ANRs; introducing another telemetry SDK requires a deliberate privacy review.
6. Measure battery/CPU/memory and cold-start behavior on a defined device matrix. Compare matched idle/screen-off/active-use scenarios with monitoring disabled and enabled. Establish acceptable budgets from measured baseline before the release candidate; do not invent an unsupported battery percentage.
7. Eliminate repeated wide-window event scans, suspend unnecessary screen-off polling, and avoid waking the device for routine housekeeping. Preprocess poster transparency at build time where possible; load media only where needed; remove unused duplicate assets after reference checks; validate video codec compatibility and actual delivered download/install size.
8. Audit relevant OWASP MASVS storage, network, platform, privacy, and code controls; validate exported components, FileProvider scope, backup rules, secret handling, backend authentication, and log hygiene. Narrow shareable file paths if sharing is retained. Document applicable controls and evidence, not a blanket certification claim. [S18]
9. Test upgrade from existing saved settings, stale/free/paid cat selections, interrupted break records, corrupt preferences, and removed apps. Use schema migrations and recoverable defaults. Never clear all user data as the normal migration strategy.

Acceptance: privacy text matches implemented flows; legal/support links work; reset behavior is tested; no sensitive data is included in routine logs/backups; upgrade tests preserve user intent; performance evidence is available for the release candidate.

**Phase 6 — prove release readiness**

Test allocation follows Google's risk-based testing guidance: fast unit tests for rules, component/integration tests for storage and billing, and device/end-to-end tests for OS behavior. Do not use a coverage percentage as a substitute for testing the failure modes. [S19]

| Layer | Required evidence |
|---|---|
| Unit | Clock and usage traces; overlapping/reordered events; budgets; trigger state; offline entitlement policy; idempotent completion |
| Integration | DataStore transactions; Room migrations if used; billing response mapping; retry persistence; session restoration; backend verification/notification ordering |
| UI/accessibility | Onboarding, permission repair, empty state, cat selection, purchase states, break exit, media fallback, TalkBack and large text |
| Android device | API 24/25 if retained, 26, 31, 33, 35, 36 and any newer stable version supported at release; Pixel plus representative Samsung/Xiaomi devices; reboot, user stop, Doze, battery saver, lock, background launch restrictions |
| Play | Actual package/signing configuration, license testers, restore/lifecycle matrix, service declaration, Data safety, pre-launch report |
| Performance | Cold starts, ANRs/crashes, memory, video fallback, battery comparisons, installed/download size |

Release gates:

- Every audited blocker is closed with linked evidence; no unresolved critical/high customer-impact defect.
- Debug and release builds, lint, unit/integration tests, and device acceptance pass. Warning exceptions have an owner and rationale.
- No false active-health status, no known counting error in the defined scenarios, no lost paid access on ordinary transient failure, and no secret in shipped/source artifacts.
- Current Play target/Billing requirements and service declarations are satisfied; store listing describes tested behavior.
- Migration and incident recovery are rehearsed. Keep source tags and release artifacts; an Android rollback usually requires a corrected higher-version build, not installing an older version over users' data.
- Use internal then closed testing. For an existing production app, use a proposed 5% → 20% → 50% → 100% staged update, increasing only after sufficient observation and no blocking regression. These percentages are a recommendation, not Google's rule. A first production launch cannot use staged rollout; qualify it through test tracks first. Halting a rollout does not uninstall it from users already updated. [S20]
- Monitor crash/ANR trends, denied starts, unsuccessful break presentation, restore failures, and user feedback through the approved data model. Define halt criteria and support ownership before release. Do not collect usage history merely to measure reliability.

**Phase 7 — close the productivity-feature gaps**

These features address identified product limitations, not Android policy requirements. Add them only on the stable core, each with its own acceptance tests.

1. Per-app limits plus an optional shared distracting-app budget. Show which rule caused a break; prevent simultaneous rules from creating duplicate interruptions.
2. Work/study schedules, quiet hours, and temporary overrides. Test overnight windows, daylight-saving/timezone changes, reboot, and precedence between manual pause and schedules. Re-evaluate on user interaction/event processing; do not promise exact background wakeups without an eligible API.
3. Daily/weekly local feedback for measured selected-app usage, completed/skipped/interrupted breaks, and explicitly measured time away. Avoid labeling all break minutes as time saved or productive work.
4. Optional audio, static/reduced-motion presentation, accessible appearance choices, and tested localization. Preserve system font scaling.
5. Optional sharing only if it has a real user-facing entry point and privacy-safe output; otherwise remove the unused share generator.
6. Run voluntary usability sessions covering setup comprehension, confidence in monitoring status, interruption usefulness, and paid-access recovery. Any claim of productivity improvement requires an appropriate outcome study; retention or pet taps alone are not proof.

Acceptance: rules remain understandable, schedules do not surprise users, history is accurate and deletable, accessibility is validated, and additional features do not regress the Phase 6 gates.

**Finding-to-work mapping**

| Audit finding | Plan coverage |
|---|---|
| R1/R2 wrong service type, prohibited boot/restarts | Phase 1 |
| R3 API/Billing deadlines and metadata | Phase 0 + Phase 4 |
| R4 API-26 calls; Media3 lint errors | Phase 0 |
| R5/R6 reset loophole, screen/clock/restart accounting | Phase 2 |
| R7 swallowed failures and missing launch retry | Phase 1 + Phase 2 |
| R8/R9 restoration, stale rights, empty-on-error | Phase 4 |
| R10 pricing, loading, errors, savings | Phase 4 |
| R11 signing secrets | Phase 0 |
| Ordinary dismissible activity, missing escape policy | Phases 1–3 |
| Wake/lock-screen/audio behavior | Phase 3 |
| Media failure and lifecycle restoration | Phases 2–3 |
| False OEM/reliability status; empty selection | Phases 1 + 3 |
| Permission onboarding/recovery | Phase 3 |
| Selected premium cat and purchase-success mismatch | Phase 4 |
| Verification/acknowledgement/retry | Phase 4 |
| Accessibility/localization | Phases 3 + 7 |
| Concurrent counters/toggles; duplicate app keys | Phases 2–3 |
| Polling/poster/media performance | Phase 5 |
| Backup, legal/privacy uncertainty, obsolete docs | Phase 5 |
| Missing tests/CI; generated artifacts in Git | Phases 0 + 6; tests accompany all phases |
| Missing schedules/budgets/history; promotions during breaks | Phases 2–3 + 7 |

**Inputs required during execution**

Device/emulator access is needed for Phase 1/6 evidence. Play Console access and license testers are needed for service declarations and Phase 4/6. No backend environment or Google Play Developer API credentials are required under the user decision. Hosting access is needed to update legal pages. These inputs need not block the baseline, pure engine tests, UI repairs, or the client-only billing implementation; they do block claims that the corresponding production gate has passed.

**Official and primary references**

- [S1 — Google Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [S2 — Foreground service types, including specialUse](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [S3 — Google Play foreground service and full-screen intent declarations](https://support.google.com/googleplay/android-developer/answer/13392821?hl=en)
- [S4 — Android background activity launch restrictions](https://developer.android.com/guide/components/activities/secure-bal), [foreground-service start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [S5 — Android app signing and protecting signing information](https://developer.android.com/studio/publish/app-signing)
- [S6 — Google Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
- [S7 — Google Play Billing version deadlines](https://developer.android.com/google/play/billing/deprecation-faq)
- [S8 — Android persistent task scheduling](https://developer.android.com/develop/background-work/background-tasks/persistent)
- [S9 — UsageEvents definitions](https://developer.android.com/reference/android/app/usage/UsageEvents.Event)
- [S10 — Transactional DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [S11 — Notification permission behavior](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
- [S12 — Compose accessibility semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [S13 — Google Play Billing integration](https://developer.android.com/google/play/billing/integrate)
- [S14 — Google Play purchase security](https://developer.android.com/google/play/billing/security)
- [S15 — Subscription lifecycle and authoritative verification](https://developer.android.com/google/play/billing/lifecycle/subscriptions)
- [S16 — Billing tests and Play Billing Lab](https://developer.android.com/google/play/billing/test)
- [S17 — Android Auto Backup](https://developer.android.com/identity/data/autobackup)
- [S18 — OWASP Mobile Application Security Verification Standard](https://mas.owasp.org/MASVS/)
- [S19 — Google's Android testing strategy](https://developer.android.com/training/testing/fundamentals/strategies)
- [S20 — Google Play staged rollout rules](https://support.google.com/googleplay/android-developer/answer/6346149?hl=en)
