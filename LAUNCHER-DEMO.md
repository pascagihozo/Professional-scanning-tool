# Pascal Scanning Agent - JVM Launch Issue Fix

## 🚨 **Problem Solved!**

**Issue**: "Failed to launch JVM" when running `Pascal Scanning Agent.exe`

**Root Cause**: The jpackage-created executable has issues with the bundled JVM runtime on some systems.

**Solution**: Use the provided **batch file launchers** that run the JAR directly with your system Java.

---

## 🔧 **NEW: Launcher Files Included**

### **Option 1: Launch-Pascal-Agent.bat** (Recommended)
- ✅ Shows console output for debugging
- ✅ Clear startup messages and status
- ✅ Easy to stop with Ctrl+C
- ✅ Best for development/testing

### **Option 2: Launch-Pascal-Agent-Silent.bat**
- ✅ Runs in background (minimized)
- ✅ Automatically opens Swagger UI in browser
- ✅ Best for regular daily use

---

## 📋 **How to Use**

### **Method 1: With Console (Recommended)**
1. Extract the ZIP file
2. Double-click **`Launch-Pascal-Agent.bat`**
3. Wait for "Started ScannerDesktopApplication" message
4. Open browser: `http://localhost:17070/swagger-ui/index.html`

### **Method 2: Silent Background**
1. Double-click **`Launch-Pascal-Agent-Silent.bat`**
2. Service starts in background
3. Swagger UI opens automatically

---

## 🌐 **Access Your APIs**

Once started, you can access:

- **📖 Swagger UI**: `http://localhost:17070/swagger-ui/index.html`
- **🔧 Web Scanner UI**: `http://localhost:17070/ui`
- **❤️ Health Check**: `http://localhost:17070/v1/health`
- **📄 OpenAPI Spec**: `http://localhost:17070/v3/api-docs`

---

## 💡 **Why This Works Better**

✅ **Uses your system Java** (more reliable than bundled runtime)
✅ **Avoids jpackage JVM issues** (common on some Windows systems)
✅ **Same working command** you discovered
✅ **Direct JAR execution** (most reliable method)
✅ **Proper Java options** for compatibility

---

## 🛠️ **Requirements**

- **Java 17+** must be installed
- If not installed, get it from: https://adoptium.net/
- The launcher will check and tell you if Java is missing

---

## ⚡ **Stopping the Service**

- **Console version**: Press `Ctrl+C` in the console window
- **Silent version**: End the `java.exe` process in Task Manager

---

## 📦 **File**: `Pascal-Scanning-Agent-v0.1.1-LAUNCHER-FIX.zip`

This approach is actually **more reliable** than the packaged EXE and gives you full control over the Java runtime!

---

**Date**: 2025-09-25
**Version**: 0.1.1-launcher-fix