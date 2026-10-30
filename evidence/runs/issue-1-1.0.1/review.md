# Independent review — issue #1 candidate 1.0.1

Date: 2026-10-30. Independent reviewer inspected the working-tree change against baseline `60828f98f8dee324c0dc50212ca7d11a0d5b730f`, the four added regression tests, and generated build/test evidence. Scope is the stale status indication, not renewed telephony acceptance.

## Outcome

**OBSERVED:** No unresolved serious issue found in the focused correction. The protected canonical files, screening service, schedule/number algorithms, dependency declarations and dependency lock remain unchanged. The APK version advances to 1.0.1 / code 2 for an in-place candidate update. Signing identity verification belongs to the delivery receipt.

## Cause and correction

**PROVEN at the state-observation level:** The original readiness value sampled an `AtomicBoolean` outside the nested content scope. A child scope could refresh after the asynchronous save while the captured readiness value stayed false. A second direct atomic read could then select a missing-permission explanation despite the stale paused heading. The added old-behavior reproducer demonstrates that snapshot observation does not invalidate the parent when only that atomic changes.

**OBSERVED:** `SettingsStore` now publishes its existing pause transitions through a read-only `StateFlow`. Publication updates the original atomic and the flow under a short monitor separate from the disk lock. The existing begin-update/revision sequence, successful-save reset, and failure-keeps-paused behavior are preserved. `Home.collectAsState()` observes completion even when the composition attaches during an existing save; it does not depend on a settings inequality, a local busy transition, or Activity resume.

A single `FilterStatus` supplies both title and explanation. Saving, disabled, active, session pause and missing setup have consistent distinct presentation. There is no polling, new service, dependency, or change to call eligibility.

## Regression evidence and limits

**OBSERVED:** The full JVM result XML contains 35 tests, zero failures/errors/skips, including four new tests. The final build log reports successful debug/release APK builds and debug/release lint tasks.

The added tests cover status priority/copy, the old captured-readiness observation failure, background completion invalidating an observed status without settings/resume changes, attachment during an existing pause, repeated transitions, a retained pause after failure and successful retry. They use real Compose snapshot observation and the production flow publisher.

These are JVM state tests. The success publisher is invoked through test-only reflection; they do not execute Android atomic-file writes, render Compose UI, drive Activity recreation, or make real calls. The manually represented failed save checks the retained-state contract rather than injecting an Android storage failure. Those limits are explicit and do not invalidate their narrow regression coverage.

**INFERRED:** The fix addresses the reported foreground stale-status mechanism and the related reattachment case. **EVIDENCE_FOR:** paired publisher, observable UI subscription and passing targeted tests. **EVIDENCE_AGAINST:** no post-fix physical retest yet. **ALTERNATIVE_EXPLANATIONS:** an older installed candidate, platform permission changes or unrelated lifecycle/storage behavior could produce superficially similar status. **CONFIDENCE:** high for the observed state mechanism; phone acceptance remains unproven. **MINIMAL_DISCRIMINATING_EXPERIMENT:** install this candidate on the personal Android user, repeatedly edit days/times while active and keep the app foregrounded; confirm the completed save returns the correct heading immediately. Also check disabled/setup states and rotate during a save when feasible.

Issue #1 must remain open until the physical retest is reported. No phone operation, public release, or issue closure was performed by this reviewer.
