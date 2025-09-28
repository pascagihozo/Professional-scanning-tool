# Scanner Desktop Client Protocol (v1)

Base URL: http://127.0.0.1:17070/v1

Health
GET /health
Response: { "status":"UP", "service":"scanner-desktop-client", "version":"0.1.0" }

List Scanners
GET /scanners
Response: [ { "id":"escl_192.168.1.50", "name":"HP LaserJet ...", "devicePath":"http://192.168.1.50", "connected":true, "status":"eSCL Network Scanner" }, { "id":"wia_{GUID}", "name":"Canon USB", "devicePath":"{GUID}", "connected":true, "status":"WIA Scanner" } ]

Scan
POST /scan
Content-Type: application/json

{
  "scannerId": "escl_192.168.1.50",
  "dpi": 300,
  "colorMode": "Color",
  "paperSize": "A4",
  "format": "pdf",
  "previewOnly": false,
  "outputFileName": "my_doc"
}

Response:
{
  "status": "SUCCESS|ERROR",
  "message": "...",
  "fileName": "my_doc_20250101_101010.pdf",
  "fileFormat": "pdf",
  "fileData": "<base64>"
}

CORS
- Allowed origins configurable via api.cors.allowed-origins (default *).

Security
- Optional shared-secret header (future): X-Scanner-Secret: <token>

Errors
- 200 with { status:"ERROR", message:"..." } for backend failures.
- 5xx on unexpected exceptions.
