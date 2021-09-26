package com.zmy.rtmp_pusher.lib.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;


import com.zmy.rtmp_pusher.lib.queue.ByteQueue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AACEncoder extends IEncoder {

    private final int channels;
    private final int sampleFormat;
    private final int sampleRate;
    private final ByteQueue inputQueue;
    private EncodeWriteThread encodeWriteThread;
    private static final String MIME = "audio/mp4a-latm";
    private ByteBuffer audioSpecificConfig;

    public AACEncoder(int bitrate, EncoderCallback callback, int channels, int sampleFormat, int sampleRate, ByteQueue inputQueue) {
        super(bitrate, callback);
        this.channels = channels;
        this.sampleFormat = sampleFormat;
        this.sampleRate = sampleRate;
        this.inputQueue = inputQueue;
    }

    @Override
    protected MediaFormat getFormat() {
        MediaFormat format = MediaFormat.createAudioFormat(MIME, sampleRate, channels);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        return format;
    }

    @Override
    public void start() throws EncoderException {
        super.start();
        if (encodeWriteThread != null) {
            encodeWriteThread.exit();
        }
        encodeWriteThread = new EncodeWriteThread();
        encodeWriteThread.start();
    }

    @Override
    protected void onOutputFormatChanged() {
        ByteBuffer buffer = mediaCodec.getOutputFormat().getByteBuffer("csd-0");
        audioSpecificConfig = ByteBuffer.allocateDirect(buffer.capacity());
        buffer.limit(buffer.capacity());
        buffer.position(0);
        audioSpecificConfig.put(buffer);
        audioSpecificConfig.limit(audioSpecificConfig.capacity());
        audioSpecificConfig.position(0);
        outputQueue.enqueue(EncodeFrame.createForAudio(getAudioSpecificConfig(), getAudioSpecificConfig().capacity(), true));
    }

    public ByteBuffer getAudioSpecificConfig() {
        return audioSpecificConfig;
    }

    @Override
    protected boolean createSurface() {
        return false;
    }

    @Override
    protected void onEncode(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            return;
        }
        outputQueue.enqueue(EncodeFrame.createForAudio(buffer, info.size, false));
    }

    class EncodeWriteThread extends EncodeThread {

        public EncodeWriteThread() {
            super("EncodeWriteThread");
        }

        @Override
        protected boolean doMain() {
            try {
                int index = mediaCodec.dequeueInputBuffer(-1);
                if (index >= 0) {
                    ByteBuffer codecBuffer;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        codecBuffer = mediaCodec.getInputBuffer(index);
                    } else {
                        codecBuffer = inputBuffers[index];
                    }
                    codecBuffer.limit(codecBuffer.capacity());
                    codecBuffer.position(0);
                    int bytes = inputQueue.dequeue(codecBuffer);
                    mediaCodec.queueInputBuffer(index, 0, bytes, 0, 0);
                }
                return false;
            } catch (Exception e) {
                callback.onEncodeError(AACEncoder.this,e);
                return true;
            }
        }
    }
}
