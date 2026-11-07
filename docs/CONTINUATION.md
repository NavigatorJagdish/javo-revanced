# Continuation state

```json
{
  "updatedAt": "2026-11-07",
  "readiness": "functional-alpha.7",
  "repo": "NavigatorJagdish/javo-revanced",
  "verifiedBuildCommit": "9e47507d290319e50ad01be496379ae75e4ee24a",
  "verificationRun": 42360099255,
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
  "browserRegression": "HTTP 503 retry, late source error suppression, trusted URL handoff, and document-picker APKM import on both Android APIs",
  "release": "v0.1.0-alpha.7",
  "sourceZipSha256": "fdf71654e6511c62fd5db40276c00d2f2a9008abd2f731537f9f3faca4d6080b",
  "sourceEntriesVerifiedByWorkflow": 7246,
  "branding": "Mandatory built-in Video and Music branding checked on both Android APIs",
  "realYoutubeLocalScope": "Same-source local Manager patched, signed, installed and launched YouTube 20.40.45 with two upstream patches and automatic Javo branding. The online home feed still displayed a YouTube illustration.",
  "corePlayProtect": "Owner reported a safe scan and successful installation",
  "productionSigning": "owner-key-required",
  "productionUpdateFeed": "unconfigured",
  "playback": "not-tested",
  "accountLogin": "not-tested"
}
```

Release assets include the Manager APK, Core APK, standalone branding RVP for external patchers, complete source ZIP and checksums. INSTALLATION.md covers the alpha.6 in-place update, repatching and APKMirror retry. The release receipt is evidence/release-alpha7.json.
