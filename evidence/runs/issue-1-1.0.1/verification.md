# Issue #1 — settings-save status candidate

Date: 2026-10-30 (Europe/Rome). Candidate: **1.0.1 / versionCode 2**. Baseline: `main` at `60828f98f8dee324c0dc50212ca7d11a0d5b730f`. [Issue #1](https://github.com/sorguido/unconnected/issues/1) remains **open, pending physical retest**.

## Cause and correction

**OBSERVED (user's physical testing):** Calls continue to follow the configured rules, but the heading can remain “Filtering is paused” after saving. Both required grants appear ready. Leaving and reopening the app refreshes the heading. Device model/Android build have not been supplied. This is evidence of misleading UI, not a demonstrated screening-engine defect.

**PROVEN (source and snapshot experiment):** The former `Home` sampled the non-observable `AtomicBoolean` into a local `ready` before entering nested content. The nested content reads observable `busy`/`settings`. Save completion can invalidate that child scope without invalidating `Home`, retaining the captured false readiness. Meanwhile a second raw atomic read for the detail can already be false, producing an inaccurate missing-permission explanation. `reproducesOldCapturedReadinessAcrossChildScopeUpdates` reproduces this exact observation-scope mechanism using Compose's real `SnapshotStateObserver`. This agrees with [Compose's state-observation contract](https://developer.android.com/develop/ui/compose/state).

The correction publishes the existing pause transitions through a read-only `StateFlow`, collected with `collectAsState()` in `Home`. The original atomic flag remains the screening service's source. A short independent publication monitor keeps the two values aligned; no disk operation holds this monitor. Failed saves still leave the process paused. The signal also supplies its current value to a new UI subscriber when a prior save is finishing; there is no dependency on a resume event or a different settings value.

One presentation result now provides both heading and detail:

| State | Display |
|---|---|
| Save in progress | Saving settings; temporary pause while saving |
| Master off, no save | Filtering is off |
| Master on, session paused | Filtering is paused; session/save explanation |
| Master on, setup missing | Filtering is paused; required setup explanation |
| Master on, ready, no pause/save | Your schedule is active |

No screening-service, schedule, normalization or filtering-decision code changed. Storage format, save sequencing, failure policy, dependencies and signing identity remain unchanged. The version code increases for an update without uninstalling. `.kotlin/` is ignored as a generated compiler cache.

**EVIDENCE_FOR:** Independent parent/child snapshot invalidations and the two differently timed reads in the old source match the reported heading/detail inconsistency. **EVIDENCE_AGAINST:** No captured runtime trace or rendered UI automation directly measures the user's phone. **ALTERNATIVE_EXPLANATIONS:** Genuine save failure or missing permissions can still correctly pause filtering; they now have distinct explanations. **CONFIDENCE:** High for the source-level cause and correction; physical acceptance pending. **MINIMAL_DISCRIMINATING_EXPERIMENT:** Repeat the original edits on the same phone with the new candidate, without leaving the foreground, and record the heading after each save.

## Automated and artifact evidence

- **PROVEN:** All **35 JVM tests** pass (31 existing + 4 new); none failed/skipped. The new tests cover the old observation-scope defect, status/copy selection, completion from another thread with a newly attached subscriber, repeated saves, retained pause on failure and successful retry. They use the production publisher via test-only reflection and the same Flow-to-snapshot bridge as `collectAsState`. They do **not** execute Android disk IO, rendered Compose, real Activity recreation or telephony.
- **PROVEN:** Debug/release APK builds and debug/release lint pass. Both lint reports have zero errors and the same eight SDK/dependency-update advisories. No new dependency, lint suppression or test default-return override was added. English/Italian resources have 77 matching keys.
- **OBSERVED:** [Independent review](review.md) found no unresolved serious source issue, with the runtime limitations above.
- **PROVEN:** `apksigner verify` accepts the candidate and its signer matches the independently published Chromecast v1.1.0 APK: `2936676a220e4a9682171fbf0c9b91552acd8fc4f120318c6a0aebe6e6722ecc`. The candidate remains `it.sorguido.unconnected` and the packaged permissions are unchanged. Private signing material was reused in place.
- Candidate: `development/deliverables/issue-1-1.0.1/unconnected-1.0.1-candidate.apk`.
- Candidate SHA-256: `13df4ce6952ee4206f2f827ca5d089dbf6625dbb2363f773086a1dc22841efc1`.
- Receipts: [automated](automated.json), [build](build.log), [signature](signature.json), [checksums](SHA256SUMS), [source input hashes](source-inputs.sha256). Source hashes identify the exact inputs compiled from the baseline plus this patch; the enclosing Git commit records their final revision.

Command executed successfully:

```bash
UNCONNECTED_SIGNING_ROOT=../chromecast-killswitch-app ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug :app:lintRelease --console=plain
```

## HUMAN_REQUIRED — ISSUE_1_PHYSICAL_RETEST

**PURPOSE:** Confirm the candidate fixes the reported status on the real phone before closing issue #1. The owner previously chose to perform manual tests. No device operation or new call has been performed by the agent for this patch.

**EXPECTED_RESULT:** With the master active and setup ready, every completed successful save returns the heading to active without refocusing; while saving, the dedicated saving text is used. With master off the settled heading is off. A genuine session pause or missing setup remains accurately described.

**COMMANDS:** Install the APK through the personal profile's installer, or, after identifying/authorizing the personal user, use:

```bash
adb -s DEVICE_SERIAL shell pm list users
# Only after confirming user 0 is the authorized personal user:
adb -s DEVICE_SERIAL install --user 0 -r development/deliverables/issue-1-1.0.1/unconnected-1.0.1-candidate.apk
```

Do not modify the work profile. Keep existing rules; do not clear data or uninstall. In the foreground with filtering active, change an end time, remove/re-add a weekday, edit a label, and save an unchanged value. Check the heading/detail after each completion. Repeat with filtering off and with a normal deactivate/activate cycle. If possible, also rotate during a save and check the resulting heading. Permission-revocation/failure cases require separate owner agreement; do not simulate storage failure destructively. A consenting in/out-of-hours call may confirm the user's previously observed behavior remains intact, but it is not a replacement for the UI check.

**RISK:** This update displays the existing temporary fail-open save pause more clearly; an active filter continues to affect selected callers according to their configured rules. Profile mistakes can affect a managed installation. No data migration is introduced.

**ROLLBACK:** Deactivate filtering if needed. A source rollback is a revert of this patch followed by a new signed candidate with a higher version code. Do not uninstall, clear rules or force a version downgrade solely to roll back the UI; Android may reject lower version codes.

**EVIDENCE_TO_RETURN:** Phone model, Android version/build, confirmation of candidate 1.0.1 and personal profile, and heading/detail after each save. Record whether refocus was needed and whether a save error appeared. No phone numbers, call logs or private screenshots are required. Do not close the issue until this result is received and assessed. Broader P2 telephone/OEM and public-release gates remain separate.
