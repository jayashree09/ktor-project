# Quick Setup Guide

## Step 1: Start PostgreSQL Database

### Option A: Using Docker (Recommended - if Docker Desktop is running)
```powershell
docker-compose up -d
```

**If Docker Desktop is not running:**
1. Start Docker Desktop application
2. Wait for it to fully start (whale icon in system tray)
3. Then run the command above

### Option B: Using Local PostgreSQL
1. Make sure PostgreSQL is installed and running
2. Create the database:
```sql
CREATE DATABASE productdb;
```

## Step 2: Set Environment Variables (Optional)

If your database uses different credentials, set these in PowerShell:

```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/productdb"
$env:DATABASE_USER="postgres"
$env:DATABASE_PASSWORD="postgres"
```

## Step 3: Build and Run

### If you have Gradle installed globally:
```powershell
gradle build
gradle run
```

### If you need to generate Gradle wrapper first:
```powershell
gradle wrapper
.\gradlew.bat build
.\gradlew.bat run
```

## Step 4: Test the API

Once the server starts (you'll see it running on port 8080), test it:

```powershell
# Test GET endpoint
Invoke-WebRequest -Uri "http://localhost:8080/products?country=Sweden" -Method GET

# Or using curl if you have it
curl "http://localhost:8080/products?country=Sweden"
```

## Troubleshooting

- **Database connection error**: Make sure PostgreSQL is running and accessible
- **Port 8080 already in use**: Stop any other application using port 8080
- **Gradle not found**: Install Gradle or use the wrapper (gradle wrapper command)
