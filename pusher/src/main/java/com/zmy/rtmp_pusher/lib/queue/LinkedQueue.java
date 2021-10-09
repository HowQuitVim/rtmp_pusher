package com.zmy.rtmp_pusher.lib.queue;

import androidx.annotation.Nullable;

import java.util.LinkedList;

public class LinkedQueue<T> extends Queue<T, T[]> {
    private final LinkedList<T> list = new LinkedList<>();
    private final Deleter<T> deleter;

    public LinkedQueue(int maxCapacity, @Nullable Deleter<T> deleter) {
        super(maxCapacity);
        this.deleter = deleter;
    }

    @Override
    protected int doDequeue(T[] output) {
        if (!canRead) {
            return 0;
        }
        while (list.size() == 0) {
            block();
            if (!canRead) return 0;
        }
        int dequeueCount = Math.min(list.size(), output.length);

        for (int i = 0; i < dequeueCount; i++) {
            output[i] = list.removeFirst();
            wakeUp();
        }
        return dequeueCount;
    }

    @Override
    protected void doEnqueue(T data) {
        if (!canWrite) return;
        while (list.size() >= maxCapacity) {
            block();
            if (!canWrite) return;
        }
        list.add(data);
        wakeUp();
    }

    @Override
    protected int resize(int newSize) {
        return 0;
    }

    @Override
    public int getCapacity() {
        return maxCapacity;
    }

    @Override
    public int getSize() {
        return list.size();
    }

    @Override
    public synchronized void close() {
        super.close();
        clear();
        wakeUp();
    }

    public Deleter<T> getDeleter() {
        return deleter;
    }

    @Override
    public synchronized void clear() {
        if (deleter != null) {
            clearWithDeleter();
        } else {
            list.clear();
        }
    }

    private void clearWithDeleter() {
        while (!list.isEmpty()) {
            deleter.delete(list.removeFirst());
        }
    }

    public interface Deleter<T> {
        void delete(T t);
    }

}
