import test from 'node:test';
import assert from 'node:assert/strict';
import { generateKeyPairSync, sign, createHash } from 'node:crypto';
import { validateRelease, verifyEnvelope, verifyChecksum, httpsUrl, validateCatalog } from '../packages/release-schema/validate.mjs';
const now=Date.parse('2026-11-03T00:00:00Z');
const release=()=>({schemaVersion:1,applicationId:'app.javo.manager',displayName:'Javo Manager',version:'1.0.1',versionCode:101,minSdk:26,architectures:['universal'],channel:'stable',releasedAt:'2026-11-02T00:00:00Z',expiresAt:'2026-12-01T00:00:00Z',artifactUrl:'https://example.org/javo.apk',sha256:'a'.repeat(64),signerSha256:'b'.repeat(64),releaseNotes:'Security update'});
test('accepts a valid newer release',()=>assert.equal(validateRelease(release(),{now,applicationId:'app.javo.manager',installedVersionCode:100}).versionCode,101));
for(const [label,change] of [
  ['wrong package',{applicationId:'app.other.manager'}],['downgrade',{versionCode:99}],['expired metadata',{expiresAt:'2026-11-01T00:00:00Z'}],['future release',{releasedAt:'2027-01-01T00:00:00Z'}],['invalid timestamp',{expiresAt:'yesterday'}],['unsigned checksum',{sha256:'x'.repeat(64)}],['missing signer',{signerSha256:''}],['HTTP artifact',{artifactUrl:'http://example.org/javo.apk'}],['credential URL',{artifactUrl:'https://user:pass@example.org/javo.apk'}],['invalid ABI',{architectures:['fake']}],['missing architecture',{architectures:[]}],['schema mismatch',{schemaVersion:2}],['fractional version',{versionCode:101.5}]
])test('rejects '+label,()=>assert.throws(()=>validateRelease({...release(),...change},{now,applicationId:'app.javo.manager',installedVersionCode:100})));
const {privateKey,publicKey}=generateKeyPairSync('ec',{namedCurve:'prime256v1'});
const pem=publicKey.export({type:'spki',format:'pem'});
function envelope(r=release()){const bytes=Buffer.from(JSON.stringify(r));return {payload:bytes.toString('base64'),signature:sign('sha256',bytes,privateKey).toString('base64')};}
test('authenticates signed metadata',()=>assert.equal(verifyEnvelope(envelope(),pem,{now}).applicationId,'app.javo.manager'));
test('rejects tampered signed payload',()=>{const e=envelope();e.payload=Buffer.from(JSON.stringify({...release(),versionCode:999})).toString('base64');assert.throws(()=>verifyEnvelope(e,pem,{now}));});
test('rejects signature from a different key',()=>{const other=generateKeyPairSync('ec',{namedCurve:'prime256v1'});assert.throws(()=>verifyEnvelope(envelope(),other.publicKey.export({type:'spki',format:'pem'}),{now}));});
test('rejects oversized metadata before parsing',()=>assert.throws(()=>verifyEnvelope({payload:'a'.repeat(262145),signature:'a'},pem,{now})));
test('checksum catches altered bytes',()=>{const bytes=Buffer.from('genuine test bytes');const hash=createHash('sha256').update(bytes).digest('hex');verifyChecksum(bytes,hash);assert.throws(()=>verifyChecksum(Buffer.from('altered'),hash));});
for(const url of ['file:///tmp/a.apk','javascript:alert(1)','https://example.org:8080/a.apk','https://example.org/a.apk#fragment'])test('rejects unsafe update URL '+url,()=>assert.throws(()=>httpsUrl(url)));
test('empty catalog is valid without fake artifacts',()=>validateCatalog({schemaVersion:1,artifacts:[],limitations:[]}));
test('rejects a catalog with unverified downloads',()=>assert.throws(()=>validateCatalog({schemaVersion:1,limitations:[],artifacts:[{id:'manager',displayName:'Manager',version:'1',signing:'production',verification:'not-tested'}]})));
