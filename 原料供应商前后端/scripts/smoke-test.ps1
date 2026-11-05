param(
    [string]$BaseUrl = "http://192.168.116.145:8080",
    [string]$Username = "13910000001",
    [string]$Password = "12345678"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false
$OutputEncoding = New-Object System.Text.UTF8Encoding $false

$health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 10
if ($health.status -ne "UP") {
    throw "Backend health check failed"
}

$loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json
$session = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/api/auth/login" `
    -ContentType "application/json; charset=utf-8" `
    -Body $loginBody
$headers = @{ Authorization = "Bearer $($session.token)" }

$chain = Invoke-RestMethod -Uri "$BaseUrl/api/system/chain" -Headers $headers
$batches = Invoke-RestMethod -Uri "$BaseUrl/api/batches?page=1&size=20" -Headers $headers
if (-not $batches.items -or $batches.items.Count -eq 0) {
    throw "No accessible supplier batch"
}

$batch = $batches.items | Select-Object -First 1
$verification = Invoke-RestMethod `
    -Uri "$BaseUrl/api/batches/$($batch.id)/verify" `
    -Headers $headers

$foreignRejected = $false
try {
    $foreignBody = @{ username = "13920000001"; password = "12345678" } | ConvertTo-Json
    Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/api/auth/login" `
        -ContentType "application/json; charset=utf-8" `
        -Body $foreignBody | Out-Null
} catch {
    $foreignRejected = $_.Exception.Response.StatusCode.value__ -eq 403
}
if (-not $foreignRejected) {
    throw "A non-SUP003 account was not rejected"
}

[PSCustomObject]@{
    Health = $health.status
    Organization = $session.user.orgName
    Roles = ($session.user.roles -join ",")
    Fabric = $chain.state
    BatchTotal = $batches.total
    CheckedBatch = $batch.batchNo
    VerifiedEvents = "$($verification.verifiedCount)/$($verification.eventCount)"
    ForeignAccountRejected = $foreignRejected
} | Format-List
