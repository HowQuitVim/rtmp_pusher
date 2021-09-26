#include <jni.h>
#include <log.h>
#include "android/log.h"
#include "RtmpPusher.h"
#include "RtmpPacket.h"
//
// Created by zmy on 2021/9/24.
//
/*-----------------------------------Pusher----------------------------------------*/

extern "C"
JNIEXPORT jlong JNICALL nativeNewInstance(JNIEnv *env, jobject thiz, jstring url);
extern "C"
JNIEXPORT void JNICALL nativeRelease(JNIEnv *env, jobject thiz, jlong handle);
extern "C"
JNIEXPORT jboolean  JNICALL nativeInit(JNIEnv *env, jobject thiz, jlong handle);
extern "C"
JNIEXPORT jboolean  JNICALL nativeConnect(JNIEnv *env, jobject thiz, jlong handle);
extern "C"
JNIEXPORT jboolean  JNICALL nativePush(JNIEnv *env, jobject thiz, jlong handle, jlong packetHandle);






/*-----------------------------------EncodeFrame----------------------------------------*/
extern "C"
JNIEXPORT jlong  JNICALL
nativeCreateForSPSPPS(JNIEnv *env, jclass clazz, jobject sps, jint sps_len, jobject pps, jint pps_len);
extern "C"
JNIEXPORT jlong  JNICALL
nativeCreateForVideo(JNIEnv *env, jclass clazz, jobject data, jint data_len, jboolean is_key_frame);
extern "C"
JNIEXPORT jlong  JNICALL
nativeCreateForAudio(JNIEnv *env, jclass clazz, jobject data, jint data_len, jboolean is_config_data);





/*-----------------------------------Err----------------------------------------*/
extern "C"
JNIEXPORT jint JNICALL nativeGetErrno(JNIEnv *env, jclass clazz);
extern "C"
JNIEXPORT jobject JNICALL nativeErrDescribe(JNIEnv *env, jclass clazz, jint err);

const char *pusher_class = "com/zmy/rtmp_pusher/lib/pusher/Pusher";
static const JNINativeMethod pusher_native_method[] = {
        {"newInstance", "(Ljava/lang/String;)J", (void *) nativeNewInstance},
        {"release",     "(J)V",                  (void *) nativeRelease},
        {"init",        "(J)Z",                  (void *) nativeInit},
        {"connect",     "(J)Z",                  (void *) nativeConnect},
        {"push",        "(JJ)Z",                 (void *) nativePush},
};
const char *encode_frame_class = "com/zmy/rtmp_pusher/lib/encoder/EncodeFrame";
static const JNINativeMethod encode_frame_native_method[] = {
        {"nativeCreateForSPSPPS", "(Ljava/nio/ByteBuffer;ILjava/nio/ByteBuffer;I)J", (void *) nativeCreateForSPSPPS},
        {"nativeCreateForVideo",  "(Ljava/nio/ByteBuffer;IZ)J",                      (void *) nativeCreateForVideo},
        {"nativeCreateForAudio",  "(Ljava/nio/ByteBuffer;IZ)J",                      (void *) nativeCreateForAudio},
};

const char *err_class = "com/zmy/rtmp_pusher/lib/exception/Err";
static const JNINativeMethod err_native_method[] = {
        {"errno",       "()I",                  (void *) nativeGetErrno},
        {"errDescribe", "(I)Ljava/lang/String;", (void *) nativeErrDescribe},
};

JNIEXPORT int registerNativeMethod(JNIEnv *env, const char *clazz_name, const JNINativeMethod *method, jint size) {
    jclass clazz = env->FindClass(clazz_name);
    jint ret = env->RegisterNatives(clazz, method, size);
    return ret;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    if (registerNativeMethod(env, pusher_class, pusher_native_method,
                             sizeof(pusher_native_method) / sizeof(pusher_native_method[0])) != JNI_OK) {
        return JNI_ERR;
    }
    if (registerNativeMethod(env, encode_frame_class, encode_frame_native_method,
                             sizeof(encode_frame_native_method) / sizeof(encode_frame_native_method[0])) != JNI_OK) {
        return JNI_ERR;
    }
    if (registerNativeMethod(env, err_class, err_native_method,
                             sizeof(err_native_method) / sizeof(err_native_method[0])) != JNI_OK) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}

void callback(int level, const char *fmt, va_list vl) {
    static char buf[2048];
    vsnprintf(buf, 2048 - 1, fmt, vl);
    __android_log_print(ANDROID_LOG_DEBUG, "rtmp", "%s", buf);
}


/*-----------------------------------Pusher----------------------------------------*/
extern "C"
JNIEXPORT jlong JNICALL nativeNewInstance(JNIEnv *env, jobject thiz, jstring url) {
    RTMP_LogSetCallback(callback);
    const char *_url = env->GetStringUTFChars(url, nullptr);
    auto *pusher = new RtmpPusher(std::string(_url));
    return (int64_t) pusher;

}
extern "C"
JNIEXPORT void JNICALL nativeRelease(JNIEnv *env, jobject thiz, jlong handle) {
    delete (RtmpPusher *) handle;
}

extern "C"
JNIEXPORT jboolean  JNICALL nativeInit(JNIEnv *env, jobject thiz, jlong handle) {
    return ((RtmpPusher *) handle)->init();
}
extern "C"
JNIEXPORT jboolean JNICALL nativeConnect(JNIEnv *env, jobject thiz, jlong handle) {
    return ((RtmpPusher *) handle)->connect();
}

extern "C"
JNIEXPORT jboolean  JNICALL nativePush(JNIEnv *env, jobject thiz, jlong handle, jlong packetHandle) {
    return ((RtmpPusher *) handle)->push((RtmpPacket *) packetHandle);

}






/*-----------------------------------EncodeFrame----------------------------------------*/
extern "C"
JNIEXPORT jlong JNICALL
nativeCreateForSPSPPS(JNIEnv *env, jclass clazz, jobject sps, jint sps_len, jobject pps, jint pps_len) {
    char *sps_data = (char *) env->GetDirectBufferAddress(sps);
    char *pps_data = (char *) env->GetDirectBufferAddress(pps);
    return (int64_t) RtmpPacket::createForSPSPPS(sps_data, sps_len, pps_data, pps_len);
}
extern "C"
JNIEXPORT jlong  JNICALL
nativeCreateForVideo(JNIEnv *env, jclass clazz, jobject data, jint data_len, jboolean is_key_frame) {
    char *frame_data = (char *) env->GetDirectBufferAddress(data);
    return (int64_t) RtmpPacket::createForVideo(frame_data, data_len, is_key_frame);

}
extern "C"
JNIEXPORT jlong  JNICALL
nativeCreateForAudio(JNIEnv *env, jclass clazz, jobject data, jint data_len, jboolean is_config_data) {
    char *frame_data = (char *) env->GetDirectBufferAddress(data);
    return (int64_t) RtmpPacket::createForAudio(frame_data, data_len, is_config_data, 44100, 2, 2);//todo
}







/*-----------------------------------Err----------------------------------------*/
extern "C"
JNIEXPORT jint JNICALL nativeGetErrno(JNIEnv *env, jclass clazz) {
    return errno;
}
extern "C"
JNIEXPORT jobject JNICALL nativeErrDescribe(JNIEnv *env, jclass clazz, jint err) {
    char *desc = strerror(err);
    return env->NewStringUTF(desc);
}