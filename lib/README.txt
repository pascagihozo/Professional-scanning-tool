Place Windows JACOB artifacts here for WIA scanning (Windows only):

Required files (version 1.21 recommended):
 - jacob.jar
 - jacob-1.21-x64.dll  (64-bit Windows)
 - jacob-1.21-x86.dll  (32-bit Windows)

Notes:
 - The desktop client will attempt to load the correct DLL from this folder at startup when wia.enabled=true.
 - Ensure you run a 64-bit Java runtime with the x64 DLL.
 - If packaging with jpackage, include this folder next to the executable.

