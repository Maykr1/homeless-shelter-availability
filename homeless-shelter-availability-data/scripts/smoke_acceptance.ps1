param(
    [string]$ProjectRoot = (Resolve-Path "$PSScriptRoot\..\..").Path
)

$composeFile = Join-Path $ProjectRoot "docker-compose.yml"

$env:IMPORT_FIXTURE_MODE = "true"
$env:GOOGLE_PLACES_API_KEY = ""

docker compose -f $composeFile up --build -d

$googleRun = Invoke-RestMethod -Method Post -Uri "http://localhost:8000/imports/full-country"
$utahRun = Invoke-RestMethod -Method Post -Uri "http://localhost:8000/imports/sources/utah"
$riRun = Invoke-RestMethod -Method Post -Uri "http://localhost:8000/imports/sources/ri"

function Wait-ImportRun {
    param(
        [Parameter(Mandatory = $true)][string]$RunId,
        [int]$TimeoutSeconds = 90
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $status = Invoke-RestMethod -Uri "http://localhost:8000/imports/$RunId"
        if ($status.status -in @("completed", "failed")) {
            return $status
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "Timed out waiting for import run $RunId"
}

$googleStatus = Wait-ImportRun -RunId $googleRun.id
$utahStatus = Wait-ImportRun -RunId $utahRun.id
$riStatus = Wait-ImportRun -RunId $riRun.id
$shelters = Invoke-RestMethod -Uri "http://localhost:8081/api/shelters?bedsAvailableOnly=true"

Write-Host "Google import status: $($googleStatus.status)"
Write-Host "Utah import status: $($utahStatus.status)"
Write-Host "Rhode Island import status: $($riStatus.status)"
Write-Host "Filtered shelters: $($shelters.Count)"
