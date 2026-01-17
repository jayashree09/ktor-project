# How to Run This Project - Step by Step

## Current Status Check

Based on your system, here's what you need to do:

### ✅ What You Have:
- Project files are ready
- Docker is installed (but might not be running)

### ⚠️ What's Missing:
- Gradle (not installed globally, and no wrapper files)

## Easiest Way to Run (3 Options)

### Option 1: Use the Setup Script (Recommended)

I've created a `setup.ps1` script that will check everything and guide you:

```powershell
cd C:\Users\jaysh\Downloads\ktor-project
.\setup.ps1
```

This will:
- Check if Java is installed
- Check Docker status
- Guide you through installing Gradle if needed
- Start PostgreSQL
- Build and run the project

### Option 2: Install Gradle via Chocolatey (Fastest)

If you have Chocolatey:

```powershell
choco install gradle
cd C:\Users\jaysh\Downloads\ktor-project
gradle wrapper
.\gradlew.bat build
.\gradlew.bat run
```

### Option 3: Use IntelliJ IDEA (Easiest if you have it)

1. Open IntelliJ IDEA
2. File → Open → Select `C:\Users\jaysh\Downloads\ktor-project`
3. IntelliJ will ask to download Gradle - click "Yes"
4. Wait for indexing to complete
5. Right-click `src/main/kotlin/com/example/Application.kt` → Run 'ApplicationKt'

## Manual Steps (If you prefer)

### Step 1: Install Gradle

1. Download from: https://gradle.org/releases/
2. Extract to `C:\Gradle`
3. Add `C:\Gradle\gradle-8.x\bin` to System PATH
4. Restart PowerShell
5. Verify: `gradle --version`

### Step 2: Create Gradle Wrapper

```powershell
cd C:\Users\jaysh\Downloads\ktor-project
gradle wrapper
```

### Step 3: Start PostgreSQL

**If Docker Desktop is installed:**
1. Start Docker Desktop (if not running)
2. Run: `docker-compose up -d`

**If PostgreSQL is installed locally:**
1. Start PostgreSQL service
2. Create database: `CREATE DATABASE productdb;`

### Step 4: Build and Run

```powershell
.\gradlew.bat build
.\gradlew.bat run
```

## Quick Test

Once running, test the API in a new PowerShell window:

```powershell
# Test GET endpoint
Invoke-RestMethod -Uri "http://localhost:8080/products?country=Sweden"
```

## Need Help?

If you encounter errors, check:
1. **Java**: `java -version` should show version 17 or higher
2. **Docker**: `docker ps` should work (Docker Desktop must be running)
3. **Database**: Make sure PostgreSQL is accessible on port 5432
4. **Port 8080**: Make sure nothing else is using port 8080
