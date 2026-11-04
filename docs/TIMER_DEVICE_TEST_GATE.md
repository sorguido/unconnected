# HUMAN_REQUIRED — V1.2-P2 physical acceptance

**GATE:** `V1.2-P2_PHYSICAL_ACCEPTANCE`  
**PURPOSE:** Verify temporary blocking, weekly restoration, caller safety, notifications and the real UI on the owner's physical Android phone before any official v1.2.0 release.  
**EXPECTED_RESULT:** Anonymized T19–T30 outcomes plus the retained T01–T18 regressions, identifying OEM/model, Android version and compatibility caveats. Automated tests establish code behavior only; phone acceptance remains pending until the owner returns results.

The test target is the locally generated offline Debug APK, version `1.2.0-debug` / versionCode 8, package `it.sorguido.unconnected.debug`. Use the exact APK path and SHA-256 from the current implementation receipt. It is development-signed and cannot update the official `it.sorguido.unconnected` package. Both packages can coexist and keep separate private settings. Neither package imports the other's saved numbers or windows; enter only the test rules needed in Debug. The official v1.1.0 APK, data and release remain intact.

**COMMANDS** — owner actions after approving the identified device, APK and intended personal profile:

```bash
# On the computer: verify the delivered checksum before installing.
cd development/deliverables
sha256sum -c unconnected-1.2.0-debug-vc8.apk.sha256

# Identify the device and personal user; use the actual personal ID below.
adb devices -l
adb -s DEVICE_SERIAL shell pm list users
adb -s DEVICE_SERIAL install --user PERSONAL_USER_ID -r unconnected-1.2.0-debug-vc8.apk
```

The command assumes the receipt places APK and checksum directly in `development/deliverables/`; follow its actual path if different. Alternatively, copy the verified APK into the personal profile and open it with Android's package installer. Never omit `--user` for ADB installation on a phone with work profiles. Respect enterprise restrictions and do not clear data or uninstall the release to install this separate Debug package.

Open the Debug app from its installation result or its Android app-information page, checking package/version to distinguish it from the release with the same public app name. On a fresh Debug installation, both schedule and timer must be off. In its Phone setup card, manually choose this Debug variant as Android's screening app and grant Contacts to enable screening during the trial. Refusing either requirement keeps all calls allowed. Only one screening app can hold the role, so this temporarily displaces the official release or another selected app. No role, permission, phone setting, installation or real call is performed by the implementation agent.

Use consenting callers and manually run the following checklist. Do not change forwarding or device policy for the test.

| Trial | Record |
|---|---|
| Independent states / T19 | Both OFF; schedule ON/timer OFF inside and outside hours; schedule OFF/timer ON; both ON inside and outside. Enabled matching caller follows the specified formula in every state. |
| Exact identities and masks / T23 | Exact number and a narrow fixed-length mask; disabled, unlisted, hidden and ambiguous IDs remain allowed. Use a nonmatching mask-length countercheck where a consenting caller is available. No actual numbers in the receipt. |
| Override and expiry / T20 | Start a short timer during allowed hours. Record ring/vibration, incoming UI, regular missed-call notification, blocked log entry and what the caller hears separately. At expiry, normal allowed-hour behavior returns; outside hours, the enabled schedule continues blocking. |
| Early stop and edits / T20–T21 | Stop from the home button and timer screen. Toggle/edit the schedule during an active timer. Timer deadline stays unchanged; its stop restores the newly saved weekly state. Timer starts with no allowed windows without asking to confirm all-week scheduling. |
| Input / T22, T28 | HH:MM:SS entry, delete/clear, 1/5/10/15-minute presets, invalid/zero input, explicit Start, Back, countdown, early Stop and expiry. Do not wait 99 hours; verify the maximum is accepted and then cancel. |
| Lifecycle / T26 | Background/close the UI and lock the screen across a short expiry. Repeat after process recreation where safely reproducible. Reopen: countdown and screening reflect the deadline. Distinguish force-stop restrictions; reopen after force-stop and recheck setup. |
| Reboot/clock / T27 | With owner approval for the disruption, reboot before a sufficiently long deadline and after a short deadline. Confirm remaining duration or expiry. Same-boot wall/timezone changes must not extend elapsed duration. Record original timezone and restore it; clock-change trials are optional if disruptive. |
| Safety / T25 | Manually deny/revoke each required role/permission, verify truthful paused state and allowed calls, then restore through Android. Do not corrupt production storage or force storage failure on the real phone. |
| UI and saves / T28–T29 | Timer button immediately below schedule; green activation/red deactivation for both. Distinct schedule state and timer remaining time. Italian/English/third-locale fallback, large fonts, portrait/landscape, narrow display, light/dark and TalkBack. Repeated saves/toggles cause no extra jump, hidden controls or concurrent edits. |
| Regression / T01–T18, T24 | Number/mask editing, contact picker, split/overnight/weekday boundaries, retained settings after restart. Test upgrade compatibility on a pre-existing development installation only when its signer matches; the separate Debug package does not exercise an official-release update. |

Clock policy: elapsed realtime governs the timer within a boot, including sleep. At the first settings read after reboot, a consistent reading inside the original interval supplies a new elapsed-realtime anchor, saved atomically before use. Clock changes later in that boot must not extend the countdown, including after process recreation. Expiry observed by a settings read/save is saved as an inactive anchor; visual countdown ticks do not write storage. Backward readings before the start, invalid state, unavailable boot identity or a failed anchor save make the timer inactive conservatively. A wall-clock change before that first post-reboot observation cannot be fully distinguished from elapsed time without a trusted external source; record this limit if encountered. No background alarm or permanent service is required for expiry at the next screening decision.

**RISK:** During the trial, enabled test callers may be blocked even within allowed hours. An OEM or carrier may produce unexpected ringing, notifications or caller experience. Changing the screening role displaces the prior app. Android may retain call-log entries. Use a noncritical period and consenting callers; do not use emergency or relied-upon business calls.

**ROLLBACK:** Stop the timer **and** deactivate the schedule, or remove the Debug app's screening role in Android. Manually restore the previously selected screening app, including official unconnected if applicable, and check its existing state. Uninstall Debug only if accepting loss of its separate test settings; preserve the official package and data. With explicit owner approval, removal from the confirmed personal profile is:

```bash
adb -s DEVICE_SERIAL uninstall --user PERSONAL_USER_ID it.sorguido.unconnected.debug
```

Do not run a profile-unspecified uninstall. Restore any deliberately changed clock/timezone and permissions through Android. No forwarding, default dialer or Do Not Disturb change is needed.

**EVIDENCE_TO_RETURN:** OEM/model, Android version/security patch, carrier, timezone, APK version and SHA-256; anonymized pass/fail per trial; exact ring/vibration/UI/notification/caller observations; any reboot, process, permission or clock caveat. Do not return phone numbers, contacts, device serials, raw call logs, private keys or unredacted screenshots. Report failures before considering a screening fallback.

Passing V1.2-P2 does not authorize official signing, a tag, GitHub Release or publication. Those require a new explicit owner decision and reuse of the existing external release signing identity.
