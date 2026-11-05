# PawsUp

PawsUp is a user-controlled Android break companion. It observes foreground usage of apps the user selects and offers a voluntary cat break after a configured budget. It does not detect scrolling, measure attention, or lock the phone.

The implementation is on `fix/production-readiness`, candidate 1.0.5 (6). Read [implementation status](IMPLEMENTATION_STATUS.md), [architecture decisions](docs/ARCHITECTURE.md), and the [release checklist](docs/RELEASE_CHECKLIST.md) before publishing. Passing local builds is not Play approval or device qualification.

## Current behavior

- Monitoring starts through the app, with usage access, a visible foreground-service notification and Stop/Pause controls. There are no boot, alarm or worker revival loops. Reopen the app to resume after a system stop; force-stop is respected.
- Combined mode shares time across selected apps. Existing per-visit preferences retain their behavior. Screen-off/locked time, quiet hours, snoozes and active breaks are excluded. Only the most recently resumed app is counted; overlapping windows are not double-counted. Usage is an estimate from Android events, not attention measurement.
- Optional display-over-apps access permits attempted automatic presentation. Otherwise the user taps the notification. Neither Android nor manufacturer restrictions are bypassed. Notification denial prevents notification reminders.
- Breaks have an elapsed-time countdown, visible early exit, static/reduced-motion mode, silent videos and optional cat sounds. Completed, skipped and interrupted outcomes are distinct. Media errors cannot control the timer.
- Google Play handles subscriptions. There is no app backend or sign-in. Prices come from Play. Restore/retry and subscription management are available. Failed checks retain only the bounded 72-hour cache; this policy is a product choice, not server verification or a known subscription expiry. Conflicting cat/bundle purchases are blocked rather than silently creating overlapping subscriptions.
- Selections, settings, checkpoints and the last 200 break outcomes are local. History deletion and settings reset are available. Reset does not cancel Play subscriptions. Backup/transfer exclusions are configured. Google Billing includes its own transport dependencies; this is not a zero-network application.

## Build and verify

Use JDK 17, Android SDK platform 36 and Gradle wrapper 8.11.1. Set `ANDROID_HOME` for your machine or an ignored `local.properties`; no personal SDK path is committed.

```powershell
.\gradlew.bat testDebugUnitTest testReleaseUnitTest lintDebug lintRelease assembleDebug bundleRelease
python scripts/check_permissions.py
python scripts/check_source.py
```

The ordinary release build is unsigned unless an ignored `keystore.properties` is provided. Copy `keystore.properties.example` and use the existing Play upload identity. Never commit credentials or reset an existing signing identity casually. A newly generated debug key cannot test real purchases for `com.pawsup` without the corresponding Play setup.

CI checks both variants and archives test/lint reports. The manual release workflow uses a `release` environment and signing secrets, builds a candidate and uploads artifacts; it does not publish to Play. Configure required environment reviewers and protected branches in GitHub before relying on that gate.

## Permissions

The machine-readable [allowlist](scripts/permissions.json) records all nine merged permissions, their owners, features and denial behavior. The manifest gate checks debug and release. No accessibility service, device-admin API, exact alarm, full-screen intent, contacts/location/camera/microphone/storage access, blanket package visibility, wake-lock declaration, battery exemption or boot-revival permission is added by the app.

Official references are linked from the architecture record, allowlist and original [fix plan](PRODUCTION_FIX_PLAN.md). Remaining device/Play checks and deferred Phase 7 product expansion are explicit in the implementation status.
