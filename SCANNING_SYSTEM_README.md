#  Scanning System - Cross-Platform Architecture

## Overview

The scanning system provides **universal scanning capabilities** across all operating systems and devices through a combination of server-side and client-side technologies. This architecture ensures that users can scan documents regardless of their operating system (Windows, macOS, Linux) or device type (desktop, laptop, mobile).

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Azure Linux Server                               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────────┐ │
│  │   eSCL Service  │  │   SANE Service  │  │      Web Services          │ │
│  │ (Network Scans) │  │ (Local Scans)   │  │ (Webcam/Mobile/File)      │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTP/HTTPS
                                    │
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Client Devices                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │
│  │   Windows   │  │   macOS     │  │   Linux     │  │   Mobile    │      │
│  │   Client    │  │   Client    │  │   Client    │  │   Device    │      │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘      │
│       │                   │               │               │              │
│       └───────────────────┼───────────────┼───────────────┘              │
│                           │               │                              │
│                    ┌─────────────────────────────────┐                    │
│                    │         Web Browser             │                    │
│                    │    (Chrome/FF/Safari/Edge)     │                    │
│                    └─────────────────────────────────┘                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

## How It Works in Your Environment

### 1. **Azure Linux Server (Deployment)**

The system runs on a Linux server in Azure and provides:

- **eSCL Network Scanner Support**: Discovers and connects to network scanners via HTTP/HTTPS
- **SANE Local Scanner Support**: Uses Linux SANE framework for local scanner access
- **Web Services**: Handles webcam captures, mobile uploads, and file uploads
- **Cross-Platform Compatibility**: All scanning operations work regardless of client OS

### 2. **Client Access Patterns**

#### **Windows Users (macBook, Windows Desktop)**
- Access via web browser (Chrome, Firefox, Edge, Safari)
- **Webcam Scanning**: Uses `navigator.mediaDevices.getUserMedia()` API
- **File Upload**: Drag & drop, file picker, traditional file input
- **Network Scanner Access**: Can use network scanners discovered by the server

#### **macOS Users**
- Same web-based access as Windows users
- **Camera Integration**: Native camera support through browser APIs
- **Network Scanner Support**: Full access to eSCL and IPP network scanners

#### **Linux Users**
- Web-based access identical to other platforms
- **Local Scanner Support**: Can use SANE-compatible local scanners
- **Network Scanner Access**: Full network scanner discovery and usage

#### **Mobile Users (iOS/Android)**
- **QR Code Workflow**: Scan QR code to upload documents from phone
- **Mobile Upload Page**: Simple mobile-optimized upload interface
- **Camera Integration**: Use phone camera to capture documents

## Scanning Methods Available

### 1. **Network Scanner Scanning (eSCL/IPP/WSD)**

**How it works:**
- Server automatically discovers network scanners on the local network
- Supports multiple protocols: eSCL (AirPrint), IPP, WSD
- Users can select from discovered network scanners
- Scanning happens on the server, results sent to client

**Supported devices:**
- HP LaserJet Pro MFP series
- Canon imageRUNNER series
- Brother MFC series
- Most modern network-enabled scanners

**Configuration:**
```yaml
network:
  scanner:
    discovery:
      enabled: true
      subnet:
        scan: true
        range: 20  # Scan first 20 IPs in each subnet
    manual:
      ips: "192.168.1.100,192.168.1.101"  # Known scanner IPs
```

### 2. **Webcam Scanning**

**How it works:**
- Uses browser's `getUserMedia()` API
- Works on HTTPS connections (required for camera access)
- Real-time camera preview and capture
- Image enhancement and processing on server

**Browser support:**
- Chrome 53+ ✅
- Firefox 36+ ✅
- Safari 11+ ✅
- Edge 12+ ✅

### 3. **Mobile Phone Scanning**

**How it works:**
1. User clicks "Mobile Scan" in web interface
2. System generates unique QR code with temporary token
3. User scans QR code with phone camera
4. Phone opens mobile upload page
5. User selects/captures document and uploads
6. Web interface polls for results and displays them

**Security features:**
- Temporary tokens with expiration (5 minutes default)
- Session-based validation
- Automatic cleanup of expired tokens

### 4. **File Upload Scanning**

**How it works:**
- Drag & drop file upload
- Traditional file input selection
- File picker API (modern browsers)
- Support for multiple formats: PDF, JPG, PNG, TIFF, BMP

## Network Scanner Discovery

### **Automatic Discovery Methods**

1. **Subnet Scanning**: Scans local network subnets for scanner endpoints
2. **mDNS Discovery**: Uses Bonjour/Zeroconf for automatic device discovery
3. **WSD Discovery**: Web Services for Devices protocol support
4. **Manual IP Configuration**: Administrators can add known scanner IPs

### **Discovery Process**

```
1. Network Interface Detection
   ↓
2. Subnet Analysis
   ↓
3. Port Scanning (80, 443, 8080, 8443, 631, 9100, 515)
   ↓
4. Protocol Testing (eSCL, IPP, WSD)
   ↓
5. Scanner Registration
   ↓
6. Capability Discovery
```

### **Configuration Example**

```yaml
# Add known scanner IPs
network:
  scanner:
    manual:
      ips: "192.168.1.100,192.168.1.101,10.0.0.50"
    
    # Enable subnet scanning
    subnet:
      scan: true
      range: 20
```

## Security Considerations

### **Network Access**
- Scanner discovery only scans local network subnets
- No external network access
- Configurable discovery zones and restrictions

### **Authentication**
- All scanning operations require valid user session
- Scanner management endpoints require `MODULE_ACCESS_SCANNING` authority
- Mobile upload tokens are temporary and session-bound

### **HTTPS Requirements**
- Camera access requires HTTPS (browser security requirement)
- Network scanner communication can use HTTP or HTTPS
- Configurable SSL certificate validation

## Performance and Scalability

### **Discovery Performance**
- Parallel scanner discovery using CompletableFuture
- Configurable timeouts and retry mechanisms
- Caching of discovered scanners (5-minute TTL by default)

### **Scanning Performance**
- Asynchronous scan operations
- Connection pooling for network scanners
- Configurable scan timeouts and retries

### **Resource Management**
- Automatic cleanup of temporary files
- Memory-efficient image processing
- Configurable file size limits (50MB default)

## Troubleshooting

### **Common Issues**

1. **Camera not working**
   - Ensure HTTPS is enabled
   - Check browser permissions for camera access
   - Verify browser supports `getUserMedia()` API

2. **Network scanners not discovered**
   - Check network connectivity
   - Verify scanner IP addresses
   - Check firewall settings
   - Use manual IP configuration

3. **Mobile scanning not working**
   - Verify QR code is generated correctly
   - Check token expiration (5 minutes)
   - Ensure mobile device can access server


## Deployment Checklist

### **Azure Linux Server**
- [ ] Java 11+ installed
- [ ] Network access to local subnets
- [ ] HTTPS certificate configured (for camera access)
- [ ] Firewall allows scanner discovery ports

### **Network Configuration**
- [ ] Scanner IPs documented and configured
- [ ] Network discovery zones configured
- [ ] Security policies reviewed
- [ ] Performance monitoring enabled

### **Client Access**
- [ ] HTTPS access verified
- [ ] Browser compatibility tested
- [ ] Mobile device access tested
- [ ] User training materials prepared

## Future Enhancements

### **Planned Features**
1. **Advanced mDNS Discovery**: Full Bonjour/Zeroconf support
2. **WSD Implementation**: Complete Web Services for Devices support
3. **Scanner Profiles**: User-specific scanner preferences
4. **Batch Scanning**: Multiple document processing
5. **OCR Integration**: Text extraction from scanned documents

### **Integration Opportunities**
1. **Cloud Storage**: Direct upload to cloud services
2. **Workflow Integration**: Automated document routing
3. **AI Processing**: Intelligent document classification
4. **Mobile Apps**: Native mobile applications

## Support and Maintenance

### **Regular Tasks**
- Monitor scanner discovery logs
- Review network scanner health
- Update known scanner IPs
- Clear scanner cache periodically
- Monitor performance metrics

### **Administrative Tools**
- Scanner management dashboard
- Network discovery configuration
- Scanner health monitoring
- Performance analytics

---

**Note**: This scanning system provides enterprise-grade document scanning capabilities that work seamlessly across all platforms and devices, making it ideal for diverse deployment environments like yours with users on Windows, macOS, Linux, and mobile devices.
