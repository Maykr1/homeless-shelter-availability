param(
    [string]$ProjectRoot = (Resolve-Path "$PSScriptRoot\..\..").Path
)

$composeFile = Join-Path $ProjectRoot "docker-compose.yml"

$env:IMPORT_FIXTURE_MODE = "true"
$env:GOOGLE_PLACES_API_KEY = ""

docker compose -f $composeFile up --build -d

docker compose -f $composeFile --profile import build data
$googleStatus = docker compose -f $composeFile --profile import run --rm data python -m app.cli full-country
$utahStatus = docker compose -f $composeFile --profile import run --rm data python -m app.cli utah
$riStatus = docker compose -f $composeFile --profile import run --rm data python -m app.cli ri
$shelters = Invoke-RestMethod -Uri "http://localhost:8081/api/shelters?bedsAvailableOnly=true"

Write-Host "Google import completed."
Write-Host "Utah import completed."
Write-Host "Rhode Island import completed."
Write-Host "Filtered shelters: $($shelters.Count)"
