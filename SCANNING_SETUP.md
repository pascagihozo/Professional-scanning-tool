# Document Scanning Feature Setup

## Overview
This application includes a comprehensive document scanning feature that integrates with Windows Image Acquisition (WIA) to scan documents directly into the system.

## Prerequisites
- Windows Operating System
- Scanner device connected and configured
- JACOB (Java-COM Bridge) library for WIA integration

## Setup Instructions

### 1. Maven Dependency
The JACOB dependency is already configured in `pom.xml`:
```xml
<dependency>
    <groupId>net.sf.jacob-project</groupId>
    <artifactId>jacob</artifactId>
    <version>1.21</version>
</dependency>
```

### 2. JACOB DLL Setup
The application will automatically try to load the JACOB DLL from:
1. **Classpath** (if included in the JAR)
2. **lib directory** (fallback option)

#### Manual Setup (if needed):
1. Download JACOB 1.21 from: https://github.com/freemansoft/jacob-project/releases
2. Extract the following files to `lib/` directory:
   - `jacob-1.21-x64.dll` (for 64-bit systems)
   - `jacob-1.21-x86.dll` (for 32-bit systems)

### 3. Scanner Configuration
- Ensure your scanner is properly installed and configured in Windows
- Test scanner functionality using Windows Fax and Scan or similar utility
- The application will automatically detect available WIA-compatible scanners

## Features

### Scanning Options
- **DPI**: 200, 300, 600
- **Color Modes**: Color, Grayscale, Black & White
- **Paper Sizes**: A4, Letter, Legal, Auto Detect
- **Output Formats**: PDF, JPEG, PNG, BMP
- **Preview**: Generate preview before final scan
- **Custom Filename**: Optional user-specified filename

### Integration
- Seamlessly integrated with outgoing request workflow
- Choice between file upload or document scanning
- Preview functionality for scan verification
- Automatic document upload to Alfresco repository

## Troubleshooting

### Common Issues
1. **"No scanners found"**
   - Check scanner is connected and powered on
   - Verify scanner drivers are installed
   - Test with Windows Fax and Scan

2. **"JACOB DLL not found"**
   - Check if DLL files are in `lib/` directory
   - Verify correct architecture (x64 vs x86)
   - Check application logs for detailed error messages

3. **"Scanning failed"**
   - Ensure scanner is not in use by another application
   - Check scanner settings and paper placement
   - Review application logs for specific error details

### Logs
Check application logs for scanning-related messages:
- Info level: Successful operations and configuration
- Warn level: Non-critical issues (missing DLLs, scanner warnings)
- Error level: Critical failures requiring attention

## Usage
1. Navigate to Create Outgoing Request
2. Select "Scan Document" as document source
3. Choose scanner from dropdown
4. Configure scan settings (DPI, color mode, etc.)
5. Click "Preview Scan" to test (optional)
6. Click "Scan Document" for final scan
7. Document is automatically integrated into the workflow

## Architecture
- **ScanningService**: High-level scanning operations
- **WIAUtil**: Low-level WIA COM integration
- **ScanningConfig**: JACOB DLL initialization
- **Models**: Scanner, ScanOptions, ScanResult data structures
- **UI Integration**: OutgoingRequestMB managed bean
