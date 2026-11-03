# Javo resource branding

Two opt-in Patcher resource definitions set the application name and launcher icon for the original YouTube and YouTube Music package IDs. They do not rename packages, add playback functionality, change service identifiers or claim compatible app versions. Avoid selecting another branding patch simultaneously: this bundle has no upstream conflict metadata system.

The API was inspected in the pinned official Patcher source. Resource transformations are tested on decoded fixture manifests. Building the Kotlin bundle requires GitHub Packages access. APK rebuild, Android DEX bundle loading, and playback integration have not been verified. A plain JVM JAR is not claimed Android-loadable: it must contain classes.dex before Manager can load it. The CI build creates a D8 RVP archive for subsequent device testing.

Build: run the Manager Gradle wrapper with `-p ../../patches/javo-branding jar` from apps/javo-manager. Use the matching Patcher dependency and environment credentials documented in docs/BUILDING.md. Package via infrastructure/scripts/dex-bundle.ps1. Import only after the resulting bundle passes verification on a test device.
