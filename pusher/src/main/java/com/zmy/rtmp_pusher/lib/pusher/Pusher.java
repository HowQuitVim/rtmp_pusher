package com.zmy.rtmp_pusher.lib.pusher;

import android.util.Log;

import androidx.annotation.NonNull;

import com.zmy.rtmp_pusher.lib.RtmpPusher;
import com.zmy.rtmp_pusher.lib.encoder.RtmpPacket;
import com.zmy.rtmp_pusher.lib.exception.Err;
import com.zmy.rtmp_pusher.lib.log.RtmpLogManager;
import com.zmy.rtmp_pusher.lib.queue.LinkedQueue;
import com.zmy.rtmp_pusher.lib.util.WorkerThread;

import java.util.Locale;

public class Pusher {

    private final PusherCallback callback;
    private long handle = 0;
    private final String url;
    private final LinkedQueue<RtmpPacket> inputQueue;
    private PushThread pushThread;


    public Pusher(String url, LinkedQueue<RtmpPacket> inputQueue, @NonNull PusherCallback callback) throws PusherException {
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
        init();
        connect();
    }

    public void start() {
        if (pushThread != null) {
            pushThread.exit();
        }
        pushThread = new PushThread();
        pushThread.start();
    }

    private synchronized void releaseInternal() {
        Log.d("rtmp", "releaseInternal");
        if (handle != 0) {
            native_release(handle);
            handle = 0;
        }
    }

    public void release() {
        releaseInternal();
        getQueue().close();
        if (pushThread != null) {
            pushThread.exit();
            pushThread = null;
        }
        if (audioSpecificConfig != null) audioSpecificConfig.release();
        if (videoSpsPps != null) videoSpsPps.release();
    }

    private void assertUrl(String url) throws PusherException {
        if (url == null || !url.toLowerCase(Locale.getDefault()).startsWith("rtmp://")) {
            throw new PusherException(new IllegalStateException("invalid url"));
        }
    }

    private synchronized void init() throws PusherException {
        if (handle == 0) throw new PusherException(new IllegalStateException("handle is 0"));
        if (!native_init(handle)) {
            throw new PusherException(new RuntimeException("fail to init pusher"));
        }
    }

    private synchronized void connect() throws PusherException {
        if (handle == 0) throw new PusherException(new IllegalStateException("handle is 0"));
        if (!native_connect(handle)) {
            throw new PusherException(new RuntimeException("fail to connect to server,errno=" + Err.errno() + ",desc=" + Err.errDescribe(Err.errno())));
        }
    }

    private synchronized void push(long packet) throws PusherException {
        if (handle == 0) throw new PusherException(new IllegalStateException("handle is 0"));
        if (packet == 0) throw new PusherException(new IllegalArgumentException("packet can not be 0"));
        if (!native_push(handle, packet)) {
            throw new PusherException(new RuntimeException("fail to push,errno=" + Err.errno() + ",desc=" + Err.errDescribe(Err.errno())));
        }
    }


    private synchronized boolean isConnected() {
        return handle != 0 && native_is_connected(handle);
    }

    private static synchronized native long native_new_instance(String url);

    private static synchronized native boolean native_init(long handle);

    private static synchronized native boolean native_connect(long handle);

    private static synchronized native boolean native_push(long handle, long packetHandle);

    private static synchronized native void native_release(long handle);

    private static synchronized native boolean native_is_connected(long handle);

    public String getUrl() {
        return url;
    }

    private RtmpPacket audioSpecificConfig;
    private RtmpPacket videoSpsPps;
    private boolean needPushASC = false;
    private boolean needPushSpsPps = false;
    private boolean needPushSyncFrame = false;

    class PushThread extends WorkerThread {
        RtmpPacket[] frame = new RtmpPacket[1];

        public PushThread() {
            super("PusherThread");
        }

        @Override
        protected boolean doMain() {
            int count = inputQueue.dequeue(frame);
            if (count != 1) return true;

            RtmpPacket target = frame[0];
            if (target.getType() == RtmpPacket.PacketType.AUDIO_SPECIFIC_CONFIG) {
                audioSpecificConfig = target.copy();
            }
            if (target.getType() == RtmpPacket.PacketType.SPS_PPS) {
                videoSpsPps = target.copy();
            }
            synchronized (Pusher.this) {
                if (!isConnected()) {
                    waitUntilConnected();
                    RtmpLogManager.d("rtmp", "connect success");
                }
                if (handle == 0) return true;

                if (needPushASC && audioSpecificConfig != null) {
                    try {
                        push(audioSpecificConfig.copy().getHandle());
                        needPushASC = false;
                    } catch (PusherException e) {
                        e.printStackTrace();
                        callback.onPushError(e);
                    }
                }
                if (needPushSpsPps && videoSpsPps != null) {
                    try {
                        push(videoSpsPps.copy().getHandle());
                        needPushSpsPps = false;
                    } catch (PusherException e) {
                        e.printStackTrace();
                        callback.onPushError(e);
                    }
                }
                if (needPushASC && target.getType() == RtmpPacket.PacketType.AUDIO) {
                    target.release();//drop it ,must push asc first;
                    return false;
                }
                if (needPushSpsPps && (target.getType() == RtmpPacket.PacketType.VIDEO_P_FRAME || target.getType() == RtmpPacket.PacketType.VIDEO_SYNC_FRAME)) {
                    target.release();//drop it ,must push asc SPS/PPS
                    return false;
                }
                if (needPushSyncFrame && target.getType() == RtmpPacket.PacketType.VIDEO_P_FRAME) {
                    RtmpLogManager.d("rtmp", "drop p frame");
                    target.release();//drop it ,must push video sync frame
                    return false;
                }
                if (target.getType() != RtmpPacket.PacketType.SPS_PPS && target.getType() != RtmpPacket.PacketType.AUDIO_SPECIFIC_CONFIG) {
                    try {
                        push(target.getHandle());
                        if (target.getType() == RtmpPacket.PacketType.VIDEO_SYNC_FRAME) {
                            RtmpLogManager.d("rtmp", "push sync frame");
                            needPushSyncFrame = false;
                        }
                        if (target.getType() == RtmpPacket.PacketType.VIDEO_P_FRAME) {
                            RtmpLogManager.d("rtmp", "push P frame");
                        }
                    } catch (PusherException e) {
                        e.printStackTrace();
                        callback.onPushError(e);
                    }
                }

            }
            return false;
        }
    }

    private void waitUntilConnected() {
        while (handle != 0) {
            try {
                initialize();
                needPushASC = true;
                needPushSpsPps = true;
                needPushSyncFrame = true;
                break;
            } catch (PusherException e) {
                callback.onPushError(e);
            }
            try {
                Pusher.this.wait(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
