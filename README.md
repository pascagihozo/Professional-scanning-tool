# Pascal Scanning Tool

<div align="center">

![Pascal Scanning Tool](https://img.shields.io/badge/Pascal-Scanning%20Tool-blue?style=for-the-badge&logo=scanner)
![Version](https://img.shields.io/badge/version-1.0.0-green?style=for-the-badge)
![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey?style=for-the-badge)
![License](https://img.shields.io/badge/license-MIT-blue?style=for-the-badge)

**Professional Cross-Platform Document Scanning Solution**

*Enterprise-ready desktop application with modern web interface*

[![GitHub stars](https://img.shields.io/github/stars/pascagihozo/pascal-scanning-tool?style=social)](https://github.com/pascagihozo/pascal-scanning-tool)
[![GitHub forks](https://img.shields.io/github/forks/pascagihozo/pascal-scanning-tool?style=social)](https://github.com/pascagihozo/pascal-scanning-tool)

</div>

---

## 🌟 **Overview**

Pascal Scanning Tool is a modern, enterprise-ready desktop application that provides seamless document scanning capabilities through a beautiful web interface. Built with Spring Boot and designed for both individual users and enterprise environments.

### **Key Features**

- 🔍 **Multi-Backend Support**: Windows WIA, Linux SANE, Network eSCL
- 🌐 **Modern Web UI**: Clean, responsive interface accessible via browser
- ⚡ **Automatic Discovery**: Scanners discovered automatically on startup
- 🔄 **Real-time Caching**: Optimized performance with intelligent caching
- 📱 **Cross-Platform**: Windows, macOS, and Linux support
- 🎨 **Professional Design**: Modern UI with Pascal Scanning Tool branding
- 🔧 **Easy Integration**: RESTful API for seamless integration
- 🏢 **Enterprise Ready**: Robust architecture for business environments

---

## 🚀 **Quick Start**

### **Download & Install**

| Platform | Download | Size |
|----------|----------|------|
| **Windows** | [pascal-scanning-tool-1.0.0.exe](releases/windows/pascal-scanning-tool-1.0.0.exe) | ~50MB |
| **macOS** | [pascal-scanning-tool-1.0.0.pkg](releases/macos/pascal-scanning-tool-1.0.0.pkg) | ~50MB |
| **Linux** | [pascal-scanning-tool-1.0.0.deb](releases/linux/pascal-scanning-tool-1.0.0.deb) | ~50MB |

### **System Requirements**

- **Java**: JDK 17 or higher
- **Memory**: 512MB RAM minimum, 1GB recommended
- **Storage**: 100MB free space
- **Network**: For network scanner discovery

### **First Launch**

1. **Install** the appropriate package for your platform
2. **Launch** Pascal Scanning Tool
3. **Open** your browser and navigate to: `http://127.0.0.1:17070/ui`
4. **Start Scanning** - your scanners will be automatically discovered!

---

## 🏗️ **Architecture**

### **Technology Stack**

- **Backend**: Spring Boot 2.5.6
- **Frontend**: HTML5, CSS3, JavaScript (ES6+)
- **Scanner Backends**: 
  - Windows WIA (via JACOB COM Bridge)
  - Linux SANE (Scanner Access Now Easy)
  - Network eSCL (AirPrint Scanning)
- **Build Tool**: Maven 3.8+
- **Packaging**: jpackage (native installers)

### **Project Structure**

```
pascal-scanning-tool/
├── src/main/java/com/lci/scannerdesktop/
│   ├── api/                    # REST API controllers
│   ├── boot/                   # Application startup
│   ├── config/                 # Configuration classes
│   ├── escl/                   # eSCL network scanner support
│   ├── sane/                   # Linux SANE scanner support
│   ├── service/                # Business logic services
│   ├── wia/                    # Windows WIA scanner support
│   └── ScannerDesktopApplication.java
├── src/main/resources/
│   ├── application.properties  # Application configuration
│   └── ui/                     # Web interface files
├── lib/                        # Native libraries (JACOB)
├── packaging/                  # Cross-platform installer scripts
└── runtime/                    # JRE runtime files
```

---

## 📖 **User Guide**

### **Web Interface**

The Pascal Scanning Tool provides a modern web interface accessible at `http://127.0.0.1:17070/ui`:

#### **Scanner Discovery**
- **Automatic**: Scanners are discovered automatically on startup
- **Manual Refresh**: Click "Refresh Scanners" to discover new devices
- **Network Support**: Supports network scanners via eSCL protocol

#### **Scanning Options**
- **DPI Settings**: 75, 150, 300, 600 DPI
- **Color Modes**: Color, Grayscale, Black & White
- **Output Formats**: PDF, JPEG, PNG, TIFF
- **Paper Sources**: Platen, ADF (Automatic Document Feeder)
- **Duplex Scanning**: Double-sided document support

---

## 🔧 **Configuration**

### **Application Properties**

Configure Pascal Scanning Tool via `application.properties`:

```properties
# Server Configuration
server.port=17070
server.address=127.0.0.1

# Scanner Discovery Settings
scanner.startup-discovery.enabled=true
scanner.startup-discovery.delay-seconds=2
scanner.cache.ttl-minutes=2

# eSCL Network Scanner Settings
escl.common.ports=80,443,8080,8443,8181
escl.manual-ips=127.0.0.1,10.198.195.242

# Windows WIA Backend
wia.enabled=true

# CORS Configuration
api.cors.allowed-origins=*
api.max-result-size-bytes=52428800
```

---

## 🛠️ **Development**

### **Prerequisites**

- **Java**: JDK 17 or higher
- **Maven**: 3.8 or higher
- **Git**: For version control
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code

### **Building from Source**

```bash
# Clone the repository
git clone https://github.com/pascagihozo/pascal-scanning-tool.git
cd pascal-scanning-tool

# Build the project
mvn clean package

# Run the application
mvn spring-boot:run
```

### **Development Setup**

1. **Fork** the repository on GitHub
2. **Clone** your fork locally
3. **Create** a feature branch: `git checkout -b feature/amazing-feature`
4. **Make** your changes
5. **Commit** with clear messages: `git commit -m "Add amazing feature"`
6. **Push** to your fork: `git push origin feature/amazing-feature`
7. **Create** a Pull Request

---

## 🤝 **Contributing**

We welcome contributions to Pascal Scanning Tool! Here's how you can help:

### **Ways to Contribute**

- 🐛 **Report Bugs**: Use GitHub Issues to report bugs
- 💡 **Suggest Features**: Propose new features via Issues
- 🔧 **Submit Pull Requests**: Contribute code improvements
- 📖 **Improve Documentation**: Help improve this README
- 🌍 **Add Translations**: Help translate the interface
- 🧪 **Write Tests**: Improve test coverage

### **Development Guidelines**

- Follow the existing code style
- Write clear commit messages
- Add tests for new features
- Update documentation as needed
- Ensure cross-platform compatibility

---

## 📞 **Support**

### **Getting Help**

- 📖 **Documentation**: Check this README and inline help
- 🐛 **Bug Reports**: [GitHub Issues](https://github.com/pascagihozo/pascal-scanning-tool/issues)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/pascagihozo/pascal-scanning-tool/discussions)
- 📧 **Email**: pascagihozo@gmail.com

### **Community**

- ⭐ **Star** the repository if you find it useful
- 🍴 **Fork** the project to contribute
- 👥 **Follow** [@pascagihozo](https://github.com/pascagihozo) for updates
- 🔗 **Connect** on [LinkedIn](https://www.linkedin.com/in/pascal-gihozo-a37061178/)

---

## 📄 **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🏆 **Acknowledgments**

- **Spring Boot** team for the excellent framework
- **SANE** project for Linux scanner support
- **JACOB** project for Windows COM bridge
- **eSCL** protocol contributors
- **Open source community** for inspiration and support

---

<div align="center">

**Made with ❤️ by [Pascal Gihozo](https://github.com/pascagihozo)**

[![GitHub](https://img.shields.io/badge/GitHub-pascagihozo-black?style=flat&logo=github)](https://github.com/pascagihozo)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Pascal%20Gihozo-blue?style=flat&logo=linkedin)](https://www.linkedin.com/in/pascal-gihozo-a37061178/)
[![Email](https://img.shields.io/badge/Email-pascagihozo@gmail.com-red?style=flat&logo=gmail)](mailto:pascagihozo@gmail.com)

</div>