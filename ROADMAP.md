# Roadmap — execution state machine

Product acceptance criteria are canonical in `docs/PRODUCT_SPEC.md`. Phase details are stored on demand. `AGENTS.md` protects canonical files; this ROADMAP update is expressly authorized by the project owner on 2026-11-04.

| PHASE | PURPOSE | PRIMARY_ARTIFACT | EXIT_GATE | NEXT |
|---|---|---|---|---|
| P0 FEASIBILITY | Verify Android screening limitations and select minimal viable implementation | `docs/phase-specific/P0.md` | API response/permissions, forwarding caveat and device test plan verified | P1 |
| P1 IMPLEMENT | Create fully functional native app and tests | Android source code and test results | Real installable debug APK with multiple numbers, editable recurring schedule, toggle, localized polished UI and screening role; automated checks pass | P2 |
| P2 VERIFY | Prove behavior on physical Android device(s) | `evidence/current/verification.md` | Verified real in-hours allowance, out-of-hours silent block or documented fallback, no unexpected notifications, reboot, permissions, OEM caveats | P3 |
| P3 RELEASE | Produce reviewed deliverable signed by the existing key | Signed APK, build/checksum/signature receipt, public English guide | Requirements and test matrix accounted for, APK verifiably signed using existing Chromecast signing certificate, explicit release approval | COMPLETE |

**PRIMARY:** Android `CallScreeningService`; for listed numbers outside hours, attempt `setDisallowCall(true)`, `setRejectCall(false)`, `setSkipNotification(true)`. First adapt within this API if testing fails. **FALLBACK:** explicit rejection `setRejectCall(true)` only if evidenced and necessary; do not implement independent solutions in parallel. Blocked call-log entries may remain.

An emulator-only, unsigned, nonfunctional or design-only artifact does not satisfy device or official release gates. For a *development test* gate, a real installable **debug-signed** APK is acceptable; do not confuse it with a production-signed release. Reserve `HUMAN_REQUIRED` for genuine human-only gates (physical telephone tests, Android roles/permissions, private release signing, publication or materially changed scope), not ordinary implementation, build or test work.

## Completed baseline — v1.1.0 (2026-11-03)

- **P1 COMPLETE:** PR #3 added fixed-length numeric ranges with trailing `*`, merged into `main`; 88 automated JVM tests reported PASS, debug/release build and lint checks passed.
- **P2 OWNER-REPORTED PASS:** owner installed a test APK and reported physical-phone numeric-range and countercheck tests PASS; this is not an agent-measured OEM/carrier receipt.
- **P3 COMPLETE:** official versionName `1.1.0` / versionCode `7`; tagged and released as `v1.1.0`, using the existing external signing identity. Release assets and checksum round-trips were verified. The v1.0.0 release remains preserved.
- Release evidence and receipt: `evidence/runs/release-v1.1.0/verification.json`; `PROJECT_STATE.json` keeps the historical publication metadata.

## Completed release — v1.2.0 temporary call-block timer (2026-11-04)

**Delivered:** a temporary block override for the *same enabled exact-number and fixed-length-range rules* already stored by unconnected. The timer is independent of both the scheduling master toggle and the current allowed windows: while active, matching enabled entries are filtered even during otherwise allowed hours. On expiry or manual timer deactivation, the pre-existing schedule state and windows take effect again automatically, unchanged. Unlisted, individually disabled, hidden, incomplete and ambiguous callers remain allowed, subject to the existing fail-open policy and required Android role/permission.

**User interface implementation:** adds the timer control directly below the existing schedule control in the main status card. Label the two controls explicitly (Italian: `ATTIVA SCHEDULAZIONE` / `DISATTIVA SCHEDULAZIONE`; `ATTIVA TIMER` / `DISATTIVA TIMER`), with **green to activate an inactive function and red to deactivate an active function for both**. Opening an inactive timer leads to a real dedicated duration-entry screen patterned after Android Clock Timer: hours/minutes/seconds, convenient presets, start action and a truthful active countdown/stop experience. The status card must accurately distinguish timer override from weekly scheduling. Detailed acceptance criteria remain in `docs/PRODUCT_SPEC.md`.

| PHASE | PURPOSE | PRIMARY_ARTIFACT | EXIT_GATE | NEXT |
|---|---|---|---|---|
| V1.2-P1 SPEC + IMPLEMENT + BUILD — **COMPLETE** | Extend `docs/PRODUCT_SPEC.md` and relevant test matrix; implement durable, independent timer override, UI and decision logic without regressions | Source, updated specification/tests, automated test and lint evidence, **actual installable offline-use debug APK plus SHA-256** | Code complete; expiry and precedence tested, including schedule ON/OFF, editing, process restart/reboot semantics, cancellation, permissions and disabled numbers; reproducible debug build and test/lint PASS; debug APK exists and is accessible for user sideload testing | V1.2-P2 |
| V1.2-P2 OWNER DEVICE ACCEPTANCE — **PENDING** | Trial the supplied debug APK with real calls on the owner's Android device; record OEM/OS and privacy-safe results | Physical test checklist and owner-confirmed results | Owner explicitly reports acceptance or requests fixes; any defects return to V1.2-P1 | Owner follow-up; release already authorized |
| V1.2-P3 SIGNED RELEASE + PUBLISH — **COMPLETE (OWNER-AUTHORIZED)** | Owner-authorized build, verification and publication of v1.2.0 with the original signing identity | Signed v1.2.0 APK, checksum/signature verification, English release notes, tag and GitHub Release | Explicit later authorization, matching original certificate, verified assets and publication receipts | COMPLETE |

**Implementation receipt:** V1.2-P1 completed with 125 passing JVM tests, a verified debug-signed test APK, lint and timer coverage. Details: `evidence/runs/timer-v1.2.0/verification.md`. The owner reported positive preliminary phone tests; thorough workplace verification remains pending.

**Official release:** `versionName 1.2.0`, `versionCode 8`, tag `v1.2.0` published on 2026-11-04 with the original signing certificate. See `evidence/runs/release-v1.2.0/verification.json`. The `development` branch was merged into `main` by the owner at `25cbf826974ed04762b82654cb16f0fa9f9b4abe`.

**Continuing constraints:** preserve saved rules and fail-open behavior; no Internet permission, unnecessary background service, private signing material in Git, or unauthorized phone changes. Full physical call/notification/reboot validation remains an open owner follow-up.
