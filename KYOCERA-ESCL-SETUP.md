# Kyocera Scanner Setup Guide - eSCL Network Scanning

## Problem

Kyocera multifunction printers often have **limited or incompatible WIA driver support** for programmatic scanning. The WIA Transfer method may:
- Hang/timeout (15+ seconds)
- Require user interaction
- Fail to return image data

## Solution: Use eSCL Network Scanning

eSCL (AirPrint Scanning) is a standardized HTTP-based protocol that works reliably with Kyocera devices.

---

## Setup Steps

### 1. Find Your Kyocera Scanner's IP Address

**Option A: From the Scanner's Control Panel**
1. Press the **Menu** or **Settings** button on the scanner
2. Navigate to **Network Settings** → **TCP/IP Settings**
3. Note the **IPv4 Address** (e.g., `192.168.1.100`)

**Option B: From Your Computer**
1. Open **Control Panel** → **Devices and Printers**
2. Right-click your Kyocera printer → **Properties**
3. Go to **Ports** tab and note the IP address

**Option C: Print Network Configuration Page**
1. On the scanner, press **Menu** → **Reports** → **Network Configuration**
2. The IP address will be printed

### 2. Verify eSCL is Enabled

Most modern Kyocera scanners have eSCL enabled by default. To verify:

1. Open a web browser
2. Navigate to: `http://<scanner-ip>/eSCL/ScannerCapabilities`
   - Example: `http://192.168.1.100/eSCL/ScannerCapabilities`
3. You should see an XML response with scanner capabilities

**If you get an error:**
- Check the scanner's web interface (http://<scanner-ip>)
- Look for **AirPrint** or **eSCL** settings and enable them
- Consult your Kyocera manual for "AirPrint" or "Mobile Printing" setup

### 3. Configure the Application

Edit `application.properties`:

```properties
# Add your Kyocera scanner's IP address
escl.manual-ips=192.168.1.100

# Optional: If using non-standard port
escl.common.ports=80,443,8080,8443,8181
```

**For multiple scanners:**
```properties
escl.manual-ips=192.168.1.100,192.168.1.101,192.168.1.102
```

### 4. Restart the Application

```bash
# Stop the current instance (Ctrl+C)
# Restart:
mvn spring-boot:run
```

### 5. Test eSCL Scanning

1. Open `http://localhost:17070/ui`
2. Click **Discover Scanners**
3. You should see your Kyocera scanner listed with type "eSCL Network Scanner"
4. Select it and click **Scan**

---

## Troubleshooting

### Scanner Not Discovered

**Check network connectivity:**
```bash
ping 192.168.1.100
```

**Test eSCL endpoint manually:**
```bash
curl http://192.168.1.100/eSCL/ScannerCapabilities
```

**Common issues:**
- Firewall blocking port 80/443
- Scanner on different subnet
- eSCL/AirPrint disabled in scanner settings

### Scan Fails

**Check scanner status:**
- Is the scanner lid closed?
- Is there paper in the feeder (if using ADF)?
- Is the scanner in sleep mode? (Wake it up first)

**Check application logs:**
```properties
# Enable debug logging in application.properties
logging.level.com.lci.scannerdesktop.escl=DEBUG
```

### Slow Discovery

If discovery takes too long, reduce the range:

```properties
# Only probe first 10 IPs in subnet
escl.discovery-range=10

# Or disable auto-discovery and use manual IPs only
scanner.startup-discovery.enabled=false
escl.manual-ips=192.168.1.100
```

---

## eSCL vs WIA Comparison

| Feature | WIA (Windows) | eSCL (Network) |
|---------|---------------|----------------|
| **Kyocera Support** | ❌ Poor/Unreliable | ✅ Excellent |
| **Setup Complexity** | Easy (USB) | Medium (need IP) |
| **Cross-Platform** | ❌ Windows only | ✅ Windows/Mac/Linux |
| **Network Scanning** | ⚠️ Limited | ✅ Native |
| **Reliability** | ⚠️ Driver-dependent | ✅ Standardized |
| **Speed** | Fast (USB) | Good (network) |

---

## Supported Kyocera Models

eSCL is supported on most Kyocera multifunction printers manufactured after 2015, including:

- **TASKalfa series**: 2552ci, 3252ci, 4052ci, 5052ci, 6052ci, 7052ci, 8052ci
- **ECOSYS series**: M2635dw, M2640idw, M3145idn, M3645idn, M3655idn, M3860idn
- **FS series**: FS-C8525MFP, FS-C8650DN
- And many others

Check your scanner's manual or web interface for "AirPrint" or "eSCL" support.

---

## Alternative: TWAIN Driver

If eSCL is not available, consider using Kyocera's TWAIN driver:
- Download from Kyocera support website
- Install TWAIN driver
- Use TWAIN-compatible scanning software
- **Note**: This application does not currently support TWAIN (WIA and eSCL only)

---

## Need Help?

1. **Check scanner manual** for AirPrint/eSCL setup instructions
2. **Contact Kyocera support** for driver updates
3. **Enable debug logging** to see detailed error messages:
   ```properties
   logging.level.com.lci.scannerdesktop=DEBUG
   ```
