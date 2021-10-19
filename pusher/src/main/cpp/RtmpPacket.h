//
// Created by zmy on 2021/9/24.
//

#ifndef RTMP_PUSHER_RTMPPACKET_H
#define RTMP_PUSHER_RTMPPACKET_H


class RtmpPacket {
    RTMPPacket *packet = nullptr;
    int body_size = 0;

public:
    RtmpPacket();

    void init(int size);

    RTMPPacket *getPacket() const;

    void update_timestamp(uint32_t timestamp);

    void update_stream_id(int id);

    RtmpPacket *clone();

    virtual ~RtmpPacket();

    static RtmpPacket *create_for_sps_pps(char *sps, int sps_length, char *pps, int pps_length);

    static RtmpPacket *create_for_video(char *data, int data_length, bool keyFrame);

    static RtmpPacket *create_for_audio(char *data, int data_len, bool is_audiO_specific_config, int sample_rate, int channels, int bytes_per_sample);
};


#endif //RTMP_PUSHER_RTMPPACKET_H
