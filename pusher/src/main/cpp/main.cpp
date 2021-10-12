#include <jni.h>
#include <log.h>
#include "android/log.h"
#include "RtmpPusher.h"
#include "RtmpPacket.h"
#include "NativeLogger.h"
//
// Created by zmy on 2021/9/24.
//
/*-----------------------------------Log----------------------------------------*/
NativeLogger *native_logger = nullptr;

void log_cb(int level, const char *fmt, va_list vl);

extern "C"
JNIEXPORT void JNICALL native_register_logger(JNIEnv *env, jclass clazz, jobject logger);

/*-----------------------------------Pusher----------------------------------------*/

extern "C"
JNIEXPORT jlong JNICALL native_new_instance(JNIEnv *env, jclass clazz, jstring url);
extern "C"
JNIEXPORT void JNICALL native_release(JNIEnv *env, jclass clazz, jlong handle);
extern "C"
JNIEXPORT jboolean  JNICALL native_init(JNIEnv *env, jclass clazz, jlong handle);
extern "C"
JNIEXPORT jboolean  JNICALL native_connect(JNIEnv *env, jclass clazz, jlong handle);
extern "C"
JNIEXPORT jboolean  JNICALL native_push(JNIEnv *env, jclass clazz, jlong handle, jlong packet_handle);
extern "C"
JNIEXPORT jboolean  JNICALL native_is_connected(JNIEnv *env, jclass clazz, jlong handle);



/*-----------------------------------RtmpPacket----------------------------------------*/
extern "C"
JNIEXPORT jlong  JNICALL
native_create_for_sps_pps(JNIEnv *env, jclass clazz, jobject sps, jint sps_offset, jint sps_len, jobject pps, jint pps_offset, jint pps_len);
extern "C"
JNIEXPORT jlong  JNICALL
native_create_for_video(JNIEnv *env, jclass clazz, jobject data, jint offset, jint data_len, jboolean is_key_frame);
extern "C"
JNIEXPORT jlong  JNICALL
native_create_for_audio(JNIEnv *env, jclass clazz, jobject data, jint offset, jint data_len, int sample_rate,
                        int channels,
                        int byte_per_sample, jboolean is_config_data);
extern "C"
JNIEXPORT void  JNICALL
native_release_frame(JNIEnv *env, jclass jobject, jlong handle);
extern "C"
JNIEXPORT jlong  JNICALL
native_clone(JNIEnv *env, jclass jobject, jlong handle);





/*-----------------------------------Err----------------------------------------*/
extern "C"
JNIEXPORT jint JNICALL native_get_errno(JNIEnv *env, jclass clazz);
extern "C"
JNIEXPORT jobject JNICALL native_err_describe(JNIEnv *env, jclass clazz, jint err);

const char *pusher_class = "com/zmy/rtmp_pusher/lib/pusher/Pusher";
static const JNINativeMethod pusher_native_method[] = {
        {"native_new_instance", "(Ljava/lang/String;)J", (void *) native_new_instance},
        {"native_release",      "(J)V",                  (void *) native_release},
        {"native_init",         "(J)Z",                  (void *) native_init},
        {"native_connect",      "(J)Z",                  (void *) native_connect},
        {"native_push",         "(JJ)Z",                 (void *) native_push},
        {"native_is_connected", "(J)Z",                  (void *) native_is_connected},
};
const char *encode_frame_class = "com/zmy/rtmp_pusher/lib/encoder/RtmpPacket";
static const JNINativeMethod encode_frame_native_method[] = {
        {"native_create_for_sps_pps", "(Ljava/nio/ByteBuffer;IILjava/nio/ByteBuffer;II)J", (void *) native_create_for_sps_pps},
        {"native_create_for_video",   "(Ljava/nio/ByteBuffer;IIZ)J",                       (void *) native_create_for_video},
        {"native_create_for_audio",   "(Ljava/nio/ByteBuffer;IIIIIZ)J",                    (void *) native_create_for_audio},
        {"native_release_frame",      "(J)V",                                              (void *) native_release_frame},
        {"native_clone",              "(J)J",                                              (void *) native_clone},
};

const char *err_class = "com/zmy/rtmp_pusher/lib/exception/Err";
static const JNINativeMethod err_native_method[] = {
        {"native_errno",        "()I",                   (void *) native_get_errno},
        {"native_err_describe", "(I)Ljava/lang/String;", (void *) native_err_describe},
};
const char *log_class = "com/zmy/rtmp_pusher/lib/log/RtmpLogManager";
static const JNINativeMethod log_native_method[] = {
        {"native_register_logger", "(Lcom/zmy/rtmp_pusher/lib/log/RtmpLogger;)V", (void *) native_register_logger},
};

JNIEXPORT int register_native_method(JNIEnv *env, const char *clazz_name, const JNINativeMethod *method, jint size) {
    jclass clazz = env->FindClass(clazz_name);
    jint ret = env->RegisterNatives(clazz, method, size);
    return ret;
}

JavaVM *jvm = nullptr;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    if (register_native_method(env, pusher_class, pusher_native_method,
                               sizeof(pusher_native_method) / sizeof(pusher_native_method[0])) != JNI_OK) {
        return JNI_ERR;
    }
    if (register_native_method(env, encode_frame_class, encode_frame_native_method,
                               sizeof(encode_frame_native_method) / sizeof(encode_frame_native_method[0])) != JNI_OK) {
        return JNI_ERR;
    }
    if (register_native_method(env, err_class, err_native_method,
                               sizeof(err_native_method) / sizeof(err_native_method[0])) != JNI_OK) {
        return JNI_ERR;
    }
    if (register_native_method(env, log_class, log_native_method,
                               sizeof(log_native_method) / sizeof(log_native_method[0])) != JNI_OK) {
        return JNI_ERR;
    }
    jvm = vm;
    RTMP_LogSetCallback(log_cb);
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    RTMP_LogSetCallback(nullptr);
    if (native_logger != nullptr) {
        delete native_logger;
        native_logger = nullptr;
    }
}

void log_cb(int level, const char *fmt, va_list vl) {
    if (native_logger != nullptr) {
        static char buf[2048];
        vsnprintf(buf, 2048 - 1, fmt, vl);
        native_logger->print(static_cast<RTMP_LogLevel>(level), "native_pusher", buf);
    }

}
/*-----------------------------------Log----------------------------------------*/
extern "C"
JNIEXPORT void JNICALL native_register_logger(JNIEnv *env, jclass clazz, jobject logger) {
    if (native_logger != nullptr) {
        delete native_logger;
        native_logger = nullptr;
    }
    if (logger != nullptr) {
        native_logger = new NativeLogger(jvm, logger);
    }
}

/*-----------------------------------Pusher----------------------------------------*/
extern "C"
JNIEXPORT jlong JNICALL native_new_instance(JNIEnv *env, jclass thiz, jstring url) {
    jboolean copy = false;
    const char *_url = env->GetStringUTFChars(url, &copy);
    if (!_url) {
        return 0;
    }
    auto *pusher = new RtmpPusher(std::string(_url));
    env->ReleaseStringUTFChars(url, _url);
    return (int64_t) pusher;

}
extern "C"
JNIEXPORT void JNICALL native_release(JNIEnv *env, jclass clazz, jlong handle) {
    delete (RtmpPusher *) handle;
}

extern "C"
JNIEXPORT jboolean  JNICALL native_init(JNIEnv *env, jclass clazz, jlong handle) {
    return ((RtmpPusher *) handle)->init();
}
extern "C"
JNIEXPORT jboolean JNICALL native_connect(JNIEnv *env, jclass clazz, jlong handle) {
    return ((RtmpPusher *) handle)->connect();
}

extern "C"
JNIEXPORT jboolean  JNICALL native_push(JNIEnv *env, jclass clazz, jlong handle, jlong packet_handle) {
    return ((RtmpPusher *) handle)->push((RtmpPacket *) packet_handle);
}
extern "C"
JNIEXPORT jboolean  JNICALL native_is_connected(JNIEnv *env, jclass clazz, jlong handle) {
    return ((RtmpPusher *) handle)->is_connected();
}





/*-----------------------------------RtmpPacket----------------------------------------*/
extern "C"
JNIEXPORT jlong JNICALL
native_create_for_sps_pps(JNIEnv *env, jclass clazz, jobject sps, jint sps_offset, jint sps_len, jobject pps, jint pps_offset, jint pps_len) {
    char *sps_data = (char *) env->GetDirectBufferAddress(sps);
    char *pps_data = (char *) env->GetDirectBufferAddress(pps);
    return (int64_t) RtmpPacket::create_for_sps_pps(sps_data + sps_offset, sps_len, pps_data + pps_offset, pps_len);
}
extern "C"
JNIEXPORT jlong  JNICALL
native_create_for_video(JNIEnv *env, jclass clazz, jobject data, jint offset, jint data_len, jboolean is_key_frame) {
    char *frame_data = (char *) env->GetDirectBufferAddress(data);
    return (int64_t) RtmpPacket::create_for_video(frame_data + offset, data_len, is_key_frame);

}
extern "C"
JNIEXPORT jlong  JNICALL
native_create_for_audio(JNIEnv *env, jclass clazz, jobject data, jint offset, jint data_len, int sample_rate,
                        int channels,
                        int byte_per_sample, jboolean is_config_data) {
    char *frame_data = (char *) env->GetDirectBufferAddress(data);
    return (int64_t) RtmpPacket::create_for_audio(frame_data + offset, data_len, is_config_data, sample_rate, channels, byte_per_sample);
}

extern "C"
JNIEXPORT void  JNICALL
native_release_frame(JNIEnv *env, jclass jobject, jlong handle) {
    delete (RtmpPacket *) handle;
}

extern "C"
JNIEXPORT jlong  JNICALL
native_clone(JNIEnv *env, jclass jobject, jlong handle) {
    auto src = (RtmpPacket *) handle;
    return (int64_t) src->clone();
}



/*-----------------------------------Err----------------------------------------*/
extern "C"
JNIEXPORT jint JNICALL native_get_errno(JNIEnv *env, jclass clazz) {
    return errno;
}
extern "C"
JNIEXPORT jobject JNICALL native_err_describe(JNIEnv *env, jclass clazz, jint err) {
    char *desc = strerror(err);
    return env->NewStringUTF(desc);
}