# unconnected 1.0.0 candidate verification

Date: 2026-10-30 (Europe/Rome). Source baseline: `main` at `9e2a899a6952e8bb4a4c23a2989c033b07cfd8fa`; implementation is in the uncommitted working tree and the supplied source archive. Protected canonical files are unchanged.

## Measured outcome

**PROVEN:** Debug and original-key signed release APKs were built; 31 JVM tests passed with no failures or skips; debug and release lint each completed with zero errors. Both lint reports retain eight version/update advisories (SDK 36, pinned Gradle/libraries), not suppressed checks. There are 75 matching English/Italian string resources. Dependency versions are pinned and the resolved graph is locked. See [automated receipt](automated.json), [build log](build.log), [debug lint](lint-debug.txt), [release lint](lint-release.txt), and [independent review](review.md).

Final build command:

```bash
UNCONNECTED_SIGNING_ROOT=../chromecast-killswitch-app ./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug :app:lintRelease --write-locks --console=plain
```

**PROVEN:** A normal release build without `UNCONNECTED_SIGNING_ROOT` failed at `requireOriginalSigning`, as intended; [negative-test log](signing-fail-closed.log). No new release keystore or signing password was generated. The existing private properties and PKCS#12 were used in place, outside the repository. Debug signing is separate and is not presented as an official release.

**PROVEN:** `apksigner verify` accepts both candidate and reference APK. The candidate signer SHA-256 exactly matches the Chromecast v1.1.0 signer:

`2936676a220e4a9682171fbf0c9b91552acd8fc4f120318c6a0aebe6e6722ecc`

Candidate APK SHA-256:

`b998d661e71dfc6b920a677810984f4b5aa8d46229faa2593c35b4a934a12d24`

The local reference APK SHA-256 equals the digest returned by GitHub for the [published v1.1.0 APK](https://github.com/sorguido/chromecast-killswitch-app/releases/tag/v1.1.0), asset `671937453`: `4e1583fc7421ba2ed508966ec8c567a336c0f0be5d6fa775a3a001885bf13e43`. This links the measured reference certificate to the published artifact. Package identities remain distinct: `it.sorguido.unconnected` and `it.sorguido.chromecastkillswitch`. Candidate version is 1.0.0 / code 1, minimum API 29, target API 36. The only platform permission in the packaged manifest is READ_CONTACTS; AndroidX also declares its app-specific protected dynamic-receiver permission. No Internet permission is present. [Full signature/package receipt](signature.json); verification can be repeated with `tools/verify_apks.py`.

**OBSERVED:** `adb devices -l` returned an empty device list after read-only host enumeration. No app was installed on a phone and no role, permission, forwarding or telephone setting was changed. No real calls were placed. SDK/Gradle caches were used and the host ADB server was started. No emulator system image or KVM device was available in the inspected environment.

**INFERRED:** The candidate is ready for controlled physical-device acceptance, not yet verified for production. A signed, structurally valid APK does not establish successful installation, silent blocking, notification behavior, reboot delivery or rendered accessibility.

## Requirements coverage

| Requirement | Current evidence / remaining limit |
|---|---|
| R1 | Implemented CRUD, labels, per-number enable, region-aware normalization, duplicate checks; JVM identity/codec tests pass. Android UI/persistence workflow awaits phone. |
| R2 | PROVEN domain tests: start inclusive/end exclusive, weekdays, split/overnight, local zone/DST, merged current/next interval. Real calls at boundaries await phone. |
| R3 | OFF default, green/red master control and immediate session bypass reviewed; disabled decisions tested. Physical toggle/restart await phone. |
| R4 | Fail-open decisions and strict full-number matching tested; service requests disallow=true/reject=false/skipNotification=true only for blocked cases. Actual ring/notification/log/caller experience unproven. |
| R5 | Explicit role/Contacts prompts, denied/revoked fail-open checks and forwarding caveats implemented. Android interaction awaits phone. |
| R6 | Responsive Material 3 light/dark UI, scrollable editors, wrapping actions and control semantics implemented/reviewed. Visual, large-font and TalkBack acceptance NOT RUN. |
| R7 | 75 English/Italian strings, primary-locale IT/else EN override and system 12/24 preference implemented. Runtime locales and picker NOT RUN. |
| R8 | Atomic bounded local document, strict corruption checks, worker evaluation and main-thread three-second fail-open budget reviewed; codec/domain tests pass. Android disk lifecycle, reboot and actual deadline timing NOT RUN. |
| R9 | Packaged manifest has no Internet permission; no contact queries, telemetry, phone data logs or scheduler; backup/transfer excluded. Static source/package evidence. |
| R10 | Build/tests/lint, source, signed candidate, certificate comparison, checksum and English guides complete. Physical install/update/uninstall, real calls and release approval still required. |

## Acceptance matrix results

| ID | Result |
|---|---|
| T01 | PARTIAL: codec/duplicate/disabled-entry tests pass; three-entry UI CRUD/persistence awaits phone. |
| T02 | PASS (JVM boundaries); call-level check pending. |
| T03 | PASS (JVM weekdays/week rollover); call-level check pending. |
| T04 | PASS (JVM overnight boundaries/interval end); call-level check pending. |
| T05 | PASS (JVM split/overlapping intervals); call-level check pending. |
| T06 | PARTIAL: resource parity, locale code reviewed; runtime IT/EN/third locale pending. |
| T07 | PARTIAL: disabled decision and persistence codec pass; real switch/restart/reboot pending. |
| T08 | PARTIAL: readiness=false allows all in unit tests and platform checks reviewed; role/permission revocation on Android pending. |
| T09 | PASS (JVM international/regional/invalid/duplicate/ambiguous identity); carrier presentation pending. |
| T10 | NOT RUN — physical outside-hours call/notification/log/caller required. |
| T11 | PARTIAL: in-hours/unlisted decisions pass; physical normal ringing pending. |
| T12 | NOT RUN — saved-contact physical calls required. |
| T13 | NOT RUN — approved forwarded path and two origins required. |
| T14 | NOT RUN — actual visual, large-font and accessibility evidence required. |
| T15 | NOT RUN — physical lifecycle/background/reboot required. |
| T16 | NOT RUN — physical install/update/uninstall required. APK package/signature checks pass. |
| T17 | PASS — published-reference digest, exact signer equality, distinct package IDs and candidate checksum measured. |

## Failure investigations and limits

An initial lint run failed inside its Kotlin `UseKtx` analysis of a local response function. **EVIDENCE_FOR:** observed compiler/detector exception. **EVIDENCE_AGAINST:** compilation succeeded and the final debug/release lint runs succeed. **ALTERNATIVE_EXPLANATIONS:** lint/compiler interaction rather than rule failure. **CONFIDENCE:** high that refactoring to a per-call response object resolved this tool failure. **MINIMAL_DISCRIMINATING_EXPERIMENT:** performed final lint on both variants with no disabled detector. During review, strict parsing, API 29-compatible IO, main-thread storage and immediate-toggle races were also repaired; see review report.

Physical behavior is not inferred from API documentation or test doubles. The five-second Android deadline cannot be universally guaranteed under OS process stalls; the three-second fallback and bounded inputs reduce risk and require cold-start device measurement. Carrier national-only/ambiguous presented IDs deliberately pass. Android/OEM logs may remain. No explicit-reject fallback was introduced.

## Next terminal gate

`HUMAN_REQUIRED`, `GATE=PHYSICAL_DEVICE_ACCEPTANCE`. The complete purpose, expected result, commands, risk, rollback and anonymized evidence request are in [DEVICE_TEST_GATE.md](../../docs/DEVICE_TEST_GATE.md). P0 feasibility and P1 implementation/build checks are complete; P2 real-phone verification is unmet. Signed candidate preparation does not bypass P2 or the later `PUBLIC_RELEASE_APPROVAL` gate. No public release was created.
