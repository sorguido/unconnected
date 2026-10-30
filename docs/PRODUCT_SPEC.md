# Product specification — unconnected v1

**This is the sole owner of product behavior and acceptance requirements.** The goal is a beautiful, simple, real Android application for anyone who wants to accept calls from selected numbers at chosen weekly times and quietly block those callers outside the permitted hours.

## Acceptance requirements

**R1 — Multiple numbers.** Add, edit, remove and individually enable/disable multiple numbers; optional labels. Support international E.164 normalization and regional numbers with clearly selected/default region; reject invalid or duplicate entries. Match normalized full numbers only (not guessed suffixes). No hard-coded business number.

New-number entry offers native Android `ACTION_PICK` with `ContactsContract.Contacts.CONTENT_TYPE` to choose an aggregated contact. Using the existing READ_CONTACTS permission, read only that contact's name and phone rows through a bounded `Phone.CONTACT_ID = ?` query. Group only proven full-number E.164 equivalents using the existing normalization; retain phone-type/custom labels. One unique, unambiguous number prefills the editor; several distinct numbers require an explicit Material 3 choice. Ambiguous/unusable alternatives are reported, never merged by suffix or silently discarded in an automatic choice. At most 100 phone rows are processed; larger results receive a manual-entry fallback. Enterprise, cross-user and directory selections are not queried.

Selection prefills editable name/number fields and requires explicit Save, using the same normalization/duplicate validation as manual entry. Cancel, no-number results, insufficient permission and unavailable providers leave drafts/rules unchanged; manual entry remains available. No internal address book, synchronization, bulk import, new permission or dependency is introduced. The selected country remains explicit and unchanged.

**R2 — Flexible recurring schedule.** Select weekdays and custom start/end times, with multiple time windows for variable/split shifts, including windows crossing midnight. First-use example may be Monday–Friday 08:30–17:30; everything is editable. Start inclusive, end exclusive. Evaluate in current device local timezone at call arrival, respecting DST, with no alarms or persistent background daemon.

**R3 — Master button.** Default filtering OFF at installation. Large green **ACTIVATE** enables schedule enforcement and becomes a large red **DEACTIVATE**. Deactivation bypasses all app filtering immediately, without erasing settings. Show a truthful enabled/role/permission status.

**R4 — Filtering semantics.** With filtering enabled, listed active numbers ring normally in at least one allowed window; outside all windows, prefer silent disallow with no ringtone or regular missed-call notification where Android supports it. Explicit reject only as tested fallback. Never block unlisted, disabled, hidden, absent, malformed or ambiguous callers. Do not replace the dialer or enable global Do Not Disturb. Android may retain a blocked-call log entry.

**R5 — Onboarding and permissions.** Explain/obtain Android call-screening role; request Contacts permission for screening saved-contact calls when needed; handle refusal and revoked roles without misleading claims. Explain that a forwarded office call may display the ORIGINAL caller instead of the forwarding office number.

**R6 — Finished polished UI.** Real Material 3 interface with coherent visual hierarchy, typography, spacing, useful status, attractive cards, system light/dark modes, sufficient contrast and touch targets, scalable fonts, accessibility and clear empty states. Main screen: master state/button, schedule summary, number list, editing entry points. No inert demo screens. Saving must not insert transient rows or resize the status card/master button. Keep the scroll state, show accessible non-displacing progress, prevent concurrent edits, and retain truthful saving/paused/error states. Text must adapt to the selected language and font size without truncation. Actual additions/removals may naturally change the edited list; avoid an extra save-induced jump.

**R7 — Internationalization.** Default resources in English, Italian in `values-it/`. Locale follows Android system automatically; every other locale falls back to English. All visible text and errors localized. Respect system 12h/24h time preference.

**R8 — Reliability and state.** All rules remain local and persist across app restarts/reboot. Evaluate at call arrival, not in a persistent worker. `CallScreeningService` must always answer within Android's five-second deadline. Deterministic thread-safe decision and conservative fail-open handling.

**R9 — Privacy.** Offline, no Internet permission without essential justification, no cloud, account, analytics, ads, SMS, recording, logs uploaded or unnecessary contact access. No personal data or secrets committed. Explain stock Android log limitations.

**R10 — Working deliverable.** Reproducible Gradle build, useful automated schedule/number/decision tests, an actual installable APK, real-device call trials, clean installation/uninstallation instructions, and public English docs. The official APK must be signed using the EXISTING signing key from `chromecast-killswitch-app`, never a newly generated one; use unique `applicationId = "it.sorguido.unconnected"`. Verify signer certificate identity and publish checksum/build evidence. No mockups or placeholder functionality count as release.

## UX choices

One shared weekly schedule for all enabled numbers in v1; no per-number calendar. A schedule may include multiple named windows. With no configured numbers, no calls are blocked. An empty schedule must clearly warn and request confirmation, since it would block selected numbers all week. Show the next allowed interval with local calculation only.

## v1 non-goals

Per-number calendars, automatic PBX forwarding changes, identifying forwarding origin when Android exposes only the caller, filtering anonymous numbers, phone system replacement, root hacks, SMS auto-replies, telemetry, cloud sync, call history analytics, and complex rule engines.

## Android constraints

Support Android 10/API 29+ subject to P0 confirmation. Screening role is required; saved contacts may require `READ_CONTACTS`. Do not claim full removal of notification traces or system call history on every OEM. End-to-end behavior and call forwarding must be physically tested.

## Definition of done

An uninvolved person can install the APK, grant required role/permission, add multiple numbers, set any days/hours, activate and deactivate, observe correct REAL caller behavior, restart/reboot without loss, and use Italian or English automatically; limits are explained and a signed release exists.
