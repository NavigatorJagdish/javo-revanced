// Javo additions 2026-11-03. SPDX-License-Identifier: GPL-3.0-only
package app.javo.branding
import app.revanced.patcher.patch.resourcePatch

// Reviewed against pinned Patcher 22.0.2-dev.1 source. Full patch execution remains unverified.
val javoVideoBranding = resourcePatch(
    name = "Javo Video branding",
    description = "Sets Javo Video name and launcher icon. Does not alter playback or service identities.",
    use = false,
) {
    compatibleWith("com.google.android.youtube")
    apply {
        BrandingResources.apply(get("").toPath(), "com.google.android.youtube", "Javo Video",
            BrandingResources::class.java.getResourceAsStream("/javo-video.xml"))
    }
}
val javoMusicBranding = resourcePatch(
    name = "Javo Music branding",
    description = "Sets Javo Music name and launcher icon. Does not alter playback or service identities.",
    use = false,
) {
    compatibleWith("com.google.android.apps.youtube.music")
    apply {
        BrandingResources.apply(get("").toPath(), "com.google.android.apps.youtube.music", "Javo Music",
            BrandingResources::class.java.getResourceAsStream("/javo-music.xml"))
    }
}
