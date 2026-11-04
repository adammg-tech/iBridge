# Implementation Plan - 0.1s Precision Increments & Strict 30s Trim Validation in AudioTrimScreen

Implement two UX enhancements in `AudioTrimScreen.kt`:
1. **0.1s Precision Increments**: Change the `+` and `-` time adjustment step from 0.5 seconds to **0.1 seconds** for precise sub-second tuning.
2. **Strict <= 30s Validation on Trimming Screen**: Validate that the selected trim range (`endTimeSec - startTimeSec`) is **30 seconds or less** when the user taps "Save Trim & Return". If it exceeds 30s, display a Toast warning and prevent returning until successfully trimmed to <= 30s.

> [!IMPORTANT]
> **Core Framework Protection**: All core framework files (`UsbFdBridge.kt`, `IBridgeEngine.kt`, `BinaryManager.kt`, and C/JNI native code) remain **100% untouched**.

---

## Proposed Changes

### Audio Trimmer Refinements (`AudioTrimScreen.kt`)
- Update `+` / `-` click listeners to step by `0.1f` seconds.
- Update "Save Trim & Return" click listener:
  ```kotlin
  val duration = endTimeSec - startTimeSec
  if (duration > 30.5f) {
      Toast.makeText(context, "Ringtone duration must be 30 seconds or less!", Toast.LENGTH_LONG).show()
      return@Button
  }
  onConfirmTrim(startTimeSec, endTimeSec, ringtoneName)
  ```

---

## Verification Plan

### Manual Verification
1. Open Audio Trimming screen $\rightarrow$ Tap `+` or `-` $\rightarrow$ Verify time increments/decrements precisely by 0.1s.
2. Select a range > 30s and tap "Save Trim & Return" $\rightarrow$ Verify Toast warning appears and screen remains open until trimmed to <= 30s.
