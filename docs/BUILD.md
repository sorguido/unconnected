# Build and install

## v1.2.0 builds

The official release targets `1.2.0` / versionCode 8, package `it.sorguido.unconnected`. Debug builds use `1.2.0-debug` and `it.sorguido.unconnected.debug`. The v1.1.0 release remains unchanged. The timer blocks enabled numbers and masks independently of weekly scheduling, including during allowed hours. Expiry or early Stop restores the current weekly logic. Stop both controls, or remove the screening role, to allow all calls.

Build without accessing official signing material:

```bash
./gradlew --offline -Punsigned :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

`--offline` assumes the pinned Gradle distribution and dependencies are already cached; omit it for an initial dependency download. The installed app itself requires no Internet. The verified test APK and checksum are under `development/deliverables/unconnected-1.2.0-debug-vc8.apk` and `.apk.sha256`; see the [timer verification receipt](../evidence/runs/timer-v1.2.0/verification.md) for measured results. Follow [V1.2-P2 physical acceptance](TIMER_DEVICE_TEST_GATE.md) to install in the personal profile, select Debug as the screening app, test and restore the prior app. Debug and release coexist with separate settings. Those V1.2-P1 receipts cover Debug only; official release verification is indexed in `PROJECT_STATE.json`.

## Status and prerequisites

Release **1.2.0 / versionCode 8**, tagged `v1.2.0`, adds the independent temporary blocking timer. Fixed-length numeric ranges with trailing `*`, exact prefix/length matching, and confirmation for ranges covering more than 1,000 numbers remain supported. It updates v1.1.0/code 7 in place with the same package and signing identity, without uninstalling or clearing rules. Existing numbers, ranges and weekly settings remain compatible; an update does not start a timer. Historical [code-6 verification](../evidence/runs/dynamic-layout-1.0.0-vc6/verification.md) and the [v1.0.0 publication report](../evidence/runs/release-v1.0.0/publication.md) remain available. Installation instructions below refer to the official release; use the timer gate above for a separate Debug trial.

The owner reports that all numeric-range phone tests and counterchecks passed on 2026-11-03 and authorized publication. This is owner testimony, without OEM/model, Android-version or per-call measurements supplied to the agent. The owner also reports positive initial v1.2.0 phone trials, with no anomalies observed, and explicitly authorized this release. More thorough physical verification at work remains pending; no OEM/model or Android-version receipt was supplied. Android 10 (API 29) or newer is required. Automated checks do not establish real call, notification, carrier or OEM behavior.

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
# With a private copy under unconnected/development/ (ignored by Git):
UNCONNECTED_SIGNING_ROOT="$PWD" ./gradlew :app:assembleRelease :app:testDebugUnitTest :app:lint

# Or use the original Chromecast project checkout:
UNCONNECTED_SIGNING_ROOT=../chromecast-killswitch-app ./gradlew :app:assembleRelease :app:testDebugUnitTest :app:lint
```

See [SIGNING.md](SIGNING.md). The original private PKCS#12 remains unchanged in the Chromecast checkout; an optional private copy of the keystore and signing.properties can be kept under unconnected/development/, excluded from Git. Both configurations reuse the same certificate. Compare the APK signer with the recorded original release certificate before distributing. APKs, build logs and source archives live under ignored `development/`; current release receipts are indexed in `PROJECT_STATE.json`. Historical code-6 verification and v1.0.0 publication records remain in `evidence/runs/dynamic-layout-1.0.0-vc6/` and `evidence/runs/release-v1.0.0/`.

## Install on a test phone — human action required

Installation, changing Android roles/permissions, and real calls require the device owner's targeted approval. No such device actions have been performed by the agent. First verify the SHA-256 receipt and signer.

```bash
adb -s DEVICE_SERIAL shell pm list users
# After confirming that 0 is the personal user:
adb -s DEVICE_SERIAL install --user 0 -r development/deliverables/release-v1.2.0/unconnected-1.2.0-vc8.apk
```

**Important for work-profile phones:** Plain `adb install` may provision the APK in both the personal and managed profiles. Never omit `--user` for test installs on these devices. If the APK was already installed in a work profile, first find the actual work ID using `adb shell pm list users`, verify installation with `adb shell pm list packages --user WORK_USER_ID it.sorguido.unconnected`, and—only after owner approval—remove that profile's installation with `adb uninstall --user WORK_USER_ID it.sorguido.unconnected`. This retains the personal installation. Do not bypass company device restrictions. Never run profile-unspecified `adb uninstall` on a multi-profile phone.

Alternatively, copy that APK to the personal profile and open it with its Android package installer after verifying the active profile. Grant install permission only to the chosen installer if needed. Open **unconnected**. On a fresh install, filtering starts OFF; an update preserves existing settings. Explicitly choose unconnected as the call screening app, then grant Contacts permission from its setup card. Android requires this permission to deliver saved-contact calls to the screening service. Separately, the optional contacts picker uses the same permission to read only the name and phone numbers of the contact you choose; it does not scan or import the address book. This app conservatively allows all calls when either role or Contacts permission is missing.

Add numbers manually, or tap **Choose from contacts** and select an aggregated contact. Equivalent E.164 representations appear once; if several distinct numbers remain, choose one from the Material 3 dialog with mobile/work/home/custom labels. A single unambiguous number directly prefills the editor. Unusable or ambiguous alternatives are reported and require a deliberate choice or manual entry. The chosen number and available name remain editable; selection alone saves nothing. Tap **Save** to confirm. Cancelling either selection stage preserves the prior draft; cancelling the editor preserves existing rules. Invalid or already-listed numbers cannot be saved until corrected. No new permission is added: the existing Contacts permission must be available. Select the two-letter country/region before picking national numbers (default IT); picking does not change it. Numbers are stored as full international E.164 values. The same field also accepts a bounded numeric range: replace the final digits with trailing asterisks, for example `02 12345***`, up to four of them. The editor reports how many combinations the range covers and asks for an explicit confirmation before saving a range wider than 1,000 numbers; cancelling keeps the existing rules. A range matches only callers whose full E.164 value has exactly that prefix and that exact length, so it never grows into an open-ended prefix rule, and malformed ranges are refused like invalid numbers. Only an identical range is a duplicate: a single number already inside a range stays savable. The schedule is shared across enabled numbers. Add/edit weekdays and start/end times, including split shifts and overnight windows. Selected days belong to the start of an overnight window. Start is inclusive, end exclusive; equal start/end is invalid. Adjacent windows can provide full-day coverage. Empty schedules require explicit confirmation because they allow no time for listed active numbers. Touch **ACTIVATE SCHEDULE** when ready; **DEACTIVATE SCHEDULE** stops weekly enforcement and keeps your rules. **ACTIVATE TIMER** opens the timer controls; **DEACTIVATE TIMER** stops temporary blocking. Stop both functions to bypass all app filtering.

Times follow the current device timezone and 12/24-hour preference. On the repeated DST hour, each local occurrence follows the same rule; nonexistent local times are skipped. The screen refreshes its schedule summary only while visible; the service evaluates actual call-arrival time without alarms, polling or a background scheduler.

## Limitations and privacy

- Only full, unambiguous international caller IDs match. Incoming national-only, hidden, restricted, absent, malformed or ambiguous IDs are allowed, including international values that would require dropping a trunk prefix. This can let some listed callers through when a carrier supplies an ambiguous ID. A numeric range applies the same rule at one exact total length: an ID a digit shorter or longer than the range is allowed, so a range never widens into an open-ended prefix rule.
- A forwarded call may show the original caller instead of the office line. The app cannot recover an unavailable forwarding identity. Do not assume a forwarding line rule will catch such calls.
- Outside allowed hours with the schedule active, or while a timer is active, the app requests disallow without explicit rejection and requests missed-call notification suppression. Actual handset/carrier behavior remains unverified. Android may retain blocked-call log entries. No promise of removing every trace is made.
- Rules are local, bounded to 200 numbers and 100 windows, and excluded from cloud backup/device transfer. There is no account, telemetry, call recording, history collection or Internet permission. Labels are limited to 80 characters.
- Corrupt/unreadable configuration fails open. Resetting corrupt configuration requires confirmation. Failed saves pause screening for the current process; previously saved settings may return after process restart. Retry the save or remove the app's screening role in system settings if storage fails.
- Android may suppress callbacks for force-stopped apps. Reopen the app and verify its role/permission status after force-stop. Reboot/background behavior and the three-second response budget require physical-device trials; no universal five-second scheduling guarantee is claimed.

## Update, disable and rollback

For an approved update with the same package/signing identity and a nondecreasing version code, use `adb -s DEVICE_SERIAL install --user 0 -r APPROVED_APK`; rules are kept. Retain the previous verified APK and checksum. Android may reject version downgrades; do not uninstall or clear data merely to force a downgrade without accepting rule loss.

For immediate rollback of behavior, stop both the schedule and timer, or remove unconnected's screening role in Android settings. Uninstall through Android settings, or after explicit approval:

```bash
adb -s DEVICE_SERIAL uninstall --user 0 it.sorguido.unconnected
```

Uninstall removes app-owned rules. System-owned call logs are not erased. Restore a previously selected screening app manually if desired. unconnected never changes forwarding, Do Not Disturb, the default dialer or any phone settings on its own.
