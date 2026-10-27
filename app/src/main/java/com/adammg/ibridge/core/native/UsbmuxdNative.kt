package com.adammg.ibridge.core.native

import android.util.Log

object UsbmuxdNative {
    private const val TAG = "UsbmuxdNative"

    init {
        try {
            System.loadLibrary("usbmuxd_launcher")
            Log.i(TAG, "Successfully loaded libusbmuxd_launcher.so")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load libusbmuxd_launcher.so", e)
        }
    }

    @JNIExplicit
    external fun startNativeUsbmuxd(
        fd: Int,
        socketPath: String,
        binaryPath: String,
        libDir: String
    ): Int
}

annotation class JNIExplicit
