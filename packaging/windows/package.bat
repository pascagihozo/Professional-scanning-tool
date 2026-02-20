@echo off
setlocal enabledelayedexpansion

echo.
echo ============================================================
echo   Pascal Scanning Client - Windows Installer Builder
echo ============================================================
echo.

REM ── Resolve project root ─────────────────────────────────────
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%..\.."
set "PROJECT_ROOT=%cd%"
echo Project Root: %PROJECT_ROOT%
echo.

REM Prevent JVM options leaking in from the environment
set "_JAVA_OPTIONS="

REM ── 1. Verify JDK 17 + jpackage ──────────────────────────────
echo [Pre-flight] Checking build prerequisites...

jpackage --version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo  ERROR: jpackage not found.
    echo  Install JDK 17 or later: https://adoptium.net
    echo  Make sure it is on your PATH.
    pause & exit /b 1
)
for /f "tokens=*" %%V in ('jpackage --version 2^>nul') do echo          jpackage: %%V

REM ── 2. Locate WiX 3.x (required by jpackage on Java 17) ──────
REM  WiX 4.x (dotnet tool "wix") is NOT compatible with JDK 17 jpackage.
REM  Only WiX 3.x (candle.exe + light.exe) is supported here.

set "WIX_BIN="

REM 2a. candle.exe already on PATH?
where candle >nul 2>&1
if !errorlevel! equ 0 (
    for /f "tokens=*" %%P in ('where candle') do (
        for %%D in ("%%~dpP.") do set "WIX_BIN=%%~fD"
    )
    echo          WiX 3.x  : !WIX_BIN!
    goto :wix_ok
)

REM 2b. Check common WiX 3.x install paths
for %%P in (
    "C:\Program Files (x86)\WiX Toolset v3.14\bin"
    "C:\Program Files (x86)\WiX Toolset v3.11\bin"
    "C:\Program Files (x86)\WiX Toolset v3.10\bin"
    "C:\Program Files\WiX Toolset v3.14\bin"
    "C:\Program Files\WiX Toolset v3.11\bin"
) do (
    if exist "%%~P\candle.exe" (
        set "WIX_BIN=%%~P"
        echo          WiX 3.x  : !WIX_BIN!
        set "PATH=!PATH!;!WIX_BIN!"
        goto :wix_ok
    )
)

REM 2c. Try silent install via winget (Windows 10 1709+ / Windows 11)
echo.
echo  WiX Toolset 3.x not found. Trying automatic install via winget...
echo  (One-time setup, approx. 50 MB download)
echo.
winget install --id WiX.WiX --silent --accept-package-agreements --accept-source-agreements 2>nul
if !errorlevel! equ 0 (
    REM Search again after install
    for %%P in (
        "C:\Program Files (x86)\WiX Toolset v3.14\bin"
        "C:\Program Files (x86)\WiX Toolset v3.11\bin"
    ) do (
        if exist "%%~P\candle.exe" (
            set "WIX_BIN=%%~P"
            set "PATH=!PATH!;!WIX_BIN!"
            echo  WiX installed successfully: !WIX_BIN!
            goto :wix_ok
        )
    )
    where candle >nul 2>&1
    if !errorlevel! equ 0 goto :wix_ok
)

REM 2d. Give up — print clear instructions
echo.
echo ============================================================
echo  WiX Toolset 3.x is required to build the MSI installer.
echo  Install it with one of the options below, then re-run:
echo.
echo    Option A - winget (Windows 10/11):
echo      winget install WiX.WiX
echo.
echo    Option B - Chocolatey:
echo      choco install wixtoolset
echo.
echo    Option C - Direct download (free, ~50 MB):
echo      https://github.com/wixtoolset/wix3/releases/latest
echo.
echo  IMPORTANT: WiX 4.x is NOT compatible with JDK 17 jpackage.
echo             Install WiX 3.11 or 3.14 only.
echo ============================================================
echo.
pause & exit /b 1

:wix_ok

REM ── 3. Maven build ───────────────────────────────────────────
echo.
echo [1/3] Building application JAR...
call mvn clean package -DskipTests -q
if !errorlevel! neq 0 (
    echo ERROR: Maven build failed. Run "mvn clean package" to see details.
    pause & exit /b 1
)
if not exist "%PROJECT_ROOT%\target\scanner-desktop-client.jar" (
    echo ERROR: Expected JAR not found at target\scanner-desktop-client.jar
    pause & exit /b 1
)
echo       Done.

REM ── 4. Prepare bundle input ───────────────────────────────────
echo.
echo [2/3] Assembling application bundle...
set "BUNDLE_INPUT=%PROJECT_ROOT%\packaging\windows\target\bundle-input"
if exist "%BUNDLE_INPUT%" rmdir /s /q "%BUNDLE_INPUT%"
mkdir "%BUNDLE_INPUT%"

copy /y "%PROJECT_ROOT%\target\scanner-desktop-client.jar" "%BUNDLE_INPUT%\" >nul

REM Include JACOB native library (JacobBootstrap looks for it in lib/ relative to the JAR)
if exist "%PROJECT_ROOT%\lib" (
    mkdir "%BUNDLE_INPUT%\lib"
    xcopy /S /E /Y "%PROJECT_ROOT%\lib\*" "%BUNDLE_INPUT%\lib\" >nul
)

REM Include VBS background launcher script
if exist "%PROJECT_ROOT%\packaging\windows\run-background.vbs" (
    copy /y "%PROJECT_ROOT%\packaging\windows\run-background.vbs" "%BUNDLE_INPUT%\" >nul
)
echo       Done.

REM ── 5. jpackage — build MSI installer ────────────────────────
echo.
echo [3/3] Building Windows installer (.msi)...
cd /d "%PROJECT_ROOT%\packaging\windows"
if exist "dist" rmdir /s /q "dist"
mkdir "dist"
if exist "target\temp-jpackage" rmdir /s /q "target\temp-jpackage"
mkdir "target\temp-jpackage"

REM NOTE: No --runtime-image and no --add-modules.
REM  jpackage auto-detects required modules via jdeps and runs jlink internally.
REM  Using --add-modules causes the launcher EXE to inflate to 60+ MB on Oracle
REM  JDK 17.0.12 Windows (jpackage bug). Auto-detection produces a correct ~0.5 MB launcher.
REM
REM  Auto-start on boot is handled by AutoStartManager.java inside the app: on first
REM  launch it writes a HKCU Run registry entry so the app starts at every user logon.
REM
REM  --type msi            MSI installer (no WiX Burn bootstrapper overhead)
REM  --win-dir-chooser     User can pick the install directory
REM  --win-menu            Creates a Start Menu entry
REM  --win-shortcut        Creates a Desktop shortcut
REM  --win-upgrade-uuid    Stable GUID so re-installs upgrade in place, not side-by-side
REM
REM NOTE: icon path uses no variable — batch quoting with embedded paths is unreliable.
REM  We cd'd into packaging\windows above, so pascal.ico is a safe relative path.

if exist "pascal.ico" (
    jpackage ^
        -J-Xmx1g ^
        --temp "target\temp-jpackage" ^
        --input "target\bundle-input" ^
        --main-jar scanner-desktop-client.jar ^
        --main-class com.lci.scannerdesktop.ScannerDesktopApplication ^
        --name "Pascal Scanning Tool" ^
        --vendor "Pascal Gihozo" ^
        --description "Professional Document Scanning Solution" ^
        --app-version 1.0.0 ^
        --dest "dist" ^
        --icon pascal.ico ^
        --type msi ^
        --win-dir-chooser ^
        --win-menu ^
        --win-menu-group "Pascal Scanning Tool" ^
        --win-shortcut ^
        --win-upgrade-uuid "C7D2A1B4-93F6-4E80-B5D1-2A3C4F567890" ^
        --java-options "-Dfile.encoding=UTF-8 -Xmx512m"
) else (
    jpackage ^
        -J-Xmx1g ^
        --temp "target\temp-jpackage" ^
        --input "target\bundle-input" ^
        --main-jar scanner-desktop-client.jar ^
        --main-class com.lci.scannerdesktop.ScannerDesktopApplication ^
        --name "Pascal Scanning Tool" ^
        --vendor "Pascal Gihozo" ^
        --description "Professional Document Scanning Solution" ^
        --app-version 1.0.0 ^
        --dest "dist" ^
        --type msi ^
        --win-dir-chooser ^
        --win-menu ^
        --win-menu-group "Pascal Scanning Tool" ^
        --win-shortcut ^
        --win-upgrade-uuid "C7D2A1B4-93F6-4E80-B5D1-2A3C4F567890" ^
        --java-options "-Dfile.encoding=UTF-8 -Xmx512m"
)

if !errorlevel! neq 0 (
    echo.
    echo ERROR: jpackage failed.
    echo.
    echo Common causes:
    echo   - candle.exe / light.exe not on PATH (WiX not correctly installed)
    echo   - A previous install of "Pascal Scanning Tool" left stale registry entries
    echo   - Antivirus quarantined files in target\temp-jpackage
    echo.
    pause & exit /b 1
)

REM ── Done ─────────────────────────────────────────────────────
echo.
echo ============================================================
echo   INSTALLER READY
echo.
for /f "tokens=*" %%F in ('dir /b "dist\*.msi" 2^>nul') do (
    echo   File : %PROJECT_ROOT%\packaging\windows\dist\%%F
    for %%S in ("dist\%%F") do echo   Size : %%~zS bytes
)
echo.
echo   Distribute this single .msi to end users.
echo   They do NOT need Java or any other software installed.
echo.
echo   On first launch the app registers itself to start automatically
echo   at every Windows logon (runs silently in the background).
echo.
echo   After installing, users open any browser and visit:
echo     http://127.0.0.1:17070/ui
echo.
echo   To disable auto-start: open Task Manager ^> Startup apps
echo                           and disable "Pascal Scanning Tool".
echo ============================================================
echo.
explorer "%PROJECT_ROOT%\packaging\windows\dist"
pause
