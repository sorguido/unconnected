# MILESTONE_COMPLETE — V1.2-P1

Date: 2026-11-04. Implementation commit: `4eb608db33074c2837fcc686e7227bd352ecd157`, branch `development`. Baseline: `d5f591a` after the published v1.1.0/code-7 release. The attached owner mandate supplies the V1.2-P1 scope; the local protected roadmap still describes v1.1.0. No protected canonical file, release tag, release asset, official signing material or device setting was changed.

## Measured host results

**PROVEN:** `./gradlew --offline -Punsigned :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` completed successfully in 20 seconds. All **125 JVM tests passed**, with zero failures, errors or skips, including 37 new timer/persistence/presentation tests. Lint passed with zero errors and two inherited warnings about retaining compile/target SDK 36. The toolchain was the existing JDK 25, Gradle 9.6.0 and SDK/build-tools 36; no dependency, SDK or global configuration upgrade was needed. Gradle required authorized access to its existing cache/lock files outside the workspace; no official signing configuration was loaded (`-Punsigned`).

**PROVEN:** A real Debug APK was copied byte-for-byte to:

- `development/deliverables/unconnected-1.2.0-debug-vc8.apk` — 23,425,965 bytes.
- `development/deliverables/unconnected-1.2.0-debug-vc8.apk.sha256` — checked with `sha256sum -c`, result **OK**.
- SHA-256: `686efdefc9c6a88c1a1e772ec93e763343905441ade6ad10c170b2f97caaef5f`.

`aapt2` verified package `it.sorguido.unconnected.debug`, version `1.2.0-debug` / code 8, API 29 minimum / target 36, launchable activity and debuggable flag. ZIP CRC and required APK entries passed. `apksigner verify --verbose --print-certs` verified APK Signature Scheme v2 and the Android Debug signer, SHA-256 `c89c1294008f3b46c0bbaca0acc0146744730591ac443acc35472a7efa6a2262`, distinct from the existing official certificate. Only READ_CONTACTS and AndroidX's package-scoped receiver permission are requested; **no Internet permission** is present. This establishes an installable offline test artifact structurally; it does not claim a physical installation or real caller acceptance.

The APK and checksum are deliberately ignored local deliverables, not published assets. A fresh Debug installation coexists with release and has its own settings. Only one screening app can hold Android's role.

## Coverage and review

**PROVEN by JVM tests:** complete schedule/timer truth table inside/outside hours; timer precedence and exact expiry; early stop and schedule edits; zero/invalid/extreme durations; keypad/presets/countdown rounding; enabled/disabled exact numbers and masks; absent/unlisted/ambiguous callers; permissions/readiness; general corruption and typed invalid weekly windows; schema-1/2 migration and schema-3 anchors; process recreation and simulated reboot/clock changes; failed atomic-save contracts, preserved disk snapshots, session pause and retry; persistent post-reboot anchors, expiry tombstones and stale-UI save protection. Existing number, mask, calendar/DST, contact-picker and stable-layout regressions pass. English/Italian resource keys and the four required Italian button labels were also checked directly.

The storage tests inject a failing atomic-storage contract. They do not prove physical flash/fsync behavior or Android lifecycle dispatch. Presentation tests exercise shared state/input/layout logic; they do not prove native text shaping, TalkBack output or perceived flicker.

An independent reviewer identified three issues during implementation: post-reboot clock rollback could extend a timer, dedicated-screen save status could move its controls, and provider IO could delay the main-thread fail-open response. All were corrected and rereviewed; see [review.md](review.md). One initial test iteration failed because the existing state-inventory assertion did not yet include the new invalid-schedule state; the final test suite passes. No unresolved code blocker was found.

## Physical gate and limits

**OBSERVED:** No phone installation, role/permission change, real call, reboot, clock modification or publication was performed. OEM/model, Android version, carrier, caller presentation and notification behavior remain **UNVERIFIED** for this candidate. The owner's historical v1.1.0 acceptance is not v1.2 acceptance.

Elapsed realtime controls the duration within each observed boot, including screen-off time. The first settings read after reboot estimates offline elapsed time from the original bounded wall deadline and atomically commits a new monotonic anchor before exposing it. A failed anchor commit fails open. Subsequent wall edits and process recreation retain that anchor; unrelated saves merge the latest persisted state. Expiry observed by a settings read/save persists an inactive anchor. Visual countdown ticks do not write storage and never determine screening. Arbitrary clock edits before the first post-reboot observation remain inherently ambiguous without an external trusted clock; no universal offline-clock guarantee is claimed.

**Next: HUMAN_REQUIRED — GATE=V1.2-P2_PHYSICAL_ACCEPTANCE.** The complete [owner checklist and installation/rollback commands](../../../docs/TIMER_DEVICE_TEST_GATE.md) cover all four control combinations, masks/counterchecks, screen-off expiry, reboot, clock changes, missing permissions, schedule restoration, notifications and UI/accessibility. Install only in the confirmed personal profile; manually select Debug as the screening app and grant Contacts for the trial. Restore the prior screening app afterward and preserve the official package/data. No official signing or publication is authorized.

Machine-readable measurements: [automated.json](automated.json). Raw evidence: [build.log](build.log), [lint-debug.txt](lint-debug.txt), [signature.txt](signature.txt), [apk-metadata.txt](apk-metadata.txt), [SHA256SUMS](SHA256SUMS), [source-inputs.sha256](source-inputs.sha256).
