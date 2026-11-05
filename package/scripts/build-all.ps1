$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Maven = "C:\Users\lx\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd"

Push-Location (Join-Path $ProjectRoot "backend")
try {
    $env:JAVA_HOME = "C:\Users\lx\.jdks\openjdk-23.0.2"
    & $Maven -s .mvn/settings.xml clean package
    if ($LASTEXITCODE -ne 0) { throw "Backend build failed" }
} finally {
    Pop-Location
}

Push-Location (Join-Path $ProjectRoot "frontend")
try {
    if (-not (Test-Path -LiteralPath "node_modules")) { npm install }
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "Frontend build failed" }
} finally {
    Pop-Location
}

Write-Host "Build completed."
