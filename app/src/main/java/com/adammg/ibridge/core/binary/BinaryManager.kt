package com.adammg.ibridge.core.binary

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class BinaryManager(private val context: Context) {

    companion object {
        private const val TAG = "BinaryManager"
        private const val TARGET_ABI = "arm64-v8a"
    }

    val filesDir: File = context.filesDir
    val dataDir: File = File(context.applicationInfo.dataDir)
    val binDir: File = File(filesDir, "bin")
    val libDir: File = File(filesDir, "lib")

    // The 31-byte replacement path for /data/data/com.termux/files/usr is /data/data/com.adammg.ibridge/f
    val fDir: File = File(dataDir, "f")
    val fVarRunDir: File = File(fDir, "var/run")
    val fLockdownDir: File = File(fDir, "var/lib/lockdown")
    val socketFile: File = File(fVarRunDir, "usbmuxd")

    val lockdownDir: File = File(filesDir, "var/lib/lockdown")
    val varRunDir: File = File(filesDir, "var/run")

    init {
        prepareEnvironment()
    }

    fun prepareEnvironment(): Boolean {
        return try {
            if (!binDir.exists()) binDir.mkdirs()
            if (!libDir.exists()) libDir.mkdirs()
            if (!lockdownDir.exists()) lockdownDir.mkdirs()
            if (!varRunDir.exists()) varRunDir.mkdirs()
            if (!fDir.exists()) fDir.mkdirs()
            if (!fVarRunDir.exists()) fVarRunDir.mkdirs()
            if (!fLockdownDir.exists()) fLockdownDir.mkdirs()

            extractAssetFolder("bin/$TARGET_ABI", binDir, makeExecutable = true)
            extractAssetFolder("lib/$TARGET_ABI", libDir, makeExecutable = false)

            createLibraryAliases()

            com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Environment prepared. binDir count: ${binDir.listFiles()?.size ?: 0}, libDir count: ${libDir.listFiles()?.size ?: 0}")
            true
        } catch (e: Exception) {
            com.adammg.ibridge.core.logger.AppLogger.log(TAG, "ERROR preparing environment: ${e.message}")
            false
        }
    }

    private fun createLibraryAliases() {
        val libFiles = libDir.listFiles() ?: return
        for (file in libFiles) {
            when (file.name) {
                "libssl.so" -> {
                    val alias = File(libDir, "libssl.so.3")
                    file.copyTo(alias, overwrite = true)
                    com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Created library alias: libssl.so -> libssl.so.3 (${alias.length()} bytes)")
                }
                "libcrypto.so" -> {
                    val alias = File(libDir, "libcrypto.so.3")
                    file.copyTo(alias, overwrite = true)
                    com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Created library alias: libcrypto.so -> libcrypto.so.3 (${alias.length()} bytes)")
                }
                "libusb-1.0.so" -> {
                    val alias = File(libDir, "libusb-1.0.so.0")
                    file.copyTo(alias, overwrite = true)
                    com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Created library alias: libusb-1.0.so -> libusb-1.0.so.0")
                }
                "libusbmuxd-2.0.so" -> {
                    val alias = File(libDir, "libusbmuxd-2.0.so.6")
                    file.copyTo(alias, overwrite = true)
                }
                "libplist-2.0.so" -> {
                    val alias = File(libDir, "libplist-2.0.so.4")
                    file.copyTo(alias, overwrite = true)
                }
            }
        }
    }

    private fun patchTermuxPrefix(file: File) {
        try {
            val bytes = file.readBytes()
            val target = "/data/data/com.termux/files/usr".toByteArray(Charsets.UTF_8)
            val replacement = "/data/data/com.adammg.ibridge/f".toByteArray(Charsets.UTF_8)

            var patchedCount = 0
            for (i in 0..(bytes.size - target.size)) {
                var match = true
                for (j in target.indices) {
                    if (bytes[i + j] != target[j]) {
                        match = false
                        break
                    }
                }
                if (match) {
                    for (j in replacement.indices) {
                        bytes[i + j] = replacement[j]
                    }
                    patchedCount++
                }
            }

            if (patchedCount > 0) {
                file.writeBytes(bytes)
                com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Patched $patchedCount Termux paths in ${file.name}")
            } else {
                com.adammg.ibridge.core.logger.AppLogger.log(TAG, "No Termux paths found in ${file.name}")
            }
        } catch (e: Exception) {
            com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Error patching ${file.name}: ${e.message}")
        }
    }

    private fun extractAssetFolder(assetPath: String, targetFolder: File, makeExecutable: Boolean) {
        val assetManager = context.assets
        val assets = assetManager.list(assetPath) ?: return

        for (filename in assets) {
            val fullAssetPath = "$assetPath/$filename"
            val targetFile = File(targetFolder, filename)

            try {
                if (targetFile.exists()) {
                    targetFile.delete()
                }

                val inputStream: InputStream = assetManager.open(fullAssetPath)
                val outputStream = FileOutputStream(targetFile)

                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                // Patch hardcoded Termux prefix paths inside binary/library files to iBridge private package path
                patchTermuxPrefix(targetFile)

                if (makeExecutable) {
                    targetFile.setExecutable(true, false)
                    targetFile.setReadable(true, false)
                    targetFile.setWritable(true, false)
                    try {
                        val p = Runtime.getRuntime().exec("chmod 755 ${targetFile.absolutePath}")
                        p.waitFor()
                    } catch (e: Exception) {
                        com.adammg.ibridge.core.logger.AppLogger.log(TAG, "chmod failed for $filename: ${e.message}")
                    }
                }

                val executableStatus = if (makeExecutable) "canExecute=${targetFile.canExecute()}" else "so_library"
                com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Extracted asset $filename -> ${targetFile.absolutePath} ($executableStatus)")
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting asset $filename", e)
            }
        }
    }

    fun getEnvironmentVariables(usbFd: Int? = null, deviceName: String? = null): Map<String, String> {
        val env = mutableMapOf<String, String>()
        val existingPath = System.getenv("PATH") ?: ""
        val systemLibPath = "/system/lib64:/apex/com.android.runtime/lib64:/apex/com.android.art/lib64:/system/lib"

        env["PATH"] = "${binDir.absolutePath}:$existingPath"
        env["LD_LIBRARY_PATH"] = "${libDir.absolutePath}:$systemLibPath"
        env["HOME"] = filesDir.absolutePath
        env["PREFIX"] = filesDir.absolutePath
        env["USBMUXD_SOCKET_ADDRESS"] = socketFile.absolutePath
        env["USBMUXD_SOCKET_PATH"] = socketFile.absolutePath

        usbFd?.let {
            env["TERMUX_USB_FD"] = it.toString()
            env["LIBUSB_FD"] = it.toString()
        }

        return env
    }

    fun isBinaryAvailable(binaryName: String): Boolean {
        val file = File(binDir, binaryName)
        return file.exists()
    }

    fun getBinaryPath(binaryName: String): String {
        return File(binDir, binaryName).absolutePath
    }

    fun getLinkerPath(): String {
        val linker64 = File("/system/bin/linker64")
        return if (linker64.exists()) "/system/bin/linker64" else "/system/bin/linker"
    }
}
