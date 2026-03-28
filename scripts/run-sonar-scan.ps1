param(
    [string]$SonarHostUrl = $env:SONAR_HOST_URL,
    [string]$SonarToken = $env:SONAR_TOKEN
)

$ErrorActionPreference = "Stop"

if (-not $SonarHostUrl) {
    throw "Provide -SonarHostUrl or set SONAR_HOST_URL."
}

if (-not $SonarToken) {
    throw "Provide -SonarToken or set SONAR_TOKEN."
}

if (-not (Get-Command sonar-scanner -ErrorAction SilentlyContinue)) {
    throw "sonar-scanner is not installed or not on PATH."
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Push-Location $repoRoot
try {
    sonar-scanner "-Dsonar.host.url=$SonarHostUrl" "-Dsonar.token=$SonarToken"
}
finally {
    Pop-Location
}
