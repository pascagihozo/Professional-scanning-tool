package com.lci.scannerdesktop.escl;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ESCLService {

    public ESCLService(EsclConfig config) {
        this.config = config;
        // Initialize probe ports from configuration
        java.util.List<String> pl = config.getPortList();
        this.ports = pl.toArray(new String[0]);
        log.info("ESCL probing ports: {}", java.util.Arrays.toString(this.ports));
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

    public static class Capabilities {
        public java.util.List<Integer> supportedResolutions;
        public java.util.List<String> supportedFormats;
        public java.util.List<String> supportedColorModes;
        public boolean duplexSupported;
        public boolean adfSupported;
    }

    private final int discoveryTimeoutMs = 5000;
    private final int connectionTimeoutMs = 3000;
    private final int scanTimeoutMs = 30000;
    private final String[] ports;
    private final Map<String, ScannerInfo> cache = new LinkedHashMap<>();
    private final EsclConfig config;

    public List<ScannerInfo> discoverScanners() {
        List<ScannerInfo> scanners = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            List<CompletableFuture<List<ScannerInfo>>> futures = new ArrayList<>();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (nic.isLoopback() || !nic.isUp()) continue;
                futures.add(CompletableFuture.supplyAsync(() -> scanInterface(nic)));
            }
            for (CompletableFuture<List<ScannerInfo>> f : futures) {
                try {
                    scanners.addAll(f.get(discoveryTimeoutMs, TimeUnit.MILLISECONDS));
                } catch (Exception ignore) {}
            }
            // Manual IPs
            for (String ip : config.getManualIpList()) {
                ScannerInfo si = probeIp(ip);
                if (si != null) scanners.add(si);
            }
        } catch (Exception e) {
            log.warn("ESCL discovery failed: {}", e.getMessage());
        }
        // de-dup by devicePath
        Map<String, ScannerInfo> unique = new LinkedHashMap<>();
        for (ScannerInfo s : scanners) unique.put(s.devicePath, s);
        cache.clear();
        cache.putAll(unique);
        return new ArrayList<>(cache.values());
    }

    public Capabilities getCapabilities(String baseUrl) {
        Capabilities caps = new Capabilities();
        caps.supportedResolutions = new ArrayList<>();
        caps.supportedFormats = new ArrayList<>();
        caps.supportedColorModes = new ArrayList<>();
        caps.duplexSupported = true; // common default for MFPs
        caps.adfSupported = true;
        try {
            String xml = get(baseUrl + "/eSCL/ScannerCapabilities");
            if (xml == null) return defaults(caps);
            // Resolutions
            java.util.regex.Matcher mr = java.util.regex.Pattern.compile("<scan:(?:X|Y)Resolution>\\s*(\\d+)\\s*</scan:(?:X|Y)Resolution>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(xml);
            java.util.Set<Integer> resset = new java.util.TreeSet<>();
            while (mr.find()) {
                try { resset.add(Integer.parseInt(mr.group(1))); } catch (Exception ignore) {}
            }
            if (!resset.isEmpty()) caps.supportedResolutions = new ArrayList<>(resset);
            // Formats
            if (xml.toLowerCase().contains("application/pdf")) caps.supportedFormats.add("pdf");
            if (xml.toLowerCase().contains("image/jpeg")) caps.supportedFormats.add("jpg");
            if (xml.toLowerCase().contains("image/png")) caps.supportedFormats.add("png");
            if (xml.toLowerCase().contains("image/tiff")) caps.supportedFormats.add("tiff");
            if (caps.supportedFormats.isEmpty()) caps.supportedFormats = java.util.Arrays.asList("pdf","jpg","png","tiff");
            // Color modes
            if (xml.toLowerCase().contains("rgb24")) caps.supportedColorModes.add("Color");
            if (xml.toLowerCase().contains("grayscale8")) caps.supportedColorModes.add("Grayscale");
            if (xml.toLowerCase().contains("blackandwhite1")) caps.supportedColorModes.add("Black and White");
            if (caps.supportedColorModes.isEmpty()) caps.supportedColorModes = java.util.Arrays.asList("Color","Grayscale","Black and White");
            // ADF/duplex hints
            String lower = xml.toLowerCase();
            caps.adfSupported = lower.contains("<scan:inputsource>feeder</scan:inputsource>") || lower.contains("feeder");
            caps.duplexSupported = lower.contains("duplex") || caps.adfSupported;
            return caps;
        } catch (Exception e) {
            return defaults(caps);
        }
    }

    private Capabilities defaults(Capabilities c) {
        if (c.supportedResolutions == null || c.supportedResolutions.isEmpty()) c.supportedResolutions = java.util.Arrays.asList(150,200,300,600);
        if (c.supportedFormats == null || c.supportedFormats.isEmpty()) c.supportedFormats = java.util.Arrays.asList("pdf","jpg","png","tiff");
        if (c.supportedColorModes == null || c.supportedColorModes.isEmpty()) c.supportedColorModes = java.util.Arrays.asList("Color","Grayscale","Black and White");
        if (!c.adfSupported) c.adfSupported = true;
        return c;
    }

    private List<ScannerInfo> scanInterface(NetworkInterface nic) {
        List<ScannerInfo> found = new ArrayList<>();
        for (InterfaceAddress ia : nic.getInterfaceAddresses()) {
            InetAddress addr = ia.getAddress();
            if (!(addr instanceof Inet4Address) || !addr.isSiteLocalAddress()) continue;
            String base = addr.getHostAddress();
            base = base.substring(0, base.lastIndexOf('.'));
            List<CompletableFuture<ScannerInfo>> futures = new ArrayList<>();
            int range = Math.max(1, config.getDiscoveryRange());
            for (int i = 1; i <= range; i++) {
                final String ip = base + "." + i;
                futures.add(CompletableFuture.supplyAsync(() -> probeIp(ip)));
            }
            for (CompletableFuture<ScannerInfo> f : futures) {
                try {
                    ScannerInfo si = f.get(connectionTimeoutMs, TimeUnit.MILLISECONDS);
                    if (si != null) found.add(si);
                } catch (Exception ignore) {}
            }
        }
        return found;
    }

    private ScannerInfo probeIp(String ip) {
        for (String p : ports) {
            try {
                int port = Integer.parseInt(p);
                String base = (port == 443 ? "https://" : "http://") + ip + ((port == 80 || port == 443) ? "" : ":" + port);
                String caps = get(base + "/eSCL/ScannerCapabilities");
                if (caps != null) {
                    ScannerInfo si = new ScannerInfo();
                    si.id = "escl_" + ip;
                    si.devicePath = base;
                    si.connected = true;
                    si.status = "eSCL Network Scanner";
                    si.name = extractName(caps);
                    if (si.name == null || si.name.isBlank()) si.name = "Network Scanner (" + ip + ")";
                    return si;
                }
            } catch (Exception ignore) {}
        }
        return null;
    }

    public ScanResult scan(String baseUrl, ScanOptions options) {
        ScanResult result = new ScanResult();
        try {
            // Create job
            String settings = buildSettings(options);
            HttpURLConnection c = connect(baseUrl + "/eSCL/ScanJobs");
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            c.setDoOutput(true);
            try (OutputStreamWriter w = new OutputStreamWriter(c.getOutputStream(), StandardCharsets.UTF_8)) {
                w.write(settings);
            }
            int code = c.getResponseCode();
            if (code != 201) {
                result.status = "ERROR";
                result.message = "Failed to create job: HTTP " + code;
                return result;
            }
            String loc = c.getHeaderField("Location");
            if (loc == null || !loc.startsWith("http")) loc = baseUrl + "/eSCL/ScanJobs/1";

            // Poll NextDocument
            byte[] data = null;
            for (int i = 0; i < 60; i++) {
                Thread.sleep(1000);
                HttpURLConnection d = connect(loc + "/NextDocument");
                d.setRequestMethod("GET");
                d.setReadTimeout(scanTimeoutMs);
                int rc = d.getResponseCode();
                if (rc == 200) {
                    try (InputStream is = d.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
                        data = baos.toByteArray();
                        break;
                    }
                } else if (rc == 503) {
                    continue; // busy
                } else if (rc == 404) {
                    break; // done/not found
                }
            }

            if (data == null || data.length == 0) {
                result.status = "ERROR";
                result.message = "No scan data";
                return result;
            }

            String fmt = (options.format == null ? "pdf" : options.format.toLowerCase());
            byte[] finalBytes = data;
            if ("pdf".equals(fmt) && !isPdf(data)) {
                byte[] pdf = toPdf(data);
                if (pdf != null && isPdf(pdf)) finalBytes = pdf;
            }

            result.status = "SUCCESS";
            result.fileData = finalBytes;
            result.fileFormat = fmt;
            String baseName = (options.outputFileName == null || options.outputFileName.isBlank()) ? "escl_scan" : options.outputFileName.trim();
            String ts = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            result.fileName = baseName + "_" + ts + "." + fmt;
            result.message = "OK";
            return result;

        } catch (Exception e) {
            result.status = "ERROR";
            result.message = e.getMessage();
            return result;
        }
    }

    private String buildSettings(ScanOptions o) {
        int dpi = o.dpi == null ? 300 : o.dpi;
        String color = mapColor(o.colorMode);
        String mime = mapMime(o.format);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<scan:ScanSettings xmlns:scan=\"http://schemas.hp.com/imaging/escl/2011/05/03\" xmlns:pwg=\"http://www.pwg.org/schemas/2010/12/sm\">\n" +
                "  <pwg:Version>2.0</pwg:Version>\n" +
                "  <scan:Intent>Document</scan:Intent>\n" +
                "  <scan:DocumentFormat>" + mime + "</scan:DocumentFormat>\n" +
                "  <scan:XResolution>" + dpi + "</scan:XResolution>\n" +
                "  <scan:YResolution>" + dpi + "</scan:YResolution>\n" +
                "  <scan:ColorMode>" + color + "</scan:ColorMode>\n" +
                "  <scan:InputSource>" + (Boolean.TRUE.equals(o.adf) ? "Feeder" : "Platen") + "</scan:InputSource>\n" +
                "</scan:ScanSettings>";
    }

    private String mapMime(String fmt) {
        if (fmt == null) return "application/pdf";
        switch (fmt.toLowerCase()) {
            case "pdf": return "application/pdf";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "tiff":
            case "tif": return "image/tiff";
            default: return "application/pdf";
        }
    }

    private String mapColor(String color) {
        if (color == null) return "RGB24";
        switch (color.toLowerCase()) {
            case "color": return "RGB24";
            case "grayscale":
            case "gray": return "Grayscale8";
            case "black and white":
            case "blackandwhite":
            case "bw": return "BlackAndWhite1";
            default: return "RGB24";
        }
    }

    private boolean isPdf(byte[] data) {
        if (data == null || data.length < 5) return false;
        try {
            String header = new String(data, 0, Math.min(8, data.length), StandardCharsets.ISO_8859_1);
            if (!header.startsWith("%PDF-")) return false;
            String tail = new String(data, Math.max(0, data.length - 20), Math.min(20, data.length), StandardCharsets.ISO_8859_1);
            return tail.contains("%%EOF") || tail.contains("EOF");
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] toPdf(byte[] imgBytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imgBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BufferedImage image = ImageIO.read(bis);
            if (image == null) return null;

            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            ImageIO.write(image, "png", tmp);
            Image pdfImg = Image.getInstance(tmp.toByteArray());
            float maxW = doc.getPageSize().getWidth() - doc.leftMargin() - doc.rightMargin();
            float maxH = doc.getPageSize().getHeight() - doc.topMargin() - doc.bottomMargin();
            pdfImg.scaleToFit(maxW, maxH);
            pdfImg.setAlignment(Element.ALIGN_CENTER);
            doc.add(pdfImg);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("Image to PDF failed: {}", e.getMessage());
            return null;
        }
    }

    private HttpURLConnection connect(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", "EDMS-Desktop-Client/0.1");
        c.setRequestProperty("Accept", "*/*");
        if (c instanceof HttpsURLConnection) {
            HttpsURLConnection h = (HttpsURLConnection) c;
            h.setHostnameVerifier((s, session) -> true);
            try {
                javax.net.ssl.SSLContext ctx = javax.net.ssl.SSLContext.getInstance("TLS");
                ctx.init(null, new javax.net.ssl.TrustManager[]{new javax.net.ssl.X509TrustManager() {
                    public void checkClientTrusted(java.security.cert.X509Certificate[] xcs, String s) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] xcs, String s) {}
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                }}, new java.security.SecureRandom());
                h.setSSLSocketFactory(ctx.getSocketFactory());
            } catch (Exception ignore) {}
        }
        c.setConnectTimeout(connectionTimeoutMs);
        c.setReadTimeout(connectionTimeoutMs);
        return c;
    }

    private String get(String url) {
        try {
            HttpURLConnection c = connect(url);
            c.setRequestMethod("GET");
            int code = c.getResponseCode();
            if (code != 200) return null;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String extractName(String capsXml) {
        try {
            String[] patterns = new String[]{
                    "<scan:MakeAndModel>([^<]+)</scan:MakeAndModel>",
                    "<scan:Make>([^<]+)</scan:Make>",
                    "<scan:Model>([^<]+)</scan:Model>",
                    "<pwg:MakeAndModel>([^<]+)</pwg:MakeAndModel>"
            };
            for (String p : patterns) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(p, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(capsXml);
                if (m.find()) return m.group(1);
            }
        } catch (Exception ignore) {}
        return null;
    }
}


