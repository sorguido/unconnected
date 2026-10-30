# Stable save/toggle presentation — candidate 1.0.0 / code 5

Date: 2026-10-30 (Europe/Rome). Source baseline: `4fdfcad2200474f8b29f1eecd1393ff83806d39d` on `main`. Candidate **versionName 1.0.0, versionCode 5**, package **it.sorguido.unconnected**. Technical preparation is complete; physical visual acceptance and official publication approval remain separate gates.

## Findings before correction

**OBSERVED (owner report):** Code 4 functions correctly, including filtering and persistence, but saves/confirmations and ACTIVATE/DEACTIVATE produce visible refresh-like movement. The owner did not supply handset model, OS build or a recording. This is functional acceptance reported by the owner, not an agent device test or proof of all P2 telephone scenarios.

**PROVEN (baseline source/layout contract):** `busy` conditionally inserted a `LinearProgressIndicator` as an extra child in the scrolling Column with `Arrangement.spacedBy(18.dp)`. This added the indicator's height and another spacing interval during every save. `filterStatus` also replaced the title/detail with SAVING inside a naturally sized card; differing line counts could change its height. `rememberScrollState` was already retained; the source did not recreate the Activity or intentionally reset scroll during persistence. The disabled master button used temporary disabled colors, and its two labels could wrap differently at large font sizes. Clearing/reinserting the inline error message during retry added another conditional row.

**INFERRED:** These geometry/color changes explain the reported visual refresh and provide concrete correction targets. **EVIDENCE_FOR:** Busy-dependent row insertion, dynamic text measurement and disabled-color transition in the exact code-4 source. **EVIDENCE_AGAINST:** No frame trace or real-device geometry was recorded. **ALTERNATIVE_EXPLANATIONS:** OEM rendering, keyboard/dialog transitions, normal edited-list growth/shrinkage, scroll-range clamping or accessibility settings may contribute. **CONFIDENCE:** High for the source-level transient geometry changes; their complete contribution to perceived handset flicker remains unverified. **MINIMAL_DISCRIMINATING_EXPERIMENT:** Compare repeated identical saves/toggles before/after at the same scroll position and font size on the owner's phone.

No configured emulator image/AVD or Android UI-test harness is available locally. No dependency or emulator image was added merely to claim visual verification.

## Correction and scope

- A permanent 4-dp viewport strip precedes the scrolling area. The existing indeterminate progress indicator appears there only while busy, with localized saving semantics and a polite live region. It remains visible when scrolled and does not change the Column's child count, position or height. The strip is outside scrolling text and has no touch handler.
- A small shared Compose `MeasurePolicy` measures all status alternatives at their natural sizes and reserves their maximum bounds, then places only the selected alternative. The status still comes from the unchanged `filterStatus` function: saving, OFF, ACTIVE, setup-required and real session pause remain distinct and truthful. The selected text is a merged polite live region; inactive alternatives have cleared semantics and are not placed.
- The same policy reserves space for both master-button labels, including when only the longer label wraps. There are no fixed text heights, truncation, cached pixel heights, decorative transitions or new animations. Reservation follows actual width, locale and font size; short states may therefore have extra empty space.
- During a save, the master button retains the committed state's color while remaining disabled. After success its existing committed enabled value controls text/color. An OFF button already disabled by missing setup keeps its existing disabled appearance; no inverse green flash is introduced.
- Existing operation-error messages appear in a dismissible, scrollable Material 3 alert, visible even when the main card is offscreen. Clearing/retrying an error no longer inserts/removes a scrolling row. Dismissing the alert does not clear the underlying session pause. The new error title and Close action are translated into English/Italian.
- Existing `rememberScrollState`, persistence ordering, `busy` re-entry guard and edit-control disabling remain intact. Real additions/deletions or changed text can naturally resize the affected list and clamp the available scroll range; the correction removes the additional transient save-induced displacement, not legitimate content changes.

**PROVEN (baseline diff):** ScreeningService, time/number rules, matching, SettingsStore, SettingsCodec, FilterStatus and its observable issue #1 bridge are unchanged. Contact picking, number validation, the issue #2 fix, manifest, permissions, dependency lock and all four protected canonical files are unchanged. The master action and settings mutations are unchanged. All previous app strings, including the switchboard warning, retain their values; two English/Italian error-dialog strings were added. There is no data migration, clearing or key generation.

## Verification

**PROVEN:** All **66 JVM tests pass**, zero failures/errors/skips. The 62 prior tests are retained, including screening, schedule/DST, codec, contact picking, explicit Save and issue #1 regressions. Four new tests invoke the actual production Compose measurement/placement policy with controlled children:

1. Repeated OFF/saving/ACTIVE/failure/pause/retry/toggle sequences retain equal header bounds and place only the current alternative at the origin.
2. Narrower widths and larger supplied text blocks grow naturally, with equal bounds across status selections.
3. Parent width/height constraints are respected, without stretching each text alternative to the parent's minimum height.
4. A two-line DEACTIVATE label and one-line ACTIVATE label reserve the same button height and place only the selected label.

**LIMIT:** Those tests use synthetic child measurements. They do not execute Android text shaping, compose/render the full Home screen, inject real IO errors, measure pixel scrolling, validate TalkBack behavior, or prove the absence of perceived flicker on a phone. Time/day edits, number add/delete and master toggles all enter the reviewed existing save path; their rendered interaction sequence still requires the checklist below.

**PROVEN:** Debug/release builds and lint completed successfully. Each lint variant has zero errors and eight existing SDK/dependency update advisories. Resource keys match: 91 English and 91 Italian. [Independent review](review.md) found no unresolved blocker, with physical limits recorded explicitly.

**PROVEN:** APK signatures verify; the candidate certificate matches the original published Chromecast APK certificate exactly: `2936676a220e4a9682171fbf0c9b91552acd8fc4f120318c6a0aebe6e6722ecc`. APK metadata confirms package `it.sorguido.unconnected`, versionName `1.0.0`, versionCode `5`, min API 29, target API 36 and unchanged permissions. The original external key was reused. Updating code 4 with code 5 is supported by the package/signature/version metadata; preservation on the real phone remains in the owner check.

Candidate: `development/deliverables/stable-ui-1.0.0-vc5/unconnected-1.0.0-vc5-candidate.apk`.

SHA-256: `68d55df3b1f1d1e099a2d2af946b44ac36f1d07d4d8e263588cab8071d3c0ba9`.

Receipts: [automated results](automated.json), [build](build.log), [signing/package/version](signature.json), [checksums](SHA256SUMS), [compiled input hashes](source-inputs.sha256). The enclosing commit identifies the compiled inputs; older candidates and evidence are preserved.

```bash
UNCONNECTED_SIGNING_ROOT=../chromecast-killswitch-app ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug :app:lintRelease --console=plain
# APK verification additionally used --expected-version-name 1.0.0 --expected-version-code 5.
```

## HUMAN_REQUIRED — STABLE_UI_PHYSICAL_ACCEPTANCE

**PURPOSE:** Owner verifies visual stability and update behavior on the real phone. No installation, role/permission modification, contact change, live call or device setting change was performed by the agent. No official GitHub Release/tag was created.

**EXPECTED_RESULT:** Saves and toggles produce no extra vertical jump or refresh-like flicker. Scrolling remains at the same position unless the edited content or scroll bounds genuinely change. Saving is perceivable/accessibly announced; controls reject concurrent operations; final button state, success, real pause and errors remain correct. Existing configuration survives the update.

**COMMANDS:** Verify checksum and personal profile before installing. Use user 0 only if inspection confirms it is the intended personal profile; otherwise stop and resolve the profile identity rather than installing elsewhere automatically.

```bash
sha256sum development/deliverables/stable-ui-1.0.0-vc5/unconnected-1.0.0-vc5-candidate.apk
adb -s DEVICE_SERIAL shell pm list users
# Only after confirming user 0 is personal:
adb -s DEVICE_SERIAL install --user 0 -r development/deliverables/stable-ui-1.0.0-vc5/unconnected-1.0.0-vc5-candidate.apk
```

1. Confirm installed code 5, existing numbers, windows and master setting. Do not uninstall or clear data.
2. At the top of the screen, repeatedly save an unchanged rule, then change its time and weekdays. Confirm the cards do not jump while the save indicator appears/disappears and the final status is correct without refocus.
3. Repeatedly use ACTIVATE/DEACTIVATE. Confirm only the completed state changes its label/color, without the temporary gray flash; tapping again during saving must not start a concurrent operation.
4. Repeat rule edits while scrolled into numbers/schedules. Add and delete a consenting test entry; distinguish the expected list growth/shrinkage from an extra save-induced bounce or scroll reset. Recheck the contact picker and duplicate handling.
5. Check light/dark, primary Italian, English/fallback, large fonts and a narrow/rotated viewport. Confirm full readable status/button text, stable toggle bounds and no progress/text overlap. With TalkBack, verify saving feedback, only the selected status/label, and disabled-control semantics.
6. If a real save error is safely available, confirm the error alert is visible at any scroll position, remains readable, and closing it retains the real paused status. Retry must restore status on success without error-row insertion/removal. Do not fill storage, damage app files or change device permissions merely to manufacture failure. Record untested failure cases honestly.
7. Confirm the existing filter/contact behavior remains unchanged. Any live-call verification follows the separate [telephone test gate](../../../docs/DEVICE_TEST_GATE.md).

**RISK:** Saving/changing enabled rules or the master button affects existing filtering behavior; an incorrect profile target can affect a managed app. No such action is automatic.

**ROLLBACK:** Restore test edits or deactivate filtering if needed. Keep prior APK receipts. Do not force a lower-code downgrade, clear data or uninstall to roll back; prepare an original-key higher-code revert if necessary.

**EVIDENCE_TO_RETURN:** OEM/model, Android version/build, verified personal user, installed version/code, language/theme/font scale and anonymized pass/fail for the steps above. Do not return personal numbers, contact data, call logs or private screenshots. Physical absence of flicker remains unproven until the owner confirms it; publication still requires explicit final approval.
