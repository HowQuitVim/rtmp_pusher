package com.zmy.rtmp_pusher.lib.pusher;

import android.util.Log;

import com.zmy.rtmp_pusher.lib.encoder.EncodeFrame;
import com.zmy.rtmp_pusher.lib.exception.Err;
import com.zmy.rtmp_pusher.lib.queue.LinkedQueue;

import java.util.Locale;

public class Pusher {
    static {
        System.loadLibrary("pusher");
    }

    private final PusherCallback callback;
    private long handle = 0;
    private final String url;
    private LinkedQueue<EncodeFrame> inputQueue;
    private boolean runFlag = false;
    private boolean isInit = false;
    private PushThread pushThread;


    public Pusher(String url, LinkedQueue<EncodeFrame> inputQueue, PusherCallback callback) throws PusherException {
        assertUrl(url);
        if (inputQueue == null) throw new PusherException(new IllegalStateException("input queue must be non null"));
        this.url = url;
        this.inputQueue = inputQueue;
        this.callback = callback;
        handle = doNewInstance();
        if (handle == 0) {
            throw new PusherException(new RuntimeException("fail to alloc native pusher"));
        }
    }

    private void assertUrl(String url) throws PusherException {
        if (url == null || !url.toLowerCase(Locale.getDefault()).startsWith("rtmp://")) {
            throw new PusherException(new IllegalStateException("invalid url"));
        }
    }

    private long doNewInstance() {
        return newInstance(url);
    }

    public void start() throws PusherException {
        if (!isInit) throw new PusherException(new IllegalStateException("please init pusher first"));
        if (pushThread != null) {
            pushThread.exit();
        }
        pushThread = new PushThread();
        pushThread.start();
    }

    public void release(boolean stopPushThread) {
        if (stopPushThread && pushThread != null) {
            pushThread.exit();
            pushThread = null;
        }
        release(handle);
    }

    public synchronized void initize() throws PusherException {
        doInit();
        doConnect();
        isInit = true;
    }

    private void doInit() throws PusherException {
        if (!init(handle)) {
            throw new PusherException(new RuntimeException("fail to init pusher"));
        }
    }

    private void doConnect() throws PusherException {
        if (!connect(handle)) {
            throw new PusherException(new RuntimeException("fail to connect to server"));
        }
    }

    private synchronized native long newInstance(String url);

    private synchronized native boolean init(long handle);

    private synchronized native boolean connect(long handle);

    private synchronized native boolean push(long handle, long packetHandle);

    private synchronized native void release(long handle);


    class PushThread extends Thread {
        @Override
        public void run() {
            super.run();
            runFlag = true;
            EncodeFrame[] frame = new EncodeFrame[1];
            while (true) {
                synchronized (this) {
                    if (!runFlag) break;
                }
                int count = inputQueue.dequeue(frame);
                synchronized (this) {
                    if (!runFlag) break;
                }
                if (count != 1) {
                    break;
                }
                synchronized (Pusher.this) {
                    boolean ret = push(handle, frame[0].getHandle());
                    if (!ret) {
                        callback.onPushError(Err.errno());
                    }
                }
            }
        }

        public synchronized void exit() {
            runFlag = false;
            if (Thread.currentThread().getId() == this.getId()) {
                return;
            }
            try {
                this.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
