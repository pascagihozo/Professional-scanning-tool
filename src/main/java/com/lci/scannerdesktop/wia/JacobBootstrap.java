package com.lci.scannerdesktop.wia;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Slf4j
public class JacobBootstrap {

    private final WiaConfig config;

    public JacobBootstrap(WiaConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        if (!config.isEnabled()) return;
        try {
            String arch = System.getProperty("os.arch", "").contains("64") ? "x64" : "x86";
            Path libDir = Paths.get(System.getProperty("user.dir"), "scanner-desktop-client", "lib");
            if (!Files.exists(libDir)) {
                // also try project root lib fallback
                libDir = Paths.get(System.getProperty("user.dir"), "lib");
            }
            String dllName = arch.equals("x64") ? "jacob-1.21-x64.dll" : "jacob-1.21-x86.dll";
            Path dllPath = libDir.resolve(dllName);
            Path jarPath = libDir.resolve("jacob.jar");

            if (!Files.exists(dllPath)) {
                log.warn("JACOB DLL not found at {} — WIA will not work until provided.", dllPath.toAbsolutePath());
                return;
            }
            System.setProperty("jacob.dll.path", dllPath.toString());
            log.info("Configured JACOB DLL: {}", dllPath.toAbsolutePath());
        } catch (Exception e) {
            log.warn("Failed to configure JACOB: {}", e.getMessage());
        }
    }
}


