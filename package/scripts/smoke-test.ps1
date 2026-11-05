$ErrorActionPreference = "Stop"
$BaseUrl = "http://127.0.0.1:8080"

$login = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" `
    -Method Post `
    -ContentType "application/json" `
    -Body (@{ username = "platform_admin"; password = "Admin@15347361610" } | ConvertTo-Json)

$headers = @{ Authorization = "Bearer $($login.token)" }
$chain = Invoke-RestMethod -Uri "$BaseUrl/api/system/chain" -Headers $headers -TimeoutSec 30
$batches = Invoke-RestMethod -Uri "$BaseUrl/api/batches?page=1&size=20" -Headers $headers

if ($batches.items.Count -eq 0) { throw "No trace batches are available" }

$traceBatch = $batches.items | Where-Object { $_.traceCode } | Select-Object -First 1
if (-not $traceBatch) { throw "No finished batch with a public trace code is available" }

$publicTrace = Invoke-RestMethod -Uri "$BaseUrl/api/public/trace/$($traceBatch.traceCode)"
$verification = Invoke-RestMethod -Uri "$BaseUrl/api/batches/$($traceBatch.id)/verify" -Headers $headers -TimeoutSec 60

Write-Host "LOGIN=OK"
Write-Host "CHAIN=$($chain.state)"
Write-Host "TRACE_CODE=$($traceBatch.traceCode)"
Write-Host "PUBLIC_EVENTS=$($publicTrace.events.Count)"
Write-Host "VERIFIED=$($verification.verifiedCount)/$($verification.eventCount)"

if (-not $verification.allVerified) {
    throw "One or more trace events failed chain verification"
}
