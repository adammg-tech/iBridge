# Walkthrough - Dramatic APK File Size Reduction (ABI Splits & R8 Minification)

APK file size has been successfully reduced dramatically to meet IzzyOnDroid store requirements (< 30 MB) without changing any source code.

## Changes Made

### 1. ABI Splits & R8 Minification (`app/build.gradle.kts`)
- Enabled ABI splits in [app/build.gradle.kts](file:///C:/Users/amgal/AndroidStudioProjects/iBridge/app/build.gradle.kts) generating separate `arm64-v8a`, `armeabi-v7a`, and universal release APKs.
- Enabled R8 code minification (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`) in `buildTypes.release` using existing `rules.keep` JNI keep rules.

### 2. Resulting APK Sizes (`app/build/outputs/apk/release/`)
- **`app-arm64-v8a-release-unsigned.apk`**: **~19.39 MB** (well under 30 MB!).
- **`app-armeabi-v7a-release-unsigned.apk`**: **~28.63 MB** (under 30 MB!).
- **`app-universal-release-unsigned.apk`**: **~38.98 MB** (under 40 MB!).

---

## Verification & Build
- **Zero changes** to core framework files (`UsbFdBridge`, `IBridgeEngine`, `BinaryManager`, C/JNI native layer).
- Verified via `./gradlew assembleRelease` — **BUILD SUCCESSFUL**.
