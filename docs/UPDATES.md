# Signed updates

No Javo update feed is published or configured. The Manager's update repository has been replaced with isolated Javo update verification source. It must not download an upstream Manager APK as its own update.

## Envelope

JSON fields: `payload` contains base64 of exact UTF-8 release JSON bytes, `signature` contains base64 DER ECDSA-SHA256 signature over those bytes. The pinned public key is base64 X.509 SubjectPublicKeyInfo for a P-256 EC key. The private key belongs outside the repository. Use infrastructure/scripts/sign-manifest.mjs with JAVO_MANIFEST_KEY_FILE to sign a real release manifest.

The release JSON schema is represented by JavoRelease.kt and packages/release-schema/validate.mjs. Required fields: schemaVersion=1, applicationId, displayName, version, versionCode, minSdk, architectures, channel, releasedAt, expiresAt, artifactUrl, sha256, signerSha256 and releaseNotes. Dates use UTC ISO-8601 with Z. URLs use HTTPS without credentials, nonstandard ports or fragments. Digests are lowercase 64-character SHA-256.

The configured Android updater accepts stable-channel metadata for its exact package, a newer version code, compatible SDK/ABI, valid release/expiry timestamps and a valid signed payload. Before installation it checks the file checksum, parsed APK package/version, signed metadata certificate digest and the currently installed signing certificate. Android still requires user installation approval. Debug builds do not have a production updater identity.

Set JAVO_UPDATE_URL and JAVO_UPDATE_KEY at build time only after owner-controlled hosting, signing and publication are ready. The current host Java tests exercise actual crypto primitives, tampering, wrong keys, size limits, HTTPS restrictions and file hashes. Android manifest parsing, APK signer comparison, downloader behavior and installer integration are unverified because Manager could not be built locally. This is not a claim of a deployed verified update service.

The website catalog is a separate unsigned local build inventory, not an authenticated network update feed. verify-apk.py populates it only from real signed APKs. Video/Music updates must use a user-authorized repatching workflow. Core updates are currently manual, artifact-verified test downloads rather than an integrated automatic Core updater.
