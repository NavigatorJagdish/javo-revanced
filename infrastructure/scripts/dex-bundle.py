from pathlib import Path
import sys,zipfile
jar,dex=map(Path,sys.argv[1:])
if dex.read_bytes()[:4]!=b'dex\n':raise ValueError('D8 did not produce valid DEX header')
with zipfile.ZipFile(jar) as src,zipfile.ZipFile(jar.with_suffix('.rvp'),'w',zipfile.ZIP_DEFLATED) as dst:
    for name in src.namelist():
        if not name.endswith('.class'):dst.writestr(name,src.read(name))
    dst.write(dex,'classes.dex')
print('Created RVP; Android loading and patch execution still require verification.')
