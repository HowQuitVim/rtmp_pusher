package com.zmy.rtmp_pusher.lib.audio_collector;

import com.zmy.rtmp_pusher.lib.queue.ByteQueue;
import com.zmy.rtmp_pusher.lib.queue.Queue;

import java.nio.ByteBuffer;

public abstract class AudioCollector {
    protected CollectorCallback callback;
    protected ByteQueue queue;

    public AudioCollector(CollectorCallback callback) {
        this.callback = callback;
    }

    abstract public void initialize() throws AudioCollectorException;

    abstract public void start() throws AudioCollectorException;

    abstract public void stop() throws AudioCollectorException;

    abstract public void release() throws AudioCollectorException;

    public void setQueue(ByteQueue queue) {
        this.queue = queue;
    }
}
