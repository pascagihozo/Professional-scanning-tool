package com.lci.scannerdesktop.boot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

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
            // Resolve the path to the currently running executable.
            // ProcessHandle gives the real launcher .exe when running as a jpackage app.
            // If the command is java/javaw (dev mode), skip registration entirely.
            Optional<String> cmd = ProcessHandle.current().info().command();
            if (cmd.isEmpty()) {
                log.debug("Autostart: cannot determine current process path, skipping");
                return;
            }
            String appPath = cmd.get();
            String lower = appPath.toLowerCase();

            // Only register when running from the installed jpackage launcher, not from dev
            // JVM
            if (!lower.endsWith(".exe") || lower.contains("java.exe") || lower.contains("javaw.exe")) {
                log.debug("Autostart: running in dev mode ({}), skipping autostart registration", appPath);
                return;
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

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    private boolean isLinux() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("linux");
    }

    private void enableWindowsAutostart(String exePath) {
        try {
            String runKey = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
            String valueName = "PascalScanningTool";

            // Derive the VBS script path from the exe path for silent background startup
            // The VBS script runs javaw.exe invisibly without any console window
            Path exeFile = Paths.get(exePath);
            Path appDir = exeFile.getParent();
            Path vbsPath = appDir.resolve("app").resolve("run-background.vbs");

            String startupCommand;
            if (Files.exists(vbsPath)) {
                // Use wscript to run VBS silently - this ensures no window appears at all
                startupCommand = "wscript.exe //B //Nologo \"" + vbsPath.toString() + "\"";
                log.info("Autostart: Using VBS launcher for silent startup: {}", vbsPath);
            } else {
                // Fallback to direct exe if VBS not found
                startupCommand = "\"" + exePath + "\"";
                log.info("Autostart: VBS not found, using direct exe: {}", exePath);
            }

            // Check if already registered with the same command — avoid redundant writes
            Process query = new ProcessBuilder(
                    "reg", "query", runKey, "/v", valueName)
                    .redirectErrorStream(true).start();
            String queryOut = new String(query.getInputStream().readAllBytes());
            query.waitFor();
            if (queryOut.contains(startupCommand) || queryOut.contains(vbsPath.toString())) {
                log.debug("Autostart: Windows registry entry already current, nothing to do");
                return;
            }

            Process reg = new ProcessBuilder(
                    "reg", "add", runKey,
                    "/v", valueName,
                    "/t", "REG_SZ",
                    "/d", startupCommand,
                    "/f")
                    .redirectErrorStream(true).start();
            int rc = reg.waitFor();
            if (rc == 0) {
                log.info("Autostart: Successfully registered Windows startup: {}", startupCommand);
            } else {
                log.warn("Autostart: reg add failed with exit code {}", rc);
            }
        } catch (Exception e) {
            log.error("Windows autostart registration failed: {}", e.getMessage());
        }
    }

    private void enableMacAutostart(String appPath) {
        try {
            String plist = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                    +
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
            try (FileWriter fw = new FileWriter(file.toFile())) {
                fw.write(plist);
            }
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
            try (FileWriter fw = new FileWriter(desktop.toFile())) {
                fw.write(content);
            }
            log.info("Configured Linux autostart at {}", desktop);
        } catch (Exception e) {
            log.warn("Linux autostart failed: {}", e.getMessage());
        }
    }
}
