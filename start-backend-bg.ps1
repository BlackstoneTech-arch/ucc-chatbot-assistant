#Requires -Version 5.1
$ErrorActionPreference = "Continue"
$ProjectRoot = "C:\Users\M\Documents\ucc-chatbot-assistant"
$LogFile = Join-Path $ProjectRoot "logs\backend.log"
if (-not (Test-Path (Split-Path -Parent $LogFile))) { New-Item -ItemType Directory -Path (Split-Path -Parent $LogFile) -Force | Out-Null }

Get-Content (Join-Path $ProjectRoot ".env") | ForEach-Object {
    if ($_ -match '^(DB_|AI_|JWT_|ADMIN_|FRONTEND_URL|CORS_|ENCRYPTION_)') {
        $parts = $_ -split '=', 2
        $name = $parts[0]
        $val = $parts[1]
        [System.Environment]::SetEnvironmentVariable($name, $val, "Process")
    }
}

Write-Host "DB_URL=$($env:DB_URL)"
Write-Host "AI_PROVIDER=$($env:AI_PROVIDER)"

$javaExe = Join-Path $ProjectRoot ".tools\java\bin\java.exe"
if (-not (Test-Path $javaExe)) {
    $javaExe = (Get-Command java -ErrorAction SilentlyContinue).Path
}
if (-not $javaExe -or -not (Test-Path $javaExe)) {
    Write-Host "Java not found. Install JDK 25+ or run build-backend.ps1 first." -ForegroundColor Red
    exit 1
}

$jar = Join-Path $ProjectRoot "backend\target\ucc-chatbot-1.0.0.jar"
if (-not (Test-Path $jar)) {
    Write-Host "JAR not found at $jar. Run build-backend.ps1 first." -ForegroundColor Red
    exit 1
}

Write-Host "Starting Java..."
& $javaExe -Xmx768m -jar $jar --server.port=8081 *>&1 | Tee-Object -FilePath $LogFile -Append
