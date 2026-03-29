param(
    [Parameter(Mandatory = $true)]
    [string]$CsvPath,
    [string]$ContainerName = "homeless-shelter-availability-db-1",
    [string]$Database = "homeless_shelter_db",
    [string]$Username = "postgres",
    [switch]$TruncateFirst
)

$resolvedCsvPath = (Resolve-Path $CsvPath).Path
$containerCsvPath = "/tmp/shelters-import.csv"

if (-not (Test-Path $resolvedCsvPath)) {
    throw "CSV file not found: $CsvPath"
}

docker cp $resolvedCsvPath "${ContainerName}:$containerCsvPath"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to copy CSV into container '$ContainerName'."
}

try {
    if ($TruncateFirst) {
        docker exec $ContainerName psql -U $Username -d $Database -c "TRUNCATE TABLE shelters RESTART IDENTITY;"
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to truncate shelters table in database '$Database'."
        }
    }

    docker exec $ContainerName psql -U $Username -d $Database -c "COPY shelters (name, address, city, state, zip_code, phone_number, email, total_beds, available_beds, description) FROM '$containerCsvPath' WITH (FORMAT csv, HEADER true);"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to import shelter CSV into database '$Database'."
    }

    docker exec $ContainerName psql -U $Username -d $Database -c "SELECT COUNT(*) AS shelters FROM shelters;"
    if ($LASTEXITCODE -ne 0) {
        throw "CSV import completed, but the verification query failed."
    }
}
finally {
    docker exec $ContainerName rm -f $containerCsvPath | Out-Null
}
