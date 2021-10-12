//
// Created by zmy on 2021/9/30.
//

#include "NativeLogger.h"
#include "jni_env_ptr.h"
#include "log.h"
#include "android/log.h"

NativeLogger::NativeLogger(JavaVM *jvm, jobject logger) : jvm(jvm) {
    JNIEnvPtr env(jvm);
    logger_ref = env->NewGlobalRef(logger);
}

NativeLogger::~NativeLogger() {
    JNIEnvPtr env(jvm);
    env->DeleteGlobalRef(logger_ref);
}

int getLevel(RTMP_LogLevel level) {
    switch (level) {
        case RTMP_LOGERROR:
            return ANDROID_LOG_ERROR;
        case RTMP_LOGWARNING:
            return ANDROID_LOG_WARN;
        case RTMP_LOGINFO:
            return ANDROID_LOG_INFO;
        case RTMP_LOGDEBUG:
        case RTMP_LOGDEBUG2:
            return ANDROID_LOG_DEBUG;
        default:
            return ANDROID_LOG_INFO;
    }
}

void NativeLogger::print(RTMP_LogLevel level, const char *tag, const char *msg) {
    JNIEnvPtr env(jvm);
    jclass clazz = env->FindClass("com/zmy/rtmp_pusher/lib/log/RtmpLogger");
    jmethodID print_method = env->GetMethodID(clazz, "print", "(ILjava/lang/String;Ljava/lang/String;)V");
    int android_level = getLevel(level);
    jstring tag_string = env->NewStringUTF(tag);
    jstring msg_string = env->NewStringUTF(msg);
    env->CallVoidMethod(logger_ref, print_method, android_level, tag_string, msg_string);
}

