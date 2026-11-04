# Implementation Plan - ARM ABI Filtering & FFmpeg Command Formatting Fix

Address the user's finding regarding stripped `x86`/`x86_64` native binaries in `ffmpeg-kit-audio-2.2.3.aar`:
1. **ARM ABI Filtering (`build.gradle.kts`)**: Specify `ndk { abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a")) }` in `defaultConfig` so Android builds target ARM devices (matching the stripped AAR's `arm64-v8a` / `armeabi-v7a` native libraries) and prevents `UnsatisfiedLinkError` on x86 emulators.
2. **FFmpeg Command String Formatting (`RingtoneRepository.kt`)**: Remove embedded double quotes (`\"`) from file paths and add non-interactive `-y` flag to prevent command parsing and stdin prompt failures.

> [!IMPORTANT]
> **Core Framework Protection**: All core framework files (`UsbFdBridge.kt`, `IBridgeEngine.kt`, `BinaryManager.kt`, and C/JNI native code) remain **100% untouched**.

---

## Detailed Plan

### 1. ARM ABI Target Filtering (`app/build.gradle.kts`)
- Add `ndk { abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a")) }` inside `defaultConfig`.

### 2. Command String Formatting (`RingtoneRepository.kt`)
- Format FFmpeg command string without embedded quotes:
  ```kotlin
  "-y -i ${inputTemp.absolutePath} -ss $startTimeSec -t $duration -c:a aac -b:a 192k ${outputFile.absolutePath}"
  ```

---

## Verification Plan

### Manual Verification
1. Build APK $\rightarrow$ Verify native JNI packaging targets ARM architectures (`arm64-v8a`, `armeabi-v7a`).
2. Test conversion on physical ARM device $\rightarrow$ Verify FFmpeg converts MP3/WAV to `.m4r` and transfers to iPhone without command parsing errors.
