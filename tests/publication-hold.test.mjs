import test from 'node:test';
import assert from 'node:assert/strict';
import {validateCatalog} from '../packages/release-schema/validate.mjs';

test('Core-only review release refuses a Manager artifact before rendering download links',()=>{
  assert.throws(()=>validateCatalog({schemaVersion:1,status:'core-alpha-manager-review',limitations:[],artifacts:[{id:'manager'}]}),/Manager distribution is held/);
  assert.doesNotThrow(()=>validateCatalog({schemaVersion:1,status:'core-alpha-manager-review',limitations:[],artifacts:[]}));
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
