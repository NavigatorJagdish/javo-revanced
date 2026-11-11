# Runtime dependency review — 11 November 2026

The current release-runtime Gradle graphs resolved 385 Manager and 191 Core package/version pairs, with 536 distinct pairs across both products. Both dependency-report tasks completed successfully. An OSV Maven batch query against those selected versions found no matches for Manager and three distinct advisories affecting two Core dependencies. This is a time-specific dependency lookup, not a malware or Play Protect verdict; build plugins and optional Vending distributions are outside this runtime scan.

| Remaining Core dependency | Review and remaining work |
| --- | --- |
| `org.codehaus.jackson:jackson-mapper-asl:1.9.13` | [GHSA-c27h-mcmw-48hv](https://github.com/advisories/GHSA-c27h-mcmw-48hv), [GHSA-r6j9-8759-g62w](https://github.com/advisories/GHSA-r6j9-8759-g62w). Brought in by the legacy Cast library. Inspected usage has explicit named subtypes; default typing and the XML targets involved in the advisories were not observed. Replacing the mapper requires a tested Cast dependency migration. These findings remain open; inspection is not proof that every input is safe. |
| `org.yaml:snakeyaml:1.33` | [GHSA-mjmj-j48q-9wg2](https://github.com/advisories/GHSA-mjmj-j48q-9wg2). The address formatter uses `SafeConstructor` and bundled templates. Its old no-argument constructor is incompatible with SnakeYAML 2.x. A compatible formatter migration and malformed-input testing are required before this match can be closed. |

The updated graphs select Wire runtime 6.4.5 and the newer pinned Manager security dependencies. The older Wire 4.9.9 source generator remains a build-time compatibility dependency and is not covered by the runtime-only result. Upstream Core security backports and their validation scope are documented separately in `CORE-SECURITY-BACKPORTS.md`.

Do not describe these results as “zero vulnerabilities”, “Google approved”, or safe on every device. Exact signed APK identity, runtime behavior and Google's classification need separate evidence.
