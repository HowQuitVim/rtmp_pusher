package com.zmy.rtmp_pusher.lib.queue;

public abstract class Queue<T, O> {
    protected int maxCapacity;
    protected boolean canWrite = true;
    protected boolean canRead = true;

    public Queue(int maxCapacity) {
        if (maxCapacity <= 0)
            throw new IllegalArgumentException("maxCapacity must be more than 0");
        this.maxCapacity = maxCapacity;
    }

    public final synchronized void enqueue(T data) {
        doEnqueue(data);
    }

    public final synchronized int dequeue(O o) {
        return doDequeue(o);
    }

    protected abstract void doEnqueue(T data);

    protected abstract int doDequeue(O o);

    protected abstract int resize(int newSize);

    public abstract int getCapacity();

    public abstract int getSize();
    public abstract void clear();
    public int getMaxCapacity() {
        return maxCapacity;
    }

    protected synchronized void block() {
        try {
            this.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected synchronized void wakeUp() {
        notifyAll();
    }

    public synchronized void close() {
        canRead = false;
        canWrite = false;
    }

    public synchronized boolean isClosed() {
        return !canRead && !canWrite;
    }

    public synchronized void flush() {
        canWrite = false;
    }
}
