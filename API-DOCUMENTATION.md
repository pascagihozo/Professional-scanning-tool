# Pascal Scanning Agent API Documentation

## Base URL
```
http://localhost:17070
```

## Web Interface
- **UI**: `http://localhost:17070/ui`
- **Description**: Modern web interface for scanner management and testing

---

## REST API Endpoints

### 1. Health Check
**GET** `/v1/health`

**Description**: Check if the service is running

**Response**:
```json
{
  "status": "UP",
  "service": "scanner-desktop-client",
  "version": "0.1.0"
}
```

---

### 2. List Scanners
**GET** `/v1/scanners`

**Description**: Get all available scanners (Windows WIA, Linux SANE, Network eSCL)

**Response**:
```json
[
  {
    "id": "WIA_Scanner_123",
    "name": "HP LaserJet Pro MFP M428fdw",
    "devicePath": "/dev/usb/scanner0",
    "connected": true,
    "status": "Available"
  }
]
```

---

### 3. Get Scanner Capabilities
**GET** `/v1/capabilities?scannerId={scannerId}`

**Parameters**:
- `scannerId` (required): Scanner ID from the scanners list

**Response**:
```json
{
  "scannerId": "WIA_Scanner_123",
  "supportedResolutions": [150, 200, 300, 600, 1200],
  "supportedFormats": ["pdf", "jpg", "png", "tiff"],
  "supportedColorModes": ["Color", "Grayscale", "Black and White"],
  "duplexSupported": true,
  "adfSupported": true
}
```

---

### 4. Perform Scan
**POST** `/v1/scan`

**Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "scannerId": "WIA_Scanner_123",
  "dpi": 300,
  "colorMode": "Color",
  "paperSize": "A4",
  "format": "pdf",
  "previewOnly": false,
  "outputFileName": "document_scan",
  "duplex": false,
  "adf": false
}
```

**Parameters**:
- `scannerId` (required): Scanner ID from the scanners list
- `dpi` (optional, default: 300): Resolution - 150, 200, 300, 600, 1200
- `colorMode` (optional, default: "Color"): "Color", "Grayscale", "Black and White"
- `paperSize` (optional, default: "A4"): "A4", "Letter", "Legal", etc.
- `format` (optional, default: "pdf"): "pdf", "jpg", "png", "tiff"
- `previewOnly` (optional, default: false): Generate preview only
- `outputFileName` (optional): Custom filename without extension
- `duplex` (optional, default: false): Enable duplex scanning (if supported)
- `adf` (optional, default: false): Use Automatic Document Feeder (if supported)

**Response** (Success):
```json
{
  "status": "SUCCESS",
  "message": "Scan completed successfully",
  "fileName": "document_scan.pdf",
  "fileFormat": "pdf",
  "fileData": "base64EncodedFileContent..."
}
```

**Response** (Error):
```json
{
  "status": "ERROR",
  "message": "Scanner not available",
  "fileName": null,
  "fileFormat": null,
  "fileData": null
}
```

---

## JavaScript Integration Example

```javascript
// Health check
const health = await fetch('http://localhost:17070/v1/health');
const healthData = await health.json();

// Get scanners
const scannersResponse = await fetch('http://localhost:17070/v1/scanners');
const scanners = await scannersResponse.json();

// Get capabilities
const capsResponse = await fetch(`http://localhost:17070/v1/capabilities?scannerId=${scanners[0].id}`);
const capabilities = await capsResponse.json();

// Perform scan
const scanRequest = {
  scannerId: scanners[0].id,
  dpi: 300,
  colorMode: "Color",
  format: "pdf",
  outputFileName: "my_document"
};

const scanResponse = await fetch('http://localhost:17070/v1/scan', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify(scanRequest)
});

const scanResult = await scanResponse.json();

if (scanResult.status === 'SUCCESS') {
  // Convert base64 to blob and download
  const byteChars = atob(scanResult.fileData);
  const byteNums = new Array(byteChars.length);
  for (let i = 0; i < byteChars.length; i++) {
    byteNums[i] = byteChars.charCodeAt(i);
  }
  const blob = new Blob([new Uint8Array(byteNums)], {
    type: scanResult.format === 'pdf' ? 'application/pdf' : `image/${scanResult.format}`
  });

  // Create download link
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = scanResult.fileName;
  a.click();
  URL.revokeObjectURL(url);
}
```

---

## cURL Examples

### Health Check
```bash
curl http://localhost:17070/v1/health
```

### List Scanners
```bash
curl http://localhost:17070/v1/scanners
```

### Get Capabilities
```bash
curl "http://localhost:17070/v1/capabilities?scannerId=WIA_Scanner_123"
```

### Perform Scan
```bash
curl -X POST http://localhost:17070/v1/scan \
  -H "Content-Type: application/json" \
  -d '{
    "scannerId": "WIA_Scanner_123",
    "dpi": 300,
    "colorMode": "Color",
    "format": "pdf",
    "outputFileName": "test_scan"
  }'
```

---

## Error Codes

| HTTP Status | Description |
|------------|-------------|
| 200 | Success |
| 400 | Bad Request - Invalid parameters |
| 404 | Not Found - Scanner not found |
| 500 | Internal Server Error - Scan failed |

---

## Platform Support

| Platform | Scanner Types | Notes |
|----------|---------------|-------|
| Windows | WIA USB/Network | Requires pascal scanning agent running |
| Linux | SANE USB/Network | Limited support |
| Network | eSCL Protocol | Cross-platform |

---

## Security

- **Local only**: Service runs on localhost (127.0.0.1)
- **No authentication**: Designed for local desktop use
- **CORS enabled**: Allows browser integration

---

## Troubleshooting

1. **Service not responding**: Ensure Pascal Scanning Agent is running
2. **No scanners found**: Check scanner drivers and connections
3. **Scan fails**: Verify scanner is not in use by other applications
4. **CORS errors**: Make sure requests come from localhost

---

## Version: 0.1.0
Last updated: 2025-09-25