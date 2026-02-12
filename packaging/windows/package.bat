@echo off
setlocal enabledelayedexpansion
REM Pascal Scanning Client - Windows Standalone Packaging Script (Final Robust Version)

echo.
echo ========================================
echo   Pascal Scanning Client - Windows
echo   Standalone Application Bundler
echo ========================================
echo.

REM Set root
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%..\.."
set "PROJECT_ROOT=%cd%"
echo Project Root: %PROJECT_ROOT%

REM Clear problematic env var
set "_JAVA_OPTIONS="

REM Check if jpackage is available (Java 14+)
jpackage --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: jpackage is not available. 
    echo Please ensure you have JDK 17 or higher installed and that it's in your PATH.
    pause
    exit /b 1
)

REM 1. Clean and Build
echo.
echo [1/4] Building JAR...
call mvn clean package -DskipTests
if !errorlevel! neq 0 (
    echo ERROR: Maven build failed.
    pause
    exit /b 1
)

REM 2. Create Minimal Custom Runtime
echo.
echo [2/4] Creating Minimal Runtime...
if exist "packaging\windows\target\runtime" rmdir /s /q "packaging\windows\target\runtime"

jlink ^
    --add-modules java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.sql,java.xml,jdk.crypto.ec,jdk.jfr,jdk.unsupported,jdk.httpserver,jdk.charsets,jdk.localedata,jdk.crypto.mscapi,java.security.jgss,java.instrument,java.security.sasl ^
    --output "packaging\windows\target\runtime" ^
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
    copy "%JAVA_HOME%\bin\msvcp140.dll" "packaging\windows\target\runtime\bin\" >nul 2>&1
    copy "%JAVA_HOME%\bin\vcruntime140.dll" "packaging\windows\target\runtime\bin\" >nul 2>&1
    copy "%JAVA_HOME%\bin\vcruntime140_1.dll" "packaging\windows\target\runtime\bin\" >nul 2>&1
)

REM 3. Prepare Input
echo.
echo [3/4] Preparing bundle input...
if exist "packaging\windows\target\bundle-input" rmdir /s /q "packaging\windows\target\bundle-input"
mkdir "packaging\windows\target\bundle-input"
copy "target\scanner-desktop-client.jar" "packaging\windows\target\bundle-input\"
if exist "lib" (
    mkdir "packaging\windows\target\bundle-input\lib"
    xcopy /S /Y "lib" "packaging\windows\target\bundle-input\lib\"
)

REM 4. Run jpackage
echo.
echo [4/4] Running jpackage...
cd packaging\windows
if exist "dist" rmdir /s /q "dist"
mkdir "dist"
if exist "target\temp-jpackage" rmdir /s /q "target\temp-jpackage"
mkdir "target\temp-jpackage"

set "ICON_OPT="
if exist "pascal.ico" (
    set "ICON_OPT=--icon "pascal.ico""
)

jpackage ^
    --temp "target\temp-jpackage" ^
    --runtime-image "target\runtime" ^
    --input "target\bundle-input" ^
    --main-jar scanner-desktop-client.jar ^
    --main-class com.lci.scannerdesktop.ScannerDesktopApplication ^
    --name "Pascal Scanning Tool" ^
    --vendor "Pascal Gihozo" ^
    --app-version 1.0.0 ^
    --dest "dist" ^
    %ICON_OPT% ^
    --type app-image ^
    --win-console ^
    --java-options "-Dfile.encoding=UTF-8 -Xmx512m"

if !errorlevel! neq 0 (
    echo ERROR: jpackage failed.
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
pause
