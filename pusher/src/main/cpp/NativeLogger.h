//
// Created by zmy on 2021/9/30.
//

#ifndef RTMP_PUSHER_NATIVELOGGER_H
#define RTMP_PUSHER_NATIVELOGGER_H

#include <jni.h>
#include <log.h>

class NativeLogger {
private:
    JavaVM *jvm = nullptr;
    jobject logger_ref = nullptr;
public:
    NativeLogger(JavaVM *jvm, jobject logger);

    void print(RTMP_LogLevel level, const char *tag, const char *msg);

    virtual ~NativeLogger();

};


#endif //RTMP_PUSHER_NATIVELOGGER_H
