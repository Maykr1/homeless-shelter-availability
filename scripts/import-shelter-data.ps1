param(
    [string]$ProjectRoot = (Resolve-Path "$PSScriptRoot\..").Path,
    [ValidateSet("all", "full-country", "google", "utah", "ri")]
    [string]$Target = "all",
    [switch]$Live
)

$composeFile = Join-Path $ProjectRoot "docker-compose.yml"

$env:IMPORT_FIXTURE_MODE = if ($Live) { "false" } else { "true" }

docker compose -f $composeFile up -d db
docker compose -f $composeFile --profile import build data
docker compose -f $composeFile --profile import run --rm data python -m app.cli $Target
