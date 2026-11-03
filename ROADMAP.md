# Roadmap — execution state machine

Product acceptance criteria are canonical in `docs/PRODUCT_SPEC.md`. Phase details are stored on demand.

| PHASE | PURPOSE | PRIMARY_ARTIFACT | EXIT_GATE | NEXT |
|---|---|---|---|---|
| P0 FEASIBILITY | Verify Android screening limitations and select minimal viable implementation | `docs/phase-specific/P0.md` | API response/permissions, forwarding caveat and device test plan verified | P1 |
| P1 IMPLEMENT | Create fully functional native app and tests | Android source code and test results | Real installable debug APK with multiple numbers, editable recurring schedule, toggle, localized polished UI and screening role; automated checks pass | P2 |
| P2 VERIFY | Prove behavior on physical Android device(s) | `evidence/current/verification.md` | Verified real in-hours allowance, out-of-hours silent block or documented fallback, no unexpected notifications, reboot, permissions, OEM caveats | P3 |
| P3 RELEASE | Produce reviewed deliverable signed by the existing key | Signed APK, build/checksum/signature receipt, public English guide | Requirements and test matrix accounted for, APK verifiably signed using existing Chromecast signing certificate, explicit release approval | COMPLETE |

**PRIMARY:** Android `CallScreeningService`; for listed numbers outside hours, attempt `setDisallowCall(true)`, `setRejectCall(false)`, `setSkipNotification(true)`. First adapt within this API if testing fails. **FALLBACK:** explicit rejection `setRejectCall(true)` only if evidenced and necessary; do not implement independent solutions in parallel. Blocked call-log entries may remain.

An emulator-only, unsigned, nonfunctional or design-only artifact does not satisfy the device or release gates. Stop at an actionable `HUMAN_REQUIRED` if device access, signing, or publishing cannot be performed by the agent.

## Current follow-up — v1.1.0 (2026-11-03)

- **P1 COMPLETE:** PR #3 (fixed-length numeric ranges with trailing `*`) merged into `main`; manual GitHub Actions debug APK build available.
- **P2 OWNER-REPORTED PASS:** installed debug APK and numeric-range call-screening/counterchecks on a physical Android phone. This is owner testimony, not an agent-measured OEM/carrier receipt.
- **P3 PENDING:** on the local Fedora checkout, bump to `versionName 1.1.0` / `versionCode 7`, build the official APK using the existing external signing key, run required release checks, make only essential public-documentation corrections, then publish tag/Release `v1.1.0` with English notes, APK/checksum and corresponding source as established for v1.0.0. The v1.0.0 Release remains intact; no new signing key or cloud transfer of private material.
