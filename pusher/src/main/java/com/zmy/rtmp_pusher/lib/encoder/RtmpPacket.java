package com.zmy.rtmp_pusher.lib.encoder;


import com.zmy.rtmp_pusher.lib.queue.LinkedQueue;

import java.nio.ByteBuffer;

public class RtmpPacket {


    public static final LinkedQueue.Deleter<RtmpPacket> DELETER = new RtmpPacketDeleter();

    private long handle = 0;
    private final PacketType type;

    private RtmpPacket(long handle, PacketType type) {
        if (handle == 0) {
            throw new IllegalArgumentException("fail to create RtmpPacket,check the arguments");
        }
        this.handle = handle;
        this.type = type;
    }

    public long getHandle() {
        return handle;
    }

    public PacketType getType() {
        return type;
    }

    public synchronized void release() {
        if (handle != 0) {
            native_release_frame(handle);
            handle = 0;
        }
    }

    private native static long native_create_for_sps_pps(ByteBuffer sps, int spsOffset, int spsLen, ByteBuffer pps, int ppsOffset, int ppsLen);

    private native static long native_create_for_video(ByteBuffer data, int offset, int dataLen, boolean keyFrame);

    private native static long native_create_for_audio(ByteBuffer data, int offset, int dataLen, int sampleRate, int channels, int bytesPerSample, boolean isConfigData);

    private native void native_release_frame(long handle);

    private native long native_clone(long handle);

    public RtmpPacket copy() {
        return new RtmpPacket(native_clone(handle), type);
    }


    public static RtmpPacket createForSpsPps(ByteBuffer sps, int spsOffset, int spsLen, ByteBuffer pps, int ppsOffset, int ppsLen) {
        return new RtmpPacket(native_create_for_sps_pps(sps, spsOffset, spsLen, pps, ppsOffset, ppsLen), PacketType.SPS_PPS);
    }

    public static RtmpPacket createForVideo(ByteBuffer data, int offset, int dataLen, boolean keyFrame) {
        return new RtmpPacket(native_create_for_video(data, offset, dataLen, keyFrame), keyFrame ? PacketType.VIDEO_SYNC_FRAME : PacketType.VIDEO_P_FRAME);
    }

    public static RtmpPacket createForAudio(ByteBuffer data, int offset, int dataLen, int sampleRate, int channels, int bytesPerSample, boolean isConfigData) {
        if (sampleRate != 5500 && sampleRate != 11000 && sampleRate != 22000 && sampleRate != 44100) {
            throw new IllegalArgumentException("invalid sampleRate");
        }
        if (channels != 1 && channels != 2) {
            throw new IllegalArgumentException("invalid channels");
        }
        if (bytesPerSample != 1 && bytesPerSample != 2) {
            throw new IllegalArgumentException("invalid bytesPerSample");
        }
        return new RtmpPacket(native_create_for_audio(data, offset, dataLen, sampleRate, channels, bytesPerSample, isConfigData), isConfigData ? PacketType.AUDIO_SPECIFIC_CONFIG : PacketType.AUDIO);
    }

    public enum PacketType {
        AUDIO_SPECIFIC_CONFIG, SPS_PPS, VIDEO_SYNC_FRAME, VIDEO_P_FRAME, AUDIO
    }

    private static class RtmpPacketDeleter implements LinkedQueue.Deleter<RtmpPacket> {
        private RtmpPacketDeleter() {
        }

        @Override
        public void delete(RtmpPacket rtmpPacket) {
            if (rtmpPacket != null)
                rtmpPacket.release();
        }
    }

}
