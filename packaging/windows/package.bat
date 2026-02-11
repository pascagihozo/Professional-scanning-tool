@echo off
setlocal enabledelayedexpansion
REM Pascal Scanning Client - Windows Standalone Packaging Script (Final Robust Version)

echo.
echo ========================================
echo   Pascal Scanning Client - Windows
echo   Standalone Application Bundler
echo ========================================
echo.

<<<<<<< HEAD
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
=======
REM Set root
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%..\.."
set "PROJECT_ROOT=%cd%"
echo Project Root: %PROJECT_ROOT%

REM Clear problematic env var
set "_JAVA_OPTIONS="

REM 1. Clean and Build
>>>>>>> 9ec10fb3eb1e991fdded367f6760a38f0d11cb69
echo.
echo [1/4] Building JAR...
call mvn clean package -DskipTests
if !errorlevel! neq 0 (
    echo ERROR: Maven build failed.
    pause
    exit /b 1
)

<<<<<<< HEAD
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
=======
REM 2. Create Minimal Custom Runtime
echo.
echo [2/4] Creating Minimal Runtime...
if exist "target\runtime" rmdir /s /q "target\runtime"

jlink ^
    --add-modules java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.sql,java.xml,jdk.crypto.ec,jdk.jfr,jdk.unsupported,jdk.httpserver,jdk.charsets,jdk.localedata,jdk.crypto.mscapi,java.security.jgss,java.instrument,java.security.sasl ^
    --output "target\runtime" ^
    --strip-debug ^
    --no-man-pages ^
    --no-header-files ^
    --compress=2

if !errorlevel! neq 0 (
    echo ERROR: jlink failed.
    pause
    exit /b 1
)

REM Inject system DLLs to prevent Side-by-Side errors
echo Injecting runtime dependencies...
if not "%JAVA_HOME%"=="" (
    copy "%JAVA_HOME%\bin\msvcp140.dll" "target\runtime\bin\" >nul 2>&1
    copy "%JAVA_HOME%\bin\vcruntime140.dll" "target\runtime\bin\" >nul 2>&1
    copy "%JAVA_HOME%\bin\vcruntime140_1.dll" "target\runtime\bin\" >nul 2>&1
)

REM 3. Prepare Input
echo.
echo [3/4] Preparing bundle input...
if exist "target\bundle-input" rmdir /s /q "target\bundle-input"
mkdir "target\bundle-input"
copy "target\scanner-desktop-client.jar" "target\bundle-input\"
if exist "lib" (
    mkdir "target\bundle-input\lib"
    xcopy /S /Y "lib" "target\bundle-input\lib\"
)

REM 4. Run jpackage
echo.
echo [4/4] Running jpackage...
if exist "dist" rmdir /s /q "dist"
mkdir "dist"
if exist "target\temp-jpackage" rmdir /s /q "target\temp-jpackage"
mkdir "target\temp-jpackage"

set "ICON_OPT="
if exist "%SCRIPT_DIR%pascal.ico" (
    set "ICON_OPT=--icon "%SCRIPT_DIR%pascal.ico""
)

jpackage ^
    --temp "target\temp-jpackage" ^
    --runtime-image "target\runtime" ^
    --input "target\bundle-input" ^
>>>>>>> 9ec10fb3eb1e991fdded367f6760a38f0d11cb69
    --main-jar scanner-desktop-client.jar ^
    --main-class com.lci.scannerdesktop.ScannerDesktopApplication ^
    --name "Pascal Scanning Tool" ^
    --vendor "Pascal Gihozo" ^
<<<<<<< HEAD
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
=======
    --app-version 1.0.0 ^
    --dest "dist" ^
    %ICON_OPT% ^
    --type app-image ^
    --win-console ^
    --java-options "-Dfile.encoding=UTF-8 -Xmx512m"

if !errorlevel! neq 0 (
    echo ERROR: jpackage failed.
>>>>>>> 9ec10fb3eb1e991fdded367f6760a38f0d11cb69
    pause
    exit /b 1
)

REM Create a bulletproof fallback launcher
echo Creating fallback launcher...
set "LAUNCHER=dist\Pascal Scanning Tool\Run-Direct.bat"
echo @echo off > "%LAUNCHER%"
echo cd /d "%%~dp0" >> "%LAUNCHER%"
echo echo Starting Pascal Scanning Client... >> "%LAUNCHER%"
echo "runtime\bin\java.exe" -jar "app\scanner-desktop-client.jar" >> "%LAUNCHER%"
echo if %%errorlevel%% neq 0 pause >> "%LAUNCHER%"

echo.
echo ========================================
echo   BUILD COMPLETE
echo   1. Try running "Pascal Scanning Tool.exe"
echo   2. If it fails, try "Run-Direct.bat"
echo ========================================
echo.
<<<<<<< HEAD
if "%choice%"=="2" (
    echo Portable folder created: %DIST_FOLDER%\Pascal Scanning Tool
) else (
    echo Installer created: %DIST_FOLDER%\Pascal Scanning Tool-1.0.0.exe
)
echo.
echo You can now distribute this to users.
echo.
pause
=======
pause
>>>>>>> 9ec10fb3eb1e991fdded367f6760a38f0d11cb69
