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

    void update_timestamp(uint32_t timestamp);

    void update_stream_id(int id);


    virtual ~RtmpPacket();

    static RtmpPacket *create_for_sps_pps(char *sps, int sps_length, char *pps, int pps_length);

    static RtmpPacket *create_for_video(char *data, int dataLen, bool keyFrame);

    static RtmpPacket *create_for_audio(char *data, int dataLen, bool  isConfigData, int sampleRate, int channels, int bytesPerSample);
};


#endif //RTMP_PUSHER_RTMPPACKET_H
