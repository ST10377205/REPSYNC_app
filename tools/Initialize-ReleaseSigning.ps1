param([string]$SigningDirectory = (Join-Path $env:LOCALAPPDATA 'RepSync\Signing'))
$ErrorActionPreference = 'Stop'
$keytool = if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin\keytool.exe'}else{'C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe'}
$store = Join-Path $SigningDirectory 'repsync-upload.p12'
$encryptedPassword = Join-Path $SigningDirectory 'upload.password.dpapi'
if((Test-Path -LiteralPath $store) -or (Test-Path -LiteralPath $encryptedPassword)) {
    if(-not((Test-Path -LiteralPath $store) -and (Test-Path -LiteralPath $encryptedPassword))) {throw 'Signing files are incomplete. Restore the matching files; do not overwrite an existing key.'}
    Write-Output "Using existing signing files in $SigningDirectory"
    exit 0
}
New-Item -ItemType Directory -Path $SigningDirectory -Force | Out-Null
$principal = [Security.Principal.WindowsIdentity]::GetCurrent().Name
& icacls.exe $SigningDirectory /inheritance:r /grant:r "$($principal):(OI)(CI)F" 'SYSTEM:(OI)(CI)F' | Out-Null
if($LASTEXITCODE -ne 0){throw 'Could not restrict signing directory access'}
$bytes = [byte[]]::new(32)
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$password = [Convert]::ToBase64String($bytes)
try {
    $env:REPSYNC_UPLOAD_PASSWORD = $password
    & $keytool -genkeypair -keystore $store -storetype PKCS12 -alias repsync-upload -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=RepSync, OU=Student Project, O=RepSync, C=ZA' -storepass:env REPSYNC_UPLOAD_PASSWORD -keypass:env REPSYNC_UPLOAD_PASSWORD -noprompt
    if($LASTEXITCODE -ne 0){throw 'Release key generation failed'}
    ConvertTo-SecureString $password -AsPlainText -Force | ConvertFrom-SecureString | Set-Content -LiteralPath $encryptedPassword
    & $keytool -list -v -keystore $store -storetype PKCS12 -alias repsync-upload -storepass:env REPSYNC_UPLOAD_PASSWORD | Select-String 'SHA1:|SHA256:|Valid from:'
    if($LASTEXITCODE -ne 0){throw 'Release key verification failed'}
    Write-Output "Private signing key created in $SigningDirectory. Its password is encrypted for this Windows account."
} finally { Remove-Item Env:REPSYNC_UPLOAD_PASSWORD -ErrorAction SilentlyContinue; $password=$null; [Array]::Clear($bytes,0,$bytes.Length) }
