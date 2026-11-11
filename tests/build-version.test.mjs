import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

test('Manager root and application declare the same release version', () => {
  const versions = ['apps/javo-manager/gradle.properties', 'apps/javo-manager/app/gradle.properties']
    .map(file => readFileSync(file, 'utf8').match(/^version\s*=\s*(\S+)\s*$/m)?.[1]);
  assert.ok(versions.every(Boolean), 'Both build scopes must declare a version');
  assert.equal(versions[0], versions[1], 'The application version controls the APK identity');
});
