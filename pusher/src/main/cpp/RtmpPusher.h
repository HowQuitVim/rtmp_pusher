//
// Created by zmy on 2021/9/24.
//

#ifndef RTMP_PUSHER_RTMPPUSHER_H
#define RTMP_PUSHER_RTMPPUSHER_H

#include "rtmp.h"
#include "string"
#include "RtmpPacket.h"

class RtmpPusher {
private:
    std::string url;
    RTMP *rtmp = nullptr;
    uint32_t start_time = 0;

    uint32_t current_timestamp();

public:

    RtmpPusher(std::string &&url);


    bool connect();

    bool init();

    const std::string &get_url() const;

    virtual ~RtmpPusher();

    bool push(RtmpPacket *pPacket);
};


#endif //RTMP_PUSHER_RTMPPUSHER_H
