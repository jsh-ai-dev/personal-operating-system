$ErrorActionPreference = 'Stop'

$rootPath = $PSScriptRoot

function Wait-DockerContainer {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $status = (& docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $Name 2>$null)
        if ($LASTEXITCODE -eq 0) {
            if ($status -eq 'healthy' -or $status -eq 'running') {
                Write-Host "  [ready] $Name ($status)"
                return
            }
            Write-Host "  [wait]  $Name ($status)"
        } else {
            Write-Host "  [wait]  $Name (not found)"
        }
        Start-Sleep -Seconds 2
    }

    throw "Timed out waiting for Docker container '$Name'."
}

function Wait-HttpEndpoint {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [string]$Name = $Url,
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3 | Out-Null
            Write-Host "  [ready] $Name"
            return
        } catch {
            Write-Host "  [wait]  $Name"
            Start-Sleep -Seconds 2
        }
    }

    throw "Timed out waiting for '$Name'."
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker command was not found. Start Docker Desktop first."
}

Write-Host "Starting mk1 infrastructure..."
& docker compose -f (Join-Path $rootPath "compose.yaml") up -d postgres redis elasticsearch
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ""
Write-Host "Waiting for mk1 infrastructure..."
Wait-DockerContainer -Name "personal-operating-system-mk1-postgres"
Wait-DockerContainer -Name "personal-operating-system-mk1-redis"
Wait-HttpEndpoint -Url "http://127.0.0.1:9200" -Name "personal-operating-system-mk1-elasticsearch"

Write-Host ""
Write-Host "Starting mk1 app..."
& (Join-Path $rootPath "bootRun.ps1") -SkipInfra
