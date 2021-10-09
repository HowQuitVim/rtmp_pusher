cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_BUILD_TYPE Release)

link_directories(${CMAKE_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}/)

add_library(
        pusher
        SHARED
        main.cpp
        RtmpPusher.cpp
        RtmpPacket.cpp
        jni_env_ptr.cpp
        NativeLogger.cpp
)

target_include_directories(pusher PUBLIC librtmp)

target_link_libraries(pusher
        rtmp
        log)
