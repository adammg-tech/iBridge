package com.adammg.ibridge.core.native

import android.util.Log

object IBridgeNativeEngine {
    private const val TAG = "IBridgeNativeEngine"

    init {
        try {
            System.loadLibrary("ibridge_core")
            Log.i(TAG, "Successfully loaded libibridge_core.so")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load libibridge_core.so", e)
        }
    }

    external fun startNativeUsbmuxd(
        fd: Int,
        socketPath: String,
        binaryPath: String,
        libDir: String
    ): Int

    external fun executeNativeCommand(
        binaryPath: String,
        args: Array<String>,
        libDir: String
    ): Int
}
