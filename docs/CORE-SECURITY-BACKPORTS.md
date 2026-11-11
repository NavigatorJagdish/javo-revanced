# Core security backport review

The pinned ReVanced Core base is `80bfe22cfaf0de84b6c1e96fe3435877d3b9e08c`.
The review compares it with the microG `v0.3.16.1.252432` security fixes.
Exact commit IDs and deliberate source moves/removals are recorded in
`packages/source-provenance/security-backports.json`. Copyright and license notices remain.

| Upstream advisory / area | Source changes under validation |
| --- | --- |
| [Work-account token access](https://github.com/microg/GmsCore/security/advisories/GHSA-qqqw-h762-w2hw) | Replaced the separate work authenticator with the regular caller-aware authenticator. Adapted account creation to the older nullable API and removed creation-token logging. |
| [Block Store isolation](https://github.com/microg/GmsCore/security/advisories/GHSA-gxx5-cwvx-xch5) | Ownership includes the package delimiter. Added regression vectors for package prefixes and siblings. |
| [Consent UI spoofing](https://github.com/microg/GmsCore/security/advisories/GHSA-rp4p-8x89-jhv5) | Consent and override activities are private. Recovery uses the upstream immutable PendingIntent wrapper. |
| [Check-in callback](https://github.com/microg/GmsCore/security/advisories/GHSA-wrpf-76x8-cmh6) | Removed the caller-supplied service callback intent. |
| [Assisted sign-in](https://github.com/microg/GmsCore/security/advisories/GHSA-hgxr-rxcr-m6vf) | Compare the supplied client package with the actual calling activity. |
| [Location injection](https://github.com/microg/GmsCore/security/advisories/GHSA-frxh-v6rf-xfg4) and [cache injection](https://github.com/microg/GmsCore/security/advisories/GHSA-29m7-f2vx-8cqq) | Separate internal intent delivery into a private service and UID-checked binder. Location providers are private; duplicate Huawei declarations are removed. |
| [SafetyNet caller identity](https://github.com/microg/GmsCore/security/advisories/GHSA-phcc-cx4h-vmq3) | Added the upstream calling-package check. The unrelated removal of deprecated attestation code is not imported. No attestation certification is claimed. |
| [GServices access](https://github.com/microg/GmsCore/security/advisories/GHSA-h23g-4pvv-5hw9) | Restrict reads/writes and android_id access. Remove configuration-value logging. |
| MCS transport | Import explicit TLS handshake and hostname verification from upstream commit `7df7e922e7c733db38e682e7cdf2a6ebb4bd36f6`. |
| FIDO UI | Adapt the opaque consent UI change from commit `0347b71d80ad58948ee3583fc085253ef6de614f` to the older activity. Its newer hybrid activity is absent from this base. |

The Vending-only asset-pack, billing and Play Integrity fixes were reviewed but not imported.
No Vending APK is distributed by this project; its source is not certified by the Core review.

Validation includes compiled-manifest checks for eight private components, isolated external
start probes, protobuf malformed-input tests and Block Store ownership tests. Test results
must match the actual APK checksum before being reported as passed. Real account consent,
work-profile provisioning, FIDO hardware, push delivery and location behavior still require
integration testing. Source inspection and a successful build do not prove that an exploit
chain is impossible or that Play Protect will allow installation.

The legacy Cast Jackson mapper and address formatter's SnakeYAML advisory matches remain
open. No clean dependency verdict, complete upstream rebase or Play Protect clearance is
claimed. Manager publication remains held. The owner reported a safe phone result for the exact offline-signed Core javo.6 candidate; that does not certify every device or close these advisories.
