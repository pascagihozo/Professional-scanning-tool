package com.lci.scannerdesktop.escl;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "escl")
@Getter
@Setter
public class EsclConfig {
    /** How many hosts per subnet to probe (starting from .1). Default 20 */
    private int discoveryRange = 20;
    /** Comma-separated list of known scanner IPs (e.g., 192.168.1.50,192.168.1.60) */
    private String manualIps = "";
    /** Comma-separated list of common ports to probe (e.g., 80,443,8080,8443,8181) */
    private String commonPorts = "80,443,8080,8443";

    public List<String> getManualIpList() {
        if (manualIps == null || manualIps.trim().isEmpty()) return List.of();
        return Arrays.stream(manualIps.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public List<String> getPortList() {
        String source = (commonPorts == null || commonPorts.trim().isEmpty()) ? "80,443,8080,8443" : commonPorts;
        return Arrays.stream(source.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}


