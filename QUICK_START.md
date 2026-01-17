# Quick Start - How to Run This Project

## Prerequisites Check

You need:
1. ✅ Java 17 or higher
2. ✅ PostgreSQL (via Docker or local installation)
3. ⚠️ Gradle (to build and run)

## Option 1: Install Gradle (Recommended)

### Windows Installation:

1. **Download Gradle:**
   - Visit: https://gradle.org/releases/
   - Download the latest binary-only distribution
   - Extract to `C:\Gradle` (or your preferred location)

2. **Add to PATH:**
   - Open System Environment Variables
   - Add `C:\Gradle\gradle-8.x\bin` to PATH
   - Restart PowerShell

3. **Verify:**
   ```powershell
   gradle --version
   ```

4. **Create Gradle Wrapper:**
   ```powershell
   cd C:\Users\jaysh\Downloads\ktor-project
   gradle wrapper
   ```

5. **Build and Run:**
   ```powershell
   .\gradlew.bat build
   .\gradlew.bat run
   ```

## Option 2: Use Chocolatey (Easier)

If you have Chocolatey package manager:

```powershell
choco install gradle
cd C:\Users\jaysh\Downloads\ktor-project
gradle wrapper
.\gradlew.bat build
.\gradlew.bat run
```

## Option 3: Use IntelliJ IDEA (If installed)

IntelliJ IDEA can automatically download Gradle and run the project:
1. Open IntelliJ IDEA
2. File → Open → Select the `ktor-project` folder
3. IntelliJ will detect Gradle and offer to download it
4. Right-click on `Application.kt` → Run

## Start Database First!

**Before running the app, start PostgreSQL:**

### If Docker Desktop is installed:
1. Start Docker Desktop
2. Run: `docker-compose up -d`

### If PostgreSQL is installed locally:
1. Make sure PostgreSQL service is running
2. Create database: `CREATE DATABASE productdb;`

## Once Running:

The server will start on: `http://localhost:8080`

Test it:
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/products?country=Sweden"
```
