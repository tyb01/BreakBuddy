# Implementation status

Branch: `fix/production-readiness`. Candidate: 1.0.5 (6). Updated 5 November 2026.

**The complete production plan is not yet qualified for release.** This branch implements the principal code repairs. Device, Play, signing and hosted-policy evidence remain required, and Phase 7 expansion is explicitly gated by the original plan. No backend has been added or is requested.

## Implemented

| Plan area | Changes |
|---|---|
| Build/security | SDK 36, AGP 8.10.1, Gradle 8.11.1, Billing 8.0.0; external signing configuration; generated output removed from tracking but retained locally; CI for both variants; manual signed-candidate workflow |
| Monitoring | User-started specialUse service and subtype; removed dataSync, boot/alarm/worker revival and battery-exemption request; health and stale-observation status; pause/stop actions; notification fallback and bounded presentation retry |
| Timing | Combined/per-visit engine; selected-package filtering, pause/screen/keyguard handling; elapsed-time accounting; incremental event cursor; bounded checkpoint recovery; reboot-aware break deadlines and monotonic snooze |
| Break experience | Timer independent of video; early exit; screen-off rest and interruption distinction; persisted session IDs and atomic counters; static/poster fallback; quiet video and optional audio focus; no in-break premium guest promotion |
| Setup/UI | Saved onboarding step; usage required for monitoring, notification/overlay optional; picker deduplication and transactional toggles; empty selection prevents starting; Home/Cafe/Reliability controls; accessible switch rows and slider labels; externalized new static copy |
| Billing | Reactive Play prices, matching offer phases, loading/pending/error/cancel states; no fabricated prices/savings; successful-empty vs failure; atomic entitlements; repeated successful checks refresh cache; 72-hour bounded offline policy; acknowledgement retry; Restore/Manage; overlap refusal |
| Privacy | Local bounded history, deletion/reset, backup/transfer exclusions, safe corruption recovery; unused share provider and obsolete helper code removed; documented nine-permission allowlist and build checks |
| Documentation | Architecture decisions, no-backend plan amendment, current README, privacy draft, release checklist, signing example; unsupported enforcement/zero-network claims removed |

## Verification

Final local Gradle command succeeded on 5 November 2026:

```text
:app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:lintRelease
:app:assembleDebug :app:assembleDebugAndroidTest :app:bundleRelease
```

- **20 unit tests passed per variant**, zero failures/errors (40 executions of the same 20 tests).
- **Six Android storage integration tests compiled and packaged**, not executed because no device was connected.
- **Zero lint errors, 45 warnings per variant.** Warning categories and disposition are in [LINT_REVIEW.md](docs/LINT_REVIEW.md); these have not been hidden behind a baseline.
- Debug APK and unsigned release AAB built. Local sizes are about 67.7 MiB and 52.5 MiB respectively, not Play-delivered download sizes. The release bundle is not signed for publication.
- Debug APK `zipalign -c -P 16 4` passed; inspected native ELF LOAD segments in the APK/AAB have 16,384-byte alignment. This static inspection does not replace 16 KB device and Play-split tests.
- Artifact hashes and machine-readable test/lint counts: [LOCAL_VERIFICATION.json](docs/LOCAL_VERIFICATION.json).

- Permission checks passed on both merged manifests: exactly nine explained permissions each.
- Source hygiene check passed: no tracked generated build output, keystore or literal Gradle signing password.
- `adb devices`: no connected device. No installed Android emulator executable was found.
- Public privacy URL could not be retrieved with the web tool; its live content/availability is unverified. The repository draft has not been published.
- GitHub workflows were authored locally, not executed on GitHub. Release environment protection and secrets are not configured by this change.

## Explicitly unfinished

1. **Device qualification:** background-start behavior on target 36; API 24/25 compatibility; process death/rotation/lock/reboot; real permissions/notification channels; TalkBack/large-font/layout; DataStore upgrade/corruption/reset and transfer tests. Pure unit tests do not establish these outcomes.
2. **Play qualification:** real package/signing/products, license-test subscription lifecycle/acknowledgement failures, specialUse declaration/review, Data safety and pre-launch results. No Play approval has been claimed.
3. **Release operations:** signing-history exposure review; protect/configure the GitHub release environment; validate the final signed artifact, version-code availability, native compatibility and delivered size; publish the reviewed privacy/support content.
4. **Performance and diagnostics:** measure battery/CPU/memory/cold start and long-running behavior. Status/heartbeat and local outcomes exist; a persisted redacted diagnostic journal/export and full instrumentation acceptance coverage are not yet implemented. Six storage integration tests have been added and compiled; they have not been executed on a device. No battery or reliability percentage is asserted.
5. **Phase 7 after qualification:** per-app overrides, weekday work/study schedules, measured daily/weekly usage and measured time-away summaries, complete localization/accessibility validation and voluntary usability/outcome studies. Shared budget, quiet hours, recent break history, sound/static options and deletion are implemented now.
6. **Build/security follow-through:** review all dependency upgrades as compatible groups and independently establish dependency-verification hashes/locking; CI currently pins versions and checks source/manifest hygiene, but it is not a complete secret-scanning or MASVS certification system.

The Play-only design intentionally cannot provide server-authoritative expiry, RTDN updates or closed-app acknowledgement guarantees. Its 72-hour cache is an explicit policy, not proof a subscription is still paid. The user rejected a backend; no server tasks remain required by this implementation.

See [release checklist](docs/RELEASE_CHECKLIST.md) for concrete evidence to collect, [architecture](docs/ARCHITECTURE.md) for limits and official sources, and [permission inventory](scripts/permissions.json) for each permission's owner and actual purpose.
