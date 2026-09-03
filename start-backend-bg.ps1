#Requires -Version 5.1
$ErrorActionPreference = "Continue"
$ProjectRoot = "C:\Users\M\Documents\ucc-chatbot-assistant"
$LogFile = Join-Path $ProjectRoot "logs\backend.log"
if (-not (Test-Path (Split-Path -Parent $LogFile))) { New-Item -ItemType Directory -Path (Split-Path -Parent $LogFile) -Force | Out-Null }

Get-Content (Join-Path $ProjectRoot ".env") | ForEach-Object {
    if ($_ -match '^(DB_|AI_|JWT_|ADMIN_)') {
        $parts = $_ -split '=', 2
        $name = $parts[0]
        $val = $parts[1]
        [System.Environment]::SetEnvironmentVariable($name, $val, "Process")
    }
}

Write-Host "DB_URL=$($env:DB_URL)"
Write-Host "AI_PROVIDER=$($env:AI_PROVIDER)"
Write-Host "Starting Java..."

& "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot\bin\java.exe" -Xmx768m -jar (Join-Path $ProjectRoot "backend\target\ucc-chatbot-1.0.0.jar") --server.port=8081 *>&1 | Tee-Object -FilePath $LogFile -Append
