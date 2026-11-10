import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile, readdir, stat } from 'node:fs/promises';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
const root=path.resolve('apps/javo-website/dist');
execFileSync(process.execPath,['apps/javo-website/build.mjs'],{stdio:'pipe'});
async function allFiles(dir){const out=[];for(const f of await readdir(dir,{withFileTypes:true})){const p=path.join(dir,f.name);out.push(...(f.isDirectory()?await allFiles(p):[p]));}return out;}
const htmlFiles=(await allFiles(root)).filter(x=>x.endsWith('.html'));
test('builds all 16 requested pages plus a not-found page',()=>assert.equal(htmlFiles.length,17));
test('every internal link and asset resolves',async()=>{for(const file of htmlFiles){const html=await readFile(file,'utf8');for(const [,url] of html.matchAll(/(?:href|src)="([^"]+)"/g)){if(!url.startsWith('/'))continue;const out=path.join(root,url.endsWith('/')?url+'index.html':url);assert.ok((await stat(out)).isFile(),`${file}: ${url}`);}}});
test('every page has accessibility and SEO foundations',async()=>{for(const file of htmlFiles){const html=await readFile(file,'utf8');assert.match(html,/<html lang="en">/);assert.match(html,/<h1>/);assert.match(html,/name="description"/);assert.match(html,/Skip to content/);assert.match(html,/aria-controls="nav"/);}});
test('generated website contains no legacy brand',async()=>{for(const file of htmlFiles)assert.doesNotMatch(await readFile(file,'utf8'),/NexVanced/i);});
test('downloads only link to catalog APKs when distribution is open',async()=>{const html=await readFile(path.join(root,'downloads/index.html'),'utf8');const catalog=JSON.parse(await readFile('infrastructure/release/catalog.json','utf8'));const urls=[...html.matchAll(/href="([^\"]+\.apk)"/g)].map(x=>x[1]);assert.deepEqual(urls,catalog.status==='release-review'?[]:catalog.artifacts.map(a=>a.path));if(catalog.status==='release-review')assert.match(html,/APK downloads are under review/);});
test('reduced-motion and small-screen styles are included',async()=>{const css=await readFile(path.join(root,'styles.css'),'utf8');assert.match(css,/prefers-reduced-motion/);assert.match(css,/max-width:420px/);assert.match(css,/:focus-visible/);});
