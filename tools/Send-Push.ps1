[CmdletBinding(DefaultParameterSetName = 'Device')]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z][a-z0-9-]{4,28}[a-z0-9]$')]
    [string] $ProjectId,
    [Parameter(Mandatory = $true, ParameterSetName = 'Device')]
    [ValidateNotNullOrEmpty()]
    [string] $DeviceToken,
    [Parameter(Mandatory = $true, ParameterSetName = 'Topic')]
    [ValidatePattern('^[a-zA-Z0-9-_.~%]+$')]
    [string] $Topic
)

$ErrorActionPreference = 'Stop'
if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw 'Google Cloud CLI is required. Alternatively use Send test message in the Firebase console.'
}
$accessToken = (& gcloud auth print-access-token --quiet) -join ''
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($accessToken)) {
    throw 'No Google Cloud access token available. Sign in with an account authorized for this Firebase project.'
}
$message = @{
    android = @{ priority = 'high'; ttl = '300s' }
    data = @{
        title = 'New training plan'
        body = 'Your home workout is ready.'
        title_zu = 'Uhlelo olusha lokuzivocavoca'
        body_zu = 'Ukuzivocavoca kwakho kwasekhaya sekulungile.'
        title_af = 'Nuwe oefenplan'
        body_af = 'Jou tuisoefening is gereed.'
        destination = 'workout'
    }
}
if ($PSCmdlet.ParameterSetName -eq 'Device') { $message.token = $DeviceToken } else { $message.topic = $Topic }
$body = @{ message = $message } | ConvertTo-Json -Depth 6
$response = Invoke-RestMethod -Method Post -Uri "https://fcm.googleapis.com/v1/projects/$ProjectId/messages:send" `
    -Headers @{ Authorization = "Bearer $accessToken" } -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($body))
"FCM accepted message: $($response.name)"
"Verify arrival on the phone; server acceptance alone is not proof of delivery."
