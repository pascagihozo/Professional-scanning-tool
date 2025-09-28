package com.lci.scannerdesktop.sane;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SaneService {

    public static class ScannerInfo {
        public String id;
        public String name;
        public String devicePath;
        public boolean connected;
        public String status;
    }

    public static class ScanOptions {
        public String scannerId;
        public Integer dpi = 300;
        public String colorMode = "Color";
        public String paperSize = "A4";
        public String format = "pdf";
        public boolean previewOnly = false;
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

    private boolean isUnixLike() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("mac");
    }

    public List<ScannerInfo> discoverScanners() {
        List<ScannerInfo> list = new ArrayList<>();
        if (!isUnixLike()) return list;
        try {
            Process p = new ProcessBuilder("scanimage", "-L").redirectErrorStream(true).start();
            p.waitFor(5, TimeUnit.SECONDS);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    // Example: device `epjitsu:libusb:001:004' is a FUJITSU ScanSnap ...
                    int s = line.indexOf('`');
                    int e = line.indexOf("'", s + 1);
                    if (s >= 0 && e > s) {
                        String device = line.substring(s + 1, e);
                        String name = line.substring(e + 5).trim();
                        ScannerInfo si = new ScannerInfo();
                        si.id = "sane_" + device;
                        si.name = name.isEmpty() ? device : name;
                        si.devicePath = device;
                        si.connected = true;
                        si.status = "SANE Scanner";
                        list.add(si);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("SANE discovery failed: {}", e.getMessage());
        }
        return list;
    }

    public ScanResult scan(ScanOptions options) {
        ScanResult r = new ScanResult();
        if (!isUnixLike()) {
            r.status = "ERROR";
            r.message = "SANE not available on this OS";
            return r;
        }
        Path tmp = null;
        try {
            String fmt = options.format == null ? "pdf" : options.format.toLowerCase();
            String outExt = "pdf".equals(fmt) ? "png" : mapExt(fmt); // scan to png if pdf requested
            tmp = Files.createTempFile("sane_", "." + outExt);

            List<String> cmd = new ArrayList<>();
            cmd.add("scanimage");
            if (options.scannerId != null && options.scannerId.startsWith("sane_")) {
                cmd.add("--device-name");
                cmd.add(options.scannerId.substring(5));
            }
            cmd.add("--resolution");
            cmd.add(String.valueOf(options.dpi == null ? 300 : options.dpi));
            cmd.add("--mode");
            cmd.add(mapMode(options.colorMode));
            cmd.add("--format");
            cmd.add(outExt);
            cmd.add("--output-file");
            cmd.add(tmp.toString());

            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            p.waitFor(120, TimeUnit.SECONDS);
            if (p.exitValue() != 0 || !Files.exists(tmp) || Files.size(tmp) == 0) {
                r.status = "ERROR";
                r.message = "SANE scan failed";
                return r;
            }

            byte[] data = Files.readAllBytes(tmp);
            byte[] out = data;
            if ("pdf".equals(fmt)) {
                out = toPdf(data);
            }
            r.status = "SUCCESS";
            r.fileFormat = fmt;
            String base = (options.outputFileName == null || options.outputFileName.isBlank()) ? "sane_scan" : options.outputFileName;
            r.fileName = base + "_" + UUID.randomUUID().toString().substring(0,8) + "." + fmt;
            r.fileData = out;
            r.message = "OK";
            return r;
        } catch (Exception e) {
            r.status = "ERROR";
            r.message = e.getMessage();
            return r;
        } finally {
            if (tmp != null) try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}
        }
    }

    private String mapExt(String fmt) {
        switch (fmt) {
            case "jpg":
            case "jpeg": return "jpeg";
            case "png": return "png";
            case "tiff":
            case "tif": return "tiff";
            default: return "pnm";
        }
    }

    private String mapMode(String color) {
        if (color == null) return "Color";
        switch (color.toLowerCase()) {
            case "color": return "Color";
            case "grayscale":
            case "gray": return "Gray";
            case "black and white":
            case "bw": return "Lineart";
            default: return "Color";
        }
    }

    private byte[] toPdf(byte[] imageData) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(bis);
            if (image == null) return imageData;
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();
            ByteArrayOutputStream img = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", img);
            com.itextpdf.text.Image pdfImage = com.itextpdf.text.Image.getInstance(img.toByteArray());
            float maxW = doc.getPageSize().getWidth() - doc.leftMargin() - doc.rightMargin();
            float maxH = doc.getPageSize().getHeight() - doc.topMargin() - doc.bottomMargin();
            pdfImage.scaleToFit(maxW, maxH);
            pdfImage.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            doc.add(pdfImage);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("SANE image->PDF failed: {}", e.getMessage());
            return imageData;
        }
    }
}


