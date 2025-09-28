#!/bin/bash
# Pascal Scanning Client - macOS Installer Build Script
# Author: Pascal Gihozo
# Description: Creates macOS PKG installer using jpackage

echo "========================================"
echo "  Pascal Scanning Client - macOS"
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
echo "Creating macOS installer..."
echo

# Create installer using jpackage
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
    --type pkg \
    --dest packaging/macos \
    --icon packaging/macos/pascal.icns \
    --mac-package-identifier com.pascagihozo.scanner \
    --mac-package-name "Pascal Scanning Client" \
    --mac-package-signing-prefix "Pascal Scanning Client" \
    --mac-sign

if [ $? -ne 0 ]; then
    echo "ERROR: jpackage failed to create installer"
    exit 1
fi

echo
echo "========================================"
echo "  Build completed successfully!"
echo "========================================"
echo
echo "Installer created: packaging/macos/Pascal Scanning Client-1.0.0.pkg"
echo
echo "You can now distribute this installer to users."
echo