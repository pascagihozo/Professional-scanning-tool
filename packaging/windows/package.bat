@echo off
REM Pascal Scanning Client - Windows Installer Build Script
REM Author: Pascal Gihozo
REM Description: Creates Windows EXE installer using jpackage

echo ========================================
echo   Pascal Scanning Client - Windows
echo   Professional Document Scanning Solution
echo ========================================
echo.

REM Check if jpackage is available (Java 14+)
jpackage --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: jpackage is not available. 
    echo Please ensure you have JDK 17 or higher installed and that it's in your PATH.
    pause
    exit /b 1
)

REM Check if Maven is available
call mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven is required for building
    echo Please install Maven and try again
    pause
    exit /b 1
)

echo Building project with Maven...
cd ../..
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Maven build failed
    pause
    exit /b 1
)
cd packaging/windows

echo Bundling native dependencies...
if not exist target mkdir target
copy /Y ..\..\target\scanner-desktop-client.jar target\
copy /Y ..\..\lib\jacob-1.21-x64.dll target\

echo.
echo Please choose a packaging type:
echo [1] EXE Installer (Requires WiX 3.11)
echo [2] Portable Folder (App-Image - No extra dependencies)
echo.
set /p choice="Enter choice [1 or 2]: "

echo.
echo ========================================
echo   Optimized Build Process
echo ========================================
echo.

REM Define required modules for a Spring Boot application
set MODULES=java.base,java.desktop,java.logging,java.naming,java.management,java.sql,java.xml,jdk.httpserver,java.net.http,java.security.jgss,jdk.crypto.ec

echo Step 1: Creating Minimal Runtime (jlink)...
echo This is the heavy part, using D: drive to save RAM...
if exist D:\jpackage_runtime rd /s /q D:\jpackage_runtime
call jlink ^
    --add-modules %MODULES% ^
    --output D:\jpackage_runtime ^
    --strip-debug ^
    --no-header-files ^
    --no-man-pages ^
    --strip-native-commands
if %errorlevel% neq 0 (
    echo ERROR: jlink failed. Your RAM might be too low.
    pause
    exit /b 1
)

echo.
echo Step 2: Packaging Application...

REM Find next available dist folder number
set DIST_NUM=1
:find_dist_folder
if exist "dist_%DIST_NUM%" (
    set /a DIST_NUM+=1
    goto find_dist_folder
)
set DIST_FOLDER=dist_%DIST_NUM%
mkdir %DIST_FOLDER%
echo Using output folder: %DIST_FOLDER%

if exist D:\jpackage_temp rd /s /q D:\jpackage_temp
mkdir D:\jpackage_temp

if "%choice%"=="2" goto appimage
goto exeinstaller

:appimage
echo Creating Portable Folder (App-Image)...
jpackage ^
    --type app-image ^
    --runtime-image D:\jpackage_runtime ^
    --input target ^
    --main-jar scanner-desktop-client.jar ^
    --main-class com.lci.scannerdesktop.ScannerDesktopApplication ^
    --name "Pascal Scanning Tool" ^
    --vendor "Pascal Gihozo" ^
    --dest %DIST_FOLDER% ^
    --temp D:\jpackage_temp ^
    --verbose ^
    --icon pascal.ico
goto checkresult

:exeinstaller
echo Creating Windows Installer (EXE)...
jpackage ^
    --input target ^
    --runtime-image D:\jpackage_runtime ^
    --main-jar scanner-desktop-client.jar ^
    --main-class com.lci.scannerdesktop.ScannerDesktopApplication ^
    --name "Pascal Scanning Tool" ^
    --app-version 1.0.0 ^
    --vendor "Pascal Gihozo" ^
    --description "Professional Document Scanning Solution" ^
    --copyright "Copyright (c) 2025 Pascal Gihozo" ^
    --license-file ../../LICENSE ^
    --type exe ^
    --dest %DIST_FOLDER% ^
    --temp D:\jpackage_temp ^
    --verbose ^
    --icon pascal.ico ^
    --win-console ^
    --win-shortcut ^
    --win-menu ^
    --win-menu-group "Pascal Scanning Tool" ^
    --win-dir-chooser ^
    --win-per-user-install
goto checkresult

:checkresult
if %errorlevel% neq 0 (
    echo.
    echo ERROR: jpackage failed.
    echo Check the verbose output above for clues.
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Build completed successfully!
echo ========================================
echo.
if "%choice%"=="2" (
    echo Portable folder created: %DIST_FOLDER%\Pascal Scanning Tool
) else (
    echo Installer created: %DIST_FOLDER%\Pascal Scanning Tool-1.0.0.exe
)
echo.
echo You can now distribute this to users.
echo.
pause