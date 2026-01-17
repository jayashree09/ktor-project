# Setup and Run Script for Ktor Project
Write-Host "=== Ktor Project Setup ===" -ForegroundColor Cyan

# Check Java
Write-Host "`n[1/4] Checking Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "✓ Java found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Java not found. Please install Java 17 or higher." -ForegroundColor Red
    exit 1
}

# Check Docker
Write-Host "`n[2/4] Checking Docker..." -ForegroundColor Yellow
try {
    docker --version | Out-Null
    Write-Host "✓ Docker found" -ForegroundColor Green
    
    # Check if Docker Desktop is running
    $dockerRunning = docker ps 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Docker is running" -ForegroundColor Green
    } else {
        Write-Host "⚠ Docker Desktop might not be running. Please start Docker Desktop." -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ Docker not found. You'll need PostgreSQL running manually." -ForegroundColor Yellow
}

# Check Gradle
Write-Host "`n[3/4] Checking Gradle..." -ForegroundColor Yellow
$gradleFound = $false
try {
    $gradleVersion = gradle --version 2>&1 | Select-String "Gradle"
    if ($gradleVersion) {
        Write-Host "✓ Gradle found: $gradleVersion" -ForegroundColor Green
        $gradleFound = $true
    }
} catch {
    Write-Host "⚠ Gradle not found globally" -ForegroundColor Yellow
}

# Check Gradle Wrapper
if (Test-Path ".\gradlew.bat") {
    Write-Host "✓ Gradle wrapper found" -ForegroundColor Green
    $gradleFound = $true
} else {
    Write-Host "⚠ Gradle wrapper not found" -ForegroundColor Yellow
}

if (-not $gradleFound) {
    Write-Host "`n❌ Gradle is required to build and run this project." -ForegroundColor Red
    Write-Host "`nPlease choose one option:" -ForegroundColor Yellow
    Write-Host "  1. Install Gradle: https://gradle.org/install/" -ForegroundColor White
    Write-Host "  2. Use IntelliJ IDEA to open this project (it will download Gradle automatically)" -ForegroundColor White
    Write-Host "  3. Install via Chocolatey: choco install gradle" -ForegroundColor White
    exit 1
}

# Start Database
Write-Host "`n[4/4] Setting up Database..." -ForegroundColor Yellow
if (Test-Path ".\docker-compose.yml") {
    Write-Host "Starting PostgreSQL with Docker Compose..." -ForegroundColor Cyan
    docker-compose up -d
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ PostgreSQL started" -ForegroundColor Green
        Start-Sleep -Seconds 3  # Wait for DB to be ready
    } else {
        Write-Host "⚠ Failed to start PostgreSQL. Make sure Docker Desktop is running." -ForegroundColor Yellow
    }
} else {
    Write-Host "⚠ docker-compose.yml not found. Make sure PostgreSQL is running manually." -ForegroundColor Yellow
}

# Build and Run Instructions
Write-Host "`n=== Ready to Run ===" -ForegroundColor Cyan
Write-Host "`nTo build the project:" -ForegroundColor Yellow
if (Test-Path ".\gradlew.bat") {
    Write-Host "  .\gradlew.bat build" -ForegroundColor White
} else {
    Write-Host "  gradle build" -ForegroundColor White
}

Write-Host "`nTo run the application:" -ForegroundColor Yellow
if (Test-Path ".\gradlew.bat") {
    Write-Host "  .\gradlew.bat run" -ForegroundColor White
} else {
    Write-Host "  gradle run" -ForegroundColor White
}

Write-Host "`nThe server will start on: http://localhost:8080" -ForegroundColor Green
Write-Host "`nPress any key to build and run now (or Ctrl+C to exit)..." -ForegroundColor Cyan
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

if (Test-Path ".\gradlew.bat") {
    .\gradlew.bat build
    if ($LASTEXITCODE -eq 0) {
        .\gradlew.bat run
    }
} else {
    gradle build
    if ($LASTEXITCODE -eq 0) {
        gradle run
    }
}
