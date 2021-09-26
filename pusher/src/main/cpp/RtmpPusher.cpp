//
// Created by zmy on 2021/9/24.
//

#include "RtmpPusher.h"

RtmpPusher::RtmpPusher(std::string &&url) : url(url) {}

const std::string &RtmpPusher::getUrl() const {
    return url;
}

bool RtmpPusher::connect() {
    if (RTMP_Connect(rtmp, nullptr) == FALSE) {
        return false;
    }
    if (RTMP_ConnectStream(rtmp, 0) == FALSE) {
        return false;
    }
    return true;
}


bool RtmpPusher::init() {
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    rtmp = RTMP_Alloc();
    if (!rtmp)return false;
    RTMP_Init(rtmp);
    if (RTMP_SetupURL(rtmp, const_cast<char *>(url.c_str())) == FALSE) {
        return false;
    }
    RTMP_EnableWrite(rtmp);
    return true;
}

RtmpPusher::~RtmpPusher() {
    RTMP_Close(rtmp);
    RTMP_Free(rtmp);
    rtmp = nullptr;
}


bool RtmpPusher::push(RtmpPacket *packet) {
    packet->updateStreamId(rtmp->m_stream_id);
    packet->updateTimestamp(currentTimestamp());
    int ret = RTMP_SendPacket(rtmp, packet->getPacket(), 1);
    delete packet;
    return ret;
}

uint32_t RtmpPusher::currentTimestamp() {
    uint32_t current = RTMP_GetTime();
    if (startTime == 0) {
        startTime = current;
    }
    return current - startTime;
}
