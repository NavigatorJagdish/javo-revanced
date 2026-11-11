"""Build a source/evidence/website archive, excluding caches and signing secrets."""
from pathlib import Path
import zipfile,hashlib,json,os,re,argparse

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'deliverables';OUT.mkdir(exist_ok=True)
ROOT_ONLY_DIRS={'tools','deliverables','inputs','.agents','.codex','.aws'}
CACHE_DIRS={'node_modules','.gradle','.git','__pycache__','.cxx','.kotlin'}
SKIP_NAMES={'local.properties','.env','.env.local','key.properties','source-inventory.json','javo-ci-source.zip','javo-ci-source.zip.sha256'}
SKIP_EXT={'.jks','.keystore','.p12','.pfx','.pkcs12','.jceks','.key','.dpapi'}
PUBLIC_EVIDENCE={'upstream-lock.json','source-modifications.json'}
def source_files(root=ROOT):
    for directory,dirs,names in os.walk(root):
        parent=Path(directory).relative_to(root)
        def included(d):
            if d in CACHE_DIRS or (Path(directory)/d).is_symlink():return False
            child=parent/d
            if parent==Path('.') and d in ROOT_ONLY_DIRS:return False
            if child==Path('assets/references') or (parent==Path('evidence')):return False
            # Java/Kotlin packages can legitimately be called tools or build.
            if d in {'build','dist'} and 'src' not in parent.parts:return False
            return True
        dirs[:]=sorted(d for d in dirs if included(d))
        for name in sorted(names):
            p=Path(directory)/name;rel=p.relative_to(root)
            if name in SKIP_NAMES or name.startswith('.env.') or p.suffix.lower() in SKIP_EXT or p.is_symlink():continue
            if parent==Path('evidence') and name not in PUBLIC_EVIDENCE and not re.fullmatch(r'release-alpha\d+\.json',name):continue
            yield p,rel.as_posix()
def verify_branding_evidence(bundle,integration,catalog):
    if integration.get('success') is not True or integration.get('bundleSha256')!=hashlib.sha256(bundle.read_bytes()).hexdigest():
        raise ValueError('Branding bundle lacks matching Android integration evidence')
    manager=next((a for a in catalog['artifacts'] if a['id']=='manager'),None)
    if manager is None or integration.get('managerApkSha256')!=manager['sha256']:
        raise ValueError('Patch integration evidence belongs to a different Manager APK')
    products=integration.get('products',[])
    if {p.get('id') for p in products}!={'video','music'} or any(
        p.get('success') is not True or p.get('patchedFixtureInstalled') is not True or
        p.get('fixtureProcessRunning') is not True or p.get('fixtureCrashObserved') is not False for p in products
    ):raise ValueError('Both patched fixtures must pass installation and launch')

def package(source_only=False):
    files=list(source_files())
    catalog=json.loads((ROOT/'infrastructure/release/catalog.json').read_text())
    dist=ROOT/'apps/javo-website/dist'
    approved_downloads={a['path'].lstrip('/') for a in catalog['artifacts']}
    for p in sorted(dist.rglob('*')):
        if not p.is_file() or p.is_symlink():continue
        relative=p.relative_to(dist).as_posix()
        if relative.startswith('downloads/files/') and (source_only or relative not in approved_downloads):continue
        files.append((p,'website/'+relative))
    for a in catalog['artifacts']:
        if source_only:continue
        p=OUT/'apks'/Path(a['path']).name
        digest=hashlib.sha256(p.read_bytes()).hexdigest()
        if digest!=a['sha256']:raise ValueError('Verified APK changed after validation')
        files.append((p,'apks/'+p.name))
    bundle=ROOT/'patches/javo-branding/build/libs/javo-branding-0.1.0-alpha.1.rvp'
    if not source_only and bundle.exists() and catalog['status'] != 'release-review' and any(a['id']=='manager' for a in catalog['artifacts']):
        integration=json.loads((ROOT/'evidence/manager-patch-integration.json').read_text())
        verify_branding_evidence(bundle,integration,catalog)
        files.append((bundle,'bundles/'+bundle.name))
    manifest={name:hashlib.sha256(p.read_bytes()).hexdigest() for p,name in files}
    (ROOT/'evidence/source-inventory.json').write_text(json.dumps(manifest,indent=2)+'\n')
    archive=OUT/'javo-revanced-development.zip'
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p,name in files:z.write(p,name)
        z.write(ROOT/'evidence/source-inventory.json','evidence/source-inventory.json')
    with zipfile.ZipFile(archive) as z:
        if z.testzip():raise ValueError('ZIP integrity failed')
        for name,digest in manifest.items():
            if hashlib.sha256(z.read(name)).hexdigest()!=digest:raise ValueError('ZIP content differs from source inventory: '+name)
    digest=hashlib.sha256(archive.read_bytes()).hexdigest()
    (OUT/'SHA256SUMS').write_text(f'{digest}  {archive.name}\n')
    print(json.dumps({'archive':str(archive),'size':archive.stat().st_size,'files':len(files),'sha256':digest,'zipIntegrity':'passed'},indent=2))
if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--source-only',action='store_true',help='Package corresponding source and website without installation binaries')
    package(parser.parse_args().source_only)
