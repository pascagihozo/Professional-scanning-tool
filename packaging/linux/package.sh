#!/bin/bash
# Pascal Scanning Client - Linux Installer Build Script
# Author: Pascal Gihozo
# Description: Creates Linux DEB/RPM installers using jpackage

echo "========================================"
echo "  Pascal Scanning Client - Linux"
echo "  Professional Document Scanning Solution"
echo "========================================"
echo

# Check if Java 17+ is available
if ! java -version 2>&1 | grep -E "version.*1[7-9]|version.*2[0-9]" > /dev/null; then
    echo "ERROR: Java 17 or higher is required for jpackage"
    echo "Please install JDK 17+ and try again"
    exit 1
fi

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is required for building"
    echo "Please install Maven and try again"
    exit 1
fi

echo "Building Pascal Scanning Client..."
echo

# Build the project
mvn -f scanner-desktop-client/pom.xml clean package
if [ $? -ne 0 ]; then
    echo "ERROR: Maven build failed"
    exit 1
fi

echo
echo "Creating Linux installers..."
echo

# Create DEB installer
cd scanner-desktop-client
jpackage \
    --input target \
    --main-jar scanner-desktop-client.jar \
    --main-class com.lci.scannerdesktop.ScannerDesktopApplication \
    --name "Pascal Scanning Client" \
    --app-version 1.0.0 \
    --vendor "Pascal Gihozo" \
    --description "Professional Document Scanning Solution" \
    --copyright "Copyright (c) 2025 Pascal Gihozo" \
    --license-file ../LICENSE \
    --type deb \
    --dest packaging/linux \
    --icon packaging/linux/pascal.png \
    --linux-package-name "pascal-scanning-client" \
    --linux-package-deps "default-jre" \
    --linux-menu-group "Office" \
    --linux-shortcut

if [ $? -ne 0 ]; then
    echo "ERROR: jpackage failed to create DEB installer"
    exit 1
fi

# Create RPM installer
jpackage \
    --input target \
    --main-jar scanner-desktop-client.jar \
    --main-class com.lci.scannerdesktop.ScannerDesktopApplication \
    --name "Pascal Scanning Client" \
    --app-version 1.0.0 \
    --vendor "Pascal Gihozo" \
    --description "Professional Document Scanning Solution" \
    --copyright "Copyright (c) 2025 Pascal Gihozo" \
    --license-file ../LICENSE \
    --type rpm \
    --dest packaging/linux \
    --icon packaging/linux/pascal.png \
    --linux-package-name "pascal-scanning-client" \
    --linux-package-deps "java-17-openjdk" \
    --linux-menu-group "Office" \
    --linux-shortcut

if [ $? -ne 0 ]; then
    echo "ERROR: jpackage failed to create RPM installer"
    exit 1
fi

echo
echo "========================================"
echo "  Build completed successfully!"
echo "========================================"
echo
echo "Installers created:"
echo "  - packaging/linux/pascal-scanning-client_1.0.0-1_amd64.deb"
echo "  - packaging/linux/pascal-scanning-client-1.0.0-1.x86_64.rpm"
echo
echo "You can now distribute these installers to users."
echo