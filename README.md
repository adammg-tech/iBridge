# iBridge

iBridge is an advanced Android application that enables direct USB connection, communication, file management, and diagnostics between Android devices and iOS devices (iPhones), operating entirely on-device without requiring a PC or root access.

---

## Features

### 1. Device Connection and Management
- **Direct USB iOS Connectivity**: Connect an iPhone directly to an Android phone or tablet via USB.
- **USB Multiplexer Tunnel**: Establishes a background native usbmuxd communication daemon using Android USB Host API file descriptors.
- **Device Dashboard (Overview)**: Real-time inspection of iOS device details including UDID, iOS version, model, serial number, battery level, storage capacity, and trust/pairing state.

### 2. File and Application Management
- **File Browser (AFC)**: Full file system access via Apple File Conduit (afcclient) to browse, upload, download, and manage files and application sandboxes on iOS.
- **Application Manager**: List installed iOS applications, view package identifiers and versions, and manage app installation and removal.
- **Photos and Media Explorer**: Browse, view, and export photos, videos, and media library assets from the connected iOS device.

### 3. Backup, Recovery, and Security
- **Backup and Restore**: Create, manage, and restore full or incremental device backups directly onto Android storage using idevicebackup2.
- **Device Recovery**: Put the connected iOS device into recovery mode, exit recovery, or reboot.
- **Diagnostics and System Logs**: Stream real-time iOS system logs (idevicesyslog) and execute diagnostic queries.
- **Lockdown and Security Management**: Inspect and manage pairing records, cryptographic certificates, and trust states (/var/lib/lockdown).

### 4. Developer Tools
- **Live Terminal Console**: Built-in interactive terminal and command runner executing native libimobiledevice binaries inside the Android application sandbox.

---

## How It Works

iBridge bridges Android USB Host APIs and iOS device protocols entirely on-device:

1. **USB File Descriptor Bridging**:
   - Detects USB connection via Android UsbManager and requests permission.
   - Opens a raw USB device connection and extracts the native file descriptor (fd).
2. **Native usbmuxd Daemon**:
   - Runs a compiled arm64-v8a native C usbmuxd daemon in a foreground service.
   - Configures process environment variables (`TERMUX_USB_FD`, `LIBUSB_FD`) pointing to the Android USB file descriptor.
3. **Bundled Binaries and Path Prefix Patching**:
   - Bundles compiled arm64-v8a binaries (`usbmuxd`, `afcclient`, `idevice_id`, `ideviceinfo`, `idevicebackup2`, `ideviceinstaller`, `idevicesyslog`, `ideviceenterrecovery`, etc.) and shared libraries (`libimobiledevice`, `libusbmuxd`, `libplist`, `libusb`, `libssl`, `libcrypto`).
   - Automatically prepares the working environment, patches binary path prefixes (rewriting `/data/data/com.termux/files/usr` to `/data/data/com.adammg.ibridge/f`), creates dynamic library aliases, and executes them via the system dynamic linker (`/system/bin/linker64`).
4. **Reactive MVVM Architecture**:
   - Built with Jetpack Compose (Material 3), Kotlin Coroutines, and StateFlow reactive repositories.

---

## Project Structure

```
com.adammg.ibridge/
├── core/
│   ├── binary/     # BinaryManager (extraction, patching, environment setup)
│   ├── engine/     # IBridgeEngine (ProcessBuilder command runner & stream parser)
│   ├── native/     # JNI & Native wrappers
│   └── usb/        # UsbFdBridge & USB connection state machine
├── data/
│   ├── mapper/     # DTO / Info mappers (ideviceinfo parser)
│   ├── model/      # Data models (DeviceInfo, AppInfo, BackupInfo, etc.)
│   └── repository/ # Domain repositories (Device, File, App, Backup, Photo, Log, Recovery)
├── service/
│   └── UsbmuxdService # Foreground service managing the native usbmuxd daemon
└── ui/
    ├── components/ # LiveTerminalConsole, TopDeviceBanner, etc.
    ├── screens/    # Overview, FileBrowser, Apps, Backup, Photos, Recovery, Security, DevTools, ToolsHub, Logs
    └── theme/      # Material 3 Theme, Color, Type
```

---

## Tech Stack

- **UI Framework**: Jetpack Compose, Material 3, Edge-to-Edge
- **Architecture**: MVVM, Kotlin Coroutines, StateFlow
- **Native / Low-Level**: C, CMake, ProcessBuilder, Android USB Host API (`UsbManager`), `libimobiledevice`, `usbmuxd`, `libplist`, `libusb`, OpenSSL (`libssl`, `libcrypto`)
- **Build System**: Android Gradle Plugin (AGP), Gradle Kotlin DSL (`build.gradle.kts`)

---

## Compatibility and Stability

- **Tested Hardware**: This project has been tested exclusively using a Samsung Galaxy A15 and an iPhone X. Compatibility with other device models, Android versions, or iOS versions is not guaranteed.
- **Project Status**: iBridge is experimental, under active development, and may occasionally throw errors, fail to connect, or exhibit unstable behavior.

---

## Acknowledgments

This project was built and developed with the assistance of AI. 

---

## License

Original project materials are copyrighted © 2026 adammg-tech. All rights reserved.
