"""Retire build-only SDK NDKs after compilation on a temporary GitHub runner."""
from pathlib import Path
import os,re,shutil,subprocess

if os.environ.get('GITHUB_ACTIONS')!='true':
    raise SystemExit('This cleanup is restricted to temporary GitHub Actions runners.')
sdk=Path(os.environ['ANDROID_HOME']).resolve()
manager=shutil.which('sdkmanager')
if not manager:raise SystemExit('Official sdkmanager is required')
before=shutil.disk_usage(sdk).free
ndk=sdk/'ndk'
if ndk.is_dir():
    for version in sorted(ndk.iterdir()):
        if version.is_dir() and re.fullmatch(r'\d+\.\d+\.\d+',version.name):
            print('Removing build-only NDK from temporary runner:',version.name,flush=True)
            subprocess.run([manager,'--uninstall','ndk;'+version.name],check=True)
after=shutil.disk_usage(sdk).free
print(f'Runner free disk: {before/2**30:.2f} GiB before, {after/2**30:.2f} GiB after')
