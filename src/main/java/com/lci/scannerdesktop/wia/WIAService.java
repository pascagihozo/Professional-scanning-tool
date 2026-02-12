package com.lci.scannerdesktop.wia;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class WIAService {

    public WIAService(WiaConfig config) {
        this.config = config;
    }

    public static class ScannerInfo {
        public String id;
        public String name;
        public String devicePath;
        public boolean connected;
        public String status;
    }

    public static class ScanOptions {
        public String scannerId;
        public Integer dpi;
        public String colorMode;
        public String paperSize;
        public String format; // pdf|png|jpg
        public boolean previewOnly;
        public String outputFileName;
        public Boolean duplex = false;
        public Boolean adf = false;
        public Integer maxPages; // New: Limit number of pages
        public Integer copies = 1; // New: Number of copies
    }

    public static class ScanResult {
        public String status;
        public String message;
        public String fileName;
        public String fileFormat;
        public byte[] fileData;
    }

    public boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    private final WiaConfig config;
    private volatile boolean cancelRequested = false;

    public void stopScan() {
        this.cancelRequested = true;
        log.info("WIA: Cancellation requested");
    }

    public List<ScannerInfo> discoverScanners() {
        List<ScannerInfo> list = new ArrayList<>();
        if (!isWindows() || !config.isEnabled())
            return list;

        // Check if JACOB is available
        try {
            Class.forName("com.jacob.com.ComThread");
        } catch (ClassNotFoundException e) {
            log.warn(
                    "JACOB library not available. WIA scanning disabled. Install jacob.jar in lib/ directory to enable Windows WIA support.");
            return list;
        }

        try {
            // Initialize COM (STA) for JACOB interactions
            com.jacob.com.ComThread.InitSTA();
            try {
                System.out.println("[WIA] Starting discovery (WIA enabled)");
                // Try standard DeviceManager
                com.jacob.activeX.ActiveXComponent wia = new com.jacob.activeX.ActiveXComponent("WIA.DeviceManager");
                com.jacob.com.Dispatch devices = wia.getPropertyAsComponent("DeviceInfos").getObject();
                int count = com.jacob.com.Dispatch.get(devices, "Count").getInt();
                System.out.println("[WIA] DeviceInfos count=" + count);
                for (int i = 1; i <= count; i++) {
                    com.jacob.com.Dispatch item = com.jacob.com.Dispatch.call(devices, "Item", i).toDispatch();
                    com.jacob.com.Variant tVar = com.jacob.com.Dispatch.get(item, "Type");
                    int type = tVar != null ? tVar.getInt() : -1;
                    System.out.println("[WIA] DeviceInfo[" + i + "] Type=" + type);
                    // WIA device type: 1=Scanner, 65535=Unknown/Composite (often used by modern
                    // drivers)
                    if (type != 1 && type != 65535)
                        continue;
                    String id = com.jacob.com.Dispatch.get(item, "DeviceID").toString();
                    String name = com.jacob.com.Dispatch.get(item, "Properties").toDispatch() != null
                            ? safeGetProperty(item, "Properties", "Name")
                            : "WIA Scanner";
                    ScannerInfo si = new ScannerInfo();
                    si.id = "wia_" + id;
                    si.name = name != null ? name : "WIA Scanner";
                    si.devicePath = id;
                    si.connected = true;
                    si.status = "WIA Scanner";
                    list.add(si);
                }
            } catch (Throwable primary) {
                log.warn("WIA.DeviceManager discovery failed ({}). Trying DeviceManager1...", primary.getMessage());
                try {
                    com.jacob.activeX.ActiveXComponent wia = new com.jacob.activeX.ActiveXComponent(
                            "WIA.DeviceManager1");
                    com.jacob.com.Dispatch devices = wia.getPropertyAsComponent("DeviceInfos").getObject();
                    int count = com.jacob.com.Dispatch.get(devices, "Count").getInt();
                    System.out.println("[WIA] DeviceManager1 count=" + count);
                    for (int i = 1; i <= count; i++) {
                        com.jacob.com.Dispatch item = com.jacob.com.Dispatch.call(devices, "Item", i).toDispatch();
                        com.jacob.com.Variant tVar = com.jacob.com.Dispatch.get(item, "Type");
                        int type = tVar != null ? tVar.getInt() : -1;
                        System.out.println("[WIA] (DM1) DeviceInfo[" + i + "] Type=" + type);
                        if (type != 1 && type != 65535)
                            continue;
                        String id = com.jacob.com.Dispatch.get(item, "DeviceID").toString();
                        String name = com.jacob.com.Dispatch.get(item, "Properties").toDispatch() != null
                                ? safeGetProperty(item, "Properties", "Name")
                                : "WIA Scanner";
                        ScannerInfo si = new ScannerInfo();
                        si.id = "wia_" + id;
                        si.name = name != null ? name : "WIA Scanner";
                        si.devicePath = id;
                        si.connected = true;
                        si.status = "WIA Scanner";
                        list.add(si);
                    }
                } catch (Throwable secondary) {
                    log.warn("WIA.DeviceManager1 discovery also failed: {}", secondary.getMessage());
                }
            } finally {
                com.jacob.com.ComThread.Release();
            }
        } catch (Throwable t) {
            log.warn("WIA discovery failed: {}", t.getMessage());
        }
        System.out.println("[WIA] Discovered scanners: " + list.size());
        return list;
    }

    public ScanResult scan(ScanOptions options) {
        ScanResult r = new ScanResult();
        if (!isWindows()) {
            r.status = "ERROR";
            r.message = "WIA not available on this OS";
            return r;
        }
        if (!config.isEnabled()) {
            r.status = "ERROR";
            r.message = "WIA disabled. Set wia.enabled=true and install JACOB/JNA.";
            return r;
        }

        // Check if JACOB is available
        try {
            Class.forName("com.jacob.com.ComThread");
        } catch (ClassNotFoundException e) {
            r.status = "ERROR";
            r.message = "JACOB library not available. Install jacob.jar in lib/ directory to enable Windows WIA support.";
            return r;
        }

        try {
            com.jacob.com.ComThread.InitSTA();
            // Connect to scanner
            String rawId = options.scannerId.startsWith("wia_") ? options.scannerId.substring(4) : options.scannerId;
            com.jacob.activeX.ActiveXComponent wia = new com.jacob.activeX.ActiveXComponent("WIA.DeviceManager");
            com.jacob.com.Dispatch devices = wia.getPropertyAsComponent("DeviceInfos").getObject();
            com.jacob.com.Dispatch target = null;
            int count = com.jacob.com.Dispatch.get(devices, "Count").getInt();
            for (int i = 1; i <= count; i++) {
                com.jacob.com.Dispatch item = com.jacob.com.Dispatch.call(devices, "Item", i).toDispatch();
                String id = com.jacob.com.Dispatch.get(item, "DeviceID").toString();
                if (rawId.equals(id)) {
                    target = item;
                    break;
                }
            }
            if (target == null) {
                r.status = "ERROR";
                r.message = "Selected WIA device not found";
                return r;
            }

            com.jacob.com.Dispatch device = null;
            int connectRetries = 3;
            while (connectRetries > 0) {
                try {
                    device = com.jacob.com.Dispatch.call(target, "Connect").toDispatch();
                    break;
                } catch (com.jacob.com.ComFailException e) {
                    connectRetries--;
                    if (connectRetries > 0) {
                        log.warn("WIA connect failed ({}). Retrying... ({} attempts left)", e.getMessage(),
                                connectRetries);
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ignore) {
                        }
                    } else {
                        throw e;
                    }
                }
            }

            log.info(
                    "WIA Scan Request: scanner={} | source={} adf={} duplex={} | dpi={} color={} format={} paper={} | maxPages={} copies={} outputFile={}",
                    options.scannerId,
                    Boolean.TRUE.equals(options.duplex) ? "ADF Both Sides"
                            : Boolean.TRUE.equals(options.adf) ? "ADF Single Side" : "Flatbed",
                    options.adf, options.duplex,
                    options.dpi, options.colorMode, options.format, options.paperSize,
                    options.maxPages, options.copies, options.outputFileName);

            boolean useFeeder = Boolean.TRUE.equals(options.adf) || Boolean.TRUE.equals(options.duplex);
            if (Boolean.TRUE.equals(options.duplex) && !Boolean.TRUE.equals(options.adf)) {
                log.info("WIA: Duplex requested without ADF flag - forcing Feeder mode");
            }

            // Duplex scanning of a single physical sheet produces 2 logical pages (front +
            // back).
            // If the user set maxPages=1 and also requested duplex, they will only ever
            // receive
            // the front side because the page-limit check fires after the first Transfer.
            // Silently raise the floor to 2 so both sides of at least one sheet can
            // transfer.
            if (Boolean.TRUE.equals(options.duplex)
                    && options.maxPages != null && options.maxPages == 1) {
                log.info("WIA: maxPages=1 with duplex enabled only captures one side. Auto-adjusting to 2.");
                options.maxPages = 2;
            }

            this.cancelRequested = false; // Reset cancellation for new scan

            // Set Document Handling (Feeder vs Flatbed)
            // 3088: WIA_DPS_DOCUMENT_HANDLING_SELECT
            // Flags: 1=FEEDER, 2=FLATBED, 4=DUPLEX, 32=FRONT_AND_BACK
            int handling = 2; // Default Flatbed
            if (useFeeder) {
                handling = 1; // Feeder
                if (Boolean.TRUE.equals(options.duplex)) {
                    handling |= 4; // Add Duplex flag
                    log.info("WIA: Duplex scanning enabled (flags: {})", handling);
                } else {
                    log.info("WIA: ADF Simplex scanning (flags: {})", handling);
                }
            } else {
                log.info("WIA: Flatbed scanning (flags: {})", handling);
            }

            // --- Read driver capabilities before writing any properties ---
            // 3085: WIA_DPS_DOCUMENT_HANDLING_CAPABILITIES
            // bit 1=FEEDER, bit 2=FLATBED, bit 4=DUPLEX, bit 8=DETECT_FEED
            int handlingCaps = getItemInt(device, 3085);
            int handlingStatus = getItemInt(device, 3087);
            log.info("WIA: Device caps 3085={} status 3087={}", handlingCaps, handlingStatus);
            if (Boolean.TRUE.equals(options.duplex) && handlingCaps >= 0 && (handlingCaps & 4) == 0) {
                log.warn("WIA: Driver does NOT advertise DUPLEX in caps (3085={}). " +
                        "Duplex via WIA COM automation may not be supported.", handlingCaps);
            }

            log.debug("WIA: Setting 3088 (Handling) on Device to {}", handling);
            setItemInt(device, 3088, handling);

            // USAGE NOTE: Some HP drivers return "Incorrect Parameter" for 0 (all pages).
            // We set to 1 and let our Java while() loop pull subsequent sheets.
            int pagesToScan = 1;
            log.debug("WIA: Setting 3096 (WIA1 Pages) and 3093 (WIA2 Pages) on Device to {}", pagesToScan);
            setItemInt(device, 3096, pagesToScan); // WIA_DPS_PAGES (WIA 1.0)
            setItemInt(device, 3093, pagesToScan); // WIA_IPS_PAGES (WIA 2.0)

            // Read back to verify the driver accepted the writes (setItemInt is silent on
            // failure)
            int rb3088 = getItemInt(device, 3088);
            int rb3096 = getItemInt(device, 3096);
            int rb3093 = getItemInt(device, 3093);
            log.info("WIA: Device property readback → 3088={} 3096={} 3093={}", rb3088, rb3096, rb3093);

            com.jacob.com.Dispatch items = com.jacob.com.Dispatch.get(device, "Items").toDispatch();
            int itemCount = com.jacob.com.Dispatch.get(items, "Count").getInt();
            log.info("WIA: Device has {} items", itemCount);

            com.jacob.com.Dispatch item = null;
            if (itemCount > 0) {
                // LOG ALL ITEMS to find the correct one for duplex
                for (int i = 1; i <= itemCount; i++) {
                    com.jacob.com.Dispatch candidate = com.jacob.com.Dispatch.call(items, "Item", i).toDispatch();
                    String name = safeGetProperty(candidate, "Properties", "Item Name");
                    String category = getItemString(candidate, 4125); // ID 4125 = Item Category
                    log.info("WIA: Item {} - Name: '{}', Category: {}", i, name, category);
                }

                // Try to find Feeder/Document item if useFeeder is true
                if (useFeeder) {
                    for (int i = 1; i <= itemCount; i++) {
                        com.jacob.com.Dispatch candidate = com.jacob.com.Dispatch.call(items, "Item", i).toDispatch();
                        String category = getItemString(candidate, 4125); // Use PID 4125 (Category)
                        String catUpper = category != null ? category.toUpperCase() : "";
                        // Feeder: FE142255 / Document: 6291BD2C / Scan (Generic): F193526F
                        if (catUpper.contains("FE142255") || catUpper.contains("6291BD2C")
                                || catUpper.contains("F193526F")) {
                            item = candidate;
                            log.info("WIA: Selected Feeder/Document/Scan item at index {} (Category: {})", i, category);
                            break;
                        }
                    }
                }

                if (item == null) {
                    item = com.jacob.com.Dispatch.call(items, "Item", 1).toDispatch();
                    log.debug("WIA: Using fallback/default Item 1");
                }
            }

            if (item == null) {
                r.status = "ERROR";
                r.message = "Scanner has no available items";
                log.error("WIA: No scanner items found");
                return r;
            }

            // Dump all properties on the device so we can confirm which IDs the driver
            // exposes.
            dumpProperties("Device", device);
            dumpProperties("Item", item);

            // Re-apply document handling flags on the selected child item.
            // Many drivers (Kyocera, Brother, Canon) require 3088 / 3096 / 3093 to be set
            // on the feeder sub-item in addition to the device root.
            if (useFeeder) {
                log.debug("WIA: Re-applying 3088={} 3096={} 3093={} on Feeder Item", handling, pagesToScan,
                        pagesToScan);
                setItemInt(item, 3088, handling);
                setItemInt(item, 3096, pagesToScan); // WIA_DPS_PAGES (WIA 1.0)
                setItemInt(item, 3093, pagesToScan); // WIA_IPS_PAGES (WIA 2.0)
                int irb3088 = getItemInt(item, 3088);
                int irb3096 = getItemInt(item, 3096);
                int irb3093 = getItemInt(item, 3093);
                log.info("WIA: Item property readback → 3088={} 3096={} 3093={}", irb3088, irb3096, irb3093);
            }

            // Apply basic properties to ITEM (Resolution/Intent are Item properties)
            // Use name-based lookup for PIDs because WIA 2.0 drivers (like HP) often shift
            // IDs by +1.
            int pidIntent = findPropertyIdByName(item, "Current Intent", 6145);
            int pidXRes = findPropertyIdByName(item, "Horizontal Resolution", 6146);
            int pidYRes = findPropertyIdByName(item, "Vertical Resolution", 6147);
            int pidXExt = findPropertyIdByName(item, "Horizontal Extent", 6151);
            int pidYExt = findPropertyIdByName(item, "Vertical Extent", 6152);
            int pidFormat = findPropertyIdByName(item, "Format", 4106);

            int dpi = options.dpi != null ? options.dpi : 300;
            log.info("WIA: Item Setup → Intent_PID={} Res_PIDs={} Ext_PIDs={} Format_PID={}", pidIntent, pidXRes,
                    pidXExt, pidFormat);

            setItemInt(item, pidIntent, mapColor(options.colorMode));
            setItemInt(item, pidXRes, dpi);
            setItemInt(item, pidYRes, dpi);

            // AUTO-SCALE EXTENTS: Many drivers return "Incorrect Parameter" if pixels
            // width/height
            // don't match resolution * physical size.
            int physWidthMil = useFeeder ? getItemInt(device, 3076) : getItemInt(device, 3074);
            int physHeightMil = useFeeder ? getItemInt(device, 3077) : getItemInt(device, 3075);
            if (physWidthMil > 0 && physHeightMil > 0) {
                int pxW = (physWidthMil * dpi) / 1000;
                int pxH = (physHeightMil * dpi) / 1000;
                log.info("WIA: Scaling extents to {}x{} (Physical: {}x{} mil)", pxW, pxH, physWidthMil, physHeightMil);
                setItemInt(item, pidXExt, pxW);
                setItemInt(item, pidYExt, pxH);
            }

            // Sync Format property
            String guid = formatGuid(options.format);
            setItemString(item, pidFormat, guid);

            List<byte[]> pageDatas = new ArrayList<>();
            boolean hasMorePages = true;

            while (hasMorePages) {
                // Check for manual cancellation
                if (cancelRequested) {
                    log.info("WIA: Scan cancelled by user");
                    hasMorePages = false;
                    break;
                }

                // Check for page limit
                if (options.maxPages != null && options.maxPages > 0 && pageDatas.size() >= options.maxPages) {
                    log.info("WIA: Page limit reached ({})", options.maxPages);
                    hasMorePages = false;
                    break;
                }

                com.jacob.com.Dispatch imageFile = null;
                try {
                    int currentPage = pageDatas.size() + 1;
                    log.info("WIA: Starting Transfer for Page {}", currentPage);
                    com.jacob.com.Variant transferResult = com.jacob.com.Dispatch.call(item, "Transfer", guid);

                    if (transferResult != null && !transferResult.isNull()) {
                        imageFile = transferResult.toDispatch();
                    } else {
                        log.info("WIA: Transfer call returned null for Page {} - finishing loop", currentPage);
                        hasMorePages = false;
                    }
                } catch (Throwable e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown Error";
                    // If requested format failed, try BMP fallback (often required by HP ADF)
                    if (msg.contains("0x80070057") || msg.toLowerCase().contains("parameter is incorrect")) {
                        String bmpGuid = "{B96B3CAB-0728-11D3-9D7B-0000F81EF32E}";
                        if (!guid.equals(bmpGuid)) {
                            log.warn("WIA: Transfer failed with 'Incorrect Parameter'. Retrying with BMP fallback...");
                            try {
                                com.jacob.com.Variant retryResult = com.jacob.com.Dispatch.call(item, "Transfer",
                                        bmpGuid);
                                if (retryResult != null && !retryResult.isNull()) {
                                    imageFile = retryResult.toDispatch();
                                }
                            } catch (Throwable e2) {
                                log.error("WIA: BMP fallback also failed: {}", e2.getMessage());
                            }
                        }
                    }

                    if (imageFile == null) {
                        log.info("WIA: Transfer finished or failed at Page {}: {}", pageDatas.size() + 1, msg);
                        if (msg.contains("0x80210003") || msg.toLowerCase().contains("paper empty")) {
                            log.info("WIA: Verified paper empty status.");
                        }
                        hasMorePages = false;
                    }
                }

                if (imageFile != null) {
                    byte[] data = getImageData(imageFile, options);
                    if (data != null && data.length > 0) {
                        pageDatas.add(data);
                        log.info("WIA: Captured Page {} ({} bytes)", pageDatas.size(), data.length);

                        // Give the duplexer time to flip the sheet before requesting the next
                        // Transfer. Without this delay some drivers report "Paper Empty"
                        // (0x80210003) immediately for the reverse side even though the paper
                        // is still physically in the mechanism.
                        if (Boolean.TRUE.equals(options.duplex) && hasMorePages) {
                            // Read status BEFORE the sleep so it reflects the true post-Transfer state.
                            int postStatus = getItemInt(device, 3087);
                            log.info("WIA: Post-page-{} device status 3087={}", pageDatas.size(), postStatus);
                            try {
                                Thread.sleep(600);
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    } else {
                        log.warn("WIA: ImageData retrieval failed for Page {} - terminating loop",
                                pageDatas.size() + 1);
                        hasMorePages = false;
                    }
                }

                // Break loop if neither ADF nor Duplex is active (i.e. flatbed scan).
                // Duplex must be treated the same as ADF here so that side 2 is fetched.
                // EXTRA SAFETY: flatbed (adf=false, duplex=false) always stops after 1 page.
                boolean feederActive = Boolean.TRUE.equals(options.adf) || Boolean.TRUE.equals(options.duplex);
                if (!feederActive
                        || (options.maxPages != null && options.maxPages > 0 && pageDatas.size() >= options.maxPages)) {
                    log.debug("WIA: Loop termination condition met (adf={}, duplex={}, currentPages={})",
                            options.adf, options.duplex, pageDatas.size());
                    hasMorePages = false;
                }

                // Safety break to prevent infinite loops
                if (pageDatas.size() > 500) {
                    log.error("WIA: Safety limit reached (500 pages) - forcing loop exit");
                    break;
                }
            }

            log.info("WIA Scan Session Complete: {} pages captured", pageDatas.size());

            if (pageDatas.isEmpty()) {
                r.status = "ERROR";
                r.message = "No images scanned. Please check the scanner/feeder and try again.";
                return r;
            }

            byte[] finalData;
            String fmt = options.format == null ? "pdf" : options.format.toLowerCase();
            if ("pdf".equals(fmt)) {
                finalData = toPdf(pageDatas);
            } else {
                finalData = pageDatas.get(0);
            }

            r.status = "SUCCESS";
            r.fileData = finalData;
            r.fileFormat = fmt;
            r.fileName = (options.outputFileName == null || options.outputFileName.isBlank() ? "wia_scan"
                    : options.outputFileName) + "." + fmt;
            r.message = "OK";
            return r;
        } catch (Throwable t) {
            r.status = "ERROR";
            r.message = t.getMessage();
            return r;
        } finally {
            try {
                com.jacob.com.ComThread.Release();
            } catch (Throwable ignore) {
            }
        }
    }

    private String safeGetProperty(com.jacob.com.Dispatch item, String collName, String propName) {
        try {
            com.jacob.com.Dispatch props = com.jacob.com.Dispatch.get(item, collName).toDispatch();
            int count = com.jacob.com.Dispatch.get(props, "Count").getInt();
            for (int i = 1; i <= count; i++) {
                com.jacob.com.Dispatch p = com.jacob.com.Dispatch.call(props, "Item", i).toDispatch();
                String name = com.jacob.com.Dispatch.get(p, "Name").toString();
                if (propName.equalsIgnoreCase(name))
                    return com.jacob.com.Dispatch.get(p, "Value").toString();
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    /**
     * Read an integer WIA property from a device or item.
     *
     * The WIA automation Properties collection on many WIA 2.0 drivers only
     * supports sequential-index access (Item(1), Item(2)...) and does NOT
     * respond to property-ID-keyed access (Item(3088)). We therefore iterate
     * the full collection and match on the "Id" attribute of each entry,
     * mirroring the approach already used in safeGetProperty().
     */
    private int getItemInt(com.jacob.com.Dispatch target, int propertyId) {
        try {
            com.jacob.com.Dispatch props = com.jacob.com.Dispatch.get(target, "Properties").toDispatch();
            int count = com.jacob.com.Dispatch.get(props, "Count").getInt();
            for (int i = 1; i <= count; i++) {
                com.jacob.com.Dispatch p = com.jacob.com.Dispatch.call(props, "Item", i).toDispatch();
                com.jacob.com.Variant idVar = com.jacob.com.Dispatch.get(p, "PropertyID");
                if (idVar != null && idVar.getInt() == propertyId) {
                    return com.jacob.com.Dispatch.get(p, "Value").getInt();
                }
            }
        } catch (Throwable ignore) {
        }
        return -1;
    }

    /**
     * Write an integer WIA property to a device or item.
     *
     * Uses the same sequential-index iteration as getItemInt() because
     * Properties.Item(propertyId) keyed access fails on WIA 2.0 drivers.
     */
    private void setItemInt(com.jacob.com.Dispatch target, int propertyId, int value) {
        try {
            com.jacob.com.Dispatch props = com.jacob.com.Dispatch.get(target, "Properties").toDispatch();
            int count = com.jacob.com.Dispatch.get(props, "Count").getInt();
            for (int i = 1; i <= count; i++) {
                com.jacob.com.Dispatch p = com.jacob.com.Dispatch.call(props, "Item", i).toDispatch();
                com.jacob.com.Variant idVar = com.jacob.com.Dispatch.get(p, "PropertyID");
                if (idVar != null && idVar.getInt() == propertyId) {
                    com.jacob.com.Dispatch.put(p, "Value", new com.jacob.com.Variant(value));
                    log.debug("WIA: setItemInt({}, {}) OK", propertyId, value);
                    return;
                }
            }
            log.warn("WIA: setItemInt({}, {}) - property ID not found in collection (count={})",
                    propertyId, value, count);
        } catch (Throwable e) {
            log.warn("WIA: setItemInt({}, {}) failed: {}", propertyId, value, e.getMessage());
        }
    }

    /** Write a string WIA property to a device or item. */
    private void setItemString(com.jacob.com.Dispatch target, int propertyId, String value) {
        try {
            com.jacob.com.Dispatch props = com.jacob.com.Dispatch.get(target, "Properties").toDispatch();
            int count = com.jacob.com.Dispatch.get(props, "Count").getInt();
            for (int i = 1; i <= count; i++) {
                com.jacob.com.Dispatch p = com.jacob.com.Dispatch.call(props, "Item", i).toDispatch();
                com.jacob.com.Variant idVar = com.jacob.com.Dispatch.get(p, "PropertyID");
                if (idVar != null && idVar.getInt() == propertyId) {
                    com.jacob.com.Dispatch.put(p, "Value", new com.jacob.com.Variant(value));
                    log.debug("WIA: setItemString({}, '{}') OK", propertyId, value);
                    return;
                }
            }
        } catch (Throwable e) {
            log.warn("WIA: setItemString({}, '{}') failed: {}", propertyId, value, e.getMessage());
        }
    }

    /**
     * Finds a property ID by its display name. Essential for WIA 2.0 drivers
     * which often use non-standard PID offsets.
     */
    private int findPropertyIdByName(com.jacob.com.Dispatch target, String name, int fallbackId) {
        try {
            com.jacob.com.Dispatch props = com.jacob.com.Dispatch.get(target, "Properties").toDispatch();
            int count = com.jacob.com.Dispatch.get(props, "Count").getInt();
            for (int i = 1; i <= count; i++) {
                com.jacob.com.Dispatch p = com.jacob.com.Dispatch.call(props, "Item", i).toDispatch();
                com.jacob.com.Variant nmVar = com.jacob.com.Dispatch.get(p, "Name");
                if (nmVar != null && name.equalsIgnoreCase(nmVar.toString())) {
                    return com.jacob.com.Dispatch.get(p, "PropertyID").getInt();
                }
            }
        } catch (Throwable ignore) {
        }
        return fallbackId;
    }

    /** Helper to read a string property by ID. */
    private String getItemString(com.jacob.com.Dispatch target, int propertyId) {
        try {
            com.jacob.com.Dispatch props = com.jacob.com.Dispatch.get(target, "Properties").toDispatch();
            int count = com.jacob.com.Dispatch.get(props, "Count").getInt();
            for (int i = 1; i <= count; i++) {
                com.jacob.com.Dispatch p = com.jacob.com.Dispatch.call(props, "Item", i).toDispatch();
                com.jacob.com.Variant idVar = com.jacob.com.Dispatch.get(p, "PropertyID");
                if (idVar != null && idVar.getInt() == propertyId) {
                    com.jacob.com.Variant valVar = com.jacob.com.Dispatch.get(p, "Value");
                    return valVar != null ? valVar.toString() : null;
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    /**
     * Log every property (PropertyID, Name, Value) exposed by a WIA device or item.
     */
    private void dumpProperties(String label, com.jacob.com.Dispatch target) {
        try {
            com.jacob.com.Dispatch props = com.jacob.com.Dispatch.get(target, "Properties").toDispatch();
            int count = com.jacob.com.Dispatch.get(props, "Count").getInt();
            log.info("WIA: {} has {} properties:", label, count);
            for (int i = 1; i <= count; i++) {
                try {
                    com.jacob.com.Dispatch p = com.jacob.com.Dispatch.call(props, "Item", i).toDispatch();
                    com.jacob.com.Variant idVar = com.jacob.com.Dispatch.get(p, "PropertyID");
                    com.jacob.com.Variant nmVar = com.jacob.com.Dispatch.get(p, "Name");
                    com.jacob.com.Variant valVar = com.jacob.com.Dispatch.get(p, "Value");
                    log.info("WIA:   [{}] pid={} name='{}' value={}",
                            i,
                            idVar != null ? idVar.toString() : "?",
                            nmVar != null ? nmVar.toString() : "?",
                            valVar != null ? valVar.toString() : "?");
                } catch (Throwable e) {
                    log.info("WIA:   [{}] error reading: {}", i, e.getMessage());
                }
            }
        } catch (Throwable e) {
            log.info("WIA: dumpProperties({}) failed: {}", label, e.getMessage());
        }
    }

    private int mapColor(String color) {
        if (color == null)
            return 1; // WIA_INTENT_IMAGE_TYPE_COLOR
        switch (color.toLowerCase()) {
            case "color":
                return 1; // WIA_INTENT_IMAGE_TYPE_COLOR (bitmask 0x0001)
            case "grayscale":
            case "gray":
                return 2; // WIA_INTENT_IMAGE_TYPE_GRAYSCALE (bitmask 0x0002)
            case "black and white":
            case "blackandwhite":
            case "bw":
                return 4; // WIA_INTENT_IMAGE_TYPE_TEXT (bitmask 0x0004)
            default:
                return 1;
        }
    }

    private String formatGuid(String fmt) {
        if (fmt == null)
            return "{B96B3CAF-0728-11D3-9D7B-0000F81EF32E}"; // PNG
        switch (fmt.toLowerCase()) {
            case "pdf":
                return "{B96B3CAF-0728-11D3-9D7B-0000F81EF32E}"; // scan image; convert to PDF later
            case "jpg":
            case "jpeg":
                return "{B96B3CAE-0728-11D3-9D7B-0000F81EF32E}"; // JPEG
            case "png":
                return "{B96B3CAF-0728-11D3-9D7B-0000F81EF32E}"; // PNG
            case "tiff":
            case "tif":
                return "{B96B3CB1-0728-11D3-9D7B-0000F81EF32E}"; // TIFF
            default:
                return "{B96B3CAB-0728-11D3-9D7B-0000F81EF32E}"; // BMP
        }
    }

    private byte[] getImageData(com.jacob.com.Dispatch imageFile, ScanOptions options) {
        byte[] data = null;

        // Method 1: FileData.BinaryData
        try {
            com.jacob.com.Variant fileDataVar = com.jacob.com.Dispatch.get(imageFile, "FileData");
            if (fileDataVar != null && !fileDataVar.isNull()) {
                com.jacob.com.Dispatch fileDataDisp = fileDataVar.toDispatch();
                com.jacob.com.Variant binVar = com.jacob.com.Dispatch.get(fileDataDisp, "BinaryData");
                if (binVar != null && !binVar.isNull()) {
                    try {
                        com.jacob.com.SafeArray sa = binVar.toSafeArray();
                        if (sa != null)
                            data = sa.toByteArray();
                    } catch (Throwable e) {
                        Object obj = binVar.toJavaObject();
                        if (obj instanceof byte[])
                            data = (byte[]) obj;
                    }
                }
            }
        } catch (Throwable ignore) {
        }

        // Method 2: FileName property
        if (data == null) {
            try {
                com.jacob.com.Variant fileNameVar = com.jacob.com.Dispatch.get(imageFile, "FileName");
                if (fileNameVar != null && !fileNameVar.isNull()) {
                    String filePath = fileNameVar.toString();
                    if (filePath != null && !filePath.isEmpty() && !filePath.equals("null")) {
                        java.nio.file.Path path = java.nio.file.Paths.get(filePath);
                        if (java.nio.file.Files.exists(path))
                            data = java.nio.file.Files.readAllBytes(path);
                    }
                }
            } catch (Throwable ignore) {
            }
        }

        // Method 3: SaveFile
        if (data == null) {
            java.io.File tempFile = null;
            try {
                String ext = options.format != null ? options.format.toLowerCase() : "png";
                if ("pdf".equals(ext))
                    ext = "png";
                tempFile = java.io.File.createTempFile("wia_scan_", "." + ext);
                com.jacob.com.Dispatch.call(imageFile, "SaveFile",
                        new com.jacob.com.Variant(tempFile.getAbsolutePath()));
                Thread.sleep(100);
                if (tempFile.exists() && tempFile.length() > 0)
                    data = java.nio.file.Files.readAllBytes(tempFile.toPath());
            } catch (Throwable ignore) {
            } finally {
                if (tempFile != null)
                    tempFile.delete();
            }
        }
        return data;
    }

    private byte[] toPdf(java.util.List<byte[]> allPages) {
        if (allPages == null || allPages.isEmpty())
            return null;
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            for (byte[] imageBytes : allPages) {
                if (imageBytes == null)
                    continue;
                try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageBytes)) {
                    java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(bis);
                    if (image == null)
                        continue;

                    java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(image, "png", buf);
                    com.itextpdf.text.Image pdfImage = com.itextpdf.text.Image.getInstance(buf.toByteArray());

                    document.setPageSize(com.itextpdf.text.PageSize.A4);
                    document.newPage();

                    float maxW = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
                    float maxH = document.getPageSize().getHeight() - document.topMargin() - document.bottomMargin();
                    pdfImage.scaleToFit(maxW, maxH);
                    pdfImage.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    document.add(pdfImage);
                }
            }
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("WIA multi-page PDF conversion failed: {}", e.getMessage());
            return allPages.isEmpty() ? null : allPages.get(0);
        }
    }

    private byte[] toPdf(byte[] imageBytes) {
        java.util.List<byte[]> single = new java.util.ArrayList<>();
        single.add(imageBytes);
        return toPdf(single);
    }

}
