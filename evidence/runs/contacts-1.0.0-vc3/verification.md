# Native contact picking — first public-release candidate

Date: 2026-10-30 (Europe/Rome). Source baseline: `3cc8daea4735f8b6341cdfb8a1bccaf3f3475092` on `main`. Candidate **versionName 1.0.0, versionCode 3**, package **it.sorguido.unconnected**. This is a locally prepared candidate, not an approved or published GitHub Release.

## Implementation and scope

**OBSERVED:** New-number entry has an optional “Choose from contacts” / “Scegli dalla rubrica” button. It launches Android `Intent.ACTION_PICK` with `ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE`, selecting one specific phone-number data row. The app does not choose a contact's first number, build an internal address book, synchronize or bulk-import contacts.

The selected row alone is queried on an IO dispatcher, with a projection of `Phone.NUMBER` and `Phone.DISPLAY_NAME`. The returned URI must identify a standard Contacts phone/data row. No parent collection or all-number fallback is queried. Empty, ambiguous, missing or inaccessible results leave the existing draft untouched and show a localized recoverable message; cancellation is a no-op. The cursor closes on success and failure. Missing display name keeps the existing label. Numbers over the existing 80-character input bound are rejected rather than truncated into a different number.

A successful selection fills only the editable number and optional label. Region remains explicit and unchanged. Invalid and duplicate values receive the existing validation errors, can be edited, and cannot pass Save. Both manual and selected drafts use `validateNumberEntry`, which reuses the existing `Numbers.normalize` and duplicate checks, including disabled entries and the existing edit exclusion. Selection never writes settings; only the existing Save callback persists them.

During a returned-row read, fields and Save are disabled to avoid a late result overwriting newer edits; Cancel stays available. Editor visibility, edit identity, field drafts and pending selected URI survive saved-state restoration. Dismissed compositions cancel their read continuation. Android/OEM lifecycle behavior still requires execution on a phone.

**OBSERVED:** No new permissions/dependencies, services, contacts database, diagnostic logs or network access. The existing READ_CONTACTS permission remains for screening eligibility; the picker requests no permission. Android's documented picker response supplies a temporary grant for the chosen data, independently of broad address-book access. Provider refusal is handled. See [Android's official specific-contact-data picker documentation](https://developer.android.com/guide/components/intents-common#Contacts).

**PROVEN by baseline comparison:** Screening service, scheduling/normalization algorithms, SettingsStore, FilterStatus, dependency lock, manifest and four protected canonical files are unchanged. The issue #1 observable pause fix is retained and its regression tests pass. The English/Italian switchboard warning is unchanged, including “numero del centralino”. New UI text uses the existing localized resources and Material 3 controls. The native picker itself uses the system contacts application's own language and appearance.

## Verification

- **PROVEN:** 44 JVM tests pass without failures/errors/skips: all 35 existing tests and nine new contact-picking tests. New cases exercise the actual selected-row adapter with Cursor-interface fixtures, query projection/single-row identity, cancellation, missing/empty/multiple data, denied permission/provider errors, closure, missing names, invalid numbers and normalized duplicates/manual edits.
- **LIMIT:** JVM fixtures do not launch Android's picker, grant URI permissions, query a real provider, render the UI, or prove Activity/process recreation. The real code calls the platform resolver; these runtime behaviors remain in the physical plan below.
- **PROVEN:** Debug/release builds and debug/release lint complete successfully; each lint variant has zero errors and the same eight update advisories. There are 82 matching English/Italian string keys.
- **OBSERVED:** [Independent review](review.md) found no unresolved serious source issue.
- **PROVEN:** Candidate APK verifies with `apksigner`; its certificate SHA-256 equals the published Chromecast reference: `2936676a220e4a9682171fbf0c9b91552acd8fc4f120318c6a0aebe6e6722ecc`. `aapt2` confirms package, versionName 1.0.0, versionCode 3, min API 29, target API 36, and unchanged permissions. Version code 3 exceeds the prior candidate codes 1 and 2, even though the public version name returns from 1.0.1 to 1.0.0 as requested. No release key was generated or replaced.
- Candidate path: `development/deliverables/contacts-1.0.0-vc3/unconnected-1.0.0-vc3-candidate.apk`.
- Candidate SHA-256: `2b3621bb0c871111f5708257755164f0174590cc3402973c7373169e1c1cae6a`.
- Receipts: [tests](automated.json), [build](build.log), [signature/package/version](signature.json), [checksums](SHA256SUMS), [compiled source input hashes](source-inputs.sha256). The enclosing Git commit identifies these inputs; prior candidate artifacts and historical evidence are preserved.

Commands completed:

```bash
UNCONNECTED_SIGNING_ROOT=../chromecast-killswitch-app ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug :app:lintRelease --console=plain
# tools/verify_apks.py additionally received:
# --expected-version-name 1.0.0 --expected-version-code 3
```

**INFERRED:** Technical preparation is complete and this candidate is ready for personal-profile acceptance, not yet proven on a phone. **EVIDENCE_FOR:** Real native intent/resolver integration, bounded selected-row processing, preserved storage boundary, passing tests/build/lint and verified signing. **EVIDENCE_AGAINST:** No native-picker/OEM execution in this run. **ALTERNATIVE_EXPLANATIONS:** Vendor-specific/query-qualified or cross-profile URIs may not match the standard selected-row forms; these deliberately receive a recoverable error with manual entry available. **CONFIDENCE:** High for scoped adapter/validation behavior; device compatibility unverified. **MINIMAL_DISCRIMINATING_EXPERIMENT:** Select a specific work/home/mobile number from one multi-number test contact on the target phone; verify that only it prefills, Cancel changes nothing, and Save alone creates the rule.

## HUMAN_REQUIRED — CONTACT_PICKER_PHYSICAL_ACCEPTANCE

**PURPOSE:** Validate the real picker and candidate update before the first official release. No device installation, profile change, contacts modification, permission grant or real call was performed by the agent.

**EXPECTED_RESULT:** Personal-profile update keeps existing rules; native selection chooses exactly one number, shows editable fields, never auto-saves, and errors/cancellation are safe. The issue #1 status remains correct after Save. English/Italian, light/dark and accessibility remain usable.

**COMMANDS:** Only after verifying and authorizing user 0 as the personal profile:

```bash
adb -s DEVICE_SERIAL shell pm list users
adb -s DEVICE_SERIAL install --user 0 -r development/deliverables/contacts-1.0.0-vc3/unconnected-1.0.0-vc3-candidate.apk
```

Alternatively install through the personal-profile package installer. Never install/update the managed profile automatically or omit `--user` on a multi-profile phone. The current task authorizes APK preparation; the owner performs or explicitly authorizes device operations.

1. Confirm the update preserves rules and the app version is 1.0.0 (code 3).
2. In Add number, type a draft, open Choose from contacts, then cancel; the draft/list must be unchanged.
3. Select a contact with mobile/work/home numbers and choose one specific number. Verify number/name prefill, editable fields, and no new rule before Save. Change a field, then Save.
4. Pick an already-listed number in a different format; duplicate rejection must remain. Try an invalid selected value and correct it manually. A contact without a name must not erase a typed label.
5. Where available in a consenting test setup, check an empty picker or denied permission/provider error; no crash or rule changes, manual entry still works. Do not alter a work profile or deliberately damage contacts/providers.
6. Rotate/recreate while the picker is open and return; check draft/result handling. Check large font, TalkBack, light/dark and primary Italian versus English/other-language fallback for app controls.
7. With the filter active, perform repeated saves while staying in the app; the heading must settle correctly without refocus. Any optional real-call smoke test remains subject to the existing telephone test gate.

**RISK:** Saving a new enabled rule can affect that selected caller according to the existing master/schedule. Installation into the wrong profile can affect a managed app. Merely selecting or cancelling creates no rule.

**ROLLBACK:** Cancel before Save, or remove the newly saved test entry/deactivate filtering. Keep previous APK receipts. Do not uninstall/clear existing data or force a lower-code downgrade to roll back; prepare a signed higher-code revert if necessary.

**EVIDENCE_TO_RETURN:** Device model, Android version/build, personal-profile confirmation, installed version/code and pass/fail for the steps above. Return anonymized results, not actual numbers, contacts, URI identifiers, call logs or private screenshots. Official publication remains pending explicit final approval; issue #1 is still open pending its physical confirmation.
