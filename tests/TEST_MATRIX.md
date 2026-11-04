# Acceptance tests

Record actual test status and evidence; an API flag test is not proof of real phone behavior.

Historical v1.0 results: [verification receipt](../evidence/current/verification.md) and [acceptance gate](../docs/DEVICE_TEST_GATE.md). Current v1.2 timer acceptance procedure: [V1.2-P2 gate](../docs/TIMER_DEVICE_TEST_GATE.md). Automated evidence and physical outcomes must be recorded separately; the timer rows below specify acceptance expectations, not a claim that phone trials passed.

| ID | Scenario | Expected |
|---|---|---|
| T01 | Add/edit/delete/disable three phone numbers | Persist correct normalized values; other callers unaffected |
| T02 | 08:29 / 08:30 / 17:29 / 17:30 | Inclusive start, exclusive end |
| T03 | Weekday and weekend rollover | Weekly recurrence, selected days only |
| T04 | 22:00–06:00 crossing midnight | Correct overnight ownership and end boundary |
| T05 | Split shifts and different day/time windows | Each configured window respected, gaps filtered |
| T06 | Italian, English, third locale | Italian strings; English fallback; no stray text |
| T07 | Schedule activation/deactivation, restart and reboot | Persist weekly state; schedule OFF permits calls unless a timer is active |
| T08 | Missing/revoked screening role; denied Contacts permission | Truthful state, no crash, fail open |
| T09 | E.164, national numbers, invalid/duplicate/private IDs | Strict exact matching, safe normalization |
| T10 | Outside window from listed number on physical phone | No ring or ordinary missed-call notification where supported; document log/caller |
| T11 | Inside window or unlisted phone caller | Normal ringing, no app interference |
| T12 | Listed number in system Contacts | Permission behavior and actual filtering confirmed |
| T13 | Forwarded PBX call from two origins | Presented caller ID documented, no assumed forwarding identity |
| T14 | Light/dark, large fonts, accessibility | Real UI proof, usable focus/contrast |
| T15 | App backgrounded/stopped, device reboot | Service evaluates correctly when invoked |
| T16 | Clean install, update, uninstall | Reliable setup/reversal and no hidden residue |
| T17 | Official signed APK vs Chromecast signed APK | Signer SHA-256 exactly matches; application IDs distinct; checksum recorded |
| T18 | Add/edit/delete/disable numeric ranges (`02 12345***`) | Persist the canonical mask; only exact-prefix and exact-length callers match; wide ranges require confirmation |

## V1.2 temporary timer extension

| ID | Scenario | Expected | Verification type |
|---|---|---|---|
| T19 | Schedule ON/OFF × timer ON/OFF × inside/outside hours | Enabled matching caller follows `timer OR (schedule AND outside)`; both OFF allows all | Automated decision tests; physical counterpart pending |
| T20 | Start timer inside allowed hours, then expire or stop early | Timer blocks during the allowed window; exact deadline/early stop resumes unchanged weekly logic immediately | Automated boundaries and cancellation; physical calls pending |
| T21 | Timer with empty unconfirmed or invalid windows; edit schedule while running | Timer does not require valid/confirmed hours; window edits do not change its deadline; invalid weekly schedule fails open after expiry | Automated validation and state tests; physical UI pending |
| T22 | Zero, negative, malformed, overflow and extreme duration; 1 second and 99:59:59 | Reject invalid/out-of-range input; accepted bounds expire exclusively at their deadlines | Automated timer/input tests; keypad rendering pending |
| T23 | Enabled/disabled exact number and mask, wrong length, unlisted/hidden/ambiguous caller | Only enabled full unambiguous matching targets can block in either mode | Automated decision regressions; physical counterchecks pending |
| T24 | Schema-1 exact rules, schema-2 masks and schema-3 timer round trip | Legacy rules/windows/enabled flags retained, timer initially off; strict typed parsing; current timer restored safely | Automated codec/migration tests; install/update behavior pending |
| T25 | Missing role/Contacts, corrupt general config or number rule, corrupt timer, failed save | No unintended block; damaged general/number config fails open; failed save pauses screening and exposes retry/recovery | Automated safe-state tests; Android storage/permission behavior pending |
| T26 | UI closed, process recreation, screen off across expiry | Persisted deadline controls screening independently of visual countdown; sleep counts toward elapsed duration | Automated clock model; physical lifecycle/call trials pending |
| T27 | Same-boot wall/timezone change; reboot before/after deadline; uncertain boot or backward clock | Elapsed duration unchanged within each observed boot; first reboot read atomically anchors bounded wall fallback, process recreation/stale saves retain it; observed expiry cannot revive; uncertain timer fails open | Automated clock/reboot cases; physical behavior pending |
| T28 | Home buttons, keypad, 1/5/10/15 presets, Start/Stop/Back, expiry and active-state return | Timer directly below schedule, green when off/red when on; real controls; distinct truthful countdown and schedule state | Automated presentation/input tests where available; physical rendering pending |
| T29 | Saving, failure/retry, rotation, narrow/large-font light/dark and TalkBack | Stable committed content while busy; no concurrent edits; localized labels, usable targets/focus and honest paused status | Retained automated status/layout regressions; physical UI pending |
| T30 | Debug APK archive, signer, ID/version, checksum and manifest | Real installable Debug package `.debug`, `1.2.0-debug`/code 8; development signature; no Internet permission; release retained | APK/tool inspection; owner installation pending |

The next gate is V1.2-P2. No automated or emulator result proves quiet blocking, notification suppression, reboot dispatch, carrier behavior or OEM compatibility. Record model/OEM, Android version, timezone and each anonymized physical outcome in the gate receipt.

Never attach actual personal call numbers or private signing keys as evidence.

## Contact-picking candidate extension (1.0.0 / code 3)

Automated adapter, cancellation, error and shared duplicate/number-validation cases passed; native multi-number choice, no-save-until-confirmation, personal-profile update and rendered/lifecycle checks required the documented phone retest. Its receipt is archived locally under ignored `development/archive/` and is superseded by the code-6 run.

## Issue #2 extension (1.0.0 / code 4)

The owner reported that the code-3 picker tests passed except duplicated phone rows in the native selector (issue #2); device/OEM details were not supplied. The code-4 receipt covering scoped aggregate queries, unique-number/label grouping, ambiguous alternatives, cancelled drafts, explicit Save and the retained filter/status regression suites is archived locally under ignored `development/archive/` and is superseded by the code-6 run.

## Stable-save presentation extension (1.0.0 / code 5)

The owner reported code 4 functioning correctly, with a remaining visual flicker during saves/toggles. The code-5 receipt distinguishing automated natural-size/placement tests from unverified native rendering is archived locally under ignored `development/archive/` and is superseded by the code-6 run.

## Dynamic status-card layout extension (1.0.0 / code 6)

The owner reports that code 5 left a large empty block between the status description and the red **DEACTIVATE** button, and that the correction must not bring back the earlier save/toggle flicker; handset details were not supplied. [Code-6 evidence and physical checklist](../evidence/runs/dynamic-layout-1.0.0-vc6/verification.md) separate the automated results — status model, committed-copy retention across in-flight saves, failures and retries, and the retained master-button label policy — from unverified native rendering. The natural height of the visible status text and the absence of perceived flicker remain owner checks on the real phone: repeat time/day edits, number additions/deletions, master toggles, failure/retry where safely available, and narrow/rotated, large-font, light/dark and TalkBack passes. All earlier test receipts remain unchanged.
