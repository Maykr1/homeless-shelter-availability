param(
    [string]$CsvPath = "homeless-shelter-availability-data/data/public_sources/verified_homeless_service_areas_va_fy26_50_states_only.csv",
    [string]$ContainerName = "homeless-shelter-availability-db-1",
    [string]$Database = "homeless_shelter_db",
    [string]$Username = "postgres",
    [string]$OutputPath,
    [switch]$KeepTransformedCsv,
    [switch]$TransformOnly,
    [switch]$TruncateFirst
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-TrimmedValue {
    param([AllowNull()][object]$Value)

    if ($null -eq $Value) {
        return ""
    }

    return "$Value".Trim()
}

function Get-PreferredValue {
    param([object[]]$Values)

    foreach ($value in $Values) {
        $trimmedValue = Get-TrimmedValue $value
        if ($trimmedValue) {
            return $trimmedValue
        }
    }

    return ""
}

function Get-ServiceAreaLabel {
    param(
        [string]$ServiceArea,
        [string]$State
    )

    $normalizedLabel = Get-TrimmedValue $ServiceArea
    if (-not $normalizedLabel) {
        return Get-TrimmedValue $State
    }

    return ($normalizedLabel -replace "\s+\([A-Z]{2}\)$", "").Trim()
}

function Limit-Text {
    param(
        [string]$Text,
        [int]$MaxLength = 255
    )

    $normalizedText = Get-TrimmedValue $Text
    if ($normalizedText.Length -le $MaxLength) {
        return $normalizedText
    }

    if ($MaxLength -le 3) {
        return $normalizedText.Substring(0, $MaxLength)
    }

    return $normalizedText.Substring(0, $MaxLength - 3).TrimEnd() + "..."
}

if (-not $TransformOnly -and -not $TruncateFirst) {
    throw "Use -TruncateFirst when importing the VA service-area dataset so it replaces the existing shelters table contents."
}

$resolvedCsvPath = (Resolve-Path $CsvPath).Path
if (-not (Test-Path $resolvedCsvPath)) {
    throw "CSV file not found: $CsvPath"
}

$generatedOutputPath = $false
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $env:TEMP ("va-service-areas-transformed-{0}.csv" -f ([guid]::NewGuid().ToString("N")))
    $generatedOutputPath = $true
}

$resolvedOutputDirectory = Split-Path -Path $OutputPath -Parent
if ($resolvedOutputDirectory -and -not (Test-Path $resolvedOutputDirectory)) {
    New-Item -ItemType Directory -Path $resolvedOutputDirectory | Out-Null
}

$sourceRows = Import-Csv -Path $resolvedCsvPath
$transformedRows = foreach ($row in $sourceRows) {
    $state = Get-TrimmedValue $row.state
    $serviceAreaLabel = Get-ServiceAreaLabel -ServiceArea $row.serviceCountyOrArea -State $state
    $providerName = Get-PreferredValue @($row.name, "VA SSVF Provider ($state)")
    $address = Get-PreferredValue @($row.address, $serviceAreaLabel)
    $city = Get-PreferredValue @($row.city, $serviceAreaLabel)
    $zipCode = Get-TrimmedValue $row.zipCode
    $phoneNumber = Get-PreferredValue @($row.phoneNumber, $row.phoneNumber2)
    $email = Get-PreferredValue @($row.email, $row.email2)
    $grantYear = Get-PreferredValue @($row.grantFiscalYear, "2026")
    $description = Limit-Text ("VA FY{0} veteran service area covering {1}. Not a bed inventory listing; call provider for referrals and eligibility details." -f $grantYear, $serviceAreaLabel)

    [pscustomobject]@{
        name = $providerName
        address = $address
        city = $city
        state = $state
        zip_code = $zipCode
        phone_number = $phoneNumber
        email = $email
        total_beds = 0
        available_beds = 0
        description = $description
    }
}

$transformedRows | Export-Csv -Path $OutputPath -NoTypeInformation

$stateGroups = $transformedRows | Group-Object -Property state
Write-Host ("Transformed {0} rows across {1} states into {2}" -f $transformedRows.Count, $stateGroups.Count, $OutputPath)

if ($TransformOnly) {
    return
}

try {
    & (Join-Path $PSScriptRoot "import-shelters-csv.ps1") `
        -CsvPath $OutputPath `
        -ContainerName $ContainerName `
        -Database $Database `
        -Username $Username `
        -TruncateFirst

    docker exec $ContainerName psql -U $Username -d $Database -c "SELECT COUNT(*) AS shelters, COUNT(DISTINCT state) AS states FROM shelters;"
    docker exec $ContainerName psql -U $Username -d $Database -c "SELECT state, COUNT(*) AS shelters FROM shelters GROUP BY state ORDER BY state;"
}
finally {
    if ($generatedOutputPath -and -not $KeepTransformedCsv -and (Test-Path $OutputPath)) {
        Remove-Item -Path $OutputPath -Force
    }
}
