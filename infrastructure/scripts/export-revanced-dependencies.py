"""Copy resolved ReVanced Maven artifacts to a private build cache, without credentials."""
from pathlib import Path
import os,shutil,json,hashlib

ROOT=Path(__file__).resolve().parents[2]
home=Path(os.environ.get('GRADLE_USER_HOME',Path.home()/'.gradle'))
cache=home/'caches/modules-2/files-2.1/app.revanced'
out=ROOT/'tools/revanced-maven/app/revanced'
inventory={}
if cache.exists():
    for module in cache.iterdir():
        for version in module.iterdir():
            for file in version.glob('*/*'):
                if file.is_file() and file.suffix in {'.pom','.module','.jar','.aar'}:
                    target=out/module.name/version.name/file.name
                    target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(file,target)
                    inventory[target.relative_to(out.parent.parent).as_posix()]=hashlib.sha256(file.read_bytes()).hexdigest()
                    # Gradle caches transformed AAR names (library.aar / patcher.aar).
                    # Maven local resolution requires the published coordinate filename.
                    if file.suffix=='.aar':
                        alias=target.with_name(f'{module.name}-{version.name}.aar')
                        shutil.copyfile(file,alias)
                        inventory[alias.relative_to(out.parent.parent).as_posix()]=hashlib.sha256(alias.read_bytes()).hexdigest()
(ROOT/'tools/revanced-maven').mkdir(parents=True,exist_ok=True)
(ROOT/'tools/revanced-maven/inventory.json').write_text(json.dumps(inventory,indent=2)+'\n')
print('Exported',len(inventory),'resolved Maven files; no authentication data included.')
