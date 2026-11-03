$ErrorActionPreference='Stop'
$taskRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Push-Location $taskRoot
try {
    New-Item -ItemType Directory -Force evidence | Out-Null
    node --test tests/*.test.mjs *> evidence/node-tests.log
    if ($LASTEXITCODE -ne 0) { throw 'Website/release tests failed' }
    python -m unittest discover -s tests -p 'test_*.py' -v *> evidence/runtime-evidence-tests.log
    if ($LASTEXITCODE -ne 0) { throw 'Runtime evidence regression tests failed' }
    $taskClasses=Join-Path $taskRoot 'tools/java-tests'
    New-Item -ItemType Directory -Force $taskClasses | Out-Null
    & "$env:JAVA_HOME/bin/javac.exe" -d $taskClasses apps/javo-manager/app/src/main/java/app/javo/security/UpdateSecurity.java tests/UpdateSecurityTest.java patches/javo-branding/src/main/java/app/javo/branding/BrandingResources.java tests/BrandingResourcesTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Java test compilation failed' }
    & "$env:JAVA_HOME/bin/java.exe" -cp $taskClasses UpdateSecurityTest *> evidence/java-security-tests.log
    if ($LASTEXITCODE -ne 0) { throw 'Security tests failed' }
    & "$env:JAVA_HOME/bin/java.exe" -cp $taskClasses BrandingResourcesTest *> evidence/java-branding-tests.log
    if ($LASTEXITCODE -ne 0) { throw 'Branding tests failed' }
    Write-Output 'Website/release and host Java checks passed. These do not replace Android runtime tests.'
} finally { Pop-Location }
