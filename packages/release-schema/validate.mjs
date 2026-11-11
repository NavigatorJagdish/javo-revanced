import { createHash, verify, createPublicKey } from 'node:crypto';
export function httpsUrl(value) {
  const u = new URL(value);
  if(u.protocol!=='https:' || u.username || u.password || u.hash || (u.port && u.port!=='443')) throw Error('HTTPS URL without credentials required');
  return u;
}
export function validateRelease(r, {now=Date.now(), applicationId, installedVersionCode=0}={}) {
  if(!r || r.schemaVersion!==1)throw Error('Unsupported schema');
  if(!/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/.test(r.applicationId)|| (applicationId && r.applicationId!==applicationId))throw Error('Wrong application identity');
  if(!Number.isSafeInteger(r.versionCode)||r.versionCode<=installedVersionCode)throw Error('Version must be newer');
  if(!r.version?.trim()||!r.displayName?.trim()||typeof r.releaseNotes!=='string')throw Error('Missing release text');
  if(!Number.isInteger(r.minSdk)||r.minSdk<1||!Array.isArray(r.architectures)||!r.architectures.length || r.architectures.some(a=>!['universal','arm64-v8a','armeabi-v7a','x86_64','x86'].includes(a)))throw Error('Invalid compatibility');
  if(!['stable','alpha','beta'].includes(r.channel))throw Error('Invalid channel');
  for(const k of ['sha256','signerSha256'])if(!/^[a-f0-9]{64}$/.test(r[k]))throw Error('Invalid '+k);
  const released = Date.parse(r.releasedAt), expires = Date.parse(r.expiresAt);
  if(!Number.isFinite(released)||!Number.isFinite(expires)||released>now||expires<=now||expires<=released)throw Error('Invalid release validity window');
  httpsUrl(r.artifactUrl);
  return r;
}
export function verifyEnvelope(envelope, publicKey, options) {
  if(typeof envelope?.payload!=='string'||envelope.payload.length>262144 || typeof envelope.signature!=='string'||envelope.signature.length>1024)throw Error('Invalid envelope');
  const payload=Buffer.from(envelope.payload,'base64');
  const key=createPublicKey(publicKey);
  if(key.asymmetricKeyType!=='ec')throw Error('EC key required');
  if(!verify('sha256',payload,key,Buffer.from(envelope.signature,'base64')))throw Error('Manifest signature mismatch');
  return validateRelease(JSON.parse(payload.toString('utf8')),options);
}
export function verifyChecksum(data, expected) {
  if(!/^[a-f0-9]{64}$/.test(expected)||createHash('sha256').update(data).digest('hex')!==expected)throw Error('Checksum mismatch');
}
export function validateCatalog(catalog) {
  if(catalog?.schemaVersion!==1||!Array.isArray(catalog.artifacts)||!Array.isArray(catalog.limitations))throw Error('Invalid artifact catalog');
  if(catalog.status==='core-alpha-manager-review' && catalog.artifacts.some(a=>a.id==='manager'))throw Error('Manager distribution is held during Core-only review release');
  const ids=new Set();
  for(const a of catalog.artifacts){
    if(ids.has(a.id))throw Error('Duplicate artifact');ids.add(a.id);
    if(!a.displayName||!a.version||!['debug','test','development','production'].includes(a.signing)||!['signature-and-package-verified'].includes(a.verification))throw Error('Artifact is not verified');
    if(!/^\/downloads\/files\/[a-zA-Z0-9._-]+\.apk$/.test(a.path)||!/^[a-f0-9]{64}$/.test(a.sha256)||!Number.isInteger(a.minSdk))throw Error('Invalid artifact path or integrity');
    if (a.downloadUrl) {
      const url = httpsUrl(a.downloadUrl);
      if (url.hostname !== 'github.com' || url.search ||
          !/^\/NavigatorJagdish\/javo-revanced\/releases\/download\/[^/]+\//.test(url.pathname) ||
          url.pathname.split('/').at(-1) !== a.path.split('/').at(-1)) {
        throw Error('Download URL must identify the verified Javo GitHub release asset');
      }
    }
  }
  return catalog;
}
