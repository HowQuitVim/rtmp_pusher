//
// Created by zmy on 21-1-5.
//

#include "jni_env_ptr.h"

JNIEnvPtr::JNIEnvPtr(JavaVM *jvm) : jvm(jvm) {
    if (jvm->GetEnv((void **) &env_, JNI_VERSION_1_6) == JNI_EDETACHED) {
        jvm->AttachCurrentThread(&env_, nullptr);
        need_detach_ = true;
    }
}

JNIEnvPtr::~JNIEnvPtr() {
    if (need_detach_) {
        jvm->DetachCurrentThread();
    }
}

JNIEnv *JNIEnvPtr::operator->() {
    return env_;
}