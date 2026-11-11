import { readFile, writeFile, mkdir, cp, rm } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { products, pages } from './content.mjs';
import { gallery } from './gallery.mjs';
import { validateCatalog } from '../../packages/release-schema/validate.mjs';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '../..');
const dist = path.join(here, 'dist');
const catalog = JSON.parse(await readFile(path.join(root, 'infrastructure/release/catalog.json'), 'utf8'));
validateCatalog(catalog);
const site = process.env.SITE_ORIGIN ? new URL(process.env.SITE_ORIGIN) : null;
if (site) {
  if (site.protocol !== 'https:' || site.username || site.password || site.search || site.hash) {
    throw Error('SITE_ORIGIN must be an HTTPS site URL without credentials, query or fragment');
  }
  if (!site.pathname.endsWith('/')) site.pathname += '/';
}
const absoluteUrl = route => new URL(route.slice(1), site).href;
const sitePath = route => site.pathname.slice(0, -1) + route;
const projectUrl = 'https://github.com/NavigatorJagdish/javo-revanced';
await mkdir(dist, { recursive: true });
await cp(path.join(root, 'assets/logos'), path.join(dist, 'assets/logos'), { recursive: true });
const screenshots = path.join(dist, 'assets/screenshots');
await mkdir(screenshots, { recursive: true });
for (const filename of ['manager-emulator.png', 'core-emulator.png', 'website-desktop.png', 'website-mobile-menu.png']) {
  await cp(path.join(root, 'evidence/screenshots', filename), path.join(screenshots, filename));
}
for (const f of ['styles.css', 'app.js']) await cp(path.join(here, f), path.join(dist, f));
await cp(path.join(root, 'infrastructure/release/catalog.json'), path.join(dist, 'catalog.json'));
const tokens = JSON.parse(await readFile(path.join(root, 'packages/design-tokens/tokens.json'), 'utf8'));
await writeFile(path.join(dist, 'tokens.css'), `:root{${Object.entries(tokens).map(([k,v]) => `--${k}:${v}`).join(';')}}`);
export const escapeHtml = s => String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
function searchMetadata(title, description, route) {
  if (route === '/404.html') return '<meta name="robots" content="noindex,follow">';
  if (!site) return '';
  const url = absoluteUrl(route);
  const image = absoluteUrl('/assets/logos/javo-manager.svg');
  const data = {
    '@context': 'https://schema.org',
    '@type': route === '/' ? 'WebSite' : 'WebPage',
    name: route === '/' ? 'Javo ReVanced' : title,
    description,
    url,
    inLanguage: 'en',
    '@id': url + '#page',
    ...(route === '/' ? {sameAs: projectUrl} : {isPartOf: {'@type': 'WebSite', name: 'Javo ReVanced', url: site.href}}),
  };
  const product = products.find(p => route === `/apps/${p.id}/`);
  const artifact = product && catalog.artifacts.find(a => a.id === product.id);
  if (product && ['manager', 'core'].includes(product.id)) {
    data.mainEntity = {
      '@type': 'SoftwareApplication',
      '@id': url + '#application',
      name: product.name,
      description: product.details,
      applicationCategory: 'UtilitiesApplication',
      operatingSystem: product.id === 'manager' ? 'Android 8.0 or later' : 'Android 5.0 or later',
      url,
      ...(artifact ? {softwareVersion: artifact.version, downloadUrl: absoluteUrl('/downloads/') + '#' + product.id, releaseNotes: artifact.downloadUrl?.replace(/\/download\/([^/]+)\/[^/]+$/, '/tag/$1')} : {}),
      license: product.id === 'manager' ? 'https://www.gnu.org/licenses/gpl-3.0.html' : 'https://www.apache.org/licenses/LICENSE-2.0',
    };
  }
  if (route !== '/') {
    data.breadcrumb = {
      '@type': 'BreadcrumbList',
      itemListElement: [
        {'@type': 'ListItem', position: 1, name: 'Javo ReVanced', item: site.href},
        {'@type': 'ListItem', position: 2, name: title, item: url},
      ],
    };
  }
  return [
    ...(route === '/' ? ['<meta name="google-site-verification" content="qCCOz6yJWe6uQb7NbwdPed01EMd47sy8tV6iI8g6eWQ">'] : []),
    `<link rel="canonical" href="${escapeHtml(url)}">`,
    `<meta property="og:url" content="${escapeHtml(url)}">`,
    '<meta property="og:type" content="website">',
    '<meta property="og:site_name" content="Javo ReVanced">',
    `<meta property="og:image" content="${escapeHtml(image)}">`,
    `<script type="application/ld+json">${JSON.stringify(data).replaceAll('<', '\\u003c')}</script>`,
  ].join('');
}
const icon = (id, cls='') => `<img class="${cls}" src="/assets/logos/javo-${id}.svg" alt="" width="80" height="80">`;
const link = (url, text, cls='') => `<a class="${cls}" href="${url}">${text}</a>`;
const button = (url, text) => link(url, text+' <span aria-hidden="true">↗</span>', 'button');
const cards = () => products.map(p=>`<article class="product ${p.color}">${icon(p.id)}<p class="eyebrow">${p.eyebrow}</p><h3>${p.name}</h3><p>${p.text}</p>${link('/apps/'+p.id+'/', 'Explore '+p.name+' <span aria-hidden="true">↗</span>', 'text-link')}</article>`).join('');
const productStatus = p => p.id==='manager' && catalog.status==='experimental-review'
  ? 'Experimental review APK. Play Protect harmful indication unresolved; Google appeal submitted. Keep protection enabled and stop if blocked.'
  : 'Functional alpha. See the build and test evidence before relying on a capability.';
const shell = (title, description, content, route) => `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="description" content="${escapeHtml(description)}">${searchMetadata(title, description, route)}<meta name="theme-color" content="#060710"><meta property="og:title" content="${escapeHtml(title)} · Javo ReVanced"><meta property="og:description" content="${escapeHtml(description)}"><title>${escapeHtml(title)} · Javo ReVanced</title><link rel="icon" type="image/svg+xml" href="/assets/logos/javo-manager.svg"><link rel="stylesheet" href="/tokens.css"><link rel="stylesheet" href="/styles.css"><script src="/app.js" defer></script></head><body><a class="skip" href="#main">Skip to content</a><header class="header"><a href="/" class="brand" aria-label="Javo ReVanced home">${icon('manager')}<span>JAVO <b>REVANCED</b></span></a><button class="menu" aria-label="Open navigation" aria-controls="nav" aria-expanded="false">☰</button><nav id="nav" aria-label="Main navigation">${[['/','Home'],['/apps/','Apps'],['/downloads/','Downloads'],['/docs/','Docs']].map(([u,t])=>`<a href="${u}" ${u===route?'aria-current="page"':''}>${t}</a>`).join('')}<button id="theme" class="icon-button" aria-label="Switch color theme">◐</button></nav></header><main id="main">${content}</main><footer><div><a href="/" class="brand">${icon('manager')}<span>JAVO REVANCED</span></a><p>Patch. Manage. Customize. Control.</p><small>Independent project. No affiliation with Google, YouTube, ReVanced or microG.</small></div><div><p class="eyebrow">EXPLORE</p>${link('/apps/','The ecosystem')}${link('/downloads/','Downloads')}${link('/changelog/','Changelog')}</div><div><p class="eyebrow">RESOURCES</p>${link('/docs/','Documentation')}${link('/faq/','FAQ')}${link('/docs/troubleshooting/','Troubleshooting')}</div><div><p class="eyebrow">PROJECT</p>${link('/about/','About')}${link('/licenses/','Licenses & source')}${link('/security/','Security')}${link('/privacy/','Privacy')}</div></footer></body></html>`;
const hero = `<section class="hero"><div class="hero-copy"><p class="eyebrow"><span class="status-dot"></span> YOUR ANDROID. YOUR CHOICES.</p><h1>JAVO<br><span>REVANCED</span></h1><p class="eyebrow">More Freedom For Your Android.</p><p class="lead">Javo Manager is an independent ReVanced fork for patching compatible YouTube and YouTube Music APKs on Android.</p><div class="actions">${button('/downloads/','View Manager review APK')}${link('/apps/','Explore the ecosystem →','secondary-button')}</div><p class="caption">Open-source foundations. Local APK processing. User control.</p></div><div class="hero-art" aria-hidden="true"><div class="orbit orbit-one"></div><div class="orbit orbit-two"></div><div class="art-card">${icon('manager','hero-mark')}</div><span class="floating one">YOUR PATCHES</span><span class="floating two">YOUR CONTROL</span><span class="art-label">JAVO / MANAGER</span></div></section>`;
const home = `${hero}<section class="release-note"><span class="status-dot"></span><p><strong>Development build</strong> · Follow the evidence, from source to APK.</p>${link('/downloads/','Build status ↗')}</section><section class="section"><div class="section-heading"><div><p class="eyebrow">ONE ECOSYSTEM. FOUR IDENTITIES.</p><h2>A setup that feels like yours.</h2></div><p>One control center. Distinct experiences.<br>Built on genuine open-source foundations.</p></div><div class="products">${cards()}</div></section>${gallery}<section class="workflow section"><div><p class="eyebrow">FROM SOURCE TO SOMETHING PERSONAL</p><h2>Make the choices.<br>Keep the control.</h2><p class="lead">Choose a compatible APK and trusted patch bundle. Inspect every option. Keep the signing identity that belongs to your build.</p>${link('/docs/installation/','Read the installation guide ↗','text-link')}</div><ol><li><span>01</span><div><h3>Bring your APK</h3><p>Select a supported, complete APK using Android’s file picker.</p></div></li><li><span>02</span><div><h3>Choose supported patches</h3><p>Use real bundle metadata and compatibility requirements.</p></div></li><li><span>03</span><div><h3>Build. Verify. Install.</h3><p>Process locally, sign the result, then use Android’s approval flow.</p></div></li></ol></section><section class="section transparency"><p class="eyebrow">BUILT WITH CLARITY</p><h2>No mystery downloads.</h2><p>Only real artifacts belong in Downloads. Capabilities, build results and verification limits are recorded in the evidence package.</p><div class="actions">${button('/security/','How verification works')}${link('/licenses/','Meet the foundations →','secondary-button')}</div></section>`;
const routes = new Map([['/', ['Android APK patcher and downloads', 'Javo Manager is an independent ReVanced-based APK patcher for Android. Patch compatible YouTube and YouTube Music APKs locally and check the Manager and Core release status.', home]], ['/apps/', ['The ecosystem', 'Four distinct Javo identities.', `<section class="page-head"><p class="eyebrow">JAVO ECOSYSTEM</p><h1>Four identities.<br><span>Your Android.</span></h1><p class="lead">Explore each product and its current verification status.</p></section><section class="section products">${cards()}</section>`]]]);
for (const p of products) routes.set(`/apps/${p.id}/`, [p.id==='manager' ? 'Javo Manager APK — Android patch manager' : p.id==='core' ? 'Javo Core APK — microG compatibility service' : p.name,p.text,`<section class="product-hero ${p.color}">${icon(p.id)}<p class="eyebrow">${p.eyebrow}</p><h1>${p.name}</h1><p class="lead">${p.text}</p><div class="actions">${button('/downloads/','View build status')}${link('/docs/','Documentation →','secondary-button')}</div></section><section class="article"><h2>Built on real foundations.</h2><p>${p.details}</p><aside>${productStatus(p)}</aside><h2>Designed to belong.</h2><p>Distinct ${p.id==='core'?'cyan and green':p.id==='video'?'orange and red':'purple, pink and blue'} vector artwork. Near-black surfaces. Clear controls, readable text and a consistent Javo identity.</p>${link('/security/','Read the verification policy ↗','text-link')}</section>`]);
for(const [route,[title,desc,sections]] of Object.entries(pages)) routes.set(route,[title,desc,`<section class="page-head"><p class="eyebrow">JAVO / RESOURCES</p><h1>${title}</h1><p class="lead">${desc}</p></section><section class="article">${sections.map(([h,p])=>`<section><h2>${h}</h2><p>${p}</p></section>`).join('')}${route==='/licenses/'?`<div class="actions">${link('https://github.com/ReVanced/revanced-manager','ReVanced Manager ↗','secondary-button')}${link('https://github.com/ReVanced/GmsCore','ReVanced GmsCore ↗','secondary-button')}${link('https://github.com/microg/GmsCore','microG ↗','secondary-button')}</div>`:''}</section>`]);
const releaseReview = catalog.status === 'release-review';
const downloadCards = releaseReview ? `<aside id="core" class="download-card"><h2>APK downloads are under review</h2><p>This is the official Javo Core download destination. Manager alpha.10, alpha.11 and alpha.13 were reported blocked by Google Play Protect. Keep Play Protect enabled and wait for the security review before installing.</p>${link('/security/','Read the security status ↗','text-link')}</aside>`
  : catalog.artifacts.length ? catalog.artifacts.map(a=>`<article id="${escapeHtml(a.id)}" class="download-card"><h2>${escapeHtml(a.displayName)}</h2>${a.securityReview?.playProtect==='harmful-reported' ? `<p role="note"><strong>Experimental review APK — Play Protect harmful indication unresolved.</strong> This file is available for review, with no clean verdict. Keep protection enabled and stop installation if blocked. ${link('/security/','Read the security review')}</p>` : ''}<p>${escapeHtml(a.version)} · ${escapeHtml(a.channel)} · Android API ${a.minSdk}+</p><p>${escapeHtml(a.signing)} signed · Signature and package checked</p><code>${a.sha256}</code>${button(site ? (a.downloadUrl ?? projectUrl + '/releases') : a.path,a.securityReview?.playProtect==='harmful-reported' ? 'Download Manager review APK' : 'Download '+escapeHtml(a.displayName))}</article>`).join('') : `<div class="empty"><span class="empty-symbol">◇</span><h2>No verified APKs published yet.</h2><p>Source code and build evidence are being prepared. Download buttons appear only when a real artifact passes the packaging checks.</p>${link('/docs/','Read the build documentation ↗','text-link')}</div>`;
const downloads = (catalog.status === 'core-alpha-manager-review' ? `<aside id="manager" class="download-card"><h2>Manager download is held</h2><p>The owner reported that Play Protect still indicated Manager alpha.13 as harmful. Its download is withheld. Keep protection enabled. Core availability and its separate verification limits are listed below.</p>${link('/security/','Read the security status ↗','text-link')}</aside>` : '') + downloadCards;
routes.set('/downloads/',['Download Javo Manager and Core APKs','Check Javo Manager and Javo Core release availability, Android compatibility, signatures and SHA-256 before installing.',`<section class="page-head"><p class="eyebrow">JAVO / DOWNLOADS</p><h1>Know what<br><span>you install.</span></h1><p class="lead">Verified artifact details, checksums and honest build status.</p></section><section class="article">${downloads}<h2>Current verification limits</h2><ul>${catalog.limitations.map(x=>`<li>${escapeHtml(x)}</li>`).join('')}</ul><p>Javo Video and Music are created from user-supplied APKs. Proprietary APKs are not redistributed.</p></section>`]);
routes.set('/404.html',['Page not found','This page does not exist.',`<section class="page-head"><p class="eyebrow">404</p><h1>Off the path.</h1><p class="lead">This page does not exist.</p>${button('/','Back to Javo')}</section>`]);
for (const [route, [title, description, content]] of routes) {
  const out = path.join(dist, route.endsWith('.html') ? route.slice(1) : path.join(route.slice(1), 'index.html'));
  await mkdir(path.dirname(out), { recursive: true });
  const html = shell(title, description, content, route);
  // GitHub project pages live below /repository/. Rewrite only local HTML URLs.
  const deployedHtml = site ? html.replace(/\b(href|src|data-gallery-image)="(\/(?!\/)[^"]*)"/g,
    (_match, attribute, url) => `${attribute}="${sitePath(url)}"`) : html;
  await writeFile(out, deployedHtml);
}
const sitemap = path.join(dist, 'sitemap.xml');
await writeFile(path.join(dist, 'robots.txt'), site ? `User-agent: *
Allow: /
Sitemap: ${absoluteUrl('/sitemap.xml')}
` : 'User-agent: *\nAllow: /\n');
if (site) {
  const locations = [...routes.keys()].filter(route => route !== '/404.html');
  await writeFile(sitemap, `<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">${locations.map(route => `<url><loc>${escapeHtml(absoluteUrl(route))}</loc></url>`).join('')}</urlset>`);
} else {
  await rm(sitemap, { force: true });
}
console.log(`Built ${routes.size} static pages. Catalog contains ${catalog.artifacts.length} verified artifacts.`);
