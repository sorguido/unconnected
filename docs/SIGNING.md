# Release signing — reuse of the existing certificate

**Mandatory maintainer requirement:** Official unconnected APKs MUST use the same Android release signing certificate as `sorguido/chromecast-killswitch-app`. Never create or rotate a new release key. The separate Android package identifier is `it.sorguido.unconnected`, so the two apps can coexist independently while sharing the signing certificate.

## Existing private setup (not in Git)

The Chromecast project's `app/build.gradle.kts` reads `development/signing.properties` (ignored) and a referenced PKCS#12 keystore from its local `development/` area. The existing property keys are:

```properties
storeFile=development/local/signing/release.p12
storePassword=... # existing private value; never publish
keyAlias=...      # existing private value; never publish
keyPassword=...   # existing private value; never publish
```

The text above is a **schema/example**, not a request to create that file or reveal secrets. The original keystore must remain unchanged in the Chromecast working directory, outside this repository.

## Minimal reuse mechanism for the unconnected build

Use a single optional environment variable `UNCONNECTED_SIGNING_ROOT` pointing to the **existing Chromecast project checkout** (the directory containing `development/signing.properties`). When this variable is set, the new Gradle build should load the already-existing properties from that root, and resolve a relative `storeFile` against that SAME project root, not against unconnected's root. An absolute path in `storeFile` must work unchanged.

A build without access to the private configuration must still allow a normal debug build and test run, but an **official release build must fail closed** instead of silently using a debug certificate or creating a new keystore.

Suggested maintainer invocation from the unconnected working tree:

```bash
UNCONNECTED_SIGNING_ROOT=../chromecast-killswitch-app ./gradlew :app:assembleRelease
```

This interface is implemented in `app/build.gradle.kts`. A normal release build without this configuration fails before release compilation. `-Punsigned` explicitly produces an unsigned development artifact; debug uses its separate development package. See `docs/BUILD.md` and the verification receipt for measured results.

For public contributors, document unsigned release/debug builds independently. Do not put keystore, properties, passwords, key aliases containing secret data, or signed binary packages in Git. Keep `development/` ignored.

## Signature verification

Before distribution: run `apksigner verify --verbose --print-certs` on the newly signed APK and on the previously published Chromecast signed APK. Verify that the **signer certificate SHA-256 fingerprint matches exactly**, while application IDs remain distinct. Verify that the release has the intended `versionCode`, archive checksum, source state and clean install. A shared signer does not imply that different package IDs can update each other.

If the original material is unavailable, report `HUMAN_REQUIRED`; do not generate a substitute. Protect the original keystore with an offline backup. Never log the private properties.
