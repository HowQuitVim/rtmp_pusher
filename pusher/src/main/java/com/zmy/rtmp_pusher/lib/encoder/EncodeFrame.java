package com.zmy.rtmp_pusher.lib.encoder;


import java.nio.ByteBuffer;

public class EncodeFrame {
    private long handle = 0;

    private EncodeFrame(long handle) {
        this.handle = handle;
    }

    public long getHandle() {
        return handle;
    }

    private native static long nativeCreateForSPSPPS(ByteBuffer sps, int spsLen, ByteBuffer pps, int ppsLen);

    private native static long nativeCreateForVideo(ByteBuffer data, int dataLen, boolean keyFrame);

    private native static long nativeCreateForAudio(ByteBuffer data, int dataLen, boolean isConfigData);


    public static EncodeFrame createForSPSPPS(ByteBuffer sps, int spsLen, ByteBuffer pps, int ppsLen) {
        return new EncodeFrame(nativeCreateForSPSPPS(sps, spsLen, pps, ppsLen));
    }

    public static EncodeFrame createForVideo(ByteBuffer data, int dataLen, boolean keyFrame) {
        return new EncodeFrame(nativeCreateForVideo(data, dataLen, keyFrame));
    }

    public static EncodeFrame createForAudio(ByteBuffer data, int dataLen, boolean isConfigData) {
        return new EncodeFrame(nativeCreateForAudio(data, dataLen, isConfigData));
    }
}
