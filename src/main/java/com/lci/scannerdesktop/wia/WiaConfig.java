package com.lci.scannerdesktop.wia;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wia")
@Getter
@Setter
public class WiaConfig {
    /** Enable Windows WIA backend. Default: false */
    private boolean enabled = false;
}


