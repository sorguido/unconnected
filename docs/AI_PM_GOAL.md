# AI PM — Goal Mode

## GOAL

Deliver **unconnected v1** as a complete, elegant and REAL Android call-filtering app, not a mockup, emulator-only prototype or paper specification. Finish with a verified installable APK and source, using IT when the system locale is Italian and English otherwise; public documentation is English.

## COLD START

Read `START.md` and follow its canonical routing. `docs/PRODUCT_SPEC.md` owns the complete requirements (R1–R10); `ROADMAP.md` owns the gates. Inspect Git history and tests. Use only the relevant evidence and current phase documents, minimizing context and repeated investigation.

## IMPLEMENT

Own design, API verification, Kotlin/Material 3 implementation, Android role/permission onboarding, actual screening integration, tests, builds, independent review and repair. Proceed in Goal mode until an authentic human gate or verified completion. Use platform APIs, small local architecture, offline storage and no always-running scheduler or background polling.

Implement multi-number CRUD, flexible recurring custom days/times including overnight and split schedules, ACTIVATE green / DEACTIVATE red master control, polished responsive light/dark interface, English+Italian resources, real `CallScreeningService`, and safe strict caller matching. Prefer silent disallow out of hours, request notification suppression; fallback to reject only after evidence. All unknown/unlisted callers are allowed. Explain contact-role and forwarded-ID caveats.

## MANDATORY EXISTING SIGNING IDENTITY

Never create a new release keystore, certificate or signing password. Reuse the EXISTING PKCS#12 release signing identity used in `sorguido/chromecast-killswitch-app`. Its private configuration lives locally outside Git; refer to `docs/SIGNING.md` and `research/corroborations/C3.md`. The new Android app ID is `it.sorguido.unconnected`; display label `unconnected`. Compile/test without private material if required, but release only with the original signing certificate, verifying fingerprint. If unavailable, issue a precise Human Gate; do not silently sign with debug or a generated key.

## VALIDATE

Run automated tests, lint and an actual Gradle build. Verify schedule boundaries, number normalization, master state, locale, contact permission/role, persistence, background/reboot behavior. Test incoming phone calls on a REAL physical device to establish whether calls are silent, rejected, logged or notified. Do not use a fake screening response or a screenshot as proof of telephony behavior.

## DELIVERY

All R1–R10 and T01–T17 have verified results or clearly stated limitations. Provide public English install/build guide, signed APK, checksum, certificate identity verification, release rollback and test evidence. Independent review must find no unresolved serious issue. Explicit human approval is needed to publish a public release or operate a real user's phone. If needed, emit `HUMAN_REQUIRED` with `PURPOSE`, `EXPECTED_RESULT`, `COMMANDS`, `RISK`, `ROLLBACK`, `EVIDENCE_TO_RETURN`; continue all independent work before stopping.

Proceed now in Goal mode. Avoid unnecessary bureaucracy, overengineering, speculative frameworks or repeated status diaries.
