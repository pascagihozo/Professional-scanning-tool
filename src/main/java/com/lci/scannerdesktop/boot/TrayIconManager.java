package com.lci.scannerdesktop.boot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URL;

@Component
@Slf4j
public class TrayIconManager {

    @Value("${server.port:17070}")
    private int port;

    @PostConstruct
    public void init() {
        if (GraphicsEnvironment.isHeadless()) {
            log.warn("TrayIcon: Graphical environment not detected. Skipping tray icon initialization.");
            return;
        }

        if (!SystemTray.isSupported()) {
            log.warn("TrayIcon: SystemTray is not supported on this platform.");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            // Try to load icon from resources. We'll need a small PNG/GIF for the tray.
            // If missing, we'll use a fallback or a default.
            Image image = createIconImage();
            if (image == null) {
                log.warn("TrayIcon: Could not load tray icon image.");
                return;
            }

            PopupMenu popup = new PopupMenu();

            MenuItem openItem = new MenuItem("Open Pascal Scanning Tool");
            openItem.addActionListener(e -> openBrowser());

            MenuItem aboutItem = new MenuItem("About");
            aboutItem.addActionListener(e -> showAbout());

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                log.info("TrayIcon: Exit requested via tray menu.");
                System.exit(0);
            });

            popup.add(openItem);
            popup.add(aboutItem);
            popup.addSeparator();
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "Pascal Scanning Tool", popup);
            trayIcon.setImageAutoSize(true);

            // Double click opens the UI
            trayIcon.addActionListener(e -> openBrowser());

            tray.add(trayIcon);
            log.info("TrayIcon: Successfully initialized and added to System Tray.");

        } catch (Exception e) {
            log.error("TrayIcon: Failed to initialize tray icon: {}", e.getMessage());
        }
    }

    private Image createIconImage() {
        // We'll look for icon.png in resources.
        // For now, let's try to load it from the classpath.
        URL imageURL = getClass().getResource("/ui/icon.png");
        if (imageURL != null) {
            return (new javax.swing.ImageIcon(imageURL)).getImage();
        }

        // Dynamic fallback: Create a colored square if icon is missing
        log.info("TrayIcon: Icon resource not found, creating fallback image.");
        int width = 16;
        int height = 16;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(width, height,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(new Color(0, 120, 215)); // Professional Blue
        g2.fillRect(0, 0, width, height);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("P", 3, 13);
        g2.dispose();
        return img;
    }

    private void openBrowser() {
        try {
            String url = "http://127.0.0.1:" + port + "/ui";
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            }
        } catch (Exception e) {
            log.warn("TrayIcon: Failed to open browser: {}", e.getMessage());
        }
    }

    private void showAbout() {
        // Optional: Show a simple AWT Dialog or rely on Web UI's about section.
        log.info("TrayIcon: About clicked.");
        openBrowser();
    }
}
