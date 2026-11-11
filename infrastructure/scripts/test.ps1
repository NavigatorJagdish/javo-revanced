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
    & "$env:JAVA_HOME/bin/javac.exe" -d $taskClasses apps/javo-manager/app/src/main/java/app/javo/security/UpdateSecurity.java tests/UpdateSecurityTest.java patches/javo-branding/src/main/java/app/javo/branding/BrandingResources.java tests/BrandingResourcesTest.java apps/javo-manager/api/src/main/java/app/revanced/manager/downloader/DownloadConnection.java tests/DownloadConnectionTest.java apps/javo-manager/api/src/main/java/app/revanced/manager/downloader/webview/ApkMirrorPolicy.java tests/ApkMirrorPolicyTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Java test compilation failed' }
    & "$env:JAVA_HOME/bin/java.exe" -cp $taskClasses UpdateSecurityTest *> evidence/java-security-tests.log
    if ($LASTEXITCODE -ne 0) { throw 'Security tests failed' }
    & "$env:JAVA_HOME/bin/javac.exe" -d $taskClasses apps/javo-manager/app/src/main/java/app/javo/security/RootArguments.java tests/RootArgumentsTest.java apps/javo-manager/app/src/main/java/app/javo/security/FrameworkCache.java tests/FrameworkCacheTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Patcher security test compilation failed' }
    $taskShell = Get-Command sh -ErrorAction SilentlyContinue
    if ($taskShell) { $taskShellPath = $taskShell.Source }
    else {
        $taskGit = Get-Command git -ErrorAction Stop
        $taskGitRoot = Split-Path (Split-Path $taskGit.Source -Parent) -Parent
        $taskShellPath = @('bin/sh.exe','usr/bin/sh.exe') | ForEach-Object { Join-Path $taskGitRoot $_ } | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
        if (-not $taskShellPath) { throw 'Install a POSIX shell (for example Git for Windows) to run root installer regression tests' }
    }
    & "$env:JAVA_HOME/bin/java.exe" -cp $taskClasses RootArgumentsTest $taskShellPath *> evidence/java-root-arguments-tests.log
    if ($LASTEXITCODE -ne 0) { throw 'Root argument tests failed' }
    & "$env:JAVA_HOME/bin/java.exe" -cp $taskClasses FrameworkCacheTest *> evidence/framework-cache-tests.log
    if ($LASTEXITCODE -ne 0) { throw 'Framework cache recovery tests failed' }
    & "$env:JAVA_HOME/bin/java.exe" -cp $taskClasses BrandingResourcesTest *> evidence/java-branding-tests.log
    if ($LASTEXITCODE -ne 0) { throw 'Branding tests failed' }
    & "$env:JAVA_HOME/bin/java.exe" -cp $taskClasses DownloadConnectionTest *> evidence/java-download-tests.log
    if ($LASTEXITCODE -ne 0) { throw 'Download connection tests failed' }
    & "$env:JAVA_HOME/bin/java.exe" -cp $taskClasses ApkMirrorPolicyTest *> evidence/java-browser-tests.log
    if ($LASTEXITCODE -ne 0) { throw 'Browser policy tests failed' }
    Write-Output 'Website/release and host Java checks passed. These do not replace Android runtime tests.'
} finally { Pop-Location }
