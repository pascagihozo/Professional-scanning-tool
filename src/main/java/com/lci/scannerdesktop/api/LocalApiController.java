package com.lci.scannerdesktop.api;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class LocalApiController {

    private final ScannerFacade scannerFacade;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "pascal-scanning-client",
                "name", "Pascal Scanning Client",
                "version", "1.0.0",
                "description", "Professional Document Scanning Solution"
        );
    }

    @GetMapping("/scanners")
    public List<ScannerDTO> scanners() {
        return scannerFacade.listScanners();
    }

    @GetMapping("/capabilities")
    public CapabilitiesDTO capabilities(@RequestParam("scannerId") String scannerId) {
        return scannerFacade.getCapabilities(scannerId);
    }

    @PostMapping(value = "/scan", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScanResultDTO> scan(@RequestBody ScanRequestDTO req) {
        return ResponseEntity.ok(scannerFacade.scan(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshScanners() {
        scannerFacade.clearCache();
        return ResponseEntity.ok(Map.of(
                "status", "REFRESHED",
                "message", "Scanner cache cleared"
        ));
    }

    @GetMapping("/cache-stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        return ResponseEntity.ok(Map.of(
                "status", "CACHED",
                "message", "Scanner cache is active"
        ));
    }

    @Data
    public static class ScanRequestDTO {
        private String scannerId;
        private Integer dpi = 300;
        private String colorMode = "Color";
        private String paperSize = "A4";
        private String format = "pdf"; // pdf|png|jpg
        private boolean previewOnly = false;
        private String outputFileName;
        private Boolean duplex = false;
        private Boolean adf = false;
        
        // Manual getters/setters for compatibility
        public String getScannerId() { return scannerId; }
        public void setScannerId(String scannerId) { this.scannerId = scannerId; }
        public Integer getDpi() { return dpi; }
        public void setDpi(Integer dpi) { this.dpi = dpi; }
        public String getColorMode() { return colorMode; }
        public void setColorMode(String colorMode) { this.colorMode = colorMode; }
        public String getPaperSize() { return paperSize; }
        public void setPaperSize(String paperSize) { this.paperSize = paperSize; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public boolean isPreviewOnly() { return previewOnly; }
        public void setPreviewOnly(boolean previewOnly) { this.previewOnly = previewOnly; }
        public String getOutputFileName() { return outputFileName; }
        public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
        public Boolean getDuplex() { return duplex; }
        public void setDuplex(Boolean duplex) { this.duplex = duplex; }
        public Boolean getAdf() { return adf; }
        public void setAdf(Boolean adf) { this.adf = adf; }
    }

    @Data
    public static class ScannerDTO {
        private String id;
        private String name;
        private String devicePath;
        private boolean connected;
        private String status;
        
        // Manual getters/setters for compatibility
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDevicePath() { return devicePath; }
        public void setDevicePath(String devicePath) { this.devicePath = devicePath; }
        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @Data
    public static class ScanResultDTO {
        private String status; // SUCCESS|ERROR
        private String message;
        private String fileName;
        private String fileFormat;
        private byte[] fileData; // base64 by Spring JSON
        
        // Manual getters/setters for compatibility
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFileFormat() { return fileFormat; }
        public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
        public byte[] getFileData() { return fileData; }
        public void setFileData(byte[] fileData) { this.fileData = fileData; }
    }

    @Data
    public static class CapabilitiesDTO {
        private String scannerId;
        private java.util.List<Integer> supportedResolutions;
        private java.util.List<String> supportedFormats;
        private java.util.List<String> supportedColorModes;
        private boolean duplexSupported;
        private boolean adfSupported;
        
        // Manual getters/setters for compatibility
        public String getScannerId() { return scannerId; }
        public void setScannerId(String scannerId) { this.scannerId = scannerId; }
        public java.util.List<Integer> getSupportedResolutions() { return supportedResolutions; }
        public void setSupportedResolutions(java.util.List<Integer> supportedResolutions) { this.supportedResolutions = supportedResolutions; }
        public java.util.List<String> getSupportedFormats() { return supportedFormats; }
        public void setSupportedFormats(java.util.List<String> supportedFormats) { this.supportedFormats = supportedFormats; }
        public java.util.List<String> getSupportedColorModes() { return supportedColorModes; }
        public void setSupportedColorModes(java.util.List<String> supportedColorModes) { this.supportedColorModes = supportedColorModes; }
        public boolean isDuplexSupported() { return duplexSupported; }
        public void setDuplexSupported(boolean duplexSupported) { this.duplexSupported = duplexSupported; }
        public boolean isAdfSupported() { return adfSupported; }
        public void setAdfSupported(boolean adfSupported) { this.adfSupported = adfSupported; }
    }
}