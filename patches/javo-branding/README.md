# Javo resource branding

The opt-in Video and Music definitions change app names and launcher icons. Select the Javo definition for the target app and leave other branding patches deselected. Original and recognized GmsCore-renamed package identities are preserved.

Private CI verified the Android RVP loader and used the production Manager patching and signing code to rebuild two first-party fixtures. Both signed test APKs installed and launched. See `evidence/manager-patch-integration.json` for the exact Manager and bundle hashes. Compatibility with actual YouTube and YouTube Music APKs remains unverified.

Build the Kotlin JAR with the Manager Gradle wrapper and `-p ../../patches/javo-branding jar`. The workflow converts the JAR with D8 and includes `classes.dex` in the RVP. See `docs/BUILDING.md` for dependency access and emulator test instructions.
