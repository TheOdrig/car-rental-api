$ErrorActionPreference = 'Stop'
Set-Location (git rev-parse --show-toplevel)
& "$PSScriptRoot/check-no-ai-files.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$py = Get-Command py, python3, python -ErrorAction SilentlyContinue | Select-Object -First 1
if ($py) {
    $lint = Join-Path $HOME '.claude/skills/repo-docs-wiki/scripts/lint_docs_wiki.py'
    if ((Test-Path docs) -and (Test-Path $lint)) { if ($py.Name -like 'py*') { & $py.Source -3 -I $lint . } else { & $py.Source -I $lint . } } else { Write-Host 'docs lint skipped' }
} else { Write-Host 'verify: python not found, skipping docs lint' }
foreach ($artifact in @('openapi/openapi.json', 'openapi/endpoint-inventory.txt')) {
    if (-not (Test-Path $artifact)) {
        Write-Error "verify: missing $artifact; run scripts/update-contract.sh and commit the result"
        exit 1
    }
}
& ./mvnw.cmd -B verify
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'verify: OK'
