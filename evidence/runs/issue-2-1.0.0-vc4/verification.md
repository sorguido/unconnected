# Issue #2 — distinct numbers after aggregated-contact selection

Date: 2026-10-30 (Europe/Rome). Baseline: `65856c2eeef87e5c44c715ff1243a17acb71bfe0` on `main`. Candidate **1.0.0 / versionCode 4**, package **it.sorguido.unconnected**. This is a local signed candidate; issue #2 remains open pending owner acceptance. No official GitHub Release or tag was published.

## Diagnosis and correction

**OBSERVED (owner report, not an agent device test):** The code-3 native phone-data picker displayed repeated numbers, including identical rows, +39/0039 representations, formatting variants and rows from linked contact records. The owner reports that the other contact-picking checks passed. OEM, Android version/build and profile details were not supplied.

**PROVEN (source):** The previous intent used `Phone.CONTENT_TYPE`, exposing phone-data rows. It has been replaced with `ACTION_PICK` / `Contacts.CONTENT_TYPE` to select an aggregated contact. The selected contact URI alone is queried for `_ID` and optional display name (limit 2, exactly one row accepted). Then a local phone query requests only `CONTACT_ID`, `NUMBER`, `TYPE` and `LABEL`, with mandatory `contact_id = ?` and the resolved ID as its argument. The query requests at most 101 rows; the reader accepts at most 100 and rejects overflow without a partial result. Each returned row must belong to the selected ID. No all-contact query, synchronization, persistence component or new dependency was introduced.

Individual local numeric and lookup URIs are supported. Collection, cross-user, enterprise and directory-qualified URIs are rejected before access; decoded enterprise lookup keys and platform enterprise contact IDs are rejected. A lookup URI may resolve an updated ID; a direct numeric URI must resolve that exact ID. The existing READ_CONTACTS permission is checked and provider security failures are recoverable. No corporate endpoint or profile context is requested. Android's contact identity and enterprise lookup conventions are documented in the [AOSP ContactsContract source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/provider/ContactsContract.java).

**PROVEN (source and tests):** The picker reuses `Numbers.normalize` and libphonenumber. Grouping uses exact canonical E.164 keys, never textual suffix similarity. A picker-only digit-preservation check rejects ambiguous repairs such as an international number containing a redundant trunk prefix. It accepts canonical + forms, equivalent 00 forms after regional validation, and validated national/regional dialing representations. Unsupported or ambiguous representations receive a manual-entry fallback; this is not a claim to recognize every international dialing-prefix variant. Types and custom labels from equivalent rows are combined.

A single valid unique number directly fills the unsaved editor. Distinct alternatives require an explicit Material 3 choice. Mixed valid/unusable rows display a warning and require a choice even if only one safe number remains; unusable alternatives are never silently merged or automatically ignored. An all-unusable result or contact without numbers shows a localized recoverable message. The name is optional, fields remain editable, and the selected country is unchanged. Cancel leaves the preceding draft intact. Only the explicit Save action reaches the existing persistence callback, after the shared invalid/duplicate checks. Existing disabled entries still count as duplicates; editing the same rule remains allowed.

The pending URI and draft survive saved-state restoration; the transient choice list is re-queried on recreation. While reading, editing and Save are disabled; cancellation remains available and cancels the composition continuation. This design requires the physical lifecycle checks below. The chooser uses existing Material 3 controls, scrolling and localized labels, with no automatic first choice.

**PROVEN (baseline diff):** The screening service, scheduling/normalization domain, SettingsStore, FilterStatus, master-button handling, manifest, dependency lock and protected canonical files are unchanged. The issue #1 observable-state fix remains present. English/Italian switchboard warning text is unchanged, including “numero del centralino”. README changes only the remaining “office number” phrase to “switchboard number”. No settings migration or clearing operation was added. Codes 1–3 and their historical evidence remain intact.

## Verification and candidate

- **PROVEN:** All **62 JVM tests pass**, zero failures/errors/skips: the 35 retained filter, schedule, normalization, codec and issue #1 tests, plus 23 selected-contact/query/dedup tests and four editor/Save-boundary tests.
- **PROVEN:** The principal acceptance fixture uses four distinct representations of one Italian example number from libphonenumber metadata, then adds a genuinely distinct alternative. The result contains exactly one canonical entry per number. Tests also cover label union, ambiguity warnings, missing data, overflow, scope violations, permissions, provider errors, cursor closure, cancellation and invalid/duplicate Save rejection.
- **LIMIT:** Cursor fixtures and an in-memory persistence callback exercise production adapter/draft/Save code. They do not launch the native picker, render Compose, query an OEM provider or prove disk persistence, physical update, lifecycle restoration, real calls or accessibility on a handset.
- **PROVEN:** Debug/release build and lint pass. Each lint variant has zero errors and eight existing dependency/toolchain/target update advisories. English and Italian resources have 89 matching keys.
- **OBSERVED:** [Independent review](review.md) records the reviewed scope, corrected same-country 00 edge case and physical-test limits.
- **PROVEN:** `apksigner` verifies the candidate and original published Chromecast reference. Signer SHA-256 matches exactly: `2936676a220e4a9682171fbf0c9b91552acd8fc4f120318c6a0aebe6e6722ecc`. The original external key was reused; no key was created or changed.
- **PROVEN:** APK inspection confirms package `it.sorguido.unconnected`, versionName `1.0.0`, versionCode `4`, minimum API `29`, target API `36` and unchanged permissions. Code 4 is higher than all prior candidates. Device installation/update remains unverified for this artifact.
- Candidate: `development/deliverables/issue-2-1.0.0-vc4/unconnected-1.0.0-vc4-candidate.apk`.
- SHA-256: `8daabbfd78fc82d498150d89938d30200b5b17e3473ad90f48308422713adfe1`.
- Receipts: [tests](automated.json), [build](build.log), [targeted tests](targeted-tests.log), [APK verification](signature.json), [checksums](SHA256SUMS), [compiled source inputs](source-inputs.sha256). The enclosing Git commit identifies the source without a circular self-hash.

```bash
UNCONNECTED_SIGNING_ROOT=../chromecast-killswitch-app ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug :app:lintRelease --console=plain
# tools/verify_apks.py used --expected-version-name 1.0.0 --expected-version-code 4
```

**INFERRED:** The original repeated-row display is explained by choosing phone data rather than aggregated contacts; the new source meets the scoped grouping contract. **EVIDENCE_FOR:** Previous intent, owner report and current required-fixture tests. **EVIDENCE_AGAINST:** No native/OEM execution of this candidate. **ALTERNATIVE_EXPLANATIONS:** Unlinked separate contacts can still appear separately in the native contact list; OEM URI conventions or permission behavior can cause a recoverable manual fallback. **CONFIDENCE:** High for tested grouping and Save boundary; handset behavior unverified. **MINIMAL_DISCRIMINATING_EXPERIMENT:** Open one aggregate containing four equivalent representations plus a distinct number and verify two choices, then Save only the intended one.

## HUMAN_REQUIRED — ISSUE_2_PHYSICAL_ACCEPTANCE

**PURPOSE:** Owner verifies the new candidate on the real phone before issue closure or official publication. The agent performed no installation, contact modification, role/permission change, profile change or call.

**EXPECTED_RESULT:** The personal-profile update preserves rules and enabled state. Equivalent representations collapse without losing distinct valid numbers. Selection is editable and never persists before Save; cancellation/errors are safe. Issue #1 remains fixed without refocusing the app.

**COMMANDS:** Owner checks the checksum, identifies the device and confirms user 0 is the personal profile before installation:

```bash
sha256sum development/deliverables/issue-2-1.0.0-vc4/unconnected-1.0.0-vc4-candidate.apk
adb -s DEVICE_SERIAL shell pm list users
# Only after confirming user 0 is the personal profile:
adb -s DEVICE_SERIAL install --user 0 -r development/deliverables/issue-2-1.0.0-vc4/unconnected-1.0.0-vc4-candidate.apk
```

Never omit `--user 0` or install in a managed profile. A manual package installer must also run in the verified personal profile. The owner performs these operations; no automatic device action is authorized by this receipt.

1. Confirm version 1.0.0/code 4, existing rules, schedule and master state are preserved.
2. Use a consenting personal test contact with four equivalent representations (canonical +39, 0039, formatted international, national with region IT) and an additional genuinely distinct number. Choose it via **Choose from contacts**. Expect exactly two distinct choices, retaining available phone labels; neither is chosen automatically.
3. Choose the desired number. Verify editable name/number, no new rule yet, and persistence only after **Save**. Verify the saved rule after reopening the app. A single unique-number contact should prefill directly, still without saving.
4. Type a manual draft, open and cancel the native picker, then repeat and cancel the number dialog. In both cases the previous draft and existing rules must remain unchanged. Cancel the editor after a successful pick; no new rule may appear.
5. Check an already-listed number in another format, no-number contact and non-normalizable/ambiguous entries. Mixed valid/ambiguous entries must show the warning and require a choice. Where safely available, test denied Contacts access or an unavailable picker/provider; manual entry must remain usable. Do not change work-profile settings or damage a provider to manufacture a failure.
6. Rotate/recreate while each selection stage is open; check restoration/cancellation, no unwanted first choice and no save. Check primary Italian, English/other-language fallback, light/dark, large fonts and TalkBack number/label readability.
7. Save settings repeatedly with filtering active and stay in the app: status must settle correctly without leaving/reopening. Confirm ACTIVATE/DEACTIVATE and existing rule behavior remain unchanged. Any real-call trial still follows the existing [telephone gate](../../../docs/DEVICE_TEST_GATE.md).

**RISK:** Saving an enabled test number can affect calls according to the existing master/schedule. Installing in the wrong profile can affect a managed app. Mere selection/cancellation must create no rule.

**ROLLBACK:** Cancel before saving, remove only the newly created test rule or deactivate filtering. Retain old APK receipts. Do not uninstall/clear data or force a downgrade; use a higher-code signed revert if necessary.

**EVIDENCE_TO_RETURN:** OEM/model, Android version/build, personal-user confirmation, installed version/code and anonymized pass/fail for each step. Do not return real numbers, contact names, URI IDs, call logs or private screenshots. Issue #2 remains open until this verification; official Release publication requires separate explicit approval.
