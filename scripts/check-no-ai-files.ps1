$ErrorActionPreference = 'Stop'
Set-Location (git rev-parse --show-toplevel)
$pattern = '(^|/)(\.kiro|kiro\.steering|\.agent|\.agents|\.claude|\.codex|\.obsidian|specs)/|(^|/)(AGENTS|CLAUDE)\.md$|^docs/|^(CHANGELOG|CONTRIBUTING)\.md$|Zone\.Identifier$'
$bad = git ls-files | Where-Object { $_ -match $pattern }
if ($bad) {
    Write-Error ("check-no-ai-files: tracked files that must stay local-only:`n" + ($bad -join "`n"))
    exit 1
}
Write-Host 'check-no-ai-files: OK'
