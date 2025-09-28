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

echo "Creating Linux installer..."
echo

# Create dist directory if it doesn't exist
mkdir -p dist

# Create DEB installer
jpackage \
    --input target \
    --main-jar scanner-desktop-client.jar \
    --main-class com.lci.scannerdesktop.ScannerDesktopApplication \
    --name "Pascal Scanning Tool" \
    --app-version 1.0.0 \
    --vendor "Pascal Gihozo" \
    --description "Professional Document Scanning Solution" \
    --copyright "Copyright (c) 2025 Pascal Gihozo" \
    --license-file ../LICENSE \
    --type deb \
    --dest dist \
    --linux-package-name "pascal-scanning-tool" \
    --linux-package-deps "default-jre" \
    --linux-menu-group "Office" \
    --linux-shortcut

if [ $? -ne 0 ]; then
    echo "ERROR: jpackage failed to create DEB installer"
    exit 1
fi

echo
echo "========================================"
echo "  Build completed successfully!"
echo "========================================"
echo
echo "Installer created: dist/pascal-scanning-tool_1.0.0-1_amd64.deb"
echo
echo "You can now distribute these installers to users."
echo