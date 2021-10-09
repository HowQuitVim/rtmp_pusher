//
// Created by zmy on 21-1-5.
//

#ifndef STREAMPUBLISH_JNI_ENV_PTR_H
#define STREAMPUBLISH_JNI_ENV_PTR_H

#include <jni.h>

class JNIEnvPtr {
public:
    JNIEnvPtr(JavaVM *jvm);

    ~JNIEnvPtr();

    JNIEnv *operator->();

private:
    JNIEnv *env_ = nullptr;

    JavaVM *jvm = nullptr;

    bool need_detach_ = false;

    JNIEnvPtr(const JNIEnvPtr &) = delete;

    JNIEnvPtr &operator=(const JNIEnvPtr &) = delete;
};

#endif //STREAMPUBLISH_JNI_ENV_PTR_H
