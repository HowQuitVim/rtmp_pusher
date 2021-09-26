//
// Created by zmy on 2021/9/24.
//

#ifndef RTMP_PUSHER_RTMPPACKET_H
#define RTMP_PUSHER_RTMPPACKET_H


class RtmpPacket {
    RTMPPacket *packet = nullptr;


public:
    RtmpPacket();

    void init(int size);

    RTMPPacket *getPacket() const;

    void updateTimestamp(uint32_t timestamp);

    void updateStreamId(int id);


    virtual ~RtmpPacket();

    static RtmpPacket *createForSPSPPS(char *sps, int spsLen, char *pps, int ppsLen);

    static RtmpPacket *createForVideo(char *data, int dataLen, bool keyFrame);

    static RtmpPacket *createForAudio(char *data, int dataLen,bool  isConfigData,int sampleRate,int channels,int bytePerSample);
};


#endif //RTMP_PUSHER_RTMPPACKET_H
