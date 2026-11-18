Set-Location (git rev-parse --show-toplevel)
git config core.hooksPath .githooks
Write-Host 'hooks enabled: core.hooksPath=.githooks'
