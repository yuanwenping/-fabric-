$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$BackendDir = Join-Path $ProjectRoot "backend"
$VmDirectory = Get-ChildItem -LiteralPath "F:\" -Directory |
    Where-Object { $_.Name -like "yanyuanyuanfabric*" } |
    Select-Object -First 1
$Vmx = if ($VmDirectory) { Join-Path $VmDirectory.FullName "yanyuanyuanfabric.vmx" } else { $null }
$MavenCandidates = @(
    "C:\Users\lx\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd",
    "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
)
$Maven = $MavenCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $Maven) {
    throw "Maven was not found. Install Maven or open the backend in IntelliJ IDEA."
}

& (Join-Path $PSScriptRoot "start-mysql.ps1")
$Vmrun = "D:\Program Files (x86)\VMware\VMware Workstation\vmrun.exe"
$FabricHost = (& $Vmrun -T ws getGuestIPAddress $Vmx).Trim()
if ($FabricHost -notmatch '^\d+\.\d+\.\d+\.\d+$') {
    throw "Unable to resolve the Fabric VM IP address."
}

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

Push-Location $BackendDir
try {
    & $Maven -s .mvn/settings.xml spring-boot:run
} finally {
    Pop-Location
}
