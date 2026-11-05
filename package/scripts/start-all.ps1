$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$RuntimeDir = Join-Path $ProjectRoot "runtime"
$BackendDir = Join-Path $ProjectRoot "backend"
$FrontendDir = Join-Path $ProjectRoot "frontend"
$Jar = Join-Path $BackendDir "target\trace-backend-1.0.0.jar"
$Java = "C:\Users\lx\.jdks\openjdk-23.0.2\bin\java.exe"
$Npm = "D:\Program Files\nodejs\npm.cmd"
$VmDirectory = Get-ChildItem -LiteralPath "F:\" -Directory |
    Where-Object { $_.Name -like "yanyuanyuanfabric*" } |
    Select-Object -First 1
$Vmx = if ($VmDirectory) { Join-Path $VmDirectory.FullName "yanyuanyuanfabric.vmx" } else { $null }

New-Item -ItemType Directory -Force -Path $RuntimeDir | Out-Null

& (Join-Path $PSScriptRoot "start-fabric.ps1")
& (Join-Path $PSScriptRoot "start-mysql.ps1")

$Vmrun = "D:\Program Files (x86)\VMware\VMware Workstation\vmrun.exe"
$FabricHost = (& $Vmrun -T ws getGuestIPAddress $Vmx).Trim()
if ($FabricHost -notmatch '^\d+\.\d+\.\d+\.\d+$') {
    throw "Unable to resolve the Fabric VM IP address."
}

if (-not (Test-Path -LiteralPath $Jar)) {
    & (Join-Path $PSScriptRoot "build-all.ps1")
}

$backendListener = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue
if (-not $backendListener) {
    $env:JAVA_HOME = "C:\Users\lx\.jdks\openjdk-23.0.2"
    $env:DB_URL = "jdbc:mysql://127.0.0.1:3306/changde_trace_multipeer?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
    $env:DB_USERNAME = "root"
    $env:DB_PASSWORD = "Yuan15347361610"
    $env:FABRIC_ENABLED = "true"
    $env:FABRIC_CHANNEL = "tracechannel"
    $env:FABRIC_CHAINCODE = "tracecc"
    $env:FABRIC_SUPPLIER_ENDPOINT = "grpcs://${FabricHost}:7051"
    $env:FABRIC_PROCESSOR_ENDPOINT = "grpcs://${FabricHost}:8051"
    $env:FABRIC_LOGISTICS_ENDPOINT = "grpcs://${FabricHost}:9051"
    $env:FABRIC_RETAILER_ENDPOINT = "grpcs://${FabricHost}:10051"
    $env:FABRIC_ORDERER_ENDPOINT = "grpcs://${FabricHost}:7050"
    $backend = Start-Process -WindowStyle Hidden `
        -FilePath $Java `
        -ArgumentList @("-jar", $Jar) `
        -WorkingDirectory $BackendDir `
        -RedirectStandardOutput (Join-Path $RuntimeDir "backend.out.log") `
        -RedirectStandardError (Join-Path $RuntimeDir "backend.err.log") `
        -PassThru
    $backend.Id | Set-Content -LiteralPath (Join-Path $RuntimeDir "backend.pid") -Encoding ascii
}

$frontendListener = Get-NetTCPConnection -State Listen -LocalPort 5173 -ErrorAction SilentlyContinue
if (-not $frontendListener) {
    $frontend = Start-Process -WindowStyle Hidden `
        -FilePath $Npm `
        -ArgumentList @("run", "dev", "--", "--host", "127.0.0.1") `
        -WorkingDirectory $FrontendDir `
        -RedirectStandardOutput (Join-Path $RuntimeDir "frontend.out.log") `
        -RedirectStandardError (Join-Path $RuntimeDir "frontend.err.log") `
        -PassThru
    $frontend.Id | Set-Content -LiteralPath (Join-Path $RuntimeDir "frontend.pid") -Encoding ascii
}

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Milliseconds 500
    $ready = Get-NetTCPConnection -State Listen -LocalPort 8080, 5173 -ErrorAction SilentlyContinue
    if (($ready | Measure-Object).Count -ge 2) { break }
}

Write-Host "Frontend: http://127.0.0.1:5173"
Write-Host "Backend:  http://127.0.0.1:8080"
Write-Host "Platform administrator: platform_admin / Admin@15347361610"

$edge = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
if (Test-Path -LiteralPath $edge) {
    Start-Process -FilePath $edge -ArgumentList "http://127.0.0.1:5173/login"
}
