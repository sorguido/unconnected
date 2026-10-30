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
