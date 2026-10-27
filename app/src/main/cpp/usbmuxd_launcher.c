#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <android/log.h>

#define LOG_TAG "UsbmuxdNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern char **environ;

JNIEXPORT jint JNICALL
Java_com_adammg_ibridge_core_native_UsbmuxdNative_startNativeUsbmuxd(
    JNIEnv *env,
    jclass clazz,
    jint fd,
    jstring socketPath,
    jstring binaryPath,
    jstring libDir
) {
    const char *socket_str = (*env)->GetStringUTFChars(env, socketPath, NULL);
    const char *binary_str = (*env)->GetStringUTFChars(env, binaryPath, NULL);
    const char *lib_str = (*env)->GetStringUTFChars(env, libDir, NULL);

    char fd_buf[16];
    snprintf(fd_buf, sizeof(fd_buf), "%d", fd);

    // Set C environment variables in current process memory
    setenv("TERMUX_USB_FD", fd_buf, 1);
    setenv("LIBUSB_FD", fd_buf, 1);
    setenv("USBMUXD_SOCKET_ADDRESS", socket_str, 1);
    setenv("USBMUXD_SOCKET_PATH", socket_str, 1);
    setenv("LD_LIBRARY_PATH", lib_str, 1);

    LOGI("UsbmuxdNative: Spawning POSIX child process via fork/execve for USB FD %s, Socket %s", fd_buf, socket_str);

    pid_t pid = fork();
    if (pid == 0) {
        // Child process: execute usbmuxd via linker64 in POSIX C
        char *argv[] = {"/system/bin/linker64", (char*)binary_str, "-f", "-v", NULL};
        execve("/system/bin/linker64", argv, environ);
        _exit(127);
    } else if (pid > 0) {
        // Parent process
        LOGI("UsbmuxdNative: Successfully spawned usbmuxd child process PID=%d", pid);
        if (socket_str) (*env)->ReleaseStringUTFChars(env, socketPath, socket_str);
        if (binary_str) (*env)->ReleaseStringUTFChars(env, binaryPath, binary_str);
        if (lib_str) (*env)->ReleaseStringUTFChars(env, libDir, lib_str);
        return (jint) pid;
    } else {
        LOGE("UsbmuxdNative: fork failed!");
        if (socket_str) (*env)->ReleaseStringUTFChars(env, socketPath, socket_str);
        if (binary_str) (*env)->ReleaseStringUTFChars(env, binaryPath, binary_str);
        if (lib_str) (*env)->ReleaseStringUTFChars(env, libDir, lib_str);
        return -1;
    }
}
