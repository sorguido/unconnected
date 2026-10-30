# Independent review — dynamic status-card layout, candidate 1.0.0 / code 6

Date: 2026-10-30 (Europe/Rome). Baseline: `ffaa2649792cf82501d880d066aa549d6ec130cf` on `main`, plus the uncommitted working-tree change under review. The reviewer was briefed with the binding requirements and constraints, inspected the source diff and both rewritten test files, and independently re-measured resource parity and the protected-file/`git diff --name-only` scope. It performed no device action, no build and no edit.

## Verdict

**APPROVE — no blocker.** Requirements 1–4 and 6–8 are met by construction; requirement 5 is met by construction for the card/strip separation and **by argument** on two accessibility points recorded below. The freeze logic was found correct under Compose semantics. No protected canonical file and no screening, matching, scheduling, permission, persistence or codec source was touched.

## Freeze-logic correctness

- **PROVEN (code-path enumeration):** `held` can only initialize while `busy == false`, because `busy` becomes true solely from controls that exist inside the same `else` branch that declares it. The first composition therefore stores a truthful committed state.
- **PROVEN:** `held` is read during composition only when `busy` is true and written by `SideEffect` only when `busy` is false, so there is no read/write overlap, no self-invalidation loop and no extra recomposition. Enum structural equality would suppress a redundant write in any case.
- **INFERRED:** `SideEffect` captures the same composition's `committed`; click handlers cannot interleave between apply and effects on the main thread, so no backwards write occurs.
- **INFERRED (dispatcher FIFO, not JVM-testable):** `persist()` calls `beginUpdate()` then sets `busy` synchronously; `SettingsStore.save()` re-enters `beginUpdate()` and clears the pause only on success, publishing `false` on the IO dispatcher before the main-thread continuation that assigns `settings` and clears `busy`. No frame should therefore observe `busy == false` together with a stale `sessionPaused == true` after a successful save.
- **PROVEN (pure functions) / INFERRED (composition):** on failure the pause stays published, the card shows `SESSION_PAUSED` beside the `save_failed` dialog, and a retry holds `SESSION_PAUSED` instead of flashing `ACTIVE`.
- **INFERRED (pre-existing, unchanged):** on Activity/process recreation during an in-flight save, `busy` resets but the static `SettingsStore.sessionPaused` survives in process, so a fresh composition truthfully shows `SESSION_PAUSED` while screening fails open.

## Requirements

Met by construction: a plain `Column` card with no `Layout` and no maximum reservation; the same `Section` padding and 10-dp spacing as every other card; the progress strip outside the scrolling `Column` in a fixed four-dp overlay; four distinguishable committed states; the button disabled while busy, guarded against double taps by the synchronous `busy` check, colours held, and `heightIn(min = 64.dp)` retained as pre-existing button sizing rather than card padding. The retained `stableContentMeasurePolicy` now serves only the two short master-button labels, where reserving their natural maximum prevents a toggle-induced button-height jump; it is no longer applied to status text.

Truthfulness: the card keeping "Your schedule is active" while the atomic write briefly pauses filtering is what requirement 5 mandates, with the pause carried by the autonomous strip; the reviewer flagged the momentarily inaccurate `on_detail` claim as a spec-sanctioned nuance, not a blocker. `OFF` taking precedence over a pause is pre-existing and truthful, since all calls are allowed either way. Post-failure state is truthful.

Regressions: issue #1 pause observability intact and arguably strengthened, because a genuine pause now surfaces on the card instead of being masked by a transient `SAVING` state. Issue #2 picker untouched. Reviewer independently confirmed 90/90 English/Italian string-name parity. Dark mode, scrolling and font scale unaffected. The `build.gradle.kts` diff is the version code alone; no new dependency.

Tests: the rewritten tests assert the new specification, and the two removed tests asserted exactly the reported defect (one fixed 150-px height for every status), so they are legitimately obsolete. **Gap recorded:** `anInFlightSaveNeverReplacesTheCommittedCardCopy` reproduces `Home`'s hold/`SideEffect` rule by hand, so the simulation could drift from the real composition; actual `remember`/`SideEffect`/frame timing is not JVM-testable without Robolectric or instrumentation, which the brief forbids adding.

## Should-fix items and their disposition

1. **Broken evidence link (fixed before commit).** `docs/BUILD.md` and `tests/TEST_MATRIX.md` pointed at this run's `verification.md`, which did not exist yet when the reviewer ran. It is written and committed with this receipt.
2. **No audible completion feedback (accepted limitation, owner decision).** In code 5 the card's polite live region changed from the saving copy to the final state, so TalkBack announced completion. With the card frozen, a save that does not change the committed state — editing a time window or a number while `ACTIVE` — announces the start sentence and then nothing; the strip disappears silently. Whether a newly inserted polite live-region node is announced at all is TalkBack/OEM dependent. No completion announcer was added: doing so needs new localized strings and a new mechanism whose real behavior is unverifiable here, which would exceed a small presentation-only change and could itself become a defect. It is recorded as a known regression against code 5 and placed explicitly in the owner's TalkBack checklist, with a follow-up candidate to be requested only if the owner finds it material.
3. **Pause sentence is not visible (deliberate, owner decision).** Code 5 showed "Filtering is temporarily paused…" as card text; code 6 carries it as the strip's `contentDescription` on a four-dp bar, so a low-vision user who does not run a screen reader has no readable pause statement during the write. This is what requirements 4–5 ask for — autonomous, non-displacing feedback — and the pause lasts only for one small atomic file write. Growing the overlay to display text would cover scrolled content and reintroduce a transient visual change. Recorded for explicit owner acceptance.

## Residual risks and limits

Natural card height, the absence of the code-5 empty block, the absence of perceived flicker, live-region announcement behavior, TalkBack completion feedback, large-font and narrow-width wrapping, and scroll-position retention across a legitimate committed-state resize are all owner-only real-phone checks. The dispatcher-FIFO ordering assumption above is untestable on the JVM. An abandoned save leaves filtering failed open until the next successful save; that is pre-existing behavior, unchanged here.
