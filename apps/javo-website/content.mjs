export const products = [
  { id: 'manager', name: 'Javo Manager', eyebrow: 'THE CONTROL CENTER', color: 'primary', text: 'Import your APK. Choose your patches. Keep control of the result.', details: 'An independent fork of the real ReVanced Manager, with Javo branding and a dark neon palette. The upstream patch selection, local processing, signing identity, log export and Android installation architecture are retained. Android build and runtime verification are reported separately in the evidence package.' },
  { id: 'video', name: 'Javo Video', eyebrow: 'MAKE IT YOURS', color: 'video', text: 'A personal video experience, built from an APK you supply.', details: 'Javo Video is a patch-based customization target for a compatible user-supplied YouTube APK. It is not an independent streaming service. Branding resource patch source is included. Playback and feature patches depend on a compatible trusted catalog; the official API serves a signed patch bundle, but execution compatibility is unverified. No proprietary APK is distributed.' },
  { id: 'music', name: 'Javo Music', eyebrow: 'YOUR OWN RHYTHM', color: 'music', text: 'Your listening experience. Your choice of supported patches.', details: 'Javo Music uses a compatible user-supplied YouTube Music APK and genuine patch definitions. A clean violet and blue identity is included. Background playback, account integration and other upstream feature patches remain subject to catalog availability and end-to-end verification. No music service or streaming data is simulated.' },
  { id: 'core', name: 'Javo Core', eyebrow: 'THE CONNECTION LAYER', color: 'core', text: 'Real GmsCore foundations. A distinct Javo identity.', details: 'Built from the ReVanced GmsCore source, retaining app.revanced.android.gms, account contracts, registration services and messaging interfaces. Branding does not change the internal compatibility identity. Installation and launch passed an Android 10 emulator smoke check; authentication, notifications and device registration still need integration validation. No Play Integrity certification is claimed.' }
];

export const pages = {
  '/docs/': ['Documentation', 'A clear path from source to your Android device.', [
    ['Start with a verified build', 'Use the build report and SHA256SUMS in the source package. A successful source download is not proof that an APK builds or runs. Test builds are labeled separately from production releases.'],
    ['Use compatible inputs', 'Import a full APK using Android’s file picker. Split packages need a supported bundle workflow. A trusted compatible patch bundle must be present before patching. Never download proprietary APKs from unverified sources.'],
    ['Keep your signing identity', 'The Manager retains upstream signing and keystore management. Keep a secure backup of your personal patch signing identity. Android will refuse an update signed by a different identity.']
  ]],
  '/docs/installation/': ['Javo Manager and Core installation guide', 'Check the Downloads page for the current release status before installing Javo Manager or Core.', [
    ['1. Check the release status', 'Continue when the Downloads page lists an approved release. Match the APK filename and SHA-256 to its release inventory. Keep Play Protect enabled and stop if Android reports a harmful app.'],
    ['2. Install Manager and Core', 'Open the APK and approve Android’s installation prompt. Allow installation from the chosen app only when needed. Core shares the upstream package identity: a differently signed existing GmsCore cannot be updated in place.'],
    ['3. Patch a source APK', 'Choose a compatible source APK. Use the APKMirror screen to download or select a downloaded APK or APKM from storage. Choose trusted, compatible patches, then save the signed result or use Android’s installer.'],
    ['4. Protect account data', 'Use Core’s own account flow. Never provide account passwords to Javo support or paste them into build scripts. Authentication is unverified in this package.']
  ]],
  '/docs/troubleshooting/': ['Troubleshooting', 'Failures should tell you what actually happened.', [
    ['Patch catalog unavailable', 'GitHub blocks the original patch repository, while the official ReVanced API still serves patch bundle 6.2.1 and links to GitLab sources. Its detached signature was checked. This does not establish that patch execution or playback works in Javo.'],
    ['GitHub Packages authentication', 'Manager’s pinned dependencies may require a GitHub identity with read:packages. Use local environment variables or CI secrets. Never commit a token. See the packaged build evidence for the exact observed failure.'],
    ['Installation conflict', 'A package with the same identity but a different signature cannot update the existing app. Back up needed data and review the signing identities before choosing a clean installation.'],
    ['Patching memory or source error', 'Check available storage, use a supported full APK and compatible patches, and export the actual logs. Failed patching must never be treated as a successful output.'],
    ['Why does Google Play Protect recommend scanning Javo Core?', 'Play Protect may recommend a scan before installing an APK downloaded from the internet. Keep it enabled and let the scan finish. If Google blocks installation after the scan, record the exact message, Core version and Android version, and compare the APK hash to the release evidence.']
  ]],
  '/faq/': ['Frequently asked questions', 'Straight answers before you install.', [
    ['Is this an official ReVanced project?', 'No. Javo is an independent branded fork. It is not affiliated with Google, YouTube, ReVanced or microG. Upstream credits and licenses remain in the source.'],
    ['Are Video and Music APKs included?', 'No. Proprietary applications are not redistributed. You supply a compatible APK and create your own patched result when a compatible trusted patch bundle is available.'],
    ['Can Javo Manager patch YouTube or YouTube Music?', 'Javo is an independent ReVanced-based Android APK patcher. Supply a compatible APK and use a trusted patch bundle on your device. See Downloads for the current Manager and Core availability.'],
    ['Is sign-in verified?', 'Core installation and launch were checked on an Android 10 emulator. Account authentication, playback and push delivery were not tested. A settings screen is not proof of a working account workflow.'],
    ['Can I install a production release?', 'Only artifacts with actual build, checksum and signing evidence may appear in Downloads. The current project status is a functional alpha.']
  ]],
  '/about/': ['More freedom. Clear foundations.', 'Javo ReVanced is an independent open-source Android project.', [
    ['Patch. Manage. Customize. Control.', 'Javo brings a unified visual identity to Manager, Video, Music and Core. The implementation builds on proven open-source components rather than recreating service and patching engines.'],
    ['Built with attribution', 'ReVanced Manager and Patcher provide the patching architecture. ReVanced GmsCore and microG provide the services foundation. Their authors retain copyright and attribution.']
  ]],
  '/licenses/': ['Open-source & licenses', 'Credit stays with the people who built the foundations.', [
    ['Manager and patching', 'Manager source and Javo modifications are distributed under GPL-3.0. ReVanced Patcher and Library licenses remain applicable. Full corresponding Manager source and build instructions are included.'],
    ['Core', 'GmsCore includes Apache-2.0 and other notices. All upstream license and copyright files remain in the Core source tree. Review LICENSES and docs/LICENSING.md for the component inventory.'],
    ['Brand assets', 'Javo vector artwork is recreated from the owner-supplied concept images. Original references are preserved separately. No concept image is labeled as an actual application screenshot.']
  ]],
  '/security/': ['Verify before you install.', 'Trust is a chain of checks, not a badge.', [
    ['Current installation status · 11 November 2026', 'APK distribution is paused. A phone reported Play Protect blocking Manager alpha.11 after scanning. The exact APK was submitted to VirusTotal: 66 engines reported undetected, two timed out and seven could not process the file type. Google received a Play Protect appeal. Neither result confirms that the device block is cleared. Keep Play Protect enabled.'],
    ['Check the exact file', 'The reviewed Manager alpha.11 SHA-256 is 86d04a37709528a986c79b09df7ff03a3622b7061b37b20394af3f9737f563a2. A different APK is a different review target. Build, dependency and controlled emulator checks do not establish that an APK is free of all vulnerabilities.'],
    ['Artifact integrity', 'Compare an APK’s SHA-256 with the packaged inventory and inspect its Android signing certificate. A checksum alone does not establish the identity of the publisher.'],
    ['Signed update design', 'Javo’s Manager update source is isolated from upstream Manager releases. Configured feeds require an ECDSA-signed manifest, correct package and newer version, unexpired metadata, compatible Android/ABI, SHA-256 and the installed signing identity. The production feed is not configured.'],
    ['Report a concern', 'Do not send passwords, account tokens or private signing keys. Use the private security process of your own source repository when configured. No Javo contact address has been invented.']
  ]],
  '/privacy/': ['Privacy policy', 'Local tools with explicit network dependencies.', [
    ['Website', 'This static website has no analytics, advertising, tracking cookies or account system. Theme choice is stored in your browser. A hosting provider may log ordinary HTTP requests under its own policy.'],
    ['Manager', 'Patching is designed to run locally using the upstream architecture. Explicit patch/download/update actions contact the selected services. Upstream announcement and metadata checks may also use the configured ReVanced API. Logs and exported backups can contain package names and file paths.'],
    ['Core and accounts', 'Core communicates with Google-compatible services for account, registration and messaging workflows. Their privacy terms apply. Javo does not provide a proxy account service and must never receive your password.']
  ]],
  '/changelog/': ['Changelog', 'Changes recorded against real source.', [
    ['2026-11-03 · Development regeneration', 'Recovered all four design references and the master prompt. Imported pinned real Manager and GmsCore source, applied Javo identity, added update verification source, a responsive static website, release checks, CI and build evidence. Read the final implementation report for actual build and test outcomes.']
  ]]
};
