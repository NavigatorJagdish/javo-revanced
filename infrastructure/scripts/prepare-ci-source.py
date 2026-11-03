"""Prepare a checksum-verified source-only upload for the private build repository."""
import importlib.util,zipfile,hashlib,json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('javo_package',ROOT/'infrastructure/scripts/package.py')
pack=importlib.util.module_from_spec(spec);spec.loader.exec_module(pack)
archive=ROOT/'deliverables/javo-ci-source.zip'
files=list(pack.source_files())
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for p,name in files:z.write(p,name)
with zipfile.ZipFile(archive) as z:
    if z.testzip():raise RuntimeError('Source ZIP integrity failed')
    if any(n.endswith(('.jks','.keystore','.p12','.key','.apk')) for n in z.namelist()):raise RuntimeError('Unexpected key or APK in source-only upload')
digest=hashlib.sha256(archive.read_bytes()).hexdigest()
archive.with_suffix('.zip.sha256').write_text(digest+'  '+archive.name+'\n',encoding='ascii')
print(json.dumps({'archive':str(archive),'size':archive.stat().st_size,'files':len(files),'sha256':digest},indent=2))
