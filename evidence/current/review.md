# Independent implementation review

Date: 2026-10-30. Reviewer: separate review agent, independent of the implementation agents. Scope: working-tree Android source, manifest, Gradle signing configuration, localization resources, domain tests and generated validation reports. The protected canonical files were not changed.

## Outcome

**OBSERVED:** No unresolved serious source-code issue was found after the corrections below. This is a source review, not physical-phone acceptance or authorization to publish. The final rebuild, APK identity and test receipt belong in `verification.md`; earlier reports inspected during review must not substitute for that final run.

**INFERRED:** The implementation is suitable for the next gated physical-device validation. It is not yet established that the target phone suppresses ringing or notifications, exposes forwarded caller identity as expected, or invokes the service reliably after reboot.

## Findings corrected during review

| Finding | Evidence and correction checked in final source |
|---|---|
| Incoming international numbers could be repaired by libphonenumber and match an ambiguous ID | `Rules.shouldBlock` now compares the presented digits, after removing formatting only, with normalized E.164. A redundant national trunk prefix cannot become a match. National/non-`+` incoming IDs fail open. |
| JSON coercion could turn fractional/string fields into active rules | `SettingsCodec` now requires integer and boolean types, bounds document/collection sizes, rejects duplicate weekdays and validates the full domain snapshot. Malformed data never becomes an empty blocking schedule. |
| API 33 stream method conflicted with minimum API 29 | `SettingsStore` uses a bounded byte-buffer loop instead of `readNBytes`. |
| Initial settings read could block the main-thread screening deadline | `Home` loads on `Dispatchers.IO` with a separate loading state. Screening reads occur on its worker; a main-handler fail-open response is scheduled after three seconds. |
| Deactivation could race a pending screening result before an IO save began | UI calls `SettingsStore.beginUpdate()` synchronously before dispatching the write. The service checks the paused flag and revision before responding. A save failure leaves this process paused and the UI explains that previous durable settings can return after restart. |
| Foreground schedule summary became stale | A foreground-only minute update refreshes the current/next interval; it stops when the activity pauses and is not used by screening decisions. |
| Third-locale fallback did not cover platform time pickers | Activity context uses Italian only when the primary system locale is Italian and English otherwise. System 12/24-hour preference is read from the original application context. |
| UI omitted interval ends and had ambiguous accessibility resource names | Current/next interval includes `Rules.allowedUntil`, including overlapping/overnight windows. App state resources no longer replace Compose's private `state_on`/`state_off`. |
| Light-mode system bar contrast needed explicit handling | `enableEdgeToEdge()` is used before content; actual appearance still requires visual testing. |

For the material incoming-ID, persistence and deadline findings: **EVIDENCE_FOR** was the original call/parse/dispatch paths described above; **EVIDENCE_AGAINST** is the corrected source plus relevant regression tests; **ALTERNATIVE_EXPLANATIONS** include Android framework scheduling, caller-ID presentation and OEM behavior that source review cannot establish; **CONFIDENCE** is high for the static fixes, limited for actual telephony; **MINIMAL_DISCRIMINATING_EXPERIMENT** is the targeted real-phone matrix with a listed caller, an unlisted caller, missing permissions, deactivation during an incoming call, restart and reboot.

## Safety and delivery checks

- **OBSERVED:** First installation is disabled. Missing permissions, invalid configuration, hidden/absent/ambiguous IDs, unlisted numbers and disabled entries allow calls. Empty-schedule blocking requires explicit saved consent.
- **OBSERVED:** Matching uses full normalized numbers; the domain covers inclusive starts, exclusive ends, split/overnight schedules, local timezone and DST transitions. There is no alarm, persistent worker, network access, contact query, call recording or uploaded log.
- **OBSERVED:** The screening response requests disallow without reject and requests notification suppression only for blocked calls. The implementation does not claim to remove Android call-log records.
- **OBSERVED:** Settings use an atomic, bounded local file in `noBackupFilesDir`; backup and transfer exclusions are explicit. Failure messages distinguish session state from durable state.
- **OBSERVED:** Release signing loads only the configured existing external Chromecast properties/keystore. The release build fails without that configuration unless explicitly producing an unsigned artifact; debug has a separate application ID. This review does not itself establish signer fingerprint equality.
- **OBSERVED:** English/Italian resources, editable number and schedule flows, role/permission actions, reset/delete confirmations and disable/uninstall guidance are present. No real device was altered by the reviewer.

## Remaining acceptance limits

Real call behavior, physical notification suppression, saved-contact eligibility, forwarding identity, OEM/version compatibility, background/reboot behavior and physical install/update/uninstall require the human-gated test plan. Visual light/dark, large-font, TalkBack, runtime locale and time-picker behavior also need executed UI evidence. A successful APK build or a source-level flag test does not prove these outcomes. The three-second response fallback is a design budget; a universal five-second guarantee under Android process stalls cannot be proved by this review.
