"""Reuse the working alpha.2 Core binary after CI verifies its source tree is unchanged."""
from pathlib import Path
import hashlib
import json
import os
import shutil
import zipfile

root = Path(__file__).resolve().parents[2]
archive = root / 'tools/core-alpha2/javo-revanced-development.zip'
with archive.open('rb') as source:
    assert hashlib.file_digest(source, 'sha256').hexdigest() == '258b44552a485fb2792b2f53571781affe27f4a0e89576b4e12a4b23722aca25'
outputs = list((root / 'apps/javo-core/play-services-core/build/outputs/apk/vtmDefault/release').glob('*.apk'))
assert len(outputs) == 1
with zipfile.ZipFile(archive) as source:
    with source.open('apks/javo-core-0.3.13.2.250932-javo.2-test.apk') as stream, outputs[0].open('wb') as target:
        shutil.copyfileobj(stream, target)
with outputs[0].open('rb') as stream:
    digest = hashlib.file_digest(stream, 'sha256').hexdigest()
assert digest == '4316c1a5778e66fadd7d3d28fe689a2d74fb7c601ec36504998622cd78aaa55f'
record = {'sourceRelease': 'v0.1.0-alpha.2', 'sourceCommit': '624a2ae8974c9c9e93af76300e187694e6174eca',
          'sourceTreeMatchedByCI': True, 'apkSha256': digest, 'currentVerificationCommit': os.environ['GITHUB_SHA']}
(root / 'evidence/core-reuse.json').write_text(json.dumps(record, indent=2) + '\n')
print(json.dumps(record, indent=2))
