package com.lci.scannerdesktop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
public class ScannerDesktopApplication {
    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("                    Pascal Scanning Client                      ");
        System.out.println("              Professional Document Scanning Solution            ");
        System.out.println("                        Version 1.0.0                           ");
        System.out.println("=================================================================");
        System.out.println();
        System.out.println("Starting Pascal Scanning Client...");
        System.out.println("Web UI: http://127.0.0.1:17070/ui");
        System.out.println("API: http://127.0.0.1:17070/v1");
        System.out.println();
        
        SpringApplication.run(ScannerDesktopApplication.class, args);
    }
}


