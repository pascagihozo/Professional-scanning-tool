# Changelog

All notable changes to Pascal Scanning Tool will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial release preparation
- Cross-platform installer support
- Comprehensive documentation

### Changed
- Repository structure optimization
- Code organization improvements

## [1.0.0] - 2025-01-28

### Added
- **Core Application**
  - Spring Boot 2.5.6 based desktop application
  - Modern web interface accessible via browser
  - RESTful API for scanner operations
  - Automatic scanner discovery on startup

- **Scanner Backend Support**
  - Windows WIA (Windows Image Acquisition) via JACOB COM Bridge
  - Linux SANE (Scanner Access Now Easy) support
  - Network eSCL (AirPrint Scanning) protocol support
  - Multi-backend architecture with automatic fallback

- **Web Interface**
  - Responsive HTML5/CSS3/JavaScript frontend
  - Real-time scanner status updates
  - Intuitive scanning options configuration
  - Professional Pascal Scanning Tool branding

- **Scanning Features**
  - Multiple DPI settings (75, 150, 300, 600)
  - Color mode support (Color, Grayscale, Black & White)
  - Multiple output formats (PDF, JPEG, PNG, TIFF)
  - Paper source selection (Platen, ADF)
  - Duplex scanning support
  - Batch scanning capabilities

- **Configuration**
  - Comprehensive application.properties configuration
  - CORS support for web interface
  - Network scanner discovery settings
  - Cache management for performance optimization

- **Cross-Platform Support**
  - Windows installer (.exe) with native packaging
  - macOS installer (.pkg) with native packaging
  - Linux installer (.deb) with native packaging
  - Platform-specific runtime optimization

- **Developer Experience**
  - Maven-based build system
  - Comprehensive documentation
  - Contributing guidelines
  - Issue templates and PR templates
  - GitHub Actions CI/CD pipeline

- **Enterprise Features**
  - Robust error handling and logging
  - Security considerations for network access
  - Scalable architecture for business environments
  - Professional documentation and support

### Technical Details
- **Java Version**: JDK 17+
- **Framework**: Spring Boot 2.5.6
- **Build Tool**: Maven 3.8+
- **Packaging**: jpackage for native installers
- **Scanner Libraries**: JACOB (Windows), SANE (Linux), eSCL (Network)
- **Frontend**: Vanilla JavaScript with modern ES6+ features

### Security
- Local-only network binding (127.0.0.1)
- CORS configuration for web interface
- Input validation for scanner operations
- Secure file handling for scanned documents

### Performance
- Intelligent scanner caching with TTL
- Optimized image processing
- Memory-efficient document handling
- Fast startup and discovery times

## [0.1.0] - 2025-01-25

### Added
- Initial development version
- Basic scanner functionality
- Windows WIA support
- Simple web interface

### Changed
- Project structure reorganization
- Code cleanup and optimization

### Fixed
- Scanner detection issues
- Memory leaks in image processing
- Cross-platform compatibility problems

---

## Release Notes

### Version 1.0.0
This is the first stable release of Pascal Scanning Tool, providing a complete cross-platform document scanning solution with modern web interface and enterprise-ready architecture.

**Key Highlights:**
- Complete cross-platform support (Windows, macOS, Linux)
- Professional web interface with modern design
- Multi-backend scanner support (WIA, SANE, eSCL)
- Native installer packages for easy deployment
- Comprehensive documentation and developer resources

**Migration from 0.1.0:**
- Update configuration files to new format
- Reinstall using new native installers
- Review new API endpoints for integration

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on contributing to this project.

## Support

For support and questions:
- GitHub Issues: [Report bugs or request features](https://github.com/pascagihozo/pascal-scanning-tool/issues)
- GitHub Discussions: [General questions and discussions](https://github.com/pascagihozo/pascal-scanning-tool/discussions)
- Email: pascagihozo@gmail.com

---

**Made with ❤️ by [Pascal Gihozo](https://github.com/pascagihozo)**