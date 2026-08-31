#Requires -Version 5.1
[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [int]$Port = 8081
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSCommandPath
$BackendDir = Join-Path $ProjectRoot "backend"
$LogDir = Join-Path $ProjectRoot "logs"
$LogFile = Join-Path $LogDir "backend.log"

if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir -Force | Out-Null }

function Find-Java {
    $candidates = @(
        (Get-Command java -ErrorAction SilentlyContinue).Path,
        "$env:JAVA_HOME\bin\java.exe",
        "C:\Program Files\Java\jdk-17\bin\java.exe",
        "C:\Program Files\Java\jdk-21\bin\java.exe",
        (Join-Path $ProjectRoot ".tools\java\bin\java.exe")
    )
    foreach ($c in $candidates) {
        if ($c -and (Test-Path $c)) { return $c }
    }
    return $null
}

function Find-Maven {
    $candidates = @(
        (Get-Command mvn -ErrorAction SilentlyContinue).Path,
        "$env:MAVEN_HOME\bin\mvn.cmd",
        (Get-ChildItem -LiteralPath (Join-Path $ProjectRoot ".tools") -Filter "mvn.cmd" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
    )
    foreach ($c in $candidates) {
        if ($c -and (Test-Path $c)) { return $c }
    }
    return $null
}

$javaExe = Find-Java
if (-not $javaExe) {
    Write-Host "Java not found. Run build-backend.ps1 first." -ForegroundColor Red
    exit 1
}

if (-not $SkipBuild) {
    $mvnCmd = Find-Maven
    if (-not $mvnCmd) {
        Write-Host "Maven not found. Run build-backend.ps1 first." -ForegroundColor Red
        exit 1
    }
    Push-Location $BackendDir
    try {
        Write-Host "Compiling backend..." -ForegroundColor Yellow
        & $mvnCmd compile 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Compilation failed. Run build-backend.ps1 -Clean to see details." -ForegroundColor Red
            exit 1
        }
    }
    finally {
        Pop-Location
    }
}

$env:JAVA_HOME = (Split-Path -Parent (Split-Path -Parent $javaExe))
$env:DB_URL = if ($env:DB_URL) { $env:DB_URL } else { "jdbc:mysql://localhost:3306/ucc_chatbot_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" }
$env:DB_TYPE = if ($env:DB_TYPE) { $env:DB_TYPE } else { "mysql" }
$env:DB_DRIVER = if ($env:DB_DRIVER) { $env:DB_DRIVER } else { "com.mysql.cj.jdbc.Driver" }
$env:DB_DIALECT = if ($env:DB_DIALECT) { $env:DB_DIALECT } else { "org.hibernate.dialect.MySQLDialect" }
$env:DB_USERNAME = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$env:DB_PASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "" }
$env:JWT_SECRET = if ($env:JWT_SECRET) { $env:JWT_SECRET } else { "ucc-chatbot-development-secret-please-change-in-production-32bytes" }
$env:ADMIN_EMAIL = if ($env:ADMIN_EMAIL) { $env:ADMIN_EMAIL } else { "admin@ucc.co.tz" }
$env:ADMIN_PASSWORD = if ($env:ADMIN_PASSWORD) { $env:ADMIN_PASSWORD } else { "Admin@123" }
$env:ADMIN_NAME = if ($env:ADMIN_NAME) { $env:ADMIN_NAME } else { "UCC Administrator" }

$classesDir = Join-Path $BackendDir "target\classes"
if (-not (Test-Path $classesDir)) {
    Write-Host "Classes not built. Run without -SkipBuild." -ForegroundColor Red
    exit 1
}

Write-Host "Starting UCC Chatbot backend on port $Port..." -ForegroundColor Green
Write-Host "Logs: $LogFile" -ForegroundColor DarkGray

Push-Location $BackendDir
try {
    $args = @(
        "-cp", "target\classes;$($env:CLASSPATH)",
        "com.ucc.chatbot.UccChatbotApplication"
    )
    $serverArgs = @("--server.port=$Port")
    & $javaExe @args $serverArgs 2>&1 | Tee-Object -FilePath $LogFile -Append
}
finally {
    Pop-Location
}
