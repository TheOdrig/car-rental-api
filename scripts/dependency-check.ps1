$ErrorActionPreference = 'Stop'
Set-Location (git rev-parse --show-toplevel)
if (-not $env:NVD_API_KEY) {
    Write-Warning 'dependency-check: NVD_API_KEY is not set; the first run downloads the whole NVD database and is very slow without a key'
}
& ./mvnw.cmd -B -Pdependency-check -DskipTests org.owasp:dependency-check-maven:12.2.2:check
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'dependency-check: OK'
