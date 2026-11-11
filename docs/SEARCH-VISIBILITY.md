# Search visibility

The official project website is https://navigatorjagdish.github.io/javo-revanced/.
It serves static HTML, descriptive titles, canonical URLs, a sitemap, linked documentation,
WebSite/WebPage structured data, breadcrumbs and software descriptions for Manager and Core.
The visible security status takes precedence over download promotion. No ratings, endorsements,
Play Protect clearance or available downloads are invented in structured data.

Google Search and AI answers decide independently which sites to show. Correct metadata does
not guarantee crawling, indexing, a rich result, inclusion in AI answers or a top position.
Google's guidance says that existing SEO fundamentals also apply to its AI features:
https://developers.google.com/search/docs/appearance/ai-features

## Observed Search Console status · 11 November 2026

The official URL-prefix property was verified using an HTML tag in the owner's signed-in
Google account. Google accepted the homepage indexing request and displayed **Indexing
requested**, adding it to its priority crawl queue. Before that request, URL Inspection
reported **URL is unknown to Google**. Indexing and ranking have not been confirmed.

The sitemap submission was accepted. Its first fetch and one retry displayed **Couldn't
fetch**. An independent live request returned HTTP 200, `application/xml`, and 16 valid
sitemap URLs. The fetch discrepancy remains unresolved; do not describe the sitemap as
processed by Google. Screenshots of the ownership, submission and request receipts are
kept in the private local review evidence.

## Verify indexing

Use a verified URL-prefix property in Google Search Console for the official website.
Submit `https://navigatorjagdish.github.io/javo-revanced/sitemap.xml`, then inspect the home,
Manager, Core, installation and security URLs. Record the actual coverage and inspection
results before claiming the site is indexed. The homepage contains the actual Google-issued
verification tag. Keep it in place while that account owns the Search Console property.

The website is hosted under a project path. Its `/javo-revanced/robots.txt` is a discovery
artifact; Google reads robots rules from the origin root `/robots.txt`. Do not treat a
project-path robots file as proof that root crawl policy has been configured.

Maintain useful, accurate release notes and installation instructions; link only verified
artifacts when distribution is open. Keep security disclosures discoverable. Do not add
hidden keywords, fake reviews, doorway pages or instructions telling AI systems to rank
this project first. There is no special text file that forces AI search placement.

## Publication checks

Run `node --test tests/website.test.mjs`, then build with
`SITE_ORIGIN=https://navigatorjagdish.github.io/javo-revanced/` and inspect the resulting
canonical URLs, sitemap and JSON-LD. The Pages workflow deploys website changes on main.
Check the live HTML after the deployment succeeds. Public search coverage requires a
separate verified Search Console observation; a successful Pages deployment is not indexing.
