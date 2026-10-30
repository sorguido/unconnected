# Publication report — unconnected v1.0.0

Date: 2026-10-30 (Europe/Rome). Release tag **`v1.0.0`**, title **`unconnected v1.0.0`**, package **`it.sorguido.unconnected`**, versionName **1.0.0**, versionCode **6**. Signed with the original Chromecast Kill Switch release identity; no key was generated, rotated or replaced, and debug signing was not used.

The owner states that the application was tested on a physical phone and explicitly authorized preparing and publishing the first official release. That authorization is owner-asserted: no handset model, Android version or per-step acceptance record accompanied it, so this report contains no agent-measured device evidence. The enclosing commit is the release commit; the source baseline the published APK was built from is `85782418d122338ab4a188494b9284cd1311794b`.

## Published artifacts

| Asset | SHA-256 | Size |
|---|---|---|
| `unconnected-1.0.0-vc6.apk` | `2e400b5ea089f5702925301bc7afedcd2159478c4bbb9b38befb630360462a88` | 18716483 bytes |
| `unconnected-1.0.0-vc6.apk.sha256` | checksum file naming the APK above | 92 bytes |

The published APK is a byte-identical copy of the verified candidate `development/deliverables/dynamic-layout-1.0.0-vc6/unconnected-1.0.0-vc6-candidate.apk`. It was **not rebuilt**: the application code did not change, so versionCode was deliberately left at 6. Release page: https://github.com/sorguido/unconnected/releases/tag/v1.0.0

**Round-trip check on the published assets.** Both assets were downloaded back from the Release and re-verified: `unconnected-1.0.0-vc6.apk` came down at 18716483 bytes with SHA-256 `2e400b5ea089f5702925301bc7afedcd2159478c4bbb9b38befb630360462a88`, `sha256sum -c unconnected-1.0.0-vc6.apk.sha256` returned **OK**, and `cmp` against the local verified candidate reported **byte-identical**. The downloaded artifact re-verified under `apksigner` as `CN=Chromecast Kill Switch` / `2936676a…` and re-badged as `it.sorguido.unconnected`, versionCode 6, versionName 1.0.0. GitHub's own recorded asset digest for the APK matches the same SHA-256. The Release is `draft: false`, `prerelease: false`, targets `main`, and is served by the `releases/latest` endpoint as `v1.0.0`.

## Release verification — independently re-measured

- **Signature.** `apksigner verify --verbose --print-certs` (build-tools 36.0.0) reports **Verifies**, one signer, DN `CN=Chromecast Kill Switch`, certificate SHA-256 `2936676a220e4a9682171fbf0c9b91552acd8fc4f120318c6a0aebe6e6722ecc`, SHA-1 `7450ef711dcfcaf713449c34caa02e88a3226fa1`, RSA 3072. Signing scheme v2 only (v1/v3/v3.1/v4 false), which is sufficient for minSdk 29.
- **Identity match.** That certificate digest is identical to the signer of the independently published Chromecast Kill Switch v1.1.0 asset, whose own public SHA-256 was re-measured as `4e1583fc7421ba2ed508966ec8c567a336c0f0be5d6fa775a3a001885bf13e43`. Distinct applicationId, identical signer.
- **Badging.** `aapt2 dump badging` reports package `it.sorguido.unconnected` (no `.debug` suffix), versionCode 6, versionName 1.0.0, application-label `unconnected`, minSdkVersion 29, targetSdkVersion 36, compileSdk 36.
- **Permissions.** Only `android.permission.READ_CONTACTS` and the AGP-generated internal `it.sorguido.unconnected.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. There is **no INTERNET permission**, which substantiates the offline and no-tracking claims in the README.
- **Upgrade compatibility.** Local candidates code 1, 3, 4, 5 and 6 were all re-badged and re-verified: identical package `it.sorguido.unconnected`, identical signer certificate SHA-256 `2936676a…`, strictly increasing versionCode. In-place update over any earlier candidate is therefore supported without uninstall or rule loss. Preservation of settings on the handset remains an owner check.
- **APK matches the approved sources.** All **32 of 32** hashes in `../dynamic-layout-1.0.0-vc6/source-inputs.sha256` match the working tree byte-for-byte. The code-6 discriminator holds: `state_saving` occurs **zero** times in both `app/src/` and the APK's `resources.arsc`, while `saving_detail` is present in the arsc and in both locales. All **90** English string resource names are present in `resources.arsc`.
- **No private material in the APK.** Every entry of the extracted archive was scanned. No keystore, certificate, `.p12`, properties file, database, CSV, screenshot or personal document is present; the only payload beyond code and framework resources is `assets/dexopt/baseline.prof{,m}` and AndroidX version stamps. `META-INF/version-control-info.textproto` records `local_root_path: "$PROJECT_DIR"` — a variable, not a local absolute path — and revision `ffaa2649792cf82501d880d066aa549d6ec130cf`, the commit preceding the release baseline. No phone number, username or e-mail address appears in `resources.arsc` or either `classes*.dex`.
- **Byte-level reproducibility is not claimed**, consistent with the candidate receipt: a second build from sources differing only in a comment produced the same release size and a different archive digest. The digest above identifies this exact delivered artifact.

## Public security audit

Scope: every tracked file, plus **all 171 unique blobs** reachable from any ref in history, plus the list of **all 112 paths ever added** to the repository.

- **Result: PASS.** No private key, certificate body, keystore, PKCS#12, JKS, PEM, `.env`, credential, API token, cloud-provider key, password or key alias value was ever committed. Every hit for `storePassword`, `keyPassword`, `keyAlias`, `storeFile`, `PKCS12`, `.p12`, `.jks` or `.keystore` is either a `.gitignore` pattern, a Gradle **property name** read from an external file (`app/build.gradle.kts:15,30-35`), or the explicitly-labelled schema/example in `docs/SIGNING.md:10-16`, whose values are the literal text `... # existing private value; never publish`.
- **No file was ever deleted** from the repository: the set of paths ever added is identical to the set currently tracked. Nothing sensitive was therefore removed from the tip while surviving in history.
- **Phone numbers.** The only numbers anywhere in the tree are test fixtures in `app/src/test/`, all drawn from reserved fictional ranges: US `+1 202 555-01xx` (555-0100…0199) and UK `+44 20 7946 0xxx` (Ofcom-reserved). No Italian mobile or fixed number, no real subscriber number, no contact name, no call log and no screenshot was ever committed.
- **Binaries.** The only binary blob in the whole history is the standard `gradle/wrapper/gradle-wrapper.jar`. No APK, AAB, image or archive was ever tracked; `development/` was never tracked.
- **Local paths.** Two categories appear, both benign and both pre-existing at the release baseline: `/home/guido/Repository/unconnected/...` as lint finding prefixes in `../dynamic-layout-1.0.0-vc6/lint-debug.txt` and `lint-release.txt` (8 lines each), and one cross-project reference at `../dynamic-layout-1.0.0-vc6/signature.json:43` to the independently published Chromecast APK used as the signer reference. No keystore or signing-material path is disclosed. The `/home/custom` and `/home/mobile` strings in `docs/BUILD.md` and the code-3 receipt are the Android contact phone-type labels *home* and *mobile*, not paths.
- **Personal identity.** Commit authorship throughout history is `Guido Sorarù <sorguido@gmail.com>` and `sorguido <sorguido@gmail.com>`. Making the repository public permanently exposes that name and address to indexing and scraping. This is the owner's own identity on the owner's own project and is already implied by `LICENSE:3` and the public repository URL, so it is recorded as an **accepted disclosure**, not a finding. No history rewrite was performed, and none is recommended.
- **Licensing.** `LICENSE` is BSD 2-Clause. Runtime dependencies are AndroidX Activity/Compose/Material 3 and Google libphonenumber, both Apache-2.0 and attributed in `NOTICE.md`; Gradle wrapper tooling is Apache-2.0. `junit` (EPL-1.0) and `org.json` (public domain) are `testImplementation` only and are not packaged into the released APK. Compatible with public binary distribution.

## Repository cleanup

`.gitignore:2` already contained `/development/` and was kept unchanged. Removed from the Git index with `git rm --cached` — never deleted from disk — and relocated into the ignored `development/archive/`:

| Moved out of the public tree | Destination | Reason |
|---|---|---|
| `evidence/runs/contacts-1.0.0-vc3/` (10 files) | `development/archive/evidence-runs/` | Superseded by code 6 |
| `evidence/runs/issue-2-1.0.0-vc4/` (10 files) | `development/archive/evidence-runs/` | Superseded by code 6 |
| `evidence/runs/stable-ui-1.0.0-vc5/` (9 files) | `development/archive/evidence-runs/` | Superseded by code 6 |
| `docs/AI_PM_GOAL.md` | `development/archive/` | Internal agent delegation prompt, not public documentation |

Kept public, with the reason for each retention:

- `evidence/runs/dynamic-layout-1.0.0-vc6/` — the release-traceability receipt for the published APK.
- `evidence/current/` — retained because `ROADMAP.md:9` names `evidence/current/verification.md` as the P2 primary artifact, and `ROADMAP.md` is protected. **Recorded limitation:** this folder holds the *original code-1* receipts and is therefore stale relative to the released build; correcting that pointer requires a canonical edit that was not authorized, so it was left untouched rather than silently broken.
- `evidence/runs/issue-1-1.0.1/` — retained because the preserved receipt at `evidence/current/verification.md:3` hard-links to it. Archived material would have produced a broken link in a record the project's practice keeps unmodified.
- `PROJECT_STATE.json`, `ARTIFACT_INDEX.json` — required at those paths by `START.md:3`; both are explicitly non-authoritative repairable caches, so their *contents* were updated to reflect the release.
- `docs/PRODUCT_SPEC.md`, `docs/SIGNING.md` — required by `AGENTS.md:3` and `AGENTS.md:31`.
- `docs/phase-specific/P0.md` — required by `ROADMAP.md:7`.
- `research/corroborations/C1.md`, `C2.md`, `C3.md` — required by `CORROBORATIONS.md:5,9,13`.
- `docs/BUILD.md`, `docs/DEVICE_TEST_GATE.md`, `tests/TEST_MATRIX.md`, `tools/verify_apks.py`, `NOTICE.md`, `LICENSE` — installation, maintenance, verification and attribution documentation.

Referrers updated so that no link dangles: `tests/TEST_MATRIX.md:31,35,39` (three dead hard links converted to prose stating that the receipts are archived locally and superseded), `ARTIFACT_INDEX.json:12,36,37,38` (re-pointed at `development/archive/…` and annotated as local-only), `docs/BUILD.md:5,29`, and `PROJECT_STATE.json`.

**The four protected canonical files `START.md`, `AGENTS.md`, `ROADMAP.md` and `CORROBORATIONS.md` are byte-unmodified**, and the cleanup did not require editing them: none of the four ever referenced the archived material. **No application source, resource, manifest, Gradle script, lockfile, wrapper file or test was modified**; `git diff HEAD --name-only -- app/ build.gradle.kts settings.gradle.kts gradle.properties gradle/ gradlew gradlew.bat tools/` is empty. The change set is documentation, indexes, repository layout and the README rewrite only.

## Independent review

A reviewer with no part in the cleanup re-checked the working tree before publication and returned **APPROVE WITH SHOULD-FIX, no blocker**. It independently confirmed: all four canonical files unmodified; zero application code changed; versionCode 6 and versionName 1.0.0 intact; 32/32 source-input hashes matching; the `state_saving`/`saving_detail` discriminator; APK digest, signer DN and certificate digest; badging and the absence of INTERNET permission; no tracked binary or private artifact; zero remaining references to the four archived paths anywhere in the tracked tree; 37 of 38 relative markdown links resolving. It also fact-checked every README claim against the manifest, resources, `data_extraction_rules.xml`, `SettingsStore.kt`, `ContactPicking.kt`, `Rules.kt` and `ScreeningService.kt` — including that `setSkipCallLog` is never called, so the README's call-log disclaimer is accurate.

Its three should-fix items are all closed by this report and its enclosing commit: the missing publication report now exists; the `PROJECT_STATE.json` publication statements became true before anything was readable outside the owner's own access, because the sequence was commit → tag → Release → verify assets; and the object-count wording was corrected to "171 unique blobs". One optional softening of an absolute-sounding README sentence was also applied.

## Repository visibility — owner decision

**The repository remains PRIVATE.** Asked to authorize both the Release and the visibility change, the owner authorized the Release and explicitly chose "Release only, repository private". The public security audit passed and is recorded above, so nothing blocks a later visibility change; it was simply not taken.

Consequences that stay open until the owner changes visibility:

- The Release, its two assets and the `v1.0.0` tag exist and are complete, but are reachable only by the owner and collaborators, not by end users.
- `README.md:18` links to `https://github.com/sorguido/unconnected/releases` and the installation instructions tell the user to download the APK from there. That link does not resolve for anyone without repository access, so the README is not yet actionable for a real end user. It is correct as written and needs no edit once the repository is public.
- The Release body links to this report at tag `v1.0.0`.

No history rewrite, force push or tag replacement was performed. Note that the tag `v1.0.0` was cut on `0a814be6213b808c038851536c15e63b47504335` **before** the owner's visibility decision, so the copy of this report inside the tag still describes the intended sequence as ending with "make public". That sentence is superseded by this section; the published tag was deliberately left untouched rather than replaced.

## Known limitations published with v1.0.0

The two accessibility limitations recorded for code 6 in `../dynamic-layout-1.0.0-vc6/review.md:31-32` and `../dynamic-layout-1.0.0-vc6/verification.md:47-48` are **published as known limitations of v1.0.0**. Per the release brief they were evaluated and documented, and **no functional change was introduced** for the release:

1. **Save completion is not announced.** The status card is deliberately frozen during a save so that its copy never changes transiently. A save that does not change the committed state — editing a time window or a number while the filter is active — therefore announces its start sentence and then nothing; the progress strip disappears silently. TalkBack announced completion in code 5. Closing this needs new localized strings and a live-region mechanism whose behaviour is TalkBack and OEM dependent and cannot be verified without a handset, which would exceed a presentation-only change and could itself become a defect.
2. **The temporary-pause sentence is audible but not visible.** "Filtering is temporarily paused while your changes are saved." travels as the `contentDescription` of the four-dp progress strip (`MainActivity.kt:288-290`), so a screen-reader user hears it but a low-vision user who does not run a screen reader sees no pause text. This is what the non-displacing-feedback requirement asks for; the pause spans one small atomic file write, and enlarging the overlay to show text would cover scrolled content and reintroduce the transient visual change the release was meant to remove.

Both remain open for an owner decision on a follow-up release. They are intentionally **not** in the end-user README: they are screen-reader trade-offs about transient save feedback, not part of the product promise, and the README is written for a non-technical reader. They are documented here and in the tracked code-6 receipts, which are public.

Also published as standing product limitations, and stated in plain language in the README: only full unambiguous international caller IDs are matched and anything uncertain is allowed through (fail-open); forwarded calls may present the original caller; filtered calls may remain in the system call log; and behaviour varies by manufacturer and carrier.

## Not verified — honest limits of this release

- **Real-device telephone behaviour.** No phone was attached during release preparation. Whether filtered calls stay silent, whether missed-call notifications are suppressed, whether blocked calls appear in the handset call log, whether the code-5 empty block is gone and whether flicker is absent are owner-only checks recorded in `../dynamic-layout-1.0.0-vc6/verification.md`. The owner's acceptance is asserted, not agent-measured, and carries no OEM, model or Android-version metadata.
- **JVM tests are synthetic.** 67 tests pass, but they do not shape Android text, compose the `Home` screen, run TalkBack, scroll a real surface or inject a real IO error. The natural height of the visible status text has no JVM proof at all.
- **Network targets.** External URLs were inventoried but not fetched. The Release and asset URLs were exercised by downloading the assets back, but because the repository is private they resolve only for the owner and collaborators; public reachability of `https://github.com/sorguido/unconnected/releases` has **not** been verified and cannot be until the visibility change happens.
- **Archived bytes.** The 30 files moved to `development/archive/` were confirmed present and count-matched against the index deletions, but their bytes were not diffed against their HEAD versions.
