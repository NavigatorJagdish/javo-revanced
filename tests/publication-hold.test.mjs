import test from 'node:test';
import assert from 'node:assert/strict';
import {validateCatalog} from '../packages/release-schema/validate.mjs';

test('Core-only review release refuses a Manager artifact before rendering download links',()=>{
  assert.throws(()=>validateCatalog({schemaVersion:1,status:'core-alpha-manager-review',limitations:[],artifacts:[{id:'manager'}]}),/Manager distribution is held/);
  assert.doesNotThrow(()=>validateCatalog({schemaVersion:1,status:'core-alpha-manager-review',limitations:[],artifacts:[]}));
});

test('new review APK retains prior warning without inheriting a verdict for different bytes',()=>{
  const artifact={id:'manager',displayName:'Javo Manager',version:'next',signing:'development',verification:'signature-and-package-verified',path:'/downloads/files/javo-manager.apk',sha256:'a'.repeat(64),minSdk:26,securityReview:{playProtect:'not-cleared',distribution:'review-only',previousBlockedVersion:'0.1.0-alpha.13'}};
  const catalog={schemaVersion:1,status:'experimental-review',limitations:[],artifacts:[artifact]};
  assert.doesNotThrow(()=>validateCatalog(catalog));
  delete artifact.securityReview.previousBlockedVersion;
  assert.throws(()=>validateCatalog(catalog),/previously blocked version/);
  artifact.securityReview.previousBlockedVersion='next';
  assert.throws(()=>validateCatalog(catalog),/previously blocked version/);
  artifact.securityReview.previousBlockedVersion='0.1.0-alpha.13';
  artifact.securityReview.playProtect='safe';
  assert.throws(()=>validateCatalog(catalog),/experimental review disclosure/);
  artifact.securityReview.playProtect='not-cleared';
  catalog.status='stable';
  assert.throws(()=>validateCatalog(catalog),/experimental review disclosure/);
});

test('experimental Manager download requires unresolved-warning disclosure',()=>{
  const artifact={id:'manager',displayName:'Javo Manager',version:'test',signing:'development',verification:'signature-and-package-verified',path:'/downloads/files/javo-manager.apk',sha256:'a'.repeat(64),minSdk:26,securityReview:{playProtect:'harmful-reported',distribution:'review-only'}};
  const sample={schemaVersion:1,status:'experimental-review',limitations:[],artifacts:[artifact]};
  assert.doesNotThrow(()=>validateCatalog(sample));
  sample.status='stable';
  assert.throws(()=>validateCatalog(sample),/experimental review disclosure/);
  sample.status='experimental-review';
  artifact.securityReview.distribution='recommended';
  assert.throws(()=>validateCatalog(sample),/experimental review disclosure/);
  delete artifact.securityReview;
  assert.throws(()=>validateCatalog(sample),/experimental review disclosure/);
});
