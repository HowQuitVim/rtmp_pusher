package com.zmy.rtmp_pusher.lib.pusher;

import com.zmy.rtmp_pusher.lib.encoder.RtmpPacket;
import com.zmy.rtmp_pusher.lib.exception.Err;
import com.zmy.rtmp_pusher.lib.queue.LinkedQueue;
import com.zmy.rtmp_pusher.lib.util.WorkerThread;

import java.util.Locale;

public class Pusher {
    static {
        System.loadLibrary("pusher");
    }

    private final PusherCallback callback;
    private long handle = 0;
    private final String url;
    private LinkedQueue<RtmpPacket> inputQueue;
    private boolean runFlag = false;
    private boolean ready = false;
    private PushThread pushThread;


    public Pusher(String url, LinkedQueue<RtmpPacket> inputQueue, PusherCallback callback) throws PusherException {
        assertUrl(url);
        if (inputQueue == null) throw new PusherException(new IllegalStateException("input queue must be non null"));
        this.url = url;
        this.inputQueue = inputQueue;
        this.callback = callback;
        handle = native_new_instance(this.url);
        if (handle == 0) {
            throw new PusherException(new RuntimeException("fail to alloc native pusher"));
        }
    }

    public LinkedQueue<RtmpPacket> getQueue() {
        return inputQueue;
    }

    public synchronized void initialize() throws PusherException {
        doInit();
        doConnect();
        setReady(true);
    }

    public synchronized boolean isReady() {
        return ready;
    }

    private synchronized void setReady(boolean ready) {
        this.ready = ready;
    }

    public void start() throws PusherException {
        if (!isReady()) throw new PusherException(new IllegalStateException("please init pusher first"));
        if (pushThread != null) {
            pushThread.exit();
        }
        pushThread = new PushThread();
        pushThread.start();
    }

    public void release() {
        setReady(false);
        getQueue().close();
        if (pushThread != null) {
            pushThread.exit();
            pushThread = null;
        }
        if (handle != 0) {
            native_release(handle);
            handle = 0;
        }
    }

    private void assertUrl(String url) throws PusherException {
        if (url == null || !url.toLowerCase(Locale.getDefault()).startsWith("rtmp://")) {
            throw new PusherException(new IllegalStateException("invalid url"));
        }
    }


    private void doInit() throws PusherException {
        if (!native_init(handle)) {
            throw new PusherException(new RuntimeException("fail to init pusher"));
        }
    }

    private void doConnect() throws PusherException {
        if (!native_connect(handle)) {
            throw new PusherException(new RuntimeException("fail to connect to server"));
        }
    }

    private synchronized native long native_new_instance(String url);

    private synchronized native boolean native_init(long handle);

    private synchronized native boolean native_connect(long handle);

    private synchronized native boolean native_push(long handle, long packetHandle);

    private synchronized native void native_release(long handle);


    class PushThread extends WorkerThread {
        RtmpPacket[] frame = new RtmpPacket[1];

        public PushThread() {
            super("PusherThread");
        }

        @Override
        protected boolean doMain() {
            int count = inputQueue.dequeue(frame);
            if (count != 1) {
                return true;
            }
            synchronized (Pusher.this) {
                boolean ret = native_push(handle, frame[0].getHandle());
                if (!ret) {
                    callback.onPushError(Err.errno());
                }
            }
            return false;
        }
    }


}
