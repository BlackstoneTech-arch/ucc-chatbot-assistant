#Requires -Version 5.1
[CmdletBinding()]
param(
    [switch]$SkipTests,
    [switch]$Clean,
    [switch]$Package
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$ProjectRoot = Split-Path -Parent $PSCommandPath
$BackendDir = Join-Path $ProjectRoot "backend"
$ToolsDir = Join-Path $ProjectRoot ".tools"
$MavenDir = Join-Path $ToolsDir "maven"
$MavenZip = Join-Path $ToolsDir "maven.zip"
$JavaDir = Join-Path $ToolsDir "java"
$LogDir = Join-Path $ProjectRoot "logs"
$LogFile = Join-Path $LogDir "build.log"

if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir -Force | Out-Null }
if (-not (Test-Path $ToolsDir)) { New-Item -ItemType Directory -Path $ToolsDir -Force | Out-Null }

function Write-Log {
    param([string]$Message, [string]$Color = "Cyan")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[$timestamp] $Message"
    Write-Host $line -ForegroundColor $Color
    Add-Content -LiteralPath $LogFile -Value $line
}

function Find-Java {
    $candidates = @(
        (Get-Command java -ErrorAction SilentlyContinue).Path,
        "$env:JAVA_HOME\bin\java.exe",
        "C:\Program Files\Java\jdk-17\bin\java.exe",
        "C:\Program Files\Java\jdk-21\bin\java.exe",
        "C:\Program Files\Eclipse Adoptium\jdk-17*\bin\java.exe",
        "C:\Program Files\Microsoft\jdk-17*\bin\java.exe"
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
        "C:\Program Files\Apache Maven\bin\mvn.cmd",
        (Get-ChildItem -LiteralPath $MavenDir -Filter "mvn.cmd" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
    )
    foreach ($c in $candidates) {
        if ($c -and (Test-Path $c)) { return $c }
    }
    return $null
}

function Install-Maven {
    if (Test-Path (Join-Path $MavenDir "bin\mvn.cmd")) {
        return (Get-ChildItem -LiteralPath $MavenDir -Filter "mvn.cmd" -Recurse | Select-Object -First 1).FullName
    }
    Write-Log "Downloading Apache Maven..." "Yellow"
    $url = "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $url -OutFile $MavenZip -UseBasicParsing
    Write-Log "Extracting Maven to $MavenDir..." "Yellow"
    Expand-Archive -LiteralPath $MavenZip -DestinationPath $ToolsDir -Force
    Remove-Item $MavenZip -Force
    return (Get-ChildItem -LiteralPath $ToolsDir -Filter "mvn.cmd" -Recurse | Select-Object -First 1).FullName
}

function Install-Java {
    if (Test-Path (Join-Path $JavaDir "bin\java.exe")) {
        return (Join-Path $JavaDir "bin\java.exe")
    }
    Write-Log "Downloading Eclipse Temurin JDK 17..." "Yellow"
    $url = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.11_9.zip"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $zip = Join-Path $ToolsDir "jdk.zip"
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    Write-Log "Extracting JDK to $JavaDir..." "Yellow"
    Expand-Archive -LiteralPath $zip -DestinationPath $ToolsDir -Force
    $jdkFolder = Get-ChildItem -LiteralPath $ToolsDir -Filter "jdk-*" -Directory | Select-Object -First 1
    if ($jdkFolder) {
        Rename-Item -LiteralPath $jdkFolder.FullName -NewName "java" -Force
    }
    Remove-Item $zip -Force
    return (Join-Path $JavaDir "bin\java.exe")
}

Write-Log "=== UCC Chatbot Build Script ===" "Green"

$javaExe = Find-Java
if (-not $javaExe) {
    Write-Log "Java not found, downloading..." "Yellow"
    $javaExe = Install-Java
}
Write-Log "Using Java: $javaExe" "Cyan"
& $javaExe -version 2>&1 | ForEach-Object { Write-Log "  $_" "DarkGray" }

$mvnCmd = Find-Maven
if (-not $mvnCmd) {
    Write-Log "Maven not found, downloading..." "Yellow"
    $mvnCmd = Install-Maven
}
Write-Log "Using Maven: $mvnCmd" "Cyan"

if (-not (Test-Path $BackendDir)) {
    Write-Log "Backend directory not found: $BackendDir" "Red"
    exit 1
}

Push-Location $BackendDir
try {
    $goals = @("compile")
    if ($Clean) { $goals = @("clean", "compile") }
    if (-not $SkipTests -and -not $Package) { $goals = @("test", "compile") }
    if ($Package) { $goals = @("clean", "package", "-DskipTests") }

    Write-Log "Running: mvn $($goals -join ' ')" "Yellow"
    $env:JAVA_HOME = (Split-Path -Parent (Split-Path -Parent $javaExe))
    & $mvnCmd @goals 2>&1 | Tee-Object -FilePath $LogFile -Append | ForEach-Object {
        $color = if ($_ -match "BUILD (FAILURE|SUCCESS)") { "Yellow" } else { "DarkGray" }
        Write-Host $_ -ForegroundColor $color
    }
    if ($LASTEXITCODE -ne 0) {
        Write-Log "BUILD FAILED (exit code $LASTEXITCODE)" "Red"
        exit $LASTEXITCODE
    }
    Write-Log "BUILD SUCCESS" "Green"
    if ($Package) {
        $jar = Get-ChildItem -LiteralPath "target" -Filter "*.jar" | Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } | Select-Object -First 1
        if ($jar) { Write-Log "JAR: $($jar.FullName)" "Cyan" }
    }
}
finally {
    Pop-Location
}
