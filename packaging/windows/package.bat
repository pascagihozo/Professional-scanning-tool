@echo off
REM Pascal Scanning Client - Windows Installer Build Script
REM Author: Pascal Gihozo
REM Description: Creates Windows EXE installer using jpackage

echo ========================================
echo   Pascal Scanning Client - Windows
echo   Professional Document Scanning Solution
echo ========================================
echo.

REM Check if Java 17+ is available
java -version 2>&1 | findstr "version" | findstr "17\|18\|19\|20\|21" >nul
if %errorlevel% neq 0 (
    echo ERROR: Java 17 or higher is required for jpackage
    echo Please install JDK 17+ and try again
    pause
    exit /b 1
)

REM Check if Maven is available
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven is required for building
    echo Please install Maven and try again
    pause
    exit /b 1
)

echo Creating Windows installer...
echo.

REM Create dist directory if it doesn't exist
if not exist dist mkdir dist

REM Create installer using jpackage
jpackage ^
    --input target ^
    --main-jar scanner-desktop-client.jar ^
    --main-class com.lci.scannerdesktop.ScannerDesktopApplication ^
    --name "Pascal Scanning Tool" ^
    --app-version 1.0.0 ^
    --vendor "Pascal Gihozo" ^
    --description "Professional Document Scanning Solution" ^
    --copyright "Copyright (c) 2025 Pascal Gihozo" ^
    --license-file ../LICENSE ^
    --type exe ^
    --dest dist ^
    --icon pascal.ico ^
    --win-console ^
    --win-shortcut ^
    --win-menu ^
    --win-menu-group "Pascal Scanning Tool" ^
    --win-dir-chooser ^
    --win-per-user-install

if %errorlevel% neq 0 (
    echo ERROR: jpackage failed to create installer
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Build completed successfully!
echo ========================================
echo.
echo Installer created: dist/Pascal Scanning Tool-1.0.0.exe
echo.
echo You can now distribute this installer to users.
echo.
pause