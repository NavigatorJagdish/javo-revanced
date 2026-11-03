# Design reference analysis

All four original images have been visually inspected. Filename mapping differs from the order in the master prompt:

| File | Reference | Observed design |
| --- | --- | --- |
| 1000329321.png | 01: ecosystem and website | Near-black backgrounds, magenta hairline borders, rounded product panels, large play-symbol hero, horizontal desktop navigation, compact phone lists. |
| 1000329323.png | 02: Manager screens | Five phone layouts: dashboard, searchable app list, patch checkbox list, circular progress with stage details, grouped settings. Purple/pink accents with electric-blue edges. |
| 1000329322.png | 03: Manager branding | Angular triangular play mark inside a broken circular ring, diagonal wrench across its center, magenta-purple-blue highlights; horizontal wordmark and monochrome variant. |
| 1000329138.png | 04: product identity | Ribbon-like orange/red play triangle, violet/blue note with concentric arcs, cyan/green hexagon around a luminous core. |

## Implementation rules

Use #060710 backgrounds, #111426 surfaces, #C026F5 primary, #FF2DAA pink, #289BFF blue, #F8F9FF primary text and #A2A8C1 secondary text. Video uses #FF475F/#FF8A38; Music #A855F7/#3B82F6; Core #11D9CF/#25E89A. Typography: native Android sans-serif and website system sans-serif, avoiding unbundled font requests. Headings use strong weight and restrained letter spacing. Use 8px spacing rhythm, 16–24px card padding, 16–24px radii, 48dp Android touch targets.

Dashboard must show real installed/patch counts. Apps lists show actual packages and loaded bundle metadata. Patch progress follows real events; it must not copy the mockup's 78% value. Settings retain working upstream persistence. Core keeps account, registration and messaging functionality from real source. Never copy the reference's fabricated integrity pass or account identity. Website downloads derive from verified artifact metadata and distinguish upstream sources from Javo releases.

Scalable icons are clean vector interpretations rather than pixel-perfect 3D reproductions. Native accessibility, safe insets, readable small screens, keyboard focus and reduced motion take priority over heavy glow and crowded mockup text. Original concept images are design inputs, never passed off as working-app screenshots.
