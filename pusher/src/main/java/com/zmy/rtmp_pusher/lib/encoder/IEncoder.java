package com.zmy.rtmp_pusher.lib.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.zmy.rtmp_pusher.lib.queue.LinkedQueue;
import com.zmy.rtmp_pusher.lib.util.WorkerThread;

import java.nio.ByteBuffer;

public abstract class IEncoder {

    protected int bitrate;

    protected EncoderCallback callback;
    protected MediaCodec mediaCodec;
    private Surface surface;
    protected ByteBuffer[] outputBuffers;
    protected ByteBuffer[] inputBuffers;
    private EncodeReadThread encodeReadThread;
    private final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    protected LinkedQueue<RtmpPacket> outputQueue;
    private boolean ready = false;

    public IEncoder(int bitrate, EncoderCallback callback) {
        this.bitrate = bitrate;
        this.callback = callback;
    }

    protected abstract MediaFormat getFormat();

    public synchronized boolean isReady() {
        return ready;
    }

    public synchronized void setReady(boolean ready) {
        this.ready = ready;
    }

    public void init() throws EncoderException {
        try {
            MediaFormat format = getFormat();
            mediaCodec = MediaCodec.createEncoderByType(format.getString(MediaFormat.KEY_MIME));
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            if (createSurface()) surface = mediaCodec.createInputSurface();
            setReady(true);
        } catch (Exception e) {
            throw new EncoderException(e);
        }
    }

    public Surface getSurface() {
        return surface;
    }

    protected abstract boolean createSurface();

    public void setOutputQueue(LinkedQueue<RtmpPacket> outputQueue) {
        this.outputQueue = outputQueue;
    }

    public void start() throws EncoderException {
        try {
            mediaCodec.start();
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                outputBuffers = mediaCodec.getOutputBuffers();
                if (!createSurface()) {
                    inputBuffers = mediaCodec.getInputBuffers();
                }
            }
            if (encodeReadThread != null) {
                encodeReadThread.exit();
            }
            encodeReadThread = new EncodeReadThread();
            encodeReadThread.start();
        } catch (Exception e) {
            throw new EncoderException(e);
        }
    }

    protected abstract void onOutputFormatChanged();

    protected abstract void onEncode(ByteBuffer buffer, MediaCodec.BufferInfo info);

    public void release() {
        setReady(false);
        waitForCodecDone();
        mediaCodec.release();
        mediaCodec=null;
    }

    protected void waitForCodecDone() {
        if (encodeReadThread != null) {
            try {
                encodeReadThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    class EncodeReadThread extends WorkerThread {

        public EncodeReadThread() {
            super(IEncoder.this.getClass().getSimpleName() + "_EncodeReadThread");
        }

        @Override
        protected boolean doMain() {
            try {
                int index = mediaCodec.dequeueOutputBuffer(info, -1);
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.d("rtmp", "onOutputFormatChanged-------" + IEncoder.this.getClass().getSimpleName());
                    onOutputFormatChanged();
                }
                if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                    outputBuffers = mediaCodec.getOutputBuffers();
                }
                if (index >= 0) {
                    ByteBuffer buffer;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        buffer = mediaCodec.getOutputBuffer(index);
                    } else {
                        buffer = outputBuffers[index];
                    }
                    onEncode(buffer, info);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputQueue.close();
                    }
                    mediaCodec.releaseOutputBuffer(index, false);
                }
                return (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
            } catch (Exception e) {
                e.printStackTrace();
                if (!isReady()) {
                    callback.onEncodeError(IEncoder.this, e);
                }
                return true;
            }
        }
    }
}
