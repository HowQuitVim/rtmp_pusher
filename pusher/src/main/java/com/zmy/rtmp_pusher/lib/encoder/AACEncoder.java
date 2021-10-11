package com.zmy.rtmp_pusher.lib.encoder;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;


import com.zmy.rtmp_pusher.lib.queue.ByteQueue;
import com.zmy.rtmp_pusher.lib.util.WorkerThread;

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
        if (buffer == null || buffer.capacity() < 2) {
            throw new IllegalStateException("invalid AudioSpecificConfig,buffer=" + buffer);
        }
        audioSpecificConfig = ByteBuffer.allocateDirect(buffer.capacity());
        buffer.limit(buffer.capacity());
        buffer.position(0);
        audioSpecificConfig.put(buffer);
        audioSpecificConfig.limit(audioSpecificConfig.capacity());
        audioSpecificConfig.position(0);
        outputQueue.enqueue(RtmpPacket.createForAudio(getAudioSpecificConfig(), 0, getAudioSpecificConfig().capacity(), sampleRate, channels, getBytesPerSample(), true));
    }

    private int getBytesPerSample() {
        switch (sampleFormat) {
            case AudioFormat.ENCODING_PCM_8BIT:
                return 1;
            case AudioFormat.ENCODING_PCM_16BIT:
                return 2;
            case AudioFormat.ENCODING_PCM_FLOAT:
                return 4;
            default:
                return 0;
        }
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
        if (info.size > 0) {
            outputQueue.enqueue(RtmpPacket.createForAudio(buffer, info.offset, info.size, sampleRate, channels, getBytesPerSample(), false));
        }
    }

    @Override
    protected void waitForCodecDone() {
        if (encodeWriteThread != null) {
            mediaCodec.stop();
            try {
                encodeWriteThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        super.waitForCodecDone();
    }

    class EncodeWriteThread extends WorkerThread {

        public EncodeWriteThread() {
            super("EncodeWriteThread");
        }

        @Override
        protected boolean doMain() {
            try {
                int index = mediaCodec.dequeueInputBuffer(-1);
                int flag = 0;
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
                    if (inputQueue.isClosed()) {
                        flag = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                    }
                    mediaCodec.queueInputBuffer(index, 0, bytes, 0, flag);
                }
                return (flag & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
            } catch (Exception e) {
                e.printStackTrace();
                if (isReady() && callback != null) {
                    callback.onEncodeError(AACEncoder.this, e);
                }
                return true;
            }
        }
    }
}
