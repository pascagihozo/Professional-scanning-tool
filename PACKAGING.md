# Packaging (jpackage)

Prerequisites
- Java 17+ and jpackage on PATH
- Build: `mvn -q -f scanner-desktop-client/pom.xml clean package`

Windows
- Script: `scanner-desktop-client/packaging/windows/package.bat`
- Output: `scanner-desktop-client/packaging/windows/out/*.exe`
- Include `scanner-desktop-client/lib/` with JACOB files next to the installed app for WIA

macOS (to add)
- Use jpackage `--type dmg` or `--type pkg`
- Add launch agent if you want auto-start

Linux (to add)
- Use jpackage `--type deb` or `--type rpm`
- Alternatively build AppImage

Notes
- The app binds to 127.0.0.1:17070 by default
- Configure CORS and optional shared-secret via `application.properties`
- Run headless service after install; use OS mechanisms to auto-start at login
