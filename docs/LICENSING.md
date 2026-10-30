# License and source distribution

Copyright (c) 2026, sorguido

SPDX-License-Identifier: GPL-3.0-or-later

unconnected is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later version.

unconnected is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see <https://www.gnu.org/licenses/>.

The complete, unmodified GPLv3 text is in [LICENSE](../LICENSE), obtained from
[GNU](https://www.gnu.org/licenses/gpl-3.0.txt). This declaration selects **version
3 or any later version**, and applies globally to the project's original work;
individual source-file headers were not added. Third-party work retains its
copyright notices and original licenses; see [NOTICE.md](../NOTICE.md).

## v1.0.0 correction

On 2026-10-30 the owner authorized changing the current project license and
replacing the private `v1.0.0` tag and Release. Earlier revisions and copies
distributed under BSD 2-Clause retain those grants; history was not rewritten.
The APK remains byte-identical, versionName 1.0.0 / versionCode 6, with the
original release certificate. The correction changes documentation only.

## Obtaining and building the source

The [private Release](https://github.com/sorguido/unconnected/releases/tag/v1.0.0)
offers the APK, its SHA-256 checksum, `LICENSE`, and
`unconnected-1.0.0-source.tar.gz` with its own checksum. The source companion
contains the tagged project, build scripts, wrapper, lockfile, tests, these
license documents, and `third-party-sources/`: the 57 exact runtime Maven source
JARs and POMs, plus 27 upstream module/source trees containing AndroidX resources,
native graphics-path C++/CMake instructions, and libphonenumber XML metadata and
generators.
The dependency files are unmodified; copyright headers remain inside them.
The manifest [dependency-sources.json](../evidence/runs/gpl-v1.0.0/dependency-sources.json)
records exact upstream source URLs and hashes.
[upstream-trees.json](../evidence/runs/gpl-v1.0.0/upstream-trees.json) records
the additional resource/build/metadata archives and their verified revisions.
The native-source receipt is
[native-source.json](../evidence/runs/gpl-v1.0.0/native-source.json).

GitHub's **Source code (zip/tar.gz)** downloads also contain the corrected GPL
and project sources; the additional source companion preserves dependency
sources alongside them. The Release body supplies these source directions next
to the APK, following [GPLv3 section 6(d)](https://www.gnu.org/licenses/gpl-3.0.html#section6).
Source access costs nothing extra and uses the same repository permissions as
binary access. The maintainer must keep corresponding sources available while
distributing the binary. Anyone redistributing the APK outside this private
repository must also supply these licenses and equivalent source access to
their recipients; a private GitHub link alone does not give outsiders access.

Follow [BUILD.md](BUILD.md) for the pinned toolchain, debug and unsigned builds,
and installation instructions. Dependencies are resolved at the locked versions
from Google Maven and Maven Central. Platform Android libraries are supplied by
Android. The native graphics-path module can be built from its supplied
`src/main/cpp/CMakeLists.txt` with the Android NDK and the flags in its
`build.gradle`. No private signing material is included or needed for a debug
or explicitly unsigned build. Modified APKs can use the developer's own signing
identity; Android will require a separate package or uninstall of an existing
differently signed installation (which deletes app data). Official releases
must continue to use the existing external key per [SIGNING.md](SIGNING.md).
No device, locked hardware, installation service or signing key is conveyed
with this APK download. Byte-identical rebuilds are not claimed.

## Compatibility and scope

The original project's Git history attributes contributions only to the owner's
`sorguido` and `Guido Sorarù` identities. Treating these as one rights holder is
an inference supported by the history and the owner's explicit authorization;
there is no separate contributor agreement being asserted. No independent
third-party contribution was found in the original application sources.

Runtime dependencies declare Apache-2.0, including AndroidX, Kotlin, coroutines,
libphonenumber, Guava listenablefuture and JetBrains annotations. Apache-2.0
permits combination with GPLv3; their original licenses are preserved, rather
than replaced by the project's license. See [GNU's license list](https://www.gnu.org/licenses/license-list.html#apache2).
Build tools and test-only dependencies are separate from the APK. No source,
resource, manifest, dependency, test, version or application behavior was changed
for this license correction. Existing physical-device evidence and limitations
remain as recorded; no new telephone, permission or device action was taken.
