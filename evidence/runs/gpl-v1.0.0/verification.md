# GPLv3-or-later correction and local cleanup — v1.0.0

Date: 2026-10-30 (Europe/Rome). Scope: license/documentation only, replacement of
the private `v1.0.0` tag/Release, then removal of inventoried reconstructible
outputs. The owner explicitly authorized those actions in the execution prompt.
Repository visibility must remain **private**. No branch force-push or history
rewrite, other tag/Release change, phone action or signing-material access is
part of this task.

## Baseline and retained binary

**OBSERVED:** Clean `main` at `71577e1f351a3c0f5a04f5353c8e3ce8e896ed39`.
Old annotated tag object: `ffd7178692cd8e3240801e1392504160a4a6c5aa`, targeting
`0a814be6213b808c038851536c15e63b47504335`. Original Release ID: `429496972`.
[Previous metadata](previous-release.json) preserves title, body, URL, asset IDs,
sizes and GitHub digests. A verified local candidate remains available throughout
the replacement. Other refs are inventoried locally for the final comparison.

**PROVEN:** Downloading the original private Release APK reproduces the candidate
byte for byte, size **18716483 bytes**, SHA-256
`2e400b5ea089f5702925301bc7afedcd2159478c4bbb9b38befb630360462a88`.
Build-tools 36.0.0 `apksigner verify --verbose --print-certs` succeeds (v2,
one signer), certificate `CN=Chromecast Kill Switch`, SHA-256
`2936676a220e4a9682171fbf0c9b91552acd8fc4f120318c6a0aebe6e6722ecc`.
This matches the existing independently verified Chromecast certificate receipt.
`aapt2 dump badging` reports `it.sorguido.unconnected`, versionName `1.0.0`,
versionCode `6`, minSdk 29, targetSdk 36, and the same two permissions.
No key, keystore, passwords or external signing configuration was read or changed.

**PROVEN:** All 32 hashes in
[`source-inputs.sha256`](../dynamic-layout-1.0.0-vc6/source-inputs.sha256) match.
No application source, resources, manifest, Gradle configuration, dependency,
test, wrapper or version changed. All four canonical documents are unchanged.
The original release APK is reused without recompilation; a document-only
license change does not need a versionCode increase. This preserves the exact
binary and the validity of the existing build/signature evidence.

## License and dependency review

**PROVEN:** `LICENSE` is the complete 35149-byte GNU GPLv3 text downloaded from
<https://www.gnu.org/licenses/gpl-3.0.txt>, byte-identical to that response.
`README.md` declares GPL-3.0-or-later in one line; `docs/LICENSING.md` retains
`Copyright (c) 2026, sorguido` and explicitly selects version 3 or any later
version. Earlier BSD grants are historical, not revoked or erased. No protected
canonical document mentions BSD, so no canonical-change gate is needed.

**OBSERVED / INFERRED:** Commit authorship of the original work uses the owner's
two identities only. The owner's authorization supports changing that work's
license; no unrelated third-party contribution was found. Dependencies retain
their own copyright and Apache-2.0 terms, compatible with GPLv3 according to
[GNU](https://www.gnu.org/licenses/license-list.html#apache2).

**PROVEN:** The existing release lint dependency model lists **57 distinct
runtime Maven components** (one core coordinate occurs twice for different
artifacts). Exact published source JARs and POMs were downloaded and ZIP-checked;
[hashes and URLs](dependency-sources.json) identify every file. The libphonenumber
and Guava parent POMs confirm the inherited Apache-2.0 license. All other POMs
declare Apache-2.0 directly. Full Apache text and the AndroidX embedded license
are retained in `docs/licenses/`. There was no embedded NOTICE entry in the
runtime binary artifacts inspected; source copyright headers are preserved.

The APK contains native `libandroidx.graphics.path.so` for four ABIs; its Maven
sources omit C++. The companion therefore also includes the official
graphics-path 1.0.1 module archive, including C++, headers, CMake and Gradle flags,
from the release revision identified by Android's release notes:
[`8a05a22af450d589ef911d772a001a49dcb05b71`](native-source.json).
Its native headers are Apache-2.0. There is no other native library in the APK.

**Independent-review correction:** Source JARs alone omit some Android resources
and libphonenumber preferred-form metadata. The companion additionally preserves
[27 upstream source trees](upstream-trees.json): AndroidX modules at official
release-note revisions (including resources and build scripts), and the full
libphonenumber v8.13.55 tree with XML metadata/generators. Interpolator 1.0.0
uses an equivalent immutable upstream tree with all four Java files verified
byte-identical to its published source JAR; it is not falsely described as its
release commit. All these archives have measured hashes.

## Automated verification and review

**PROVEN:** `./gradlew -Punsigned --offline :app:testDebugUnitTest :app:lintDebug
:app:lintRelease --console=plain` completed successfully, using existing caches
and no private signing access. **67 tests, zero failures/errors/skips**; lint
debug and release each report **zero errors, eight existing warnings**.
See [execution log](checks.log). The official APK was not rebuilt.

The [independent read-only review](review.md) approves this documentation-only
change after the source-completeness corrections above. It verifies the license
declaration, immutable inputs, source companion and preserved canonical files.
Final execution receipts are generated locally after the remote and cleanup
checks described below.

## Replacement and source access

The replacement tag must be annotated and identify the enclosing license commit
on `main`. The replacement Release is titled **unconnected v1.0.0**, non-draft,
non-prerelease. Assets: the unchanged APK and its original 92-byte checksum,
`LICENSE`, plus `unconnected-1.0.0-source.tar.gz` and its checksum. The companion
contains `git archive` of the corrected commit and all the dependency files
described above. GitHub's automatic tagged ZIP and tar.gz must both contain the
corrected license, global declaration, README and identical application inputs.
The release body places source/license directions beside the binary; recipients
have equal private access to both, at no extra charge. Redistributing outside
the repository requires supplying those materials/access to those recipients.
See [distribution instructions](../../../docs/LICENSING.md) and
[GNU's section 6(d) explanation](https://www.gnu.org/licenses/gpl-faq.html#SourceAndBinaryOnDifferentSites).

Only after committing and pushing the reviewed documents, the previous Release
and only `refs/tags/v1.0.0` are removed and recreated. All assets are downloaded
back; APK hash, size, signature and identity are rechecked; source downloads are
inspected. Previous downloaders may retain an older BSD-tagged source snapshot
under the same `v1.0.0` name. **This tag replacement is deliberate and disclosed;
the identical APK digest does not identify the license revision.**

## Final cleanup and postflight

[Before measurements](before-sizes.json) are actual independent `du -sb` values,
not estimates. Root: **400485237 bytes (381.932 MiB)**; `du -sh .` reports **387M**
allocated. These are different measures. Initial [Git object statistics](git-objects-before.txt)
are retained; no GC/prune is performed. `.git/` is preserved, apart from ordinary
authorized commits/ref updates and the `v1.0.0` replacement.

The explicit removal inventory is recorded locally before deletion. It covers
only `app/build/`, root `build/`, four superseded deliverable groups
(`contacts-1.0.0-vc3`, `issue-1-1.0.1`, `issue-2-1.0.0-vc4`,
`stable-ui-1.0.0-vc5`), the two initial root APKs, the stale root source archive,
and the code-6 duplicate release/debug APKs. Small historical checksums and
build-commit metadata are preserved in the local cleanup receipt before their
superseded directories are removed. No tracked file may be removed; symlinks
are rejected, not followed. No broad `git clean` is used.

The **one retained local APK** is
`development/deliverables/dynamic-layout-1.0.0-vc6/unconnected-1.0.0-vc6-candidate.apk`.
Preserved: all tracked code/tests/evidence/docs, `.git/`, all original files in
`development/archive/`, `.gradle/`, `local.properties` (not read), the wrapper,
SDK and external signing material. Temporary downloaded/upload APK copies are
also removed after their comparisons, to avoid leaving redundant local APKs.

Cleanup is performed only after every build/check and successful Release
round-trip verification. Then only read-only verification and writing the local
postflight receipt occur: retained hash/signature, tracked-file inventory,
archive hashes, unrelated refs, remote private status, tag target, asset
digests, `git status`, Git object count, and `du -sb`/`du -sh` measurements.
**No build follows cleanup.** Final measured sizes, recovered bytes, new commit
and tag SHA, new Release/asset IDs, review outcome and concrete removed paths
are saved locally in ignored `development/archive/gpl-v1.0.0/postflight.json`
and reported to the owner. This avoids a receipt-only commit moving `main`
beyond the corrected release tag.

## Limits

No new physical phone test is needed for an unchanged binary, and none was run.
Prior owner acceptance remains owner-reported without OEM/Android-version
metadata. Existing accessibility and caller/notification limitations are
unchanged. No byte-identical rebuild of the application or upstream libraries
is claimed. The repository and Release remain private by owner instruction.
