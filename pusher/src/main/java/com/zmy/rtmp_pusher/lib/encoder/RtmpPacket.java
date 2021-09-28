package com.zmy.rtmp_pusher.lib.encoder;


import com.zmy.rtmp_pusher.lib.queue.LinkedQueue;

import java.nio.ByteBuffer;

public class RtmpPacket {
    public static final LinkedQueue.Deleter<RtmpPacket> DELETER = new RtmpPacketDeleter();

    private long handle = 0;

    private RtmpPacket(long handle) {
        this.handle = handle;
    }

    public long getHandle() {
        return handle;
    }

    public synchronized void release() {
        if (handle != 0) {
            native_release_frame(handle);
            handle = 0;
        }
    }

    private native static long native_create_for_sps_pps(ByteBuffer sps, int spsLen, ByteBuffer pps, int ppsLen);

    private native static long native_create_for_video(ByteBuffer data, int dataLen, boolean keyFrame);

    private native static long native_create_for_audio(ByteBuffer data, int dataLen, int sampleRate, int channels, int bytesPerSample, boolean isConfigData);

    private native void native_release_frame(long release);

    public static RtmpPacket createForSpsPps(ByteBuffer sps, int spsLen, ByteBuffer pps, int ppsLen) {
        return new RtmpPacket(native_create_for_sps_pps(sps, spsLen, pps, ppsLen));
    }

    public static RtmpPacket createForVideo(ByteBuffer data, int dataLen, boolean keyFrame) {
        return new RtmpPacket(native_create_for_video(data, dataLen, keyFrame));
    }

    public static RtmpPacket createForAudio(ByteBuffer data, int dataLen, int sampleRate, int channels, int bytesPerSample, boolean isConfigData) {
        return new RtmpPacket(native_create_for_audio(data, dataLen, sampleRate, channels, bytesPerSample, isConfigData));
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
