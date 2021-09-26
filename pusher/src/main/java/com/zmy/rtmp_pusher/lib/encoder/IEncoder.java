package com.zmy.rtmp_pusher.lib.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.zmy.rtmp_pusher.lib.queue.LinkedQueue;

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
    protected LinkedQueue<EncodeFrame> outputQueue;

    public IEncoder(int bitrate, EncoderCallback callback) {
        this.bitrate = bitrate;
        this.callback = callback;
    }

    protected abstract MediaFormat getFormat();

    public void init() throws EncoderException {
        try {
            MediaFormat format = getFormat();
            mediaCodec = MediaCodec.createEncoderByType(format.getString(MediaFormat.KEY_MIME));
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            if (createSurface()) surface = mediaCodec.createInputSurface();
        } catch (Exception e) {
            throw new EncoderException(e);
        }
    }

    public Surface getSurface() {
        return surface;
    }

    protected abstract boolean createSurface();

    public void setOutputQueue(LinkedQueue<EncodeFrame> outputQueue) {
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

    public void stop() {

    }

    abstract class EncodeThread extends Thread {
        private boolean exitFlag = false;

        public EncodeThread(@NonNull String name) {
            super(name);
        }

        @Override
        public void run() {
            super.run();
            while (true) {
                synchronized (EncodeThread.this) {
                    if (exitFlag) {
                        break;
                    }
                }
                if (doMain()) {
                    break;
                }
            }
            synchronized (EncodeThread.this) {
                exitFlag = true;
            }
            Log.d(getName(), getName() + " exit");
        }

        protected abstract boolean doMain();

        public synchronized void exit() {
            exitFlag = true;
            try {
                this.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class EncodeReadThread extends EncodeThread {

        public EncodeReadThread() {
            super("EncodeReadThread");
        }

        @Override
        protected boolean doMain() {
            try {
                int index = mediaCodec.dequeueOutputBuffer(info, -1);
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.d("zmy", "onOutputFormatChanged-------" + IEncoder.this.getClass().getSimpleName());
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
                    mediaCodec.releaseOutputBuffer(index, false);
                }
                return false;
            } catch (Exception e) {
                callback.onEncodeError( IEncoder.this,e);
                return true;
            }
        }
    }
}
