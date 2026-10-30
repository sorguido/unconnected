# Third-party components

The application uses AndroidX Activity and Compose Material 3 (Apache License 2.0) and Google's libphonenumber (Apache License 2.0). libphonenumber provides maintained international numbering-plan validation; the app adds strict caller identity checks and does not use suffix matching.

- AndroidX source and licensing: https://android.googlesource.com/platform/frameworks/support/
- libphonenumber source and license: https://github.com/google/libphonenumber
- Gradle wrapper/tooling: https://github.com/gradle/gradle (Apache License 2.0)

Exact direct dependency versions are pinned in the Gradle files. Android platform components are supplied by the device. No private Chromecast source or signing material is bundled in this repository or source archive.
