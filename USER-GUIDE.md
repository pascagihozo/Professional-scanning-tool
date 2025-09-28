# 🎯 **User Guide: Creating Native Installers**

This guide shows you how to create native installers for Pascal Scanning Tool on your local machine.

## 🚀 **Quick Start**

### **Option 1: From JAR Distribution (Recommended)**

1. **Download** the JAR distribution for your platform from GitHub releases
2. **Extract** the ZIP file to a folder
3. **Run** the packaging script for your platform:

#### **Windows**
```bash
packaging/windows/package.bat
```

#### **macOS**
```bash
chmod +x packaging/macos/package.sh
packaging/macos/package.sh
```

#### **Linux**
```bash
chmod +x packaging/linux/package.sh
packaging/linux/package.sh
```

4. **Find** your installer in the `dist/` folder!

### **Option 2: From Source Code**

1. **Clone** the repository:
```bash
git clone https://github.com/pascagihozo/pascal-scanning-tool.git
cd pascal-scanning-tool
```

2. **Build** the application:
```bash
mvn clean package
```

3. **Create** installer for your platform (same commands as above)

## 📋 **Prerequisites**

### **Required Software**
- **Java Development Kit (JDK) 17 or higher**
- **Maven 3.8 or higher** (if building from source)

### **Platform-Specific Requirements**

#### **Windows**
- Windows 10 or higher
- Administrator privileges (for installer creation)
- JDK 17+ with `jpackage` tool

#### **macOS**
- macOS 10.15 or higher
- Xcode Command Line Tools
- JDK 17+ with `jpackage` tool

#### **Linux**
- Ubuntu 18.04+ or equivalent
- `fakeroot` package (for DEB creation)
- JDK 17+ with `jpackage` tool

## 🔧 **What Each Script Does**

### **Windows Script (`packaging/windows/package.bat`)**
1. ✅ Checks for Java 17+ and Maven
2. ✅ Builds the application (if needed)
3. ✅ Creates Windows .exe installer using `jpackage`
4. ✅ Includes Windows WIA support via JACOB library
5. ✅ Places installer in `dist/Pascal-Scanning-Tool-1.0.0.exe`

### **macOS Script (`packaging/macos/package.sh`)**
1. ✅ Checks for Java 17+ and Maven
2. ✅ Builds the application (if needed)
3. ✅ Creates macOS .pkg installer using `jpackage`
4. ✅ Includes SANE scanner support
5. ✅ Places installer in `dist/Pascal-Scanning-Tool-1.0.0.pkg`

### **Linux Script (`packaging/linux/package.sh`)**
1. ✅ Checks for Java 17+ and Maven
2. ✅ Builds the application (if needed)
3. ✅ Creates Linux .deb installer using `jpackage`
4. ✅ Includes SANE scanner support
5. ✅ Places installer in `dist/Pascal-Scanning-Tool-1.0.0.deb`

## 🎨 **Installer Features**

### **Windows Installer (.exe)**
- **Native Windows integration**
- **Desktop shortcuts** and Start Menu entries
- **Windows WIA support** for scanners
- **Bundled Java runtime** (no Java installation needed)
- **Professional installer** with progress bar

### **macOS Installer (.pkg)**
- **Native macOS integration**
- **Application bundle** structure
- **SANE scanner support** via system libraries
- **Bundled Java runtime**
- **Drag-and-drop** installation

### **Linux Installer (.deb)**
- **Native Linux package** management
- **Desktop integration** (shortcuts, menu entries)
- **SANE scanner support** via system libraries
- **Dependency management** (Java runtime)
- **APT package** manager integration

## 🐛 **Troubleshooting**

### **Common Issues**

#### **"Java 17 or higher is required"**
- **Solution**: Install JDK 17+ and ensure it's in PATH
- **Check**: `java -version` should show version 17 or higher

#### **"Maven is required for building"**
- **Solution**: Install Maven 3.8+ and ensure it's in PATH
- **Check**: `mvn -version` should work

#### **"jpackage failed to create installer"**
- **Windows**: Run Command Prompt as Administrator
- **macOS**: Install Xcode Command Line Tools: `xcode-select --install`
- **Linux**: Install fakeroot: `sudo apt-get install fakeroot`

#### **"Could not find artifact jacob.jar"**
- **Solution**: Ensure you're running from the correct directory
- **Check**: `lib/jacob.jar` should exist in the project root

### **Debug Mode**
Run scripts with verbose output:

#### **Windows**
```bash
packaging/windows/package.bat
# Check the output for error messages
```

#### **macOS/Linux**
```bash
bash -x packaging/macos/package.sh
bash -x packaging/linux/package.sh
```

## 📦 **Installer Output**

### **File Locations**
All installers are created in the `dist/` directory:
- **Windows**: `dist/Pascal-Scanning-Tool-1.0.0.exe`
- **macOS**: `dist/Pascal-Scanning-Tool-1.0.0.pkg`
- **Linux**: `dist/Pascal-Scanning-Tool-1.0.0.deb`

### **File Sizes**
- **Windows**: ~50MB (includes JRE + JACOB)
- **macOS**: ~50MB (includes JRE)
- **Linux**: ~50MB (includes JRE)

### **System Requirements**
- **Windows**: Windows 10+, 512MB RAM
- **macOS**: macOS 10.15+, 512MB RAM
- **Linux**: Ubuntu 18.04+, 512MB RAM

## 🚀 **Advanced Usage**

### **Custom Installer Settings**

You can modify the packaging scripts to customize:
- **Application name** and version
- **Vendor information**
- **Icon and branding**
- **Installation directory**
- **Shortcut creation**

### **Code Signing**

For production distribution, you can add code signing:

#### **Windows**
```bash
# Add to package.bat:
--win-signing-key "path/to/key.p12"
--win-signing-key-pass "password"
```

#### **macOS**
```bash
# Add to package.sh:
--mac-signing-key-user-name "Developer ID"
--mac-signing-keychain "path/to/keychain"
```

## 📚 **Related Documentation**

- [README.md](README.md) - Main project documentation
- [PACKAGING.md](PACKAGING.md) - Detailed packaging guide
- [CONTRIBUTING.md](CONTRIBUTING.md) - Development guidelines
- [API-DOCUMENTATION.md](API-DOCUMENTATION.md) - API reference

---

**Need Help?** 
- 📖 Check this documentation
- 🐛 [Report issues](https://github.com/pascagihozo/pascal-scanning-tool/issues)
- 💬 [Start discussions](https://github.com/pascagihozo/pascal-scanning-tool/discussions)
- 📧 Email: pascagihozo@gmail.com
