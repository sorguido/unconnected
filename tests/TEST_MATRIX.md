# Acceptance tests

Record actual test status and evidence; an API flag test is not proof of real phone behavior.

Current measured results for every row: [verification receipt](../evidence/current/verification.md). Physical-device procedure: [acceptance gate](../docs/DEVICE_TEST_GATE.md).

| ID | Scenario | Expected |
|---|---|---|
| T01 | Add/edit/delete/disable three phone numbers | Persist correct normalized values; other callers unaffected |
| T02 | 08:29 / 08:30 / 17:29 / 17:30 | Inclusive start, exclusive end |
| T03 | Weekday and weekend rollover | Weekly recurrence, selected days only |
| T04 | 22:00–06:00 crossing midnight | Correct overnight ownership and end boundary |
| T05 | Split shifts and different day/time windows | Each configured window respected, gaps filtered |
| T06 | Italian, English, third locale | Italian strings; English fallback; no stray text |
| T07 | ACTIVATE/DEACTIVATE, restart and reboot | Persist state; switch OFF allows all |
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

Never attach actual personal call numbers or private signing keys as evidence.

## Contact-picking candidate extension (1.0.0 / code 3)

Current picker checks and limits: [verification](../evidence/runs/contacts-1.0.0-vc3/verification.md). Automated adapter, cancellation, error and shared duplicate/number-validation cases pass; native multi-number choice, no-save-until-confirmation, personal-profile update and rendered/lifecycle checks still require the documented phone retest. Earlier candidate results remain in their historical receipts.

## Issue #2 extension (1.0.0 / code 4)

The owner reports that the code-3 picker tests passed except duplicated phone rows in the native selector (issue #2); device/OEM details were not supplied. This does not establish code-4 physical acceptance. See [new verification and owner retest](../evidence/runs/issue-2-1.0.0-vc4/verification.md) for scoped aggregate queries, unique-number/label grouping, ambiguous alternatives, cancelled drafts, explicit Save, and the retained filter/status regression suites. Historical receipts above remain unchanged.

## Stable-save presentation extension (1.0.0 / code 5)

The owner reports code 4 functions correctly, with a remaining visual flicker during saves/toggles. [Code-5 evidence and physical checklist](../evidence/runs/stable-ui-1.0.0-vc5/verification.md) distinguish automated natural-size/placement tests from unverified native rendering. Repeat time/day edits, number additions/deletions, master toggles, failure/retry where safely available, and scrolled/light/dark/large-font/TalkBack checks. All earlier test receipts remain unchanged.
