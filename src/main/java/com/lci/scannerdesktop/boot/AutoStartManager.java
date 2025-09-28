package com.lci.scannerdesktop.boot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Slf4j
public class AutoStartManager {

    @Value("${autostart.enabled:true}")
    private boolean autostartEnabled;

    @PostConstruct
    public void setup() {
        if (!autostartEnabled) {
            log.info("Autostart disabled by configuration");
            return;
        }
        try {
            String appPath = System.getProperty("jpackage.app-path");
            if (appPath == null || appPath.isBlank()) {
                // fallback to current java command (dev mode)
                appPath = new File(System.getProperty("java.class.path")).getAbsolutePath();
            }
            if (isWindows()) {
                enableWindowsAutostart(appPath);
            } else if (isMac()) {
                enableMacAutostart(appPath);
            } else if (isLinux()) {
                enableLinuxAutostart(appPath);
            }
        } catch (Exception e) {
            log.warn("Autostart setup failed: {}", e.getMessage());
        }
    }

    private boolean isWindows() { return System.getProperty("os.name","" ).toLowerCase().contains("win"); }
    private boolean isMac() { return System.getProperty("os.name","" ).toLowerCase().contains("mac"); }
    private boolean isLinux() { String os = System.getProperty("os.name","" ).toLowerCase(); return os.contains("nix") || os.contains("nux") || os.contains("linux"); }

    private void enableWindowsAutostart(String exePath) {
        try {
            String runKey = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
            String cmd = "reg add " + runKey + " /v \"PascalScanningAgent\" /t REG_SZ /d \"" + exePath.replace("\\", "\\\\") + "\" /f";
            new ProcessBuilder("cmd", "/c", cmd).start();
            log.info("Configured Windows autostart for Pascal Scanning Agent");
        } catch (Exception e) {
            log.warn("Windows autostart failed: {}", e.getMessage());
        }
    }

    private void enableMacAutostart(String appPath) {
        try {
            String plist = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
                    "<plist version=\"1.0\">\n" +
                    "<dict>\n" +
                    "  <key>Label</key><string>com.lci.pascal</string>\n" +
                    "  <key>ProgramArguments</key>\n" +
                    "  <array><string>" + appPath + "</string></array>\n" +
                    "  <key>RunAtLoad</key><true/>\n" +
                    "  <key>KeepAlive</key><false/>\n" +
                    "</dict>\n" +
                    "</plist>\n";
            Path dir = Paths.get(System.getProperty("user.home"), "Library", "LaunchAgents");
            Files.createDirectories(dir);
            Path file = dir.resolve("com.lci.pascal.plist");
            try (FileWriter fw = new FileWriter(file.toFile())) { fw.write(plist); }
            new ProcessBuilder("launchctl", "load", file.toString()).start();
            log.info("Configured macOS LaunchAgent for autostart: {}", file);
        } catch (Exception e) {
            log.warn("macOS autostart failed: {}", e.getMessage());
        }
    }

    private void enableLinuxAutostart(String appPath) {
        try {
            Path dir = Paths.get(System.getProperty("user.home"), ".config", "autostart");
            Files.createDirectories(dir);
            Path desktop = dir.resolve("pascal-scanning-agent.desktop");
            String content = "[Desktop Entry]\n" +
                    "Type=Application\n" +
                    "Name=Pascal Scanning Agent\n" +
                    "Exec=\"" + appPath + "\"\n" +
                    "X-GNOME-Autostart-enabled=true\n";
            try (FileWriter fw = new FileWriter(desktop.toFile())) { fw.write(content); }
            log.info("Configured Linux autostart at {}", desktop);
        } catch (Exception e) {
            log.warn("Linux autostart failed: {}", e.getMessage());
        }
    }
}


