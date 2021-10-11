package com.zmy.rtmp_pusher.lib.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.zmy.rtmp_pusher.lib.log.RtmpLogManager;

import java.nio.ByteBuffer;

public class AVCEncoder extends IEncoder implements EOFHandle {
    private final int width;
    private final int height;
    private final int fps;
    private final int keyFrameInternal;
    private static final String MIME = "video/avc";
    private ByteBuffer sps;
    private ByteBuffer pps;

    public AVCEncoder(int bitrate, EncoderCallback callback, int width, int height, int fps, int keyFrameInternal) {
        super(bitrate, callback);
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.keyFrameInternal = keyFrameInternal;
    }

    @Override
    protected MediaFormat getFormat() {
        MediaFormat baseFormat = MediaFormat.createVideoFormat(MIME, width, height);
        baseFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        baseFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        baseFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, keyFrameInternal);
        baseFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        return baseFormat;
    }

    @Override
    protected boolean createSurface() {
        return true;
    }

    @Override
    protected void onOutputFormatChanged() {
        ByteBuffer sps = mediaCodec.getOutputFormat().getByteBuffer("csd-0");
        ByteBuffer pps = mediaCodec.getOutputFormat().getByteBuffer("csd-1");
        int spsLength = sps.remaining();
        int ppsLength = pps.remaining();
        this.sps = ByteBuffer.allocateDirect(spsLength);
        this.pps = ByteBuffer.allocateDirect(ppsLength);
        this.sps.put(sps);
        this.pps.put(pps);
        outputQueue.enqueue(RtmpPacket.createForSpsPps(getSPS(), 0, getSPS().capacity(), getPPS(), 0, getPPS().capacity()));
    }

    public ByteBuffer getSPS() {
        return sps;
    }

    public ByteBuffer getPPS() {
        return pps;
    }

    @Override
    protected void onEncode(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            return;
        }
        if (info.size > 0 && outputQueue.getSize() < outputQueue.getMaxCapacity()) {
            outputQueue.enqueue(RtmpPacket.createForVideo(buffer, info.offset, info.size, (info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0));
        }
    }

    @Override
    public void release() {
        super.release();
        if (getSurface() != null) {
            getSurface().release();
        }
    }

    @Override
    public void signalEndOfInputStream() {
        mediaCodec.signalEndOfInputStream();
    }
}
