# Lint review ? production remediation

Reviewed 5 November 2026. Debug and release report zero errors. Warnings remain visible rather than being hidden by an error baseline. The complete reports are generated under app/build/reports/ and archived by CI.

| Warning | Review / disposition |
|---|---|
| ImplicitSamInstance | The flagged call is Android Context.stopService with an explicit Intent targeting MonitoringService. This API identifies the service using the Intent, not the identity of a listener/SAM instance. No listener is being unregistered. Retain the explicit stop action; confirm reset stops monitoring in device acceptance. |
| GradleDependency | Newer versions exist for pinned UI, storage, media, Billing and test libraries. This warning is not a security-vulnerability verdict. SDK/AGP/Gradle/Billing changes were compiled together; the rest retain the existing Kotlin/Compose-compatible set. Further upgrades require their own compatibility and device tests; no claim of latest-version coverage or completed vulnerability audit. |
| UnusedResources | Three obsolete guest-promotion strings remain in strings.xml. No guest overlay or promotion entry point ships in the break flow. The release resource shrinker is enabled. These can be removed in routine cleanup. |
| IconLauncherShape / IconDipSize / IconLocation / IconDuplicates | Existing raster launcher artwork was copied across densities and still needs proper legacy density/shape assets. The adaptive icon now has a monochrome paw. This branch does not claim the old artwork is corrected; final visual/package review remains open. |
| UseKtx | Style suggestions for Uri/color/bitmap helpers. The existing framework calls are supported; these suggestions are not runtime errors. No broad suppression was added. |

Owner: release maintainer. Remaining artwork and dependency work is recorded, not counted as verified production completion. Keep these reports alongside the release checklist. See the Android [Context.stopService contract](https://developer.android.com/reference/android/content/Context#stopService(android.content.Intent)) and [16 KB page-size guidance](https://developer.android.com/guide/practices/page-sizes) for the related release checks.
