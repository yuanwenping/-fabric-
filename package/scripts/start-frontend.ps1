$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$FrontendDir = Join-Path $ProjectRoot "frontend"

Push-Location $FrontendDir
try {
    if (-not (Test-Path -LiteralPath (Join-Path $FrontendDir "node_modules"))) {
        npm install
    }
    npm run dev
} finally {
    Pop-Location
}
