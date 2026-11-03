param([Parameter(Mandatory=$true)][string]$AndroidSdk)
$ErrorActionPreference='Stop'
$taskRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$taskJar = Join-Path $taskRoot 'patches/javo-branding/build/libs/javo-branding-0.1.0-alpha.1.jar'
$taskOut = Join-Path $taskRoot 'patches/javo-branding/build/dex'
New-Item -ItemType Directory -Force $taskOut | Out-Null
& (Join-Path $AndroidSdk 'build-tools/34.0.0/d8.bat') --min-api 26 --output $taskOut $taskJar
if ($LASTEXITCODE -ne 0) { throw 'D8 bundle compilation failed' }
$env:JAVO_BUNDLE_JAR=$taskJar
$env:JAVO_BUNDLE_DEX=Join-Path $taskOut 'classes.dex'
@'
import os,zipfile
from pathlib import Path
jar=Path(os.environ['JAVO_BUNDLE_JAR']);dex=Path(os.environ['JAVO_BUNDLE_DEX'])
with zipfile.ZipFile(jar) as src, zipfile.ZipFile(jar.with_suffix('.rvp'),'w',zipfile.ZIP_DEFLATED) as dst:
    for name in src.namelist():
        if not name.endswith('.class'):dst.writestr(name,src.read(name))
    dst.write(dex,'classes.dex')
print('Created D8 bundle; Android loading and patch execution still require verification.')
'@ | python -
if ($LASTEXITCODE -ne 0) { throw 'Bundle archive failed' }
