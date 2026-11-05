$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Maven = $env:MAVEN_CMD
if (-not $Maven) {
    $Maven = "C:\Users\lx\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd"
}
if (-not $env:JAVA_HOME -and (Test-Path "C:\Users\lx\.jdks\openjdk-23.0.2")) {
    $env:JAVA_HOME = "C:\Users\lx\.jdks\openjdk-23.0.2"
}

Push-Location (Join-Path $ProjectRoot "backend")
try {
    & $Maven -s .mvn/settings.xml clean package
    if ($LASTEXITCODE -ne 0) {
        throw "Backend build failed"
    }
    Copy-Item -Force "target\trace-backend-1.0.0.jar" "trace-backend-1.0.0.jar"
} finally {
    Pop-Location
}

Push-Location (Join-Path $ProjectRoot "frontend")
try {
    if (-not (Test-Path "node_modules")) {
        npm install --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) {
            throw "Frontend dependency installation failed"
        }
    }
    npm run build
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend build failed"
    }
} finally {
    Pop-Location
}

Write-Host "Supplier backend and frontend build completed."
