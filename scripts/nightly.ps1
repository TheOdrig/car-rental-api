$ErrorActionPreference = 'Stop'
Set-Location (git rev-parse --show-toplevel)
& "$PSScriptRoot/verify.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($env:NIGHTLY_DEPENDENCY_CHECK -eq '1') {
    & "$PSScriptRoot/dependency-check.ps1"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
Write-Host 'nightly: OK'
