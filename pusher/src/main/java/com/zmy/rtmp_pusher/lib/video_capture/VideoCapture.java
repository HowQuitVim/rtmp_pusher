package com.zmy.rtmp_pusher.lib.video_capture;

import android.view.Surface;

import com.zmy.rtmp_pusher.lib.encoder.EOFHandle;

public abstract class VideoCapture {
    protected VideoCaptureCallback callback;
    private boolean ready = false;


    public  void initialize(VideoCaptureCallback callback){
        this.callback=callback;
        doInitialize();
    }

    protected abstract void doInitialize();

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract void start(Surface surface, EOFHandle handle);

    public abstract void release();

    public synchronized boolean isReady() {
        return ready;
    }

    protected synchronized void setReady(boolean ready) {
        this.ready = ready;
    }
}
