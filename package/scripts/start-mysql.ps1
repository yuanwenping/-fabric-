$ErrorActionPreference = "Stop"

$ServiceName = "MySQL80"
$Port = 3306
$Password = "Yuan15347361610"
$MySQLClient = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

$service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if (-not $service) {
    throw "Windows service $ServiceName was not found"
}

if ($service.Status -ne "Running") {
    Write-Host "Starting Windows service $ServiceName..."
    & sc.exe start $ServiceName | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to start Windows service $ServiceName"
    }
}

for ($attempt = 1; $attempt -le 30; $attempt++) {
    Start-Sleep -Milliseconds 500
    if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {
        break
    }
}

if (-not (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)) {
    throw "MySQL service is running but port $Port is not listening"
}

$env:MYSQL_PWD = $Password
& $MySQLClient --no-defaults --user=root --host=127.0.0.1 --port=$Port --protocol=TCP `
    -e "CREATE DATABASE IF NOT EXISTS changde_trace CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; SELECT VERSION() AS mysql_version;"
if ($LASTEXITCODE -ne 0) {
    throw "Unable to connect to local MySQL on port $Port"
}

Write-Host "Local MySQL is ready on 127.0.0.1:$Port."
