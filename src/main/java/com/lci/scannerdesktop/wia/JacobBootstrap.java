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
        if (!config.isEnabled())
            return;
        try {
            String arch = System.getProperty("os.arch", "").contains("64") ? "x64" : "x86";
            String dllName = "jacob-1.21-" + arch + ".dll";
            String userDir = System.getProperty("user.dir");

            Path[] searchPaths = {
                    Paths.get(userDir, "app", "lib", dllName),
                    Paths.get(userDir, "lib", dllName),
                    Paths.get(userDir, "scanner-desktop-client", "lib", dllName)
            };

            Path foundPath = null;
            for (Path p : searchPaths) {
                if (Files.exists(p)) {
                    foundPath = p;
                    break;
                }
            }

            if (foundPath != null) {
                String absolutePath = foundPath.toAbsolutePath().toString();
                System.setProperty("jacob.dll.path", absolutePath);
                log.info("JACOB DLL discovered and configured: {}", absolutePath);
            } else {
                log.warn("JACOB DLL ({}) not found in search paths.", dllName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize JACOB: ", e);
        }
    }
}
