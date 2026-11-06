# Continuation state

```json
{
  "updatedAt": "2026-11-06",
  "readiness": "functional-alpha.6",
  "repo": "NavigatorJagdish/javo-revanced",
  "verifiedBuildCommit": "b24c89ea77eba222d97323de6e07ad9838976e9a",
  "verificationRun": 42152158095,
  "privateCi": "all-six-jobs-passed",
  "hostChecksPassed": 102,
  "managerAndroidApis": [
    29,
    35
  ],
  "coreAndroidApis": [
    29,
    35
  ],
  "browserRegression": "Actual Manager screen resolves protected endpoint, captures verified R2 file URL and imports document-picker APKM on both Android APIs",
  "release": "v0.1.0-alpha.6",
  "sourceZipSha256": "1060cbf9da78676ac9fe4e72106ec69cc8372707f30f7f14c7aa996e90c4a25d",
  "sourceEntriesVerifiedLocally": 7247,
  "realYoutubeLocalScope": "Separate local alpha.3 build, three selected patches, 700 MB configured heap. Patch/sign/install and main navigation after setup passed.",
  "liveApkMirrorTransport": "Actual local alpha.6 Manager downloaded all 169541734 bytes, HTTP 200 and matching source SHA; both CI APIs verified resolved CDN/native picker import",
  "productionSigning": "owner-key-required",
  "productionUpdateFeed": "unconfigured",
  "playback": "not-tested",
  "accountLogin": "not-tested"
}
```

Release assets include the Manager APK, the same working Core APK, branding RVP, complete source ZIP and checksums. INSTALLATION.md has installation, migration and APKMirror fallback steps. FINAL_REPORT.md distinguishes CI fixtures from the separate local real YouTube check. The release receipt is evidence/release-alpha6.json.
