# Architecture and boundaries

## Manager

Javo retains the complete upstream source architecture: real APK selection, loaded patch metadata, selectable patch options, compatibility guards, Room activity/install history, managed signing identity, WorkManager-driven patching, actual event progress, log export, backup/preferences and Android package installation. The Javo changes are visual identity, package/deep-link identity, palette defaults, and isolated update source/security checks. Inherited code is not equivalent to newly verified behavior: no Manager APK has been built locally due to package access.

Javo's updater separates its manifest URL/public key from the configurable upstream patch API. A signed release payload is verified before metadata is trusted; downloaded APKs are checked again before an installer session is created. Production feeds default to disabled. The current design rejects certificate rotation: only the same single signing certificate as the installed build is accepted.

## Video and Music

Products are customization targets, not independent clients. BrandingResource Java transformations update decoded application labels and launcher icons. Small Kotlin adapters call the inspected Patcher resource API. Core service integration and playback features must come from compatible trusted definitions; the inspected official bundle is not redistributed or claimed execution-compatible. Javo branding patches must not be combined with competing branding patches. No automatic conflict resolver or supported proprietary version list is claimed.

## Core

The actual ReVanced GmsCore fork is retained, including services and modules. Its internal package identity remains app.revanced.android.gms. Javo replaces display names, launcher assets and limited palette resources. Source-archive builds use a fixed version fallback. It is not a rewritten service-health dashboard, and authentication or push delivery is not proved by compilation.

## Website

Dependency-free static generation is used instead of Next.js because every page is public/static and the task does not require a server application. Node builds all pages from shared components and documentation content. CSS tokens are generated from a shared JSON palette. Navigation and theme controls run locally. Real artifact inventory drives downloads; original concept images are not shown as implementation screenshots. The site has no telemetry or fictional social links.

## Release infrastructure

Official Android tools verify APK structure, package identity and signatures. The packaging script checks APK hashes, archives full source/evidence/static output and rehashes ZIP contents. CI adds compile/lint/test and emulator-launch jobs; those jobs remain unexecuted until hosted in an authorized repository. No public deployment, remote release or external account modification has been performed.
