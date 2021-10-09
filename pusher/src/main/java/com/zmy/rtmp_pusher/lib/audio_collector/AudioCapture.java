package com.zmy.rtmp_pusher.lib.audio_collector;

import androidx.annotation.NonNull;

import com.zmy.rtmp_pusher.lib.queue.ByteQueue;

public abstract class AudioCapture {
    protected AudioCaptureCallback callback;
    protected ByteQueue queue;
    private boolean ready = false;


    public abstract int getSampleRate();

    public abstract int getChannelCount();

    public abstract int getSampleFormat();

    public void initialize(AudioCaptureCallback callback) {
        this.callback = callback;
        doInitialize();
    }

    protected abstract void doInitialize();

    public void start(@NonNull ByteQueue queue) {
        this.queue = queue;
    }


    public abstract void release();

    public ByteQueue getQueue() {
        return queue;
    }

    public synchronized boolean isReady() {
        return ready;
    }

    protected synchronized void setReady(boolean ready) {
        this.ready = ready;
    }
}
