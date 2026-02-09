# Codebase Structure and Functions - Alonix Scanning Tool

## 1. Overview
The Alonix Scanning Tool (Pascal Scanning Tool) is a professional cross-platform document scanning solution. It provides a RESTful API and a web interface to interact with local and network scanners across Windows, Linux, and macOS.

## 2. Core Technology Stack
- **Backend**: Spring Boot 2.5.6 (Java 17)
- **Frontend**: Vanilla HTML/JS/CSS (Modern Web interface)
- **Native Bridges**:
  - **Windows**: JACOB (Java COM Bridge) for WIA (Windows Image Acquisition)
  - **Linux/macOS**: SANE (Scanner Access Now Easy) via `scanimage` CLI
  - **Network**: eSCL (AirPrint/Mopria) protocol over HTTP/HTTPS
- **PDF Generation**: iText 5.5.13.2

## 3. Directory Structure
```
alonix-scanning-tool/
├── src/main/java/com/lci/scannerdesktop/
│   ├── ScannerDesktopApplication.java   # Entry point
│   ├── api/                             # REST Controller & Facade
│   │   ├── LocalApiController.java      # Main API endpoints
│   │   ├── ScannerFacade.java           # Backend orchestration & Caching
│   │   ├── LocalUiController.java       # UI delivery
│   │   └── ... (DTOs and Configs)
│   ├── boot/                            # Startup listeners
│   │   └── AutoStartManager.java
│   ├── escl/                            # Network (eSCL) scanning
│   │   ├── ESCLService.java             # Network discovery & scan logic
│   │   └── EsclConfig.java
│   ├── sane/                            # Linux (SANE) scanning
│   │   └── SaneService.java             # scanimage wrapper logic
│   └── wia/                             # Windows (WIA) scanning
│       ├── WIAService.java              # JACOB/WIA integration logic
│       └── JacobBootstrap.java          # Native DLL loader
├── src/main/resources/
│   ├── application.properties           # System configuration
│   └── ui/                              # Web frontend assets
├── lib/                                 # Native JACOB libraries
├── packaging/                           # Native installer scripts (.exe, .pkg, .deb)
└── scanner-desktop-client/              # JRE runtime for distributions
```

## 4. Key Functions and API Endpoints

### LocalApiController (`/v1`)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Returns service status and version information. |
| `/scanners` | GET | Lists all discovered scanners (Cached manually). |
| `/capabilities` | GET | Retrieves DPI, format, and color support for a specific scanner. |
| `/scan` | POST | Triggers a scan operation based on `ScanRequestDTO`. |
| `/refresh` | POST | Clears the scanner cache and re-triggers discovery. |

### core Logic
- **`ScannerFacade.scan(ScanRequestDTO req)`**: Receives a scan request, identifies the backend (WIA, SANE, or eSCL) based on the `scannerId` prefix, and delegates the operation.
- **`ScannerFacade.discoverScannersInternal()`**: Parallels discovery across all three backends to populate the scanner cache.
- **`WIAService.scan(ScanOptions options)`**: Uses COM `Wia.ImageProcess` to capture and convert images on Windows.
- **`SaneService.scan(ScanOptions options)`**: Executes `scanimage` with calculated resolutions and modes.
- **`ESCLService.probeIp(String ip)`**: Probes a specific IP address for eSCL compliance during auto-discovery.

## 5. Backend Logic Patterns
Each backend service follows a similar pattern for portability:
1. **`discoverScanners()`**: Returns a list of `ScannerInfo` objects.
2. **`scan(ScanOptions)`**: Returns a `ScanResult` containing Base64 file data or an error message.
3. **`toPdf(byte[] data)`**: Utility to wrap scanned images into a PDF document if requested.

## 6. Startup Workflow
1. **`ScannerDesktopApplication`** boots Spring Boot.
2. **`JacobBootstrap`** ensures native WIA DLLs are loaded.
3. **`AutoStartManager`** triggers initial scanner discovery via `ScannerFacade`.
4. The server starts on port `17070` by default.
