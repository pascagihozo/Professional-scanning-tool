# Create Pascal Scanning Tool Package
# This creates a deployment package for the Scanning Service

$ErrorActionPreference = "Stop"

$ProjectRoot = "C:\Users\Admin\Desktop\Professional-scanning-tool"
$PackagingDir = "$ProjectRoot\packaging\windows"
$DistDir = "$PackagingDir\dist"
$AppName = "Pascal-Scanning-Service"

Write-Host "=== Packaging Pascal Scanning Service ===" -ForegroundColor Cyan

# Ensure component is built
if (-not (Test-Path "$ProjectRoot\target\scanner-desktop-client.jar")) {
    Write-Host "Finalizing components..." -ForegroundColor Yellow
    Push-Location $ProjectRoot
    & mvn clean package -DskipTests -q
    Pop-Location
}

# Create dist folder
if (Test-Path $DistDir) { Remove-Item -Recurse -Force $DistDir }
New-Item -ItemType Directory -Path $DistDir | Out-Null

# Create deployment folder
$AppDir = "$DistDir\$AppName"
New-Item -ItemType Directory -Path $AppDir | Out-Null
New-Item -ItemType Directory -Path "$AppDir\bin" | Out-Null

# Copy core components
Copy-Item "$ProjectRoot\target\scanner-desktop-client.jar" "$AppDir\core-service.bin"

# Copy driver dependencies
if (Test-Path "$ProjectRoot\lib") {
    Copy-Item "$ProjectRoot\lib\*" "$AppDir\bin\" -Recurse
}

# Create Start-Service.vbs
$vbsContent = @'
' Pascal Scanning Service - Background Launcher
' Double-click to initialize service

Set WshShell = CreateObject("WScript.Shell")
Set FSO = CreateObject("Scripting.FileSystemObject")

strAppPath = FSO.GetParentFolderName(WScript.ScriptFullName)
strBinPath = strAppPath & "\bin"
strCore = strAppPath & "\core-service.bin"

' Locate Runtime
strRuntimeBin = ""

' Check primary path
strHomeEnv = WshShell.ExpandEnvironmentStrings("%JAVA_HOME%")
If strHomeEnv <> "%JAVA_HOME%" And strHomeEnv <> "" Then
    strTry = strHomeEnv & "\bin\javaw.exe"
    If FSO.FileExists(strTry) Then strRuntimeBin = strTry
End If

' Check system registry paths
If strRuntimeBin = "" Then
    arrPaths = Array( _
        "C:\Program Files\Java\jdk-17\bin\javaw.exe", _
        "C:\Program Files\Java\jdk-21\bin\javaw.exe", _
        "C:\Program Files\Eclipse Adoptium\jdk-17.0.10.7-hotspot\bin\javaw.exe" _
    )
    For Each p In arrPaths
        If FSO.FileExists(p) Then
            strRuntimeBin = p
            Exit For
        End If
    Next
End If

If strRuntimeBin = "" Then
    MsgBox "Scanning Service Initialization Error (Code 0x80040154)." & vbCrLf & _
           "Please ensure the required Scanning Runtime is installed or update your system drivers.", _
           vbCritical, "Pascal Scanning Service"
    WScript.Quit 1
End If

strOpts = "-Djava.library.path=""" & strBinPath & """ -Dfile.encoding=UTF-8 -Xmx512m"
strCmd = """" & strRuntimeBin & """ " & strOpts & " -jar """ & strCore & """"

WshShell.Run strCmd, 0, False
'@
$vbsContent | Out-File -FilePath "$AppDir\Start-Service.vbs" -Encoding ASCII

# Create Enable-Service.vbs
$autostartVbs = @'
' Register Scanning Service to start at Windows logon

Set WshShell = CreateObject("WScript.Shell")
Set FSO = CreateObject("Scripting.FileSystemObject")

strAppPath = FSO.GetParentFolderName(WScript.ScriptFullName)
strVbsPath = strAppPath & "\Start-Service.vbs"
strCmd = "wscript.exe //B //Nologo """ & strVbsPath & """"

WshShell.RegWrite "HKCU\Software\Microsoft\Windows\CurrentVersion\Run\PascalScanningTool", strCmd, "REG_SZ"

MsgBox "Pascal Scanning Service successfully registered for automatic startup.", vbInformation, "Service Enabled"
'@
$autostartVbs | Out-File -FilePath "$AppDir\Enable-Service.vbs" -Encoding ASCII

# Create Disable-Service.vbs
$uninstallVbs = @'
' Unregister Scanning Service from Windows startup

Set WshShell = CreateObject("WScript.Shell")
On Error Resume Next
WshShell.RegDelete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run\PascalScanningTool"
On Error GoTo 0

MsgBox "Pascal Scanning Service unregistered from Windows startup.", vbInformation, "Service Disabled"
'@
$uninstallVbs | Out-File -FilePath "$AppDir\Disable-Service.vbs" -Encoding ASCII

# Create README.txt
$readme = @"
Pascal Scanning Service
========================

QUICK START:
1. Double-click "Start-Service.vbs" to initialize the service
2. Securely access the interface at: http://127.0.0.1:17070/ui
3. Start scanning!

AUTOMATIC STARTUP:
- Double-click "Enable-Service.vbs" to start with Windows
- Double-click "Disable-Service.vbs" to stop automatic startup

The service runs invisibly in the background as a system process.

To stop the service:
- Open Task Manager > End the "Scanning Service" process or restart the computer.

"@
$readme | Out-File -FilePath "$AppDir\README.txt" -Encoding ASCII

# Create ZIP
$zipPath = "$DistDir\$AppName.zip"
if (Test-Path $zipPath) { Remove-Item $zipPath }
Compress-Archive -Path $AppDir -DestinationPath $zipPath

Write-Host "`n=== PACKAGE READY ===" -ForegroundColor Green
Write-Host "Location: $zipPath" -ForegroundColor Cyan
Write-Host "Size: $([math]::Round((Get-Item $zipPath).Length / 1MB, 2)) MB" -ForegroundColor Cyan

Write-Host "`nTo test locally:" -ForegroundColor Yellow
Write-Host "1. Run: $AppDir\Start-Background.vbs" -ForegroundColor White
Write-Host "2. Open: http://127.0.0.1:17070/ui" -ForegroundColor White
