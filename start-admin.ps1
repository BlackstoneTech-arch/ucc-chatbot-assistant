#Requires -Version 5.1
[CmdletBinding()]
param(
    [int]$Port = 3001
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSCommandPath
$FrontendDir = Join-Path $ProjectRoot "frontend"
$LogDir = Join-Path $ProjectRoot "logs"
$LogFile = Join-Path $LogDir "admin.log"

if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir -Force | Out-Null }
if (-not (Test-Path $FrontendDir)) {
    Write-Host "Frontend directory not found: $FrontendDir" -ForegroundColor Red
    exit 1
}

$npmCmd = (Get-Command npx -ErrorAction SilentlyContinue).Path
if (-not $npmCmd) {
    Write-Host "npx not found. Please install Node.js." -ForegroundColor Red
    exit 1
}

Write-Host "Starting UCC Admin Dashboard on port $Port..." -ForegroundColor Green
Write-Host "URL: http://localhost:$Port/admin/news.html" -ForegroundColor Cyan
Write-Host "Logs: $LogFile" -ForegroundColor DarkGray

Push-Location $FrontendDir
try {
    & npx serve . -l $Port 2>&1 | Tee-Object -FilePath $LogFile -Append
}
finally {
    Pop-Location
}