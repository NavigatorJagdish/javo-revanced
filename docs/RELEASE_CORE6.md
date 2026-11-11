# Core javo.6 — experimental release

This release contains Javo Core only. Manager alpha.13 remains withheld after the owner reported a harmful Play Protect indication. Keep Play Protect enabled and stop installation if it blocks a file.

Core file: `javo-core-0.3.13.2.250932-javo.6-production.apk`.
SHA-256: `c5bb18a75cc2b70a6729a43aa92054f81f3f959b586e6fb4af588ba685b96311`.
Signer SHA-256: `47aa6be0fa482463dec325dac7864c771e6a75465c973c29587bc5fe6f4d3873`.
Android identity: `app.revanced.android.gms`, versionCode `250932009`, Android 5.0 or later.

The owner reported a safe result for this exact Core file on one phone. That is not universal Google certification. The offline-signed APK passed integrity/permission/flag checks, eight private manifest boundaries, five denied external component-start probes and an Android 10 update/launch. The matching Core source passed build, unit tests, lint and Android 10/15 launch checks with disposable CI signing.

Twelve tracked upstream security changes were imported or adapted, including caller validation, private consent/location components, work-account isolation and TLS validation. Wire runtime is 6.4.5; Compose and Kotlin were aligned. See `CORE-SECURITY-BACKPORTS.md` for exact scopes and excluded Vending-only changes.

Three advisories affecting legacy Cast Jackson mapper and address formatter SnakeYAML remain open; see `DEPENDENCY-REVIEW.md`. Account login, push delivery, location, FIDO and real video playback are unverified. No production update feed is configured.

Core build source: `95cfdc6ad93b48a71aa542116c39c4f5f720e6e0`. The later verified source `bf324721e415562a0ee7910deb703b77babada37` has an identical Core tree, `b7313d34977b50766eadfe0917978acfebf1975e`. [Application checks](https://github.com/NavigatorJagdish/javo-revanced/actions/runs/42996215236).

Use `INSTALLATION.md` for normal installation and signature conflicts. Existing patched Video/Music applications must be repatched with a compatible reviewed Manager to receive the new Javo Core destination; installing Core alone does not rewrite their code. Proprietary YouTube APKs are not distributed.
