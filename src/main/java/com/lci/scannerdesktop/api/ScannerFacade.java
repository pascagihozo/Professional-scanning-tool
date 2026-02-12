package com.lci.scannerdesktop.api;

import com.lci.scannerdesktop.escl.ESCLService;
import com.lci.scannerdesktop.wia.WIAService;
import com.lci.scannerdesktop.sane.SaneService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class ScannerFacade {

    private final ESCLService esclService;
    private final WIAService wiaService;
    private final SaneService saneService;
    private final ScanPolicy scanPolicy;

    @Value("${scanner.startup-discovery.enabled:true}")
    private boolean startupDiscoveryEnabled;

    @Value("${scanner.startup-discovery.delay-seconds:2}")
    private int startupDiscoveryDelaySeconds;

    @Value("${scanner.cache.ttl-minutes:2}")
    private int cacheTtlMinutes;

    // Simple cache for scanners
    private final Map<String, List<LocalApiController.ScannerDTO>> scannerCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private long CACHE_TTL_MS;

    @PostConstruct
    public void initializeCacheSettings() {
        CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(cacheTtlMinutes);
        System.out.println("[ScannerFacade] Cache TTL configured: " + cacheTtlMinutes + " minutes");
    }

    /**
     * Automatically discover scanners when application starts
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeScannersOnStartup() {
        if (!startupDiscoveryEnabled) {
            System.out.println("[ScannerFacade] Startup discovery disabled by configuration");
            return;
        }

        System.out.println("[ScannerFacade] Application ready - starting automatic scanner discovery...");
        System.out.println("[ScannerFacade] Discovery delay: " + startupDiscoveryDelaySeconds + " seconds");

        try {
            // Perform initial discovery in background
            new Thread(() -> {
                try {
                    Thread.sleep(startupDiscoveryDelaySeconds * 1000L); // Wait for services to be ready
                    List<LocalApiController.ScannerDTO> scanners = discoverScannersInternal();
                    System.out.println(
                            "[ScannerFacade] Startup discovery complete: " + scanners.size() + " scanners found");

                    // Cache the results
                    String cacheKey = "all_scanners";
                    scannerCache.put(cacheKey, scanners);
                    cacheTimestamps.put(cacheKey, System.currentTimeMillis());

                } catch (Exception e) {
                    System.err.println("[ScannerFacade] Startup discovery failed: " + e.getMessage());
                }
            }, "ScannerStartupDiscovery").start();

        } catch (Exception e) {
            System.err.println("[ScannerFacade] Failed to start automatic discovery: " + e.getMessage());
        }
    }

    public List<LocalApiController.ScannerDTO> listScanners() {
        String cacheKey = "all_scanners";
        Long timestamp = cacheTimestamps.get(cacheKey);

        // Check if cache is valid
        if (timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_TTL_MS) {
            List<LocalApiController.ScannerDTO> cached = scannerCache.get(cacheKey);
            if (cached != null) {
                System.out.println("[ScannerFacade] Returning cached scanners: " + cached.size());
                return cached;
            }
        }

        // Cache miss or expired - discover fresh
        System.out.println("[ScannerFacade] Cache miss, discovering scanners...");
        List<LocalApiController.ScannerDTO> scanners = discoverScannersInternal();

        // Update cache
        scannerCache.put(cacheKey, scanners);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());

        System.out.println("[ScannerFacade] Discovered total: " + scanners.size());
        return scanners;
    }

    /**
     * Internal method to discover scanners from all services
     */
    private List<LocalApiController.ScannerDTO> discoverScannersInternal() {
        List<LocalApiController.ScannerDTO> out = new ArrayList<>();

        try {
            esclService.discoverScanners().forEach(s -> {
                LocalApiController.ScannerDTO dto = new LocalApiController.ScannerDTO();
                dto.setId(s.id);
                dto.setName(s.name);
                dto.setDevicePath(s.devicePath);
                dto.setConnected(s.connected);
                dto.setStatus(s.status);
                out.add(dto);
            });
        } catch (Exception e) {
            System.out.println("[ScannerFacade] eSCL discovery error: " + e.getMessage());
        }
        try {
            wiaService.discoverScanners().forEach(s -> {
                LocalApiController.ScannerDTO dto = new LocalApiController.ScannerDTO();
                dto.setId(s.id);
                dto.setName(s.name);
                dto.setDevicePath(s.devicePath);
                dto.setConnected(s.connected);
                dto.setStatus(s.status);
                out.add(dto);
            });
        } catch (Exception e) {
            System.out.println("[ScannerFacade] WIA discovery error: " + e.getMessage());
        }
        try {
            saneService.discoverScanners().forEach(s -> {
                LocalApiController.ScannerDTO dto = new LocalApiController.ScannerDTO();
                dto.setId(s.id);
                dto.setName(s.name);
                dto.setDevicePath(s.devicePath);
                dto.setConnected(s.connected);
                dto.setStatus(s.status);
                out.add(dto);
            });
        } catch (Exception e) {
            System.out.println("[ScannerFacade] SANE discovery error: " + e.getMessage());
        }

        return out;
    }

    public LocalApiController.ScanResultDTO scan(LocalApiController.ScanRequestDTO req) {
        LocalApiController.ScanResultDTO dto = new LocalApiController.ScanResultDTO();
        if (req.getScannerId() == null || req.getScannerId().isBlank()) {
            dto.setStatus("ERROR");
            dto.setMessage("scannerId is required");
            return dto;
        }

        if (req.getScannerId().startsWith("escl_")) {
            // Find scanner by ID
            String ip = req.getScannerId().substring("escl_".length());
            String base = "http://" + ip; // port autodetected during probe; try standard http first
            // In practice, we could cache devicePath from discovery; here we re-derive http
            ESCLService.ScanOptions opt = new ESCLService.ScanOptions();
            opt.scannerId = req.getScannerId();
            opt.dpi = req.getDpi();
            opt.colorMode = req.getColorMode();
            opt.paperSize = req.getPaperSize();
            opt.format = req.getFormat();
            opt.previewOnly = req.isPreviewOnly();
            opt.outputFileName = req.getOutputFileName();
            opt.duplex = req.getDuplex();
            opt.adf = req.getAdf();

            // Prefer discovered devicePath if available
            String devicePath = null;
            for (LocalApiController.ScannerDTO s : listScanners()) {
                if (req.getScannerId().equals(s.getId())) {
                    devicePath = s.getDevicePath();
                    break;
                }
            }
            if (devicePath != null)
                base = devicePath;

            ESCLService.ScanResult r = esclService.scan(base, opt);
            enforceSize(r);
            dto.setStatus(r.status);
            dto.setMessage(r.message);
            dto.setFileName(r.fileName);
            dto.setFileFormat(r.fileFormat);
            dto.setFileData(r.fileData);
            return dto;
        }

        if (req.getScannerId().startsWith("wia_")) {
            WIAService.ScanOptions opt = new WIAService.ScanOptions();
            opt.scannerId = req.getScannerId();
            opt.dpi = req.getDpi();
            opt.colorMode = req.getColorMode();
            opt.paperSize = req.getPaperSize();
            opt.format = req.getFormat();
            opt.previewOnly = req.isPreviewOnly();
            opt.outputFileName = req.getOutputFileName();
            opt.duplex = req.getDuplex();
            opt.adf = req.getAdf();
            opt.maxPages = req.getMaxPages();
            opt.copies = req.getCopies();

            WIAService.ScanResult r = wiaService.scan(opt);
            enforceSize(r);
            dto.setStatus(r.status);
            dto.setMessage(r.message);
            dto.setFileName(r.fileName);
            dto.setFileFormat(r.fileFormat);
            dto.setFileData(r.fileData);
            return dto;
        }

        if (req.getScannerId().startsWith("sane_")) {
            SaneService.ScanOptions opt = new SaneService.ScanOptions();
            opt.scannerId = req.getScannerId();
            opt.dpi = req.getDpi();
            opt.colorMode = req.getColorMode();
            opt.paperSize = req.getPaperSize();
            opt.format = req.getFormat();
            opt.previewOnly = req.isPreviewOnly();
            opt.outputFileName = req.getOutputFileName();
            opt.duplex = req.getDuplex();
            opt.adf = req.getAdf();

            SaneService.ScanResult r = saneService.scan(opt);
            enforceSize(r);
            dto.setStatus(r.status);
            dto.setMessage(r.message);
            dto.setFileName(r.fileName);
            dto.setFileFormat(r.fileFormat);
            dto.setFileData(r.fileData);
            return dto;
        }

        dto.setStatus("ERROR");
        dto.setMessage("Unsupported scanner backend: " + req.getScannerId());
        return dto;
    }

    private void enforceSize(Object backendResult) {
        try {
            byte[] data = null;
            if (backendResult instanceof ESCLService.ScanResult)
                data = ((ESCLService.ScanResult) backendResult).fileData;
            else if (backendResult instanceof WIAService.ScanResult)
                data = ((WIAService.ScanResult) backendResult).fileData;
            else if (backendResult instanceof SaneService.ScanResult)
                data = ((SaneService.ScanResult) backendResult).fileData;
            if (data != null && data.length > scanPolicy.getMaxResultSizeBytes()) {
                if (backendResult instanceof ESCLService.ScanResult) {
                    ((ESCLService.ScanResult) backendResult).status = "ERROR";
                    ((ESCLService.ScanResult) backendResult).message = "Result too large";
                    ((ESCLService.ScanResult) backendResult).fileData = null;
                }
                if (backendResult instanceof WIAService.ScanResult) {
                    ((WIAService.ScanResult) backendResult).status = "ERROR";
                    ((WIAService.ScanResult) backendResult).message = "Result too large";
                    ((WIAService.ScanResult) backendResult).fileData = null;
                }
                if (backendResult instanceof SaneService.ScanResult) {
                    ((SaneService.ScanResult) backendResult).status = "ERROR";
                    ((SaneService.ScanResult) backendResult).message = "Result too large";
                    ((SaneService.ScanResult) backendResult).fileData = null;
                }
            }
        } catch (Exception ignore) {
        }
    }

    public LocalApiController.CapabilitiesDTO getCapabilities(String scannerId) {
        String cacheKey = "capabilities_" + scannerId;
        Long timestamp = cacheTimestamps.get(cacheKey);

        // Check if capabilities cache is valid (longer TTL)
        if (timestamp != null && (System.currentTimeMillis() - timestamp) < (CACHE_TTL_MS * 2)) {
            // Return cached capabilities if available
        }

        LocalApiController.CapabilitiesDTO out = new LocalApiController.CapabilitiesDTO();
        out.setScannerId(scannerId);
        try {
            if (scannerId != null && scannerId.startsWith("escl_")) {
                String ip = scannerId.substring(5);
                String base = "http://" + ip;
                // prefer cached devicePath from discovery
                for (LocalApiController.ScannerDTO s : listScanners()) {
                    if (scannerId.equals(s.getId())) {
                        base = s.getDevicePath();
                        break;
                    }
                }
                ESCLService.Capabilities c = esclService.getCapabilities(base);
                out.setSupportedResolutions(c.supportedResolutions);
                out.setSupportedFormats(c.supportedFormats);
                out.setSupportedColorModes(c.supportedColorModes);
                out.setDuplexSupported(c.duplexSupported);
                out.setAdfSupported(c.adfSupported);
                return out;
            }
        } catch (Exception ignore) {
        }
        // Fallback defaults
        out.setSupportedResolutions(java.util.Arrays.asList(150, 200, 300, 600));
        out.setSupportedFormats(java.util.Arrays.asList("pdf", "jpg", "png", "tiff"));
        out.setSupportedColorModes(java.util.Arrays.asList("Color", "Grayscale", "Black and White"));
        out.setDuplexSupported(scannerId != null && (scannerId.startsWith("escl_") || scannerId.startsWith("wia_")));
        out.setAdfSupported(true);
        return out;
    }

    public void stopScan() {
        System.out.println("[ScannerFacade] Stop requested for all backends");
        wiaService.stopScan();
        // Add ESCL/SANE stops if they support it later
    }

    public void clearCache() {
        scannerCache.clear();
        cacheTimestamps.clear();
        System.out.println("[ScannerFacade] Cache cleared");
    }
}
