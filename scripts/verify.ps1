$ErrorActionPreference = 'Stop'
Set-Location (git rev-parse --show-toplevel)
& "$PSScriptRoot/check-no-ai-files.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$local = Join-Path $PSScriptRoot 'verify.local.ps1'
if (Test-Path $local) {
    & $local
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else {
    Write-Host 'local checks: none'
}
foreach ($artifact in @('openapi/openapi.json', 'openapi/endpoint-inventory.txt')) {
    if (-not (Test-Path $artifact)) {
        Write-Error "verify: missing $artifact; run scripts/update-contract.sh and commit the result"
        exit 1
    }
}
& ./mvnw.cmd -B verify
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'verify: OK'
