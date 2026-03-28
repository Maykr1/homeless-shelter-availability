$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$frontendRoot = Join-Path $repoRoot "homeless-shelter-availability-web"
$backendRoot = Join-Path $repoRoot "homeless-shelter-availability-api"

Write-Host "Generating frontend LCOV report..."
Push-Location $frontendRoot
try {
    python scripts/generate_lcov.py
}
finally {
    Pop-Location
}

Write-Host "Generating backend JaCoCo XML report..."
Push-Location $backendRoot
try {
    .\mvnw verify
}
finally {
    Pop-Location
}

Write-Host ""
Write-Host "Frontend LCOV: $frontendRoot\coverage\lcov.info"
Write-Host "Backend JaCoCo XML: $backendRoot\target\site\jacoco\jacoco.xml"
