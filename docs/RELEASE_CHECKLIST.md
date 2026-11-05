# Release qualification checklist

Do not publish the unsigned local bundle or describe the branch as production-qualified until the unchecked evidence below is supplied. A checkbox is evidence, not an instruction to claim a pass.

## Local gates

- [x] Re-run both variant unit suites, lint, debug APK and release AAB after the last source edit; record results in IMPLEMENTATION_STATUS.md.
- [x] Run scripts/check_source.py and scripts/check_permissions.py against fresh merged manifests.
- [x] Review lint exceptions and artifact size; no blanket lint-error baseline.
- [ ] Inspect the final signed AAB manifest and Play-delivered APK splits, including 16 KB compatibility when native libraries are present.

## Device matrix (not executed in this environment)

API 24/25, 26, 31, 33, 35, 36; representative Pixel, Samsung and Xiaomi. Record model/build/app version for each result. No emulator was installed and adb reported no connected device on 4 November 2026.

- [ ] Fresh setup, denial/permanent denial, notification channel disabled, usage/overlay revocation while monitoring; free features remain usable.
- [ ] Combined 8+7-minute app switching, per-visit resets, removed apps, empty selection, split-screen/PiP and repeated events.
- [ ] Lock/unlock, screen-off rest, Home/Recents early interruption, rotation, process recreation, reboot, force-stop and task removal. Verify no restart loop or duplicate completion.
- [ ] Manual wall-clock/timezone changes while counting and during a break; quiet hours across midnight; pause and explicit resume precedence.
- [ ] Automatic presentation acknowledged with access; denied background launch and notification fallback; both optional permissions denied must not claim reminders appeared.
- [ ] Broken/missing video and decoder timeout, static mode, silent default, audio-focus loss, music/calls; no extra phone permission.
- [ ] TalkBack, switch/keyboard navigation, 200% fonts, small screens, landscape, edge-to-edge and predictive Back.
- [ ] Upgrade existing preferences and paid/free selections, corrupt preferences notice, delete history/reset, backup/transfer exclusions and orphan break reconciliation.
- [ ] All-day screen-on/off monitoring and matched battery/CPU/memory/cold-start comparison against disabled monitoring. Record measured values rather than invented budgets.

## Play and release-owner actions

- [ ] Confirm com.pawsup signing lineage and highest uploaded version code; candidate 6 must be increased if already used. Review prior committed signing credentials and rotate/reset the upload key if exposure warrants it, without blindly changing the app-signing identity.
- [ ] Configure exact products/base plans from ProductIds.kt and localized offers in Play Console.
- [ ] Play license-test purchase, cancellation, pending completion, restart/reinstall Restore, network outage, renewal, grace, hold, pause, expiry, refund/revoke and failed acknowledgement. Verify offline-cache boundary and cat/bundle overlap refusal. Debug package has .debug suffix; use the Play-configured release package for real tests.
- [ ] Submit specialUse FGS declaration and demonstration; obtain actual approval. If rejected, revise the product contract and architecture rather than bypass restrictions.
- [ ] Publish reviewed privacy text at the real hosted URL, complete Data safety and verify legal/support URLs. The repository draft is not publication.
- [ ] Configure GitHub release environment reviewers and signing secrets, verify signed AAB, retain mapping/native-symbol artifacts and source revision.
- [ ] Run internal/closed testing and pre-launch report; define support owner, halt criteria, and staged update rollout before production.

## Phase 7 dependency gate

The original plan explicitly places productivity expansion after core device/Play qualification. Combined budget, quiet hours, recent outcomes, sound/static presentation and deletion are already present. Per-app override budgets, weekday work/study schedules, measured daily/weekly usage and time-away summaries, and expanded tested localization remain pending that gate. Do not label break duration as time saved or claim measured productivity gains. Voluntary usability/outcome research is not replaced by unit tests.
