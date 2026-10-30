# Independent review — contact picking, candidate 1.0.0 / code 3

Date: 2026-10-30. Independent reviewer inspected the working-tree implementation, existing governance and product requirements, new tests and completed Gradle output. This review covers the requested technical candidate, not approval for an official public release.

## Outcome

**OBSERVED:** No unresolved serious source issue found. Contact picking is integrated into the new-number editor while manual entry remains available. Version metadata is 1.0.0 / code 3, with the existing application ID and unchanged signing configuration. The higher version code allows an ordinary update from known candidates with codes 1 and 2 despite the requested reset of the display version.

The four protected canonical files, screening service, schedule/number algorithms, manifest, dependency lock, and issue #1's observable pause publisher/status resolver are unchanged. The corrected switchboard warning is unchanged in both English and Italian resources. No new dependency or Android permission was added.

## Implementation and privacy checks

- **OBSERVED:** The launch intent uses `ACTION_PICK` and `ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE`. Android owns the contact/multiple-number choice; the app requests one specific phone data row instead of choosing a contact's first number.
- **OBSERVED:** A returned URI is checked before querying. Only standard single-row Contacts Provider data/phone URI forms are accepted, and only NUMBER and DISPLAY_NAME are requested. No contact-directory enumeration, fallback number search, permanent URI grant, synchronization or bulk import exists.
- **OBSERVED:** Cursor null/empty/multiple-row results, missing/blank/oversized numbers, provider exceptions and denied access return a recoverable failure. The cursor is closed. Canceled results do not query or alter the draft; coroutine cancellation is not swallowed.
- **OBSERVED:** A valid selection only prefills the editable label/number draft. Missing names retain a manual label; names respect the existing length limit. Invalid or duplicate phone values receive the same validation as manual input and cannot pass Save. Region remains explicit and editable. Picking never calls the settings store or saves automatically.
- **OBSERVED:** The selected-row query runs on IO. Fields and Save are disabled while that read is pending; Cancel remains available. Dismissing the editor cancels its effect, so completed canceled IO cannot replace a later draft. The editor mode/ID, draft and pending result URI use saved UI state to support recreation while using the native picker.
- **OBSERVED:** Material buttons/text fields and localized recovery text preserve the existing theme and layout. The setup description now accurately states that only the explicitly selected number/name are read.

The URI allowlist intentionally rejects unsupported vendor-specific, collection, query-qualified and cross-profile forms. Such results use the manual-entry fallback rather than broadening contact access. This conservative behavior is a compatibility caveat to test on the target OEM, not evidence that every contacts implementation is supported.

## Executed verification and limits

**PROVEN at JVM scope:** All 44 tests passed with zero failures, errors or skips, including nine new contact-picker tests and all four issue #1 regressions. The new tests execute the production reader against controlled Cursor-interface fixtures, check the exact URI and projection, cursor closure, cancellation, provider/security errors, absent names, malformed/multiple results, draft-only updates, normalized duplicates and invalid input.

**OBSERVED:** The full Gradle run completed debug/release APK builds and debug/release lint. Both lint reports have zero errors and the existing eight dependency/SDK-update advisories. No lint suppression or test default-return override was introduced. Signature, package, version and checksum measurements belong to the parent's delivery receipts.

These tests do not launch Android's picker, use a real contacts provider, render Compose, grant URI permissions or execute Activity/process recreation. The unavailable-launch handler and lifecycle wiring are source-reviewed, not device-proven. Real multi-number selection, empty address book, temporary grants after permission refusal, OEM URI format, back/cancel behavior and personal-profile update require the documented physical test.

**INFERRED:** The implementation satisfies the requested technical flow while minimizing contact access. **EVIDENCE_FOR:** bounded selected-row reader, draft-only UI callback, shared validation and passing automated checks. **EVIDENCE_AGAINST:** no post-change physical picker/UI run yet. **ALTERNATIVE_EXPLANATIONS:** OEM contacts apps may return unsupported URI forms or handle multiple-number choice/grants differently. **CONFIDENCE:** high for source-level access/control flow, limited for device integration. **MINIMAL_DISCRIMINATING_EXPERIMENT:** update only the authorized personal user, select a contact with two labelled numbers, choose one, confirm only that number/name is prefilled, cancel once without saving, then pick again and Save; verify duplicate rejection and unchanged manual entry/status behavior.

No phone operation or official GitHub Release was performed by the reviewer. Physical acceptance and final release approval remain human-owned gates.
