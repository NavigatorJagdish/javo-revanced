import test from 'node:test';
import assert from 'node:assert/strict';
import {validateCatalog} from '../packages/release-schema/validate.mjs';

test('Core-only review release refuses a Manager artifact before rendering download links',()=>{
  assert.throws(()=>validateCatalog({schemaVersion:1,status:'core-alpha-manager-review',limitations:[],artifacts:[{id:'manager'}]}),/Manager distribution is held/);
  assert.doesNotThrow(()=>validateCatalog({schemaVersion:1,status:'core-alpha-manager-review',limitations:[],artifacts:[]}));
});
