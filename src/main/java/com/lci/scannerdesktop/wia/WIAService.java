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
                    // WIA device type: 1=Scanner, 2=Camera, 3=Video. Accept 1 as scanner (some
                    // drivers use 1).
                    if (type != 1)
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
                        if (type != 1)
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

            // Set Document Handling (Feeder vs Flatbed)
            // 3088: WIA_DPS_DOCUMENT_HANDLING_SELECT (1=Feeder, 2=Flatbed)
            int handling = (Boolean.TRUE.equals(options.adf)) ? 1 : 2;
            setItemInt(device, 3088, handling);

            // 3096: WIA_DPS_PAGES (0 = all pages from feeder)
            if (Boolean.TRUE.equals(options.adf)) {
                setItemInt(device, 3096, 0);
            }

            com.jacob.com.Dispatch items = com.jacob.com.Dispatch.get(device, "Items").toDispatch();
            int itemCount = com.jacob.com.Dispatch.get(items, "Count").getInt();
            log.debug("WIA: Device has {} item(s)", itemCount);
            
            // Try to find the best item (prefer flatbed scanner, item 1 is usually flatbed)
            com.jacob.com.Dispatch item = null;
            if (itemCount > 0) {
                item = com.jacob.com.Dispatch.call(items, "Item", 1).toDispatch();
                try {
                    String itemName = com.jacob.com.Dispatch.get(item, "ItemType").toString();
                    log.debug("WIA: Using item 1, type: {}", itemName);
                } catch (Throwable ignore) {}
            }
            
            if (item == null) {
                r.status = "ERROR";
                r.message = "Scanner has no available items";
                return r;
            }

            // Apply basic properties
            setItemInt(item, 6147, mapColor(options.colorMode)); // Current Intent (approx)
            setItemInt(item, 6146, options.dpi != null ? options.dpi : 300); // Horizontal Resolution
            setItemInt(item, 6148, options.dpi != null ? options.dpi : 300); // Vertical Resolution

            // TODO: If ADF requested, set feeder and handle multi-page loop
            if (Boolean.TRUE.equals(options.adf)) {
                // Example feeder setup (device- and driver-dependent)
                // setItemInt(item, /*ADF enable*/ 3088, 1); // placeholder property id
            }

            // Acquire image (still image transfer)
            com.jacob.com.Dispatch imageFile = com.jacob.com.Dispatch.call(item, "Transfer", formatGuid(options.format)).toDispatch();

            byte[] data = null;
            try {
                // Some drivers return an ImageFile with no FileName; prefer FileData if available
                com.jacob.com.Variant fileDataVar = com.jacob.com.Dispatch.get(imageFile, "FileData");
                if (fileDataVar != null && !fileDataVar.isNull()) {
                    com.jacob.com.Dispatch fileDataDisp = fileDataVar.toDispatch();
                    com.jacob.com.Variant binVar = com.jacob.com.Dispatch.get(fileDataDisp, "BinaryData");
                    if (binVar != null) {
                        try {
                            com.jacob.com.SafeArray sa = binVar.toSafeArray();
                            data = (byte[]) sa.toByteArray();
                        } catch (Throwable ignore2) {
                            // Some drivers expose a byte[] directly
                            try { data = (byte[]) binVar.toJavaObject(); } catch (Throwable ignore3) {}
                        }
                    }
                }
            } catch (Throwable ignore) {}

            if (data == null) {
                // Fallback: persist to a temp file if FileName is provided by the driver
                try {
                    String filePath = com.jacob.com.Dispatch.get(imageFile, "FileName").toString();
                    java.nio.file.Path path = java.nio.file.Paths.get(filePath);
                    data = java.nio.file.Files.readAllBytes(path);
                } catch (Throwable t) {
                    log.warn("WIA image retrieval failed (FileData/FileName). {}", t.getMessage());
                }
            }

            // Convert to PDF if requested
            byte[] out = null;
            String fmt = options.format == null ? "pdf" : options.format.toLowerCase();
            if ("pdf".equals(fmt)) {
                out = toPdf(pageDatas);
                if (out == null) {
                    r.status = "ERROR";
                    r.message = "Failed to convert scanned pages to PDF";
                    return r;
                }
            } else {
                // Return only the first page for image formats unless a specific multi-page
                // format is used
                out = pageDatas.get(0);
            }

            r.status = "SUCCESS";
            r.fileData = out;
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

    private void setItemInt(com.jacob.com.Dispatch target, int propertyId, int value) {
        try {
            com.jacob.com.Dispatch properties = com.jacob.com.Dispatch.get(target, "Properties").toDispatch();
            com.jacob.com.Dispatch property = com.jacob.com.Dispatch
                    .call(properties, "Item", new com.jacob.com.Variant(propertyId)).toDispatch();
            com.jacob.com.Dispatch.put(property, "Value", new com.jacob.com.Variant(value));
        } catch (Throwable ignore) {
        }
    }

    private int mapColor(String color) {
        if (color == null)
            return 0; // Color
        switch (color.toLowerCase()) {
            case "color":
                return 0; // WIA_INTENT_IMAGE_TYPE_COLOR
            case "grayscale":
            case "gray":
                return 1; // WIA_INTENT_IMAGE_TYPE_GRAYSCALE
            case "black and white":
            case "blackandwhite":
            case "bw":
                return 2; // WIA_INTENT_IMAGE_TYPE_TEXT
            default:
                return 0;
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

    private byte[] toPdf(java.util.List<byte[]> allPageBytes) {
        if (allPageBytes == null || allPageBytes.isEmpty())
            return null;
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            for (byte[] imageBytes : allPageBytes) {
                if (imageBytes == null)
                    continue;
                try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageBytes)) {
                    java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(bis);
                    if (image == null)
                        continue;

                    java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(image, "png", buf);
                    com.itextpdf.text.Image pdfImage = com.itextpdf.text.Image.getInstance(buf.toByteArray());

                    float maxW = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
                    float maxH = document.getPageSize().getHeight() - document.topMargin() - document.bottomMargin();
                    pdfImage.scaleToFit(maxW, maxH);
                    pdfImage.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);

                    document.add(pdfImage);
                    document.newPage(); // Add a new page for the next image
                }
            }
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("WIA image->PDF conversion failed: {}", e.getMessage());
            return allPageBytes.get(0); // Fallback to first page
        }
    }

}
