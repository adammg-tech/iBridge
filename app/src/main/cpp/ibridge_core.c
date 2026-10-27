#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/wait.h>
#include <android/log.h>

#define LOG_TAG "IBridgeCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern char **environ;

JNIEXPORT jint JNICALL
Java_com_adammg_ibridge_core_native_IBridgeNativeEngine_startNativeUsbmuxd(
    JNIEnv *env,
    jobject thiz,
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

    setenv("TERMUX_USB_FD", fd_buf, 1);
    setenv("LIBUSB_FD", fd_buf, 1);
    setenv("USBMUXD_SOCKET_ADDRESS", socket_str, 1);
    setenv("USBMUXD_SOCKET_PATH", socket_str, 1);
    setenv("LD_LIBRARY_PATH", lib_str, 1);

    LOGI("IBridgeCore: Spawning usbmuxd via fork/execve with FD %s, Socket %s", fd_buf, socket_str);

    pid_t pid = fork();
    if (pid == 0) {
        char *argv[] = {"/system/bin/linker64", (char*)binary_str, "-f", "-v", NULL};
        execve("/system/bin/linker64", argv, environ);
        _exit(127);
    } else if (pid > 0) {
        if (socket_str) (*env)->ReleaseStringUTFChars(env, socketPath, socket_str);
        if (binary_str) (*env)->ReleaseStringUTFChars(env, binaryPath, binary_str);
        if (lib_str) (*env)->ReleaseStringUTFChars(env, libDir, lib_str);
        return (jint) pid;
    } else {
        LOGE("IBridgeCore: fork failed!");
        if (socket_str) (*env)->ReleaseStringUTFChars(env, socketPath, socket_str);
        if (binary_str) (*env)->ReleaseStringUTFChars(env, binaryPath, binary_str);
        if (lib_str) (*env)->ReleaseStringUTFChars(env, libDir, lib_str);
        return -1;
    }
}

JNIEXPORT jint JNICALL
Java_com_adammg_ibridge_core_native_IBridgeNativeEngine_executeNativeCommand(
    JNIEnv *env,
    jobject thiz,
    jstring binaryPath,
    jobjectArray argsArray,
    jstring libDir
) {
    const char *binary_str = (*env)->GetStringUTFChars(env, binaryPath, NULL);
    const char *lib_str = (*env)->GetStringUTFChars(env, libDir, NULL);

    setenv("LD_LIBRARY_PATH", lib_str, 1);

    jsize argCount = (*env)->GetArrayLength(env, argsArray);
    char **argv = (char**) malloc(sizeof(char*) * (argCount + 3));
    argv[0] = "/system/bin/linker64";
    argv[1] = (char*) binary_str;

    for (int i = 0; i < argCount; i++) {
        jstring argStr = (jstring) (*env)->GetObjectArrayElement(env, argsArray, i);
        argv[i + 2] = (char*) (*env)->GetStringUTFChars(env, argStr, NULL);
    }
    argv[argCount + 2] = NULL;

    pid_t pid = fork();
    if (pid == 0) {
        execve("/system/bin/linker64", argv, environ);
        _exit(127);
    } else if (pid > 0) {
        int status = 0;
        waitpid(pid, &status, 0);
        int exitCode = WIFEXITED(status) ? WEXITSTATUS(status) : -1;

        for (int i = 0; i < argCount; i++) {
            jstring argStr = (jstring) (*env)->GetObjectArrayElement(env, argsArray, i);
            (*env)->ReleaseStringUTFChars(env, argStr, argv[i + 2]);
        }
        free(argv);

        if (binary_str) (*env)->ReleaseStringUTFChars(env, binaryPath, binary_str);
        if (lib_str) (*env)->ReleaseStringUTFChars(env, libDir, lib_str);
        return exitCode;
    } else {
        free(argv);
        if (binary_str) (*env)->ReleaseStringUTFChars(env, binaryPath, binary_str);
        if (lib_str) (*env)->ReleaseStringUTFChars(env, libDir, lib_str);
        return -1;
    }
}
