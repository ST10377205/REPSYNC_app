param(
    [string]$ProjectRoot = (Split-Path $PSScriptRoot -Parent),
    [string]$SigningDirectory = (Join-Path $env:LOCALAPPDATA 'RepSync\Signing'),
    [string]$GradleExecutable,
    [switch]$Offline,
    [switch]$Verify
)
$ErrorActionPreference = 'Stop'
$ProjectRoot = [IO.Path]::GetFullPath($ProjectRoot)
$store = Join-Path $SigningDirectory 'repsync-upload.p12'
$passwordFile = Join-Path $SigningDirectory 'upload.password.dpapi'
if(-not(Test-Path -LiteralPath $store) -or -not(Test-Path -LiteralPath $passwordFile)){throw 'Run tools/Initialize-ReleaseSigning.ps1 first.'}
if(-not $env:JAVA_HOME){$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'}
if(-not $GradleExecutable){$GradleExecutable=Join-Path $ProjectRoot 'gradlew.bat'}
$secure = (Get-Content -LiteralPath $passwordFile -Raw).Trim() | ConvertTo-SecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
$oldStore=$env:REPSYNC_UPLOAD_STORE; $oldPassword=$env:REPSYNC_UPLOAD_PASSWORD
try {
    $env:REPSYNC_UPLOAD_STORE = $store
    $env:REPSYNC_UPLOAD_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    $arguments = @('-p',$ProjectRoot,'--no-daemon','assembleRelease','bundleRelease')
    if($Offline){$arguments+='--offline'}
    if($Verify){$arguments+=@('testDebugUnitTest','lintDebug','lintRelease','assembleDebug','assembleDebugAndroidTest')}
    & $GradleExecutable @arguments
    if($LASTEXITCODE -ne 0){throw 'Release build failed'}
    $destination=Join-Path $ProjectRoot 'play-store\release'
    New-Item -ItemType Directory -Path $destination -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $ProjectRoot 'app\build\outputs\apk\release\app-release.apk') -Destination (Join-Path $destination 'RepSync-1.0-release.apk') -Force
    Copy-Item -LiteralPath (Join-Path $ProjectRoot 'app\build\outputs\bundle\release\app-release.aab') -Destination (Join-Path $destination 'RepSync-1.0-release.aab') -Force
    Write-Output "Signed APK and Android App Bundle copied to $destination"
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    $env:REPSYNC_UPLOAD_STORE=$oldStore; $env:REPSYNC_UPLOAD_PASSWORD=$oldPassword
    $secure.Dispose()
}
