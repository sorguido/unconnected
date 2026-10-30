# Build and install

## Status and prerequisites

The current first-public-release candidate is **1.0.0 / versionCode 6**. It sizes the status card to the text actually visible and keeps the committed copy on screen while a save runs, removing the empty block the owner saw above **DEACTIVATE** in code 5 without reintroducing save/toggle flicker. The issue #1 status fix and the issue #2 contact picker are retained. See [new candidate evidence and physical retest](../evidence/runs/dynamic-layout-1.0.0-vc6/verification.md). Codes 1–5 and their historical evidence remain available; code 6 updates without uninstalling or clearing rules. No official Release has been approved.

unconnected 1.0.0 is an implemented **release candidate**, pending physical-phone acceptance and release approval. Android 10 (API 29) or newer is required. Automated checks do not establish real call, notification, carrier or OEM behavior.

The pinned toolchain is Gradle 9.6.0, Android Gradle Plugin 9.4.0, Kotlin Compose compiler 2.2.10, Android SDK platform 36 and build-tools 36.0.0. Use a Gradle-compatible JDK (the recorded build uses OpenJDK 25). Set `ANDROID_HOME` to your installed Android SDK or set `sdk.dir` in an ignored `local.properties`. Dependencies download from Google Maven and Maven Central; the installed application has no Internet permission. The Gradle distribution checksum is pinned in the wrapper.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`, package `it.sorguido.unconnected.debug`. This separate development package may coexist with the release candidate; select only one screening app in Android. Debug signing is for development only.

To compile a deliberately unsigned artifact without private material:

```bash
./gradlew -Punsigned :app:assembleRelease
```

The unsigned artifact cannot be installed or distributed as the official release. A normal release build fails closed unless the existing identity is configured. Never generate a replacement release key.

```bash
UNCONNECTED_SIGNING_ROOT=../chromecast-killswitch-app ./gradlew :app:assembleRelease :app:testDebugUnitTest :app:lint
```

See [SIGNING.md](SIGNING.md). The original private PKCS#12 and properties remain in the existing Chromecast checkout; they are read, never copied or changed. Compare the APK signer with the independently published Chromecast APK before distributing. Candidate APKs, build logs and source archives live under ignored `development/`; public verification receipts live in `evidence/runs/dynamic-layout-1.0.0-vc6/`, alongside preserved earlier runs.

## Install on a test phone — human action required

Installation, changing Android roles/permissions, and real calls require the device owner's targeted approval. No such device actions have been performed by the agent. First verify the SHA-256 receipt and signer.

```bash
adb -s DEVICE_SERIAL shell pm list users
# After confirming that 0 is the personal user:
adb -s DEVICE_SERIAL install --user 0 -r development/deliverables/dynamic-layout-1.0.0-vc6/unconnected-1.0.0-vc6-candidate.apk
```

**Important for work-profile phones:** Plain `adb install` may provision the APK in both the personal and managed profiles. Never omit `--user` for test installs on these devices. If the APK was already installed in a work profile, first find the actual work ID using `adb shell pm list users`, verify installation with `adb shell pm list packages --user WORK_USER_ID it.sorguido.unconnected`, and—only after owner approval—remove that profile's installation with `adb uninstall --user WORK_USER_ID it.sorguido.unconnected`. This retains the personal installation. Do not bypass company device restrictions. Never run profile-unspecified `adb uninstall` on a multi-profile phone.

Alternatively, copy that APK to the personal profile and open it with its Android package installer after verifying the active profile. Grant install permission only to the chosen installer if needed. Open **unconnected**. On a fresh install, filtering starts OFF; an update preserves existing settings. Explicitly choose unconnected as the call screening app, then grant Contacts permission from its setup card. Android requires this permission to deliver saved-contact calls to the screening service. Separately, the optional contacts picker uses the same permission to read only the name and phone numbers of the contact you choose; it does not scan or import the address book. This app conservatively allows all calls when either role or Contacts permission is missing.

Add numbers manually, or tap **Choose from contacts** and select an aggregated contact. Equivalent E.164 representations appear once; if several distinct numbers remain, choose one from the Material 3 dialog with mobile/work/home/custom labels. A single unambiguous number directly prefills the editor. Unusable or ambiguous alternatives are reported and require a deliberate choice or manual entry. The chosen number and available name remain editable; selection alone saves nothing. Tap **Save** to confirm. Cancelling either selection stage preserves the prior draft; cancelling the editor preserves existing rules. Invalid or already-listed numbers cannot be saved until corrected. No new permission is added: the existing Contacts permission must be available. Select the two-letter country/region before picking national numbers (default IT); picking does not change it. Numbers are stored as full international E.164 values. The schedule is shared across enabled numbers. Add/edit weekdays and start/end times, including split shifts and overnight windows. Selected days belong to the start of an overnight window. Start is inclusive, end exclusive; equal start/end is invalid. Adjacent windows can provide full-day coverage. Empty schedules require explicit confirmation because they allow no time for listed active numbers. Touch **ACTIVATE** when ready; **DEACTIVATE** immediately bypasses app filtering and keeps your rules.

Times follow the current device timezone and 12/24-hour preference. On the repeated DST hour, each local occurrence follows the same rule; nonexistent local times are skipped. The screen refreshes its schedule summary only while visible; the service evaluates actual call-arrival time without alarms, polling or a background scheduler.

## Limitations and privacy

- Only full, unambiguous international caller IDs match. Incoming national-only, hidden, restricted, absent, malformed or ambiguous IDs are allowed, including international values that would require dropping a trunk prefix. This can let some listed callers through when a carrier supplies an ambiguous ID.
- A forwarded call may show the original caller instead of the office line. The app cannot recover an unavailable forwarding identity. Do not assume a forwarding line rule will catch such calls.
- Outside allowed hours the app requests disallow without explicit rejection and requests missed-call notification suppression. Actual handset/carrier behavior remains unverified. Android may retain blocked-call log entries. No promise of removing every trace is made.
- Rules are local, bounded to 200 numbers and 100 windows, and excluded from cloud backup/device transfer. There is no account, telemetry, call recording, history collection or Internet permission. Labels are limited to 80 characters.
- Corrupt/unreadable configuration fails open. Resetting corrupt configuration requires confirmation. Failed saves pause screening for the current process; previously saved settings may return after process restart. Retry the save or remove the app's screening role in system settings if storage fails.
- Android may suppress callbacks for force-stopped apps. Reopen the app and verify its role/permission status after force-stop. Reboot/background behavior and the three-second response budget require physical-device trials; no universal five-second scheduling guarantee is claimed.

## Update, disable and rollback

For an approved update with the same package/signing identity and a nondecreasing version code, use `adb -s DEVICE_SERIAL install --user 0 -r APPROVED_APK`; rules are kept. Retain the previous verified APK and checksum. Android may reject version downgrades; do not uninstall or clear data merely to force a downgrade without accepting rule loss.

For immediate rollback of behavior, touch **DEACTIVATE**, or remove unconnected's screening role in Android settings. Uninstall through Android settings, or after explicit approval:

```bash
adb -s DEVICE_SERIAL uninstall --user 0 it.sorguido.unconnected
```

Uninstall removes app-owned rules. System-owned call logs are not erased. Restore a previously selected screening app manually if desired. unconnected never changes forwarding, Do Not Disturb, the default dialer or any phone settings on its own.
