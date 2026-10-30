# Third-party components

The application uses AndroidX Activity and Compose Material 3 (Apache License 2.0) and Google's libphonenumber (Apache License 2.0). libphonenumber provides maintained international numbering-plan validation; the app adds strict caller identity checks and does not use suffix matching.

- AndroidX source and licensing: https://android.googlesource.com/platform/frameworks/support/
- libphonenumber source and license: https://github.com/google/libphonenumber
- Gradle wrapper/tooling: https://github.com/gradle/gradle (Apache License 2.0)

Exact direct dependency versions are pinned in the Gradle files. Android platform components are supplied by the device. No private Chromecast source or signing material is bundled in this repository or source archive.

## License texts and transitive runtime components

Third-party components retain their original copyright notices and licenses.
The project's GPL-3.0-or-later declaration does not replace those licenses.

- AndroidX (The Android Open Source Project), including Activity, Compose,
  Material, graphics-path, lifecycle, core and their runtime support libraries:
  Apache-2.0. The [embedded AndroidX license](docs/licenses/AndroidX-LICENSE.txt)
  is preserved verbatim; [full Apache-2.0 text](docs/licenses/Apache-2.0.txt).
- Kotlin standard libraries (JetBrains and contributors), Kotlin coroutines,
  and JetBrains annotations: Apache-2.0. Copyright headers are preserved in
  the corresponding upstream source archives.
- Google libphonenumber and Guava listenablefuture: Apache-2.0, including the
  licenses inherited from their Maven parent POMs. Original copyright headers
  remain in the source archives.

The [runtime source inventory](evidence/runs/gpl-v1.0.0/dependency-sources.json)
identifies all 57 distinct resolved runtime Maven components, exact versions,
upstream URLs and source/POM hashes. The Release source companion includes
those unmodified files, upstream AndroidX resource/build trees, libphonenumber
XML metadata/generators and native AndroidX graphics-path sources and build
instructions. The graphics-path native source files also carry Apache-2.0
headers. No other native library is present in the APK.

Gradle and the Android/Kotlin build tools are not packaged in the APK. JUnit
4.13.2 (EPL-1.0) and org.json 20240303 (public domain) are test-only dependencies,
not runtime components. Their dependency declarations are unchanged.

See [license and source distribution](docs/LICENSING.md) for the project's
global license declaration and instructions that accompany the binary.
