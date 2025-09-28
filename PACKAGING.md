# Packaging Guide - Pascal Scanning Tool

This guide explains how to create native installers for Pascal Scanning Tool on all supported platforms.

## 📋 **Prerequisites**

### **Required Software**
- **Java Development Kit (JDK) 17 or higher**
- **Maven 3.8 or higher**
- **Git** (for cloning the repository)

### **Platform-Specific Requirements**

#### **Windows**
- Windows 10 or higher
- JDK 17+ with `jpackage` tool
- Administrator privileges (for installer creation)

#### **macOS**
- macOS 10.15 or higher
- JDK 17+ with `jpackage` tool
- Xcode Command Line Tools (for native packaging)

#### **Linux**
- Ubuntu 18.04+ or equivalent
- JDK 17+ with `jpackage` tool
- `fakeroot` package (for DEB creation)

## 🚀 **Quick Start**

### **1. Clone and Build**
```bash
git clone https://github.com/pascagihozo/pascal-scanning-tool.git
cd pascal-scanning-tool
mvn clean package
```

### **2. Create Installers**

#### **Windows (.exe)**
```bash
packaging/windows/package.bat
```

#### **macOS (.pkg)**
```bash
chmod +x packaging/macos/package.sh
packaging/macos/package.sh
```

#### **Linux (.deb)**
```bash
chmod +x packaging/linux/package.sh
packaging/linux/package.sh
```

### **3. Find Your Installers**
All installers are created in the `dist/` directory:
- `dist/Pascal-Scanning-Tool-1.0.0.exe` (Windows)
- `dist/Pascal-Scanning-Tool-1.0.0.pkg` (macOS)
- `dist/Pascal-Scanning-Tool-1.0.0.deb` (Linux)

## 🔧 **Detailed Instructions**

### **Windows Installer Creation**

The Windows installer is created using `jpackage` with the following features:
- **Native .exe installer** with Windows integration
- **Desktop shortcuts** and Start Menu entries
- **Automatic Java runtime** bundling
- **Windows WIA support** via JACOB library

#### **Manual Command**
```bash
jpackage \
    --input target \
    --main-jar scanner-desktop-client.jar \
    --main-class com.lci.scannerdesktop.ScannerDesktopApplication \
    --name "Pascal Scanning Tool" \
    --app-version 1.0.0 \
    --vendor "Pascal Gihozo" \
    --description "Professional Document Scanning Solution" \
    --copyright "Copyright (c) 2025 Pascal Gihozo" \
    --license-file LICENSE \
    --type exe \
    --dest dist \
    --icon packaging/windows/pascal.ico \
    --win-console \
    --win-shortcut \
    --win-menu \
    --win-menu-group "Pascal Scanning Tool" \
    --win-dir-chooser \
    --win-per-user-install
```

### **macOS Installer Creation**

The macOS installer creates a native .pkg package with:
- **Native macOS integration**
- **Application bundle** structure
- **Code signing** support (when configured)
- **SANE scanner support** via system libraries

#### **Manual Command**
```bash
jpackage \
    --input target \
    --main-jar scanner-desktop-client.jar \
    --main-class com.lci.scannerdesktop.ScannerDesktopApplication \
    --name "Pascal Scanning Tool" \
    --app-version 1.0.0 \
    --vendor "Pascal Gihozo" \
    --description "Professional Document Scanning Solution" \
    --copyright "Copyright (c) 2025 Pascal Gihozo" \
    --license-file LICENSE \
    --type pkg \
    --dest dist \
    --mac-package-identifier com.pascagihozo.scanner \
    --mac-package-name "Pascal Scanning Tool"
```

### **Linux Installer Creation**

The Linux installer creates a DEB package with:
- **Native Linux package** management
- **Desktop integration** (shortcuts, menu entries)
- **Dependency management** (Java runtime)
- **SANE scanner support** via system libraries

#### **Manual Command**
```bash
jpackage \
    --input target \
    --main-jar scanner-desktop-client.jar \
    --main-class com.lci.scannerdesktop.ScannerDesktopApplication \
    --name "Pascal Scanning Tool" \
    --app-version 1.0.0 \
    --vendor "Pascal Gihozo" \
    --description "Professional Document Scanning Solution" \
    --copyright "Copyright (c) 2025 Pascal Gihozo" \
    --license-file LICENSE \
    --type deb \
    --dest dist \
    --linux-package-name "pascal-scanning-tool" \
    --linux-package-deps "default-jre" \
    --linux-menu-group "Office" \
    --linux-shortcut
```

## 🏗️ **Automated Packaging Scripts**

### **Script Features**
Each packaging script (`packaging/*/package.*`) includes:

1. **Prerequisites Check**
   - Verifies Java 17+ is installed
   - Checks for Maven availability
   - Validates required tools

2. **Application Build**
   - Runs `mvn clean package`
   - Handles build errors gracefully
   - Creates executable JAR

3. **Installer Creation**
   - Uses `jpackage` with optimized settings
   - Creates platform-specific installer
   - Places output in `dist/` directory

4. **Error Handling**
   - Clear error messages
   - Exit codes for automation
   - Helpful troubleshooting hints

### **Script Locations**
- **Windows**: `packaging/windows/package.bat`
- **macOS**: `packaging/macos/package.sh`
- **Linux**: `packaging/linux/package.sh`

## 🔄 **CI/CD Integration**

### **GitHub Actions**
The project includes automated CI/CD that:
- ✅ **Builds** the application on all platforms
- ✅ **Creates** native installers automatically
- ✅ **Uploads** artifacts for download
- ✅ **Attaches** installers to releases

### **Workflow Triggers**
- **Push to main**: Build and test
- **Pull requests**: Verify builds
- **Release tags**: Create installers and attach to releases
- **Manual dispatch**: On-demand builds

## 🐛 **Troubleshooting**

### **Common Issues**

#### **"Java 17 or higher is required"**
- **Solution**: Install JDK 17+ and ensure it's in PATH
- **Check**: `java -version` should show version 17 or higher

#### **"Maven is required for building"**
- **Solution**: Install Maven 3.8+ and ensure it's in PATH
- **Check**: `mvn -version` should work

#### **"jpackage failed to create installer"**
- **Windows**: Run as Administrator
- **macOS**: Install Xcode Command Line Tools
- **Linux**: Install `fakeroot` package

#### **"Could not find artifact jacob.jar"**
- **Solution**: Ensure `lib/jacob.jar` exists in the repository
- **Check**: `git ls-files lib/` should include `jacob.jar`

### **Debug Mode**
Run packaging scripts with verbose output:
```bash
# Windows
packaging/windows/package.bat

# macOS/Linux
bash -x packaging/macos/package.sh
bash -x packaging/linux/package.sh
```

## 📦 **Distribution**

### **Installer Features**
- **Self-contained**: Includes Java runtime
- **Native integration**: Platform-specific features
- **Easy installation**: Standard installer experience
- **Automatic updates**: Ready for update mechanisms

### **File Sizes**
- **Windows**: ~50MB (includes JRE + JACOB)
- **macOS**: ~50MB (includes JRE)
- **Linux**: ~50MB (includes JRE)

### **System Requirements**
- **Windows**: Windows 10+, 512MB RAM
- **macOS**: macOS 10.15+, 512MB RAM
- **Linux**: Ubuntu 18.04+, 512MB RAM

## 🔗 **Related Documentation**

- [README.md](README.md) - Main project documentation
- [CONTRIBUTING.md](CONTRIBUTING.md) - Development guidelines
- [API-DOCUMENTATION.md](API-DOCUMENTATION.md) - API reference
- [CHANGELOG.md](CHANGELOG.md) - Version history

---

**Need Help?** 
- 📖 Check this documentation
- 🐛 [Report issues](https://github.com/pascagihozo/pascal-scanning-tool/issues)
- 💬 [Start discussions](https://github.com/pascagihozo/pascal-scanning-tool/discussions)
- 📧 Email: pascagihozo@gmail.com