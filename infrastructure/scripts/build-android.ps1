param([ValidateSet('manager','core','all')][string]$Product='all')
$ErrorActionPreference='Stop'
$taskRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $env:JAVA_HOME -or -not $env:ANDROID_HOME) { throw 'Set JAVA_HOME to JDK 17 and ANDROID_HOME to an installed Android SDK.' }
New-Item -ItemType Directory -Force (Join-Path $taskRoot 'evidence') | Out-Null
foreach ($taskProduct in @('manager','core')) {
    if ($Product -ne 'all' -and $Product -ne $taskProduct) { continue }
    $taskApp=Join-Path $taskRoot "apps/javo-$taskProduct"
    $sdkProperty = $env:ANDROID_HOME.Replace('\','/').Replace(':','\:')
    [System.IO.File]::WriteAllText((Join-Path $taskApp 'local.properties'), ('sdk.dir='+$sdkProperty+[Environment]::NewLine))
    Push-Location $taskApp
    try {
        if ($taskProduct -eq 'manager') {
            if (-not $env:JAVO_MAVEN_CACHE -and (-not $env:ORG_GRADLE_PROJECT_githubPackagesUsername -or -not $env:ORG_GRADLE_PROJECT_githubPackagesPassword)) { throw 'Configure the verified local Maven cache or GitHub Packages read access.' }
            & .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --console=plain *> (Join-Path $taskRoot 'evidence/manager-build.log')
        } else {
            $env:GRADLE_MICROG_VERSION_WITHOUT_GIT='1'
            & .\gradlew.bat :play-services-core:assembleVtmDefaultDebug --no-daemon --no-configuration-cache --max-workers=2 --console=plain *> (Join-Path $taskRoot 'evidence/core-build.log')
        }
        if ($LASTEXITCODE -ne 0) { throw "$taskProduct build failed. Read evidence/$taskProduct-build.log" }
        $taskApks=@(Get-ChildItem -LiteralPath (Join-Path $taskApp $(if ($taskProduct -eq 'manager') {'app/build/outputs/apk/debug'} else {'play-services-core/build/outputs/apk/vtmDefault/debug'})) -Filter '*.apk' -Recurse)
        if ($taskApks.Count -ne 1) { throw 'Expected exactly one APK for verification' }
        python (Join-Path $PSScriptRoot 'verify-apk.py') $taskApks[0].FullName --sdk $env:ANDROID_HOME --product $taskProduct --signing debug
        if ($LASTEXITCODE -ne 0) { throw 'APK verification failed' }
    } finally { Pop-Location }
}
