//
// Created by zmy on 2021/9/24.
//

#include <rtmp.h>
#include <cstdlib>
#include <cstring>
#include <log.h>
#include <string>
#include "RtmpPacket.h"

RtmpPacket::RtmpPacket() {
    packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
}

void RtmpPacket::init(int size) {
    RTMPPacket_Alloc(packet, size);
    RTMPPacket_Reset(packet);
}


RTMPPacket *RtmpPacket::getPacket() const {
    return packet;
}

int get_start_code_bytes(const char *data, int len) {
    if (len < 3) {
        return -1;
    }
    if (data[2] == 0) {
        return 4;
    }
    if (data[2] == 1) {
        return 3;
    }
    return 0;
}

RtmpPacket *RtmpPacket::create_for_sps_pps(char *sps, int sps_length, char *pps, int pps_length) {

    int sps_start_code = get_start_code_bytes(sps, sps_length);
    int pps_start_code = get_start_code_bytes(pps, pps_length);

    int sps_len = sps_length - sps_start_code;
    int pps_len = pps_length - pps_start_code;

    char *sps_data = sps + sps_start_code;
    char *pps_data = pps + pps_start_code;


    auto rtmp_packet = new RtmpPacket();
    int body_size = sps_len + pps_len + 16;
    rtmp_packet->init(body_size);
    char *body = rtmp_packet->packet->m_body;

    int i = 0;
    //frame type(4bit)和CodecId(4bit)合成一个字节(byte)
    //frame type 关键帧1  非关键帧2
    //CodecId  7表示avc
    body[i++] = 0x17;

    //fixed 4byte
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;

    //configurationVersion： 版本 1byte
    body[i++] = 0x01;

    //AVCProfileIndication：Profile 1byte  sps[1]
    body[i++] = sps_data[1];

    //compatibility：  兼容性 1byte  sps[2]
    body[i++] = sps_data[2];

    //AVCLevelIndication： ProfileLevel 1byte  sps[3]
    body[i++] = sps_data[3];

    //lengthSizeMinusOne： 包长数据所使用的字节数  1byte
    body[i++] = 0xff;

    //sps个数 1byte
    body[i++] = 0xe1;
    //sps长度 2byte
    body[i++] = (sps_len >> 8) & 0xff;
    body[i++] = sps_len & 0xff;

    //sps data 内容
    memcpy(&body[i], sps_data, sps_len);
    i += sps_len;
    //pps个数 1byte
    body[i++] = 0x01;
    //pps长度 2byte
    body[i++] = (pps_len >> 8) & 0xff;
    body[i++] = pps_len & 0xff;
    //pps data 内容
    memcpy(&body[i], pps_data, pps_len);

    rtmp_packet->packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    rtmp_packet->packet->m_nBodySize = body_size;
    rtmp_packet->packet->m_hasAbsTimestamp = 0;
    rtmp_packet->packet->m_nChannel = 0x04;//音频或者视频
    rtmp_packet->packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    return rtmp_packet;
}

RtmpPacket *RtmpPacket::create_for_video(char *data, int dataLen, bool keyFrame) {
    auto rtmpPacket = new RtmpPacket();
    int start_code = get_start_code_bytes(data, dataLen);
    int data_len = dataLen - start_code;
    char *h264_data = data + start_code;
    int bodySize = data_len + 9;
    rtmpPacket->init(bodySize);


    char *body = rtmpPacket->packet->m_body;

    int i = 0;
    //frame type(4bit)和CodecId(4bit)合成一个字节(byte)
    //frame type 关键帧1  非关键帧2
    //CodecId  7表示avc
    if (keyFrame) {
        body[i++] = 0x17;
    } else {
        body[i++] = 0x27;
    }

    //fixed 4byte   0x01表示NALU单元
    body[i++] = 0x01;
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;

    //dataLen  4byte
    body[i++] = (data_len >> 24) & 0xff;
    body[i++] = (data_len >> 16) & 0xff;
    body[i++] = (data_len >> 8) & 0xff;
    body[i++] = data_len & 0xff;

    //data
    memcpy(&body[i], h264_data, data_len);

    rtmpPacket->packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    rtmpPacket->packet->m_nBodySize = bodySize;
    //进入直播播放开始时间
    rtmpPacket->packet->m_hasAbsTimestamp = 0;
    rtmpPacket->packet->m_nChannel = 0x04;//音频或者视频
    rtmpPacket->packet->m_headerType = RTMP_PACKET_SIZE_LARGE;

    return rtmpPacket;
}

char getSampleRateMask(int sampleRate) {
    char mask;
    switch (sampleRate) {
        case 5500:
            mask = 0X00;
            break;
        case 11000:
            mask = 0X04;
            break;
        case 22000:
            mask = 0X08;
            break;
        case 44100:
            mask = 0X0C;
            break;
    }
    return mask;
}
char getMask(int bytesPerSample){
    char mask;
    switch (bytesPerSample) {
        case 1:
            mask = 0X00;
            break;
        case 2:
            mask = 0X02;
            break;
    }
    return mask;
}
char getChannelCountMask(int channels){
    char mask;
    switch (channels) {
        case 1:
            mask = 0X00;
            break;
        case 2:
            mask = 0X01;
            break;
    }
    return mask;
}

RtmpPacket *RtmpPacket::create_for_audio(char *data, int dataLen, bool isConfigData, int sampleRate, int channels, int bytesPerSample) {
    auto rtmpPacket = new RtmpPacket();
    int bodySize = dataLen + 2;
    rtmpPacket->init(bodySize);
    char *body = rtmpPacket->packet->m_body;
    //前四位表示音频数据格式  10（十进制）表示AAC，16进制就是A
    char byte = 0xA0;
    byte |= getSampleRateMask(sampleRate);//第5-6位的数值表示采样率，0 = 5.5 kHz，1 = 11 kHz，2 = 22 kHz，3(11) = 44 kHz。
    byte |= getMask(bytesPerSample); //第7位表示采样精度，0 = 8bits，1 = 16bits。
    byte |= getChannelCountMask(channels); //第8位表示音频类型，0 = mono，1 = stereo
    body[0] = byte;

    //0x00 aac头信息,  0x01 aac 原始数据
    //这里都用0x01都可以
    body[1] = isConfigData ? 0x00 : 0x01;

    //data
    memcpy(&body[2], data, dataLen);

    rtmpPacket->packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    rtmpPacket->packet->m_nBodySize = bodySize;
    //持续播放时间
    //进入直播播放开始时间
    rtmpPacket->packet->m_hasAbsTimestamp = 0;
    rtmpPacket->packet->m_nChannel = 0x04;//音频或者视频
    rtmpPacket->packet->m_headerType = RTMP_PACKET_SIZE_LARGE;

    return rtmpPacket;
}


RtmpPacket::~RtmpPacket() {
    RTMPPacket_Free(packet);
    free(packet);
    packet = nullptr;
}

void RtmpPacket::update_stream_id(int id) {
    packet->m_nInfoField2 = id;
}

void RtmpPacket::update_timestamp(uint32_t timestamp) {
    packet->m_nTimeStamp = timestamp;
}
