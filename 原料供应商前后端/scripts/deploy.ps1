param(
    [string]$HostName = "192.168.116.145",
    [string]$UserName = "yuan",
    [string]$RemotePath = "/home/yuan/supplier-trace",
    [string]$SudoPassword = $env:SUPPLIER_SUDO_PASSWORD,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

if (-not $SkipBuild) {
    & (Join-Path $PSScriptRoot "build.ps1")
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed"
    }
}

$stage = Join-Path ([System.IO.Path]::GetTempPath()) ("supplier-trace-" + (Get-Date -Format "yyyyMMddHHmmss"))
New-Item -ItemType Directory -Force -Path $stage | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $stage "backend\certs") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $stage "frontend") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $stage "config") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $stage "scripts") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $stage "systemd") | Out-Null

Copy-Item -Force (Join-Path $ProjectRoot "backend\trace-backend-1.0.0.jar") (Join-Path $stage "backend")
Copy-Item -Force (Join-Path $ProjectRoot "backend\certs\*") (Join-Path $stage "backend\certs")
Copy-Item -Force (Join-Path $ProjectRoot "config\supplier.env") (Join-Path $stage "config")
Copy-Item -Force (Join-Path $ProjectRoot "frontend\gateway.py") (Join-Path $stage "frontend")
Copy-Item -Recurse -Force (Join-Path $ProjectRoot "frontend\dist") (Join-Path $stage "frontend")
Copy-Item -Force (Join-Path $ProjectRoot "scripts\*.sh") (Join-Path $stage "scripts")
Copy-Item -Force (Join-Path $ProjectRoot "systemd\*.service") (Join-Path $stage "systemd")

$archive = "$stage.tar.gz"
tar -czf $archive -C $stage .
if ($LASTEXITCODE -ne 0) {
    throw "Release archive creation failed"
}

$remoteArchive = "/tmp/supplier-trace-release.tar.gz"
scp -o StrictHostKeyChecking=no $archive "${UserName}@${HostName}:$remoteArchive"
if ($LASTEXITCODE -ne 0) {
    throw "Release upload failed"
}

$sudoPrefix = "sudo"
if ($SudoPassword) {
    $escapedPassword = $SudoPassword.Replace("'", "'\''")
    $sudoPrefix = "printf '%s\n' '$escapedPassword' | sudo -S"
}

$remoteCommand = @"
set -e
$sudoPrefix systemctl stop supplier-trace-backend supplier-trace-frontend
mkdir -p '$RemotePath'
tar -xzf '$remoteArchive' -C '$RemotePath'
chmod +x '$RemotePath'/scripts/*.sh
$sudoPrefix cp '$RemotePath'/systemd/*.service /etc/systemd/system/
$sudoPrefix systemctl daemon-reload
$sudoPrefix systemctl enable supplier-trace-backend.service supplier-trace-frontend.service
$sudoPrefix systemctl restart supplier-trace-backend.service supplier-trace-frontend.service
$sudoPrefix systemctl is-active supplier-trace-backend supplier-trace-frontend
"@
ssh -tt -o StrictHostKeyChecking=no "${UserName}@${HostName}" $remoteCommand
if ($LASTEXITCODE -ne 0) {
    throw "Remote deployment failed"
}

Remove-Item -LiteralPath $archive -Force
Remove-Item -LiteralPath $stage -Recurse -Force

Write-Host "Supplier node deployed: http://${HostName}:5173"
