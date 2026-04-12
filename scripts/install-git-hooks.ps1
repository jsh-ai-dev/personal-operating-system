param()

$ErrorActionPreference = "Stop"

$repoRoot = (& git rev-parse --show-toplevel 2>$null)
if (-not $repoRoot) {
    Write-Error "Git 저장소 루트를 찾을 수 없습니다."
    exit 1
}

Set-Location $repoRoot

& git config core.hooksPath .githooks
Write-Host "Git hooks path configured: .githooks"
Write-Host "pre-commit hook is now active."

